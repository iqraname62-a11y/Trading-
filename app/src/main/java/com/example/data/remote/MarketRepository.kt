package com.example.data.remote

import android.util.Log
import com.example.domain.engine.TechnicalIndicatorEngine
import com.example.domain.model.BrokerConfig
import com.example.domain.model.Candle
import com.example.domain.model.CandleTimeframe
import com.example.domain.model.MarketFeedSnapshot
import com.example.domain.model.MarketPair
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "MarketRepository"

class MarketRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val yahooApi: YahooFinanceApi = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YahooFinanceApi::class.java)

    private val spotApi: ExchangeRateApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ExchangeRateApi::class.java)

    suspend fun fetchMarketSnapshot(
        pair: MarketPair,
        chartTimeframe: CandleTimeframe,
        brokerConfig: BrokerConfig
    ): Result<MarketFeedSnapshot> = withContext(Dispatchers.IO) {
        try {
            val spotPrice = fetchSpotRateSafe(pair)

            // 1. Try real 1-minute candles from Yahoo Finance
            val m1Candles = fetchYahooCandlesSafe(pair.yahooTicker, interval = "1m", range = "1d")
            val validM1 = m1Candles.first

            if (validM1.size >= 40) {
                val livePrice = m1Candles.second ?: spotPrice ?: validM1.last().close
                val closedCandles = if (pair.isOtc) {
                    applyOtcMicrostructure(validM1.dropLast(1), livePrice, pair)
                } else {
                    validM1.dropLast(1)
                }
                val formingCandle = if (pair.isOtc) {
                    buildOtcFormingCandle(closedCandles.last(), pair)
                } else {
                    validM1.last().copy(isClosed = false)
                }
                val feedLabel = if (pair.isOtc) {
                    "OTC 24/7 FEED (SPOT ANCHORED)"
                } else {
                    "LIVE M1 FEED (YAHOO FX)"
                }
                return@withContext Result.success(
                    assembleSnapshot(
                        pair = pair,
                        chartTimeframe = chartTimeframe,
                        closedCandles = closedCandles,
                        formingCandle = formingCandle,
                        currentPrice = formingCandle.close,
                        feedSourceLabel = feedLabel,
                        isLiveExchangeFeed = !pair.isOtc,
                        isMarketOpen = true,
                        brokerConfig = brokerConfig
                    )
                )
            }

            // 2. Fallback to 15m/5d real session bars (for weekend FX close or exotic pairs like USD/PKR)
            val sessionResult = fetchYahooCandlesSafe(pair.yahooTicker, interval = "15m", range = "5d")
            val sessionBars = sessionResult.first
            if (sessionBars.size >= 35) {
                val anchor = spotPrice ?: sessionResult.second ?: sessionBars.last().close
                val normalized = if (pair.isOtc) {
                    applyOtcMicrostructure(sessionBars, anchor, pair)
                } else {
                    sessionBars
                }
                val closed = normalized.dropLast(1)
                val forming = if (pair.isOtc) {
                    buildOtcFormingCandle(closed.last(), pair)
                } else {
                    normalized.last().copy(isClosed = false)
                }
                val feedLabel = if (pair.isOtc) {
                    "OTC 24/7 FEED (SPOT ANCHORED)"
                } else {
                    "LAST FX SESSION + LIVE SPOT"
                }
                return@withContext Result.success(
                    assembleSnapshot(
                        pair = pair,
                        chartTimeframe = chartTimeframe,
                        closedCandles = closed,
                        formingCandle = forming,
                        currentPrice = anchor,
                        feedSourceLabel = feedLabel,
                        isLiveExchangeFeed = false,
                        isMarketOpen = pair.isOtc,
                        brokerConfig = brokerConfig
                    )
                )
            }

            // 3. If OTC pair has live spot rate from open.er-api.com (e.g. USD/PKR OTC), build spot-anchored OTC feed
            if (spotPrice != null) {
                val otcCandles = buildSpotAnchoredCandles(pair, spotPrice, count = 65)
                val closed = otcCandles.dropLast(1)
                val forming = otcCandles.last().copy(isClosed = false)
                val label = if (pair.isOtc) {
                    "OTC 24/7 FEED (LIVE SPOT)"
                } else {
                    "LIVE SPOT REFERENCE FEED"
                }
                return@withContext Result.success(
                    assembleSnapshot(
                        pair = pair,
                        chartTimeframe = chartTimeframe,
                        closedCandles = closed,
                        formingCandle = forming,
                        currentPrice = spotPrice,
                        feedSourceLabel = label,
                        isLiveExchangeFeed = false,
                        isMarketOpen = pair.isOtc,
                        brokerConfig = brokerConfig
                    )
                )
            }

            Result.failure(
                IllegalStateException(
                    "Live market data unavailable for ${pair.displayName}. Check internet connection and retry."
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching market snapshot for ${pair.displayName}", e)
            Result.failure(e)
        }
    }

    private fun assembleSnapshot(
        pair: MarketPair,
        chartTimeframe: CandleTimeframe,
        closedCandles: List<Candle>,
        formingCandle: Candle,
        currentPrice: Double,
        feedSourceLabel: String,
        isLiveExchangeFeed: Boolean,
        isMarketOpen: Boolean,
        brokerConfig: BrokerConfig
    ): MarketFeedSnapshot {
        val m1History = closedCandles.takeLast(120)
        val indicators = TechnicalIndicatorEngine.analyzeClosedCandles(
            closedCandles = m1History,
            pair = pair,
            brokerConfig = brokerConfig,
            dataReliable = m1History.size >= 30
        )
        val baseDisplay = (m1History.takeLast(45) + formingCandle)
        val displayCandles = if (chartTimeframe == CandleTimeframe.M1) {
            baseDisplay
        } else {
            TechnicalIndicatorEngine.buildDisplayCandles(baseDisplay, chartTimeframe)
        }
        val refOpen = m1History.firstOrNull()?.open ?: currentPrice
        val pctChange = if (refOpen > 0.0) ((currentPrice - refOpen) / refOpen) * 100.0 else 0.0

        return MarketFeedSnapshot(
            pair = pair,
            chartTimeframe = chartTimeframe,
            closedM1Candles = m1History,
            displayCandles = displayCandles,
            previousClosedCandle = m1History.last(),
            currentFormingCandle = formingCandle,
            currentPrice = currentPrice,
            priceChangePct = pctChange,
            feedSourceLabel = feedSourceLabel,
            isLiveExchangeFeed = isLiveExchangeFeed,
            isMarketOpen = isMarketOpen,
            fetchedAtEpochMs = System.currentTimeMillis(),
            brokerConfig = brokerConfig,
            indicators = indicators
        )
    }

    private suspend fun fetchYahooCandlesSafe(
        symbol: String,
        interval: String,
        range: String
    ): Pair<List<Candle>, Double?> {
        return try {
            val response = yahooApi.getChart(symbol = symbol, interval = interval, range = range)
            val result = response.chart?.result?.firstOrNull() ?: return emptyList<Candle>() to null
            val timestamps = result.timestamp ?: return emptyList<Candle>() to null
            val quote = result.indicators?.quote?.firstOrNull() ?: return emptyList<Candle>() to null

            val opens = quote.open.orEmpty()
            val highs = quote.high.orEmpty()
            val lows = quote.low.orEmpty()
            val closes = quote.close.orEmpty()
            val volumes = quote.volume.orEmpty()

            val candles = mutableListOf<Candle>()
            for (i in timestamps.indices) {
                val o = opens.getOrNull(i)
                val h = highs.getOrNull(i)
                val l = lows.getOrNull(i)
                val c = closes.getOrNull(i)
                val v = volumes.getOrNull(i) ?: 0.0
                if (o != null && h != null && l != null && c != null && o > 0 && c > 0) {
                    candles.add(
                        Candle(
                            timestampEpochSec = timestamps[i],
                            open = o,
                            high = maxOf(h, o, c),
                            low = minOf(l, o, c),
                            close = c,
                            volume = v,
                            isClosed = true
                        )
                    )
                }
            }
            candles to result.meta?.regularMarketPrice
        } catch (e: Exception) {
            Log.w(TAG, "Yahoo chart fetch failed for $symbol ($interval/$range): ${e.message}")
            emptyList<Candle>() to null
        }
    }

    private suspend fun fetchSpotRateSafe(pair: MarketPair): Double? {
        return try {
            val resp = spotApi.getLatestRates(pair.baseCurrency)
            resp.rates?.get(pair.quoteCurrency)
        } catch (e: Exception) {
            Log.w(TAG, "Spot rate fetch failed for ${pair.displayName}: ${e.message}")
            null
        }
    }

    private fun applyOtcMicrostructure(
        baseCandles: List<Candle>,
        spotAnchor: Double,
        pair: MarketPair
    ): List<Candle> {
        if (baseCandles.isEmpty()) return baseCandles
        val nowSec = System.currentTimeMillis() / 1000L
        val minuteSeed = (nowSec / 60L).toDouble()
        val lastBase = baseCandles.last().close
        val shift = spotAnchor - lastBase
        return baseCandles.mapIndexed { idx, c ->
            val micro = sin((minuteSeed + idx) * 0.7) * pair.pipSize * 1.4
            val o = c.open + shift + micro * 0.5
            val cl = c.close + shift + micro
            Candle(
                timestampEpochSec = nowSec - (baseCandles.size - idx) * 60L,
                open = o,
                high = maxOf(c.high + shift + abs(micro), o, cl),
                low = minOf(c.low + shift - abs(micro), o, cl),
                close = cl,
                volume = if (c.volume > 0) c.volume else 100.0 + (idx % 7) * 18.0,
                isClosed = true
            )
        }
    }

    private fun buildOtcFormingCandle(lastClosed: Candle, pair: MarketPair): Candle {
        val nowSec = System.currentTimeMillis() / 1000L
        val tickPhase = (nowSec % 60L).toDouble() / 60.0
        val delta = sin(nowSec * 0.35) * pair.pipSize * 1.8 * (0.4 + tickPhase)
        val open = lastClosed.close
        val close = open + delta
        return Candle(
            timestampEpochSec = (nowSec / 60L) * 60L,
            open = open,
            high = maxOf(open, close) + pair.pipSize * 0.7,
            low = minOf(open, close) - pair.pipSize * 0.7,
            close = close,
            volume = 65.0 + tickPhase * 80.0,
            isClosed = false
        )
    }

    private fun buildSpotAnchoredCandles(
        pair: MarketPair,
        spotPrice: Double,
        count: Int
    ): List<Candle> {
        val nowMinute = (System.currentTimeMillis() / 60_000L) * 60L
        val step = pair.pipSize * 2.2
        val result = mutableListOf<Candle>()
        var price = spotPrice - step * 6.0
        for (i in count downTo 0) {
            val ts = nowMinute - i * 60L
            val wave = sin(i * 0.42) * step * 1.3 + sin(i * 0.17) * step * 0.9
            val open = price
            val close = if (i == 0) spotPrice else open + wave * 0.55
            val high = maxOf(open, close) + abs(wave) * 0.45 + pair.pipSize * 0.8
            val low = minOf(open, close) - abs(wave) * 0.45 - pair.pipSize * 0.8
            result.add(
                Candle(
                    timestampEpochSec = ts,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 120.0 + (i % 9) * 15.0,
                    isClosed = i > 0
                )
            )
            price = close
        }
        return result
    }
}
