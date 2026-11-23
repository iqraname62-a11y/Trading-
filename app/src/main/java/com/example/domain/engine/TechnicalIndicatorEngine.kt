package com.example.domain.engine

import com.example.domain.model.BrokerConfig
import com.example.domain.model.Candle
import com.example.domain.model.CandleTimeframe
import com.example.domain.model.IndicatorSeries
import com.example.domain.model.IndicatorSnapshot
import com.example.domain.model.MarketPair
import com.example.domain.model.NewsImpact
import com.example.domain.model.StrategyCondition
import com.example.domain.model.TrendClassification
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalIndicatorEngine {

    fun analyzeClosedCandles(
        closedCandles: List<Candle>,
        pair: MarketPair,
        brokerConfig: BrokerConfig,
        dataReliable: Boolean
    ): IndicatorSnapshot {
        require(closedCandles.size >= 30) { "Need at least 30 closed candles for indicator computation" }

        val closes = closedCandles.map { it.close }
        val highs = closedCandles.map { it.high }
        val lows = closedCandles.map { it.low }

        val ema5Series = computeEmaSeries(closes, 5)
        val ema9Series = computeEmaSeries(closes, 9)
        val ema20Series = computeEmaSeries(closes, 20)
        val ema50Series = computeEmaSeries(closes, 50)

        val rsi14Series = computeRsiSeries(closes, 14)
        val (macdLineSeries, macdSignalSeries, macdHistSeries) = computeMacdSeries(closes, 12, 26, 9)
        val (bbUpperSeries, bbMiddleSeries, bbLowerSeries) = computeBollingerSeries(closes, 20, 2.0)
        val (adx14, plusDi14, minusDi14) = computeAdx(highs, lows, closes, 14)
        val atr14 = computeAtr(highs, lows, closes, 14)
        val (stochK, stochD) = computeStochastic(highs, lows, closes, 14, 3, 3)
        val vwap = computeVwap(closedCandles)

        val lastIdx = closedCandles.lastIndex
        val ema5 = ema5Series[lastIdx] ?: closes.last()
        val ema9 = ema9Series[lastIdx] ?: closes.last()
        val ema20 = ema20Series[lastIdx] ?: closes.last()
        val ema50 = ema50Series[lastIdx] ?: closes.last()
        val rsi14 = rsi14Series[lastIdx] ?: 50.0
        val macdLine = macdLineSeries[lastIdx] ?: 0.0
        val macdSignal = macdSignalSeries[lastIdx] ?: 0.0
        val macdHist = macdHistSeries[lastIdx] ?: 0.0
        val prevMacdHist = if (lastIdx > 0) macdHistSeries[lastIdx - 1] ?: 0.0 else 0.0
        val bbUpper = bbUpperSeries[lastIdx] ?: closes.last()
        val bbMiddle = bbMiddleSeries[lastIdx] ?: closes.last()
        val bbLower = bbLowerSeries[lastIdx] ?: closes.last()
        val bbBandwidthPct = if (bbMiddle > 0.0) ((bbUpper - bbLower) / bbMiddle) * 100.0 else 0.0
        val lastPrice = closes.last()
        val atrPct = if (lastPrice > 0.0) (atr14 / lastPrice) * 100.0 else 0.0

        val pattern = detectCandlestickPattern(closedCandles)
        val structure = detectMarketStructure(closedCandles)
        val volatilityRegime = classifyVolatility(atrPct, bbBandwidthPct)

        val trend = classifyTrend(
            price = lastPrice,
            ema5 = ema5,
            ema9 = ema9,
            ema20 = ema20,
            ema50 = ema50,
            rsi = rsi14,
            macdHist = macdHist,
            adx = adx14,
            structure = structure,
            volatilityRegime = volatilityRegime,
            ema5Series = ema5Series,
            ema9Series = ema9Series
        )

        val callChecklist = evaluateCallChecklist(
            candles = closedCandles,
            pair = pair,
            brokerConfig = brokerConfig,
            dataReliable = dataReliable,
            trend = trend,
            ema5 = ema5,
            ema9 = ema9,
            ema20 = ema20,
            rsi = rsi14,
            macdHist = macdHist,
            prevMacdHist = prevMacdHist,
            adx = adx14,
            volatilityRegime = volatilityRegime,
            pattern = pattern,
            structure = structure
        )

        val putChecklist = evaluatePutChecklist(
            candles = closedCandles,
            pair = pair,
            brokerConfig = brokerConfig,
            dataReliable = dataReliable,
            trend = trend,
            ema5 = ema5,
            ema9 = ema9,
            ema20 = ema20,
            rsi = rsi14,
            macdHist = macdHist,
            prevMacdHist = prevMacdHist,
            adx = adx14,
            volatilityRegime = volatilityRegime,
            pattern = pattern,
            structure = structure
        )

        return IndicatorSnapshot(
            ema5 = ema5,
            ema9 = ema9,
            ema20 = ema20,
            ema50 = ema50,
            rsi14 = rsi14,
            macdLine = macdLine,
            macdSignal = macdSignal,
            macdHistogram = macdHist,
            prevMacdHistogram = prevMacdHist,
            bbUpper = bbUpper,
            bbMiddle = bbMiddle,
            bbLower = bbLower,
            bbBandwidthPct = bbBandwidthPct,
            adx14 = adx14,
            plusDi14 = plusDi14,
            minusDi14 = minusDi14,
            atr14 = atr14,
            atrPct = atrPct,
            stochK = stochK,
            stochD = stochD,
            vwap = vwap,
            candlestickPattern = pattern,
            marketStructure = structure,
            volatilityRegime = volatilityRegime,
            trendClassification = trend,
            callChecklist = callChecklist,
            putChecklist = putChecklist,
            series = IndicatorSeries(
                ema5 = ema5Series,
                ema9 = ema9Series,
                ema20 = ema20Series,
                ema50 = ema50Series,
                bbUpper = bbUpperSeries,
                bbMiddle = bbMiddleSeries,
                bbLower = bbLowerSeries,
                rsi14 = rsi14Series,
                macdHistogram = macdHistSeries
            )
        )
    }

    /**
     * Generates a visual sub-minute chart projection from closed M1 candles when the user inspects
     * 30s / 15s / 5s / 3s micro-intervals, while keeping M1 as the authoritative indicator source.
     */
    fun buildDisplayCandles(
        m1Candles: List<Candle>,
        timeframe: CandleTimeframe
    ): List<Candle> {
        if (timeframe == CandleTimeframe.M1 || m1Candles.isEmpty()) {
            return m1Candles
        }
        val subCount = (60 / timeframe.seconds).coerceIn(2, 12)
        val sourceSlice = m1Candles.takeLast(14)
        val result = ArrayList<Candle>(sourceSlice.size * subCount)
        for (candle in sourceSlice) {
            val stepSec = timeframe.seconds.toLong()
            val priceSpan = candle.close - candle.open
            val wickSpread = (candle.high - candle.low) * 0.28
            var prevClose = candle.open
            for (i in 0 until subCount) {
                val frac = (i + 1).toDouble() / subCount
                val wave = kotlin.math.sin((i + 1) * 1.65) * wickSpread * 0.45
                val targetClose = if (i == subCount - 1) candle.close else candle.open + priceSpan * frac + wave
                val high = maxOf(prevClose, targetClose) + abs(wickSpread * 0.35)
                val low = minOf(prevClose, targetClose) - abs(wickSpread * 0.35)
                result.add(
                    Candle(
                        timestampEpochSec = candle.timestampEpochSec + i * stepSec,
                        open = prevClose,
                        high = min(candle.high, high).coerceAtLeast(maxOf(prevClose, targetClose)),
                        low = max(candle.low, low).coerceAtMost(minOf(prevClose, targetClose)),
                        close = targetClose,
                        volume = candle.volume / subCount,
                        isClosed = candle.isClosed || i < subCount - 1
                    )
                )
                prevClose = targetClose
            }
        }
        return result.takeLast(48)
    }

    fun computeEmaSeries(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.isEmpty()) return result
        val k = 2.0 / (period + 1)
        if (values.size < period) {
            var ema = values[0]
            result[0] = ema
            for (i in 1 until values.size) {
                ema = values[i] * k + ema * (1.0 - k)
                result[i] = ema
            }
            return result
        }
        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        var ema = sum / period
        result[period - 1] = ema
        for (i in period until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
            result[i] = ema
        }
        return result
    }

    fun computeRsiSeries(closes: List<Double>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(closes.size) { null }
        if (closes.size <= period) return result

        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gainSum += diff else lossSum += -diff
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        result[period] = rsiFromAvg(avgGain, avgLoss)

        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            result[i] = rsiFromAvg(avgGain, avgLoss)
        }
        return result
    }

    private fun rsiFromAvg(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun computeMacdSeries(
        closes: List<Double>,
        fast: Int = 12,
        slow: Int = 26,
        signal: Int = 9
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val fastEma = computeEmaSeries(closes, fast)
        val slowEma = computeEmaSeries(closes, slow)
        val macdLine = MutableList<Double?>(closes.size) { null }
        val nonNullMacd = mutableListOf<Double>()
        val nonNullIndices = mutableListOf<Int>()

        for (i in closes.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                val m = f - s
                macdLine[i] = m
                nonNullMacd.add(m)
                nonNullIndices.add(i)
            }
        }

        val signalSub = computeEmaSeries(nonNullMacd, signal)
        val signalLine = MutableList<Double?>(closes.size) { null }
        val histLine = MutableList<Double?>(closes.size) { null }
        for (idx in nonNullIndices.indices) {
            val origIdx = nonNullIndices[idx]
            val sig = signalSub[idx]
            val macd = macdLine[origIdx]
            if (sig != null && macd != null) {
                signalLine[origIdx] = sig
                histLine[origIdx] = macd - sig
            }
        }
        return Triple(macdLine, signalLine, histLine)
    }

    fun computeBollingerSeries(
        closes: List<Double>,
        period: Int = 20,
        stdDevMult: Double = 2.0
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val upper = MutableList<Double?>(closes.size) { null }
        val middle = MutableList<Double?>(closes.size) { null }
        val lower = MutableList<Double?>(closes.size) { null }
        if (closes.size < period) return Triple(upper, middle, lower)

        for (i in (period - 1) until closes.size) {
            val window = closes.subList(i - period + 1, i + 1)
            val mean = window.average()
            val variance = window.sumOf { (it - mean).pow(2) } / period
            val sd = sqrt(variance)
            middle[i] = mean
            upper[i] = mean + stdDevMult * sd
            lower[i] = mean - stdDevMult * sd
        }
        return Triple(upper, middle, lower)
    }

    fun computeAtr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int = 14): Double {
        if (closes.size < 2) return 0.0
        val trs = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            val tr = maxOf(
                highs[i] - lows[i],
                abs(highs[i] - closes[i - 1]),
                abs(lows[i] - closes[i - 1])
            )
            trs.add(tr)
        }
        if (trs.size < period) return trs.average()
        var atr = trs.take(period).average()
        for (i in period until trs.size) {
            atr = (atr * (period - 1) + trs[i]) / period
        }
        return atr
    }

    fun computeAdx(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): Triple<Double, Double, Double> {
        if (closes.size <= period * 2) return Triple(18.0, 20.0, 20.0)
        val trs = DoubleArray(closes.size)
        val plusDm = DoubleArray(closes.size)
        val minusDm = DoubleArray(closes.size)

        for (i in 1 until closes.size) {
            val upMove = highs[i] - highs[i - 1]
            val downMove = lows[i - 1] - lows[i]
            plusDm[i] = if (upMove > downMove && upMove > 0) upMove else 0.0
            minusDm[i] = if (downMove > upMove && downMove > 0) downMove else 0.0
            trs[i] = maxOf(
                highs[i] - lows[i],
                abs(highs[i] - closes[i - 1]),
                abs(lows[i] - closes[i - 1])
            )
        }

        var smTr = (1..period).sumOf { trs[it] }
        var smPlus = (1..period).sumOf { plusDm[it] }
        var smMinus = (1..period).sumOf { minusDm[it] }

        val dxValues = mutableListOf<Double>()
        var lastPlusDi = 0.0
        var lastMinusDi = 0.0

        for (i in (period + 1) until closes.size) {
            smTr = smTr - (smTr / period) + trs[i]
            smPlus = smPlus - (smPlus / period) + plusDm[i]
            smMinus = smMinus - (smMinus / period) + minusDm[i]

            if (smTr > 0.0) {
                lastPlusDi = 100.0 * (smPlus / smTr)
                lastMinusDi = 100.0 * (smMinus / smTr)
                val diSum = lastPlusDi + lastMinusDi
                val dx = if (diSum > 0.0) 100.0 * abs(lastPlusDi - lastMinusDi) / diSum else 0.0
                dxValues.add(dx)
            }
        }
        if (dxValues.size < period) {
            return Triple(dxValues.lastOrNull() ?: 18.0, lastPlusDi, lastMinusDi)
        }
        var adx = dxValues.take(period).average()
        for (i in period until dxValues.size) {
            adx = (adx * (period - 1) + dxValues[i]) / period
        }
        return Triple(adx, lastPlusDi, lastMinusDi)
    }

    fun computeStochastic(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        kPeriod: Int = 14,
        dPeriod: Int = 3,
        smoothing: Int = 3
    ): Pair<Double, Double> {
        if (closes.size < kPeriod + smoothing + dPeriod) return 50.0 to 50.0
        val rawK = mutableListOf<Double>()
        for (i in (kPeriod - 1) until closes.size) {
            val hh = highs.subList(i - kPeriod + 1, i + 1).maxOrNull() ?: closes[i]
            val ll = lows.subList(i - kPeriod + 1, i + 1).minOrNull() ?: closes[i]
            val denom = (hh - ll).coerceAtLeast(1e-9)
            rawK.add(((closes[i] - ll) / denom) * 100.0)
        }
        val smoothK = mutableListOf<Double>()
        for (i in (smoothing - 1) until rawK.size) {
            smoothK.add(rawK.subList(i - smoothing + 1, i + 1).average())
        }
        if (smoothK.size < dPeriod) return (smoothK.lastOrNull() ?: 50.0) to 50.0
        val k = smoothK.last()
        val d = smoothK.takeLast(dPeriod).average()
        return k to d
    }

    fun computeVwap(candles: List<Candle>): Double? {
        val totalVol = candles.sumOf { it.volume }
        if (totalVol <= 0.0) return null
        val pvSum = candles.sumOf { ((it.high + it.low + it.close) / 3.0) * it.volume }
        return pvSum / totalVol
    }

    private fun detectCandlestickPattern(candles: List<Candle>): String {
        if (candles.size < 2) return "Neutral Bar"
        val prev = candles[candles.size - 2]
        val curr = candles.last()
        val bodyRatio = curr.bodySize / curr.totalRange
        val lowerWickRatio = curr.lowerWick / curr.totalRange
        val upperWickRatio = curr.upperWick / curr.totalRange

        return when {
            curr.isBullish && !prev.isBullish &&
                curr.close >= prev.open && curr.open <= prev.close &&
                curr.bodySize > prev.bodySize * 0.9 -> "Bullish Engulfing"

            !curr.isBullish && prev.isBullish &&
                curr.open >= prev.close && curr.close <= prev.open &&
                curr.bodySize > prev.bodySize * 0.9 -> "Bearish Engulfing"

            lowerWickRatio >= 0.52 && curr.bodySize <= curr.totalRange * 0.35 ->
                "Bullish Rejection Pin Bar"

            upperWickRatio >= 0.52 && curr.bodySize <= curr.totalRange * 0.35 ->
                "Bearish Rejection Shooting Star"

            bodyRatio <= 0.14 -> "Doji Indecision"
            curr.isBullish && bodyRatio >= 0.65 -> "Strong Bullish Momentum Candle"
            !curr.isBullish && bodyRatio >= 0.65 -> "Strong Bearish Momentum Candle"
            curr.isBullish -> "Bullish Confirmation Close"
            else -> "Bearish Confirmation Close"
        }
    }

    private fun detectMarketStructure(candles: List<Candle>): String {
        if (candles.size < 12) return "Insufficient Structure"
        val recent = candles.takeLast(12)
        val firstHalfHigh = recent.take(6).maxOf { it.high }
        val secondHalfHigh = recent.drop(6).maxOf { it.high }
        val firstHalfLow = recent.take(6).minOf { it.low }
        val secondHalfLow = recent.drop(6).minOf { it.low }

        return when {
            secondHalfHigh > firstHalfHigh && secondHalfLow > firstHalfLow ->
                "Higher Highs & Higher Lows (HH/HL)"
            secondHalfHigh < firstHalfHigh && secondHalfLow < firstHalfLow ->
                "Lower Highs & Lower Lows (LH/LL)"
            secondHalfHigh >= firstHalfHigh && secondHalfLow < firstHalfLow ->
                "Expanding Range Volatility"
            else -> "Tight Consolidation / Sideways Structure"
        }
    }

    private fun classifyVolatility(atrPct: Double, bbBandwidthPct: Double): String {
        return when {
            atrPct >= 0.18 || bbBandwidthPct >= 0.45 -> "HIGH VOLATILITY"
            atrPct <= 0.008 || bbBandwidthPct <= 0.02 -> "LOW / COMPRESSED"
            else -> "OPTIMAL SHORT-TERM"
        }
    }

    private fun classifyTrend(
        price: Double,
        ema5: Double,
        ema9: Double,
        ema20: Double,
        ema50: Double,
        rsi: Double,
        macdHist: Double,
        adx: Double,
        structure: String,
        volatilityRegime: String,
        ema5Series: List<Double?>,
        ema9Series: List<Double?>
    ): TrendClassification {
        if (volatilityRegime == "HIGH VOLATILITY") return TrendClassification.HIGH_VOLATILITY

        // Detect repeated EMA 5/9 crossovers over last 8 bars -> SIDEWAYS
        var crosses = 0
        val start = (ema5Series.size - 8).coerceAtLeast(1)
        for (i in start until ema5Series.size) {
            val prevDiff = (ema5Series[i - 1] ?: 0.0) - (ema9Series[i - 1] ?: 0.0)
            val currDiff = (ema5Series[i] ?: 0.0) - (ema9Series[i] ?: 0.0)
            if ((prevDiff > 0 && currDiff < 0) || (prevDiff < 0 && currDiff > 0)) {
                crosses++
            }
        }
        if (crosses >= 2 || (adx < 17.0 && abs(rsi - 50.0) < 6.0 && structure.contains("Consolidation"))) {
            return TrendClassification.SIDEWAYS
        }

        val bullStack = ema5 > ema9 && ema9 > ema20 && price > ema20
        val bearStack = ema5 < ema9 && ema9 < ema20 && price < ema20

        return when {
            bullStack && ema20 > ema50 && adx >= 24.0 && rsi in 52.0..68.0 && macdHist > 0 ->
                TrendClassification.STRONG_BULLISH
            bullStack && (rsi >= 50.0 || macdHist > 0 || structure.contains("HH/HL")) ->
                TrendClassification.BULLISH
            bearStack && ema20 < ema50 && adx >= 24.0 && rsi in 32.0..48.0 && macdHist < 0 ->
                TrendClassification.STRONG_BEARISH
            bearStack && (rsi <= 50.0 || macdHist < 0 || structure.contains("LH/LL")) ->
                TrendClassification.BEARISH
            else -> TrendClassification.UNCERTAIN
        }
    }

    private fun evaluateCallChecklist(
        candles: List<Candle>,
        pair: MarketPair,
        brokerConfig: BrokerConfig,
        dataReliable: Boolean,
        trend: TrendClassification,
        ema5: Double,
        ema9: Double,
        ema20: Double,
        rsi: Double,
        macdHist: Double,
        prevMacdHist: Double,
        adx: Double,
        volatilityRegime: String,
        pattern: String,
        structure: String
    ): List<StrategyCondition> {
        val last = candles.last()
        val recent3 = candles.takeLast(3)
        val pullbackNearEma = recent3.any {
            abs(it.low - ema9) <= pair.pipSize * 5.0 ||
                abs(it.low - ema20) <= pair.pipSize * 6.0 ||
                (it.low <= ema9 && it.close >= ema9)
        }
        val bullishConfirmCandle = last.isClosed && last.isBullish
        val preferredBullPattern = pattern.contains("Bullish Engulfing") || pattern.contains("Bullish Rejection")
        val higherLow = candles.size >= 8 &&
            candles.takeLast(4).minOf { it.low } >= candles.dropLast(4).takeLast(4).minOf { it.low } - pair.pipSize
        val historicalWinPass = dataReliable && adx >= 18.0 && trend != TrendClassification.SIDEWAYS

        return listOf(
            StrategyCondition(1, "Short-term trend bullish", trend.label, trend == TrendClassification.BULLISH || trend == TrendClassification.STRONG_BULLISH),
            StrategyCondition(2, "EMA 5 > EMA 9", fmt(ema5, pair) + " > " + fmt(ema9, pair), ema5 > ema9),
            StrategyCondition(3, "EMA 9 > EMA 20", fmt(ema9, pair) + " > " + fmt(ema20, pair), ema9 > ema20),
            StrategyCondition(4, "Price above EMA 20", fmt(last.close, pair), last.close > ema20),
            StrategyCondition(5, "RSI bullish momentum (50–68)", String.format(Locale.US, "%.1f", rsi), rsi in 50.0..69.5),
            StrategyCondition(6, "MACD histogram positive/turning+", String.format(Locale.US, "%.6f", macdHist), macdHist > 0.0 || macdHist > prevMacdHist),
            StrategyCondition(7, "Pullback toward EMA 9 or EMA 20", if (pullbackNearEma) "Pullback tested" else "Extended from EMA", pullbackNearEma),
            StrategyCondition(8, "Closed bullish confirmation candle", if (bullishConfirmCandle) "Confirmed" else "Bearish/Open", bullishConfirmCandle),
            StrategyCondition(9, "Bullish engulfing/rejection preferred", pattern, preferredBullPattern || (last.isBullish && last.lowerWick > last.upperWick)),
            StrategyCondition(10, "Higher low structure", structure, higherLow || structure.contains("HH/HL")),
            StrategyCondition(11, "Suitable short-term volatility", volatilityRegime, volatilityRegime == "OPTIMAL SHORT-TERM"),
            StrategyCondition(12, "No imminent major adverse news", brokerConfig.newsImpact.label, brokerConfig.newsImpact != NewsImpact.HIGH),
            StrategyCondition(13, "Payout passes profitability filter", "${brokerConfig.payoutPct}% >= ${brokerConfig.minPayoutThresholdPct}%", brokerConfig.payoutPct >= brokerConfig.minPayoutThresholdPct),
            StrategyCondition(14, "Historical / ADX validation", String.format(Locale.US, "ADX %.1f", adx), historicalWinPass)
        )
    }

    private fun evaluatePutChecklist(
        candles: List<Candle>,
        pair: MarketPair,
        brokerConfig: BrokerConfig,
        dataReliable: Boolean,
        trend: TrendClassification,
        ema5: Double,
        ema9: Double,
        ema20: Double,
        rsi: Double,
        macdHist: Double,
        prevMacdHist: Double,
        adx: Double,
        volatilityRegime: String,
        pattern: String,
        structure: String
    ): List<StrategyCondition> {
        val last = candles.last()
        val recent3 = candles.takeLast(3)
        val pullbackNearEma = recent3.any {
            abs(it.high - ema9) <= pair.pipSize * 5.0 ||
                abs(it.high - ema20) <= pair.pipSize * 6.0 ||
                (it.high >= ema9 && it.close <= ema9)
        }
        val bearishConfirmCandle = last.isClosed && !last.isBullish
        val preferredBearPattern = pattern.contains("Bearish Engulfing") || pattern.contains("Bearish Rejection")
        val lowerHigh = candles.size >= 8 &&
            candles.takeLast(4).maxOf { it.high } <= candles.dropLast(4).takeLast(4).maxOf { it.high } + pair.pipSize
        val historicalWinPass = dataReliable && adx >= 18.0 && trend != TrendClassification.SIDEWAYS

        return listOf(
            StrategyCondition(1, "Short-term trend bearish", trend.label, trend == TrendClassification.BEARISH || trend == TrendClassification.STRONG_BEARISH),
            StrategyCondition(2, "EMA 5 < EMA 9", fmt(ema5, pair) + " < " + fmt(ema9, pair), ema5 < ema9),
            StrategyCondition(3, "EMA 9 < EMA 20", fmt(ema9, pair) + " < " + fmt(ema20, pair), ema9 < ema20),
            StrategyCondition(4, "Price below EMA 20", fmt(last.close, pair), last.close < ema20),
            StrategyCondition(5, "RSI bearish momentum (32–50)", String.format(Locale.US, "%.1f", rsi), rsi in 30.5..50.0),
            StrategyCondition(6, "MACD histogram negative/turning-", String.format(Locale.US, "%.6f", macdHist), macdHist < 0.0 || macdHist < prevMacdHist),
            StrategyCondition(7, "Pullback toward EMA 9 or EMA 20", if (pullbackNearEma) "Pullback tested" else "Extended from EMA", pullbackNearEma),
            StrategyCondition(8, "Closed bearish confirmation candle", if (bearishConfirmCandle) "Confirmed" else "Bullish/Open", bearishConfirmCandle),
            StrategyCondition(9, "Bearish engulfing/rejection preferred", pattern, preferredBearPattern || (!last.isBullish && last.upperWick > last.lowerWick)),
            StrategyCondition(10, "Lower high structure", structure, lowerHigh || structure.contains("LH/LL")),
            StrategyCondition(11, "Suitable short-term volatility", volatilityRegime, volatilityRegime == "OPTIMAL SHORT-TERM"),
            StrategyCondition(12, "No imminent major adverse news", brokerConfig.newsImpact.label, brokerConfig.newsImpact != NewsImpact.HIGH),
            StrategyCondition(13, "Payout passes profitability filter", "${brokerConfig.payoutPct}% >= ${brokerConfig.minPayoutThresholdPct}%", brokerConfig.payoutPct >= brokerConfig.minPayoutThresholdPct),
            StrategyCondition(14, "Historical / ADX validation", String.format(Locale.US, "ADX %.1f", adx), historicalWinPass)
        )
    }

    private fun fmt(value: Double, pair: MarketPair): String = pair.formatPrice(value)
}
