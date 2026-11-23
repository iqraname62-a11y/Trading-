package com.example.data.ai

import android.util.Log
import com.example.domain.model.AiSignalResult
import com.example.domain.model.MarketFeedSnapshot
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalOutcome
import com.example.domain.model.TradeExpiry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "TradingSignalAiRepo"

class TradingSignalAiRepository {

    private val signalResponseSchema = Schema.obj(
        properties = mapOf(
            "signal" to Schema.enumeration(
                values = listOf("CALL", "PUT", "NO TRADE"),
                description = "Disciplined binary options trading decision based on closed M1 confirmation"
            ),
            "confidenceScore" to Schema.integer(
                description = "Confidence score from 0 to 100"
            ),
            "trendClassification" to Schema.enumeration(
                values = listOf(
                    "STRONG BULLISH",
                    "BULLISH",
                    "STRONG BEARISH",
                    "BEARISH",
                    "SIDEWAYS",
                    "HIGH VOLATILITY",
                    "UNCERTAIN"
                ),
                description = "Detected short-term market trend classification"
            ),
            "candlestickPattern" to Schema.string(
                description = "Confirmed closed candlestick pattern and rejection behavior"
            ),
            "marketStructure" to Schema.string(
                description = "Higher highs/lows, lower highs/lows, or range structure"
            ),
            "volatilityAssessment" to Schema.string(
                description = "Short-term ATR and Bollinger Band volatility evaluation"
            ),
            "riskRewardVerdict" to Schema.string(
                description = "Profitability and capital preservation assessment including payout and news check"
            ),
            "reasonsForSignal" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concise technical reasons supporting CALL, PUT, or NO TRADE"
            ),
            "riskWarnings" to Schema.array(
                items = Schema.string(),
                description = "1 to 3 key risk warnings, spread/news cautions, or invalidation factors"
            ),
            "executiveSummary" to Schema.string(
                description = "Two-sentence disciplined trading desk summary with exact execution guidance"
            )
        )
    )

    private val masterSystemInstruction = content {
        text(
            """
            MASTER SYSTEM PROMPT: ADVANCED AI BINARY OPTIONS TRADING BOT
            
            1. ROLE AND OBJECTIVE:
            You are an advanced AI Binary Options Market Analysis and Trading Signal Generation System.
            Generate accurate, disciplined CALL, PUT, or NO TRADE signals based on Technical Analysis, Price Action, Market Structure, Multi-Indicator Confirmation, Trend Detection, Momentum Analysis, Volatility Analysis, Candlestick Recognition, Support and Resistance, Liquidity Analysis, Statistical Probability, and Risk Management.
            Main Chart Timeframe: 1 Minute (M1).
            Primary Trade Expiration: Exactly 1 Minute (60 Seconds) unless user selected an explicit sub-minute expiry (5s/15s/30s). Never use 5m, 10m, or 15m expiration.
            Prioritize capital preservation over signal frequency.
            
            2. CLOSED CANDLE DISCIPLINE:
            Use ONLY closed candles for indicator confirmation. Never treat an incomplete candle as a confirmed candlestick pattern.
            If market data is unreliable, sideways with repeated EMA crossovers, high adverse news impact is active, volatility is erratic/extreme, or broker payout fails the profitability threshold, return NO TRADE.
            
            3. CALL (UP) ENTRY STRATEGY (generate CALL when bullish confluence >= 9/14 and trend/momentum align):
            - Short-term trend is bullish (EMA 5 > EMA 9 > EMA 20 or bullish crossover with price > EMA 20)
            - RSI (14) confirms bullish momentum (50-68, not severely overbought >= 72)
            - MACD (12,26,9) histogram positive or turning positive
            - Pullback toward EMA 9 or EMA 20 with bullish confirmation candle (engulfing, rejection, or solid bullish close)
            - Higher low structure, suitable volatility, no high-impact adverse news, payout passes filter.
            
            4. PUT (DOWN) ENTRY STRATEGY (generate PUT when bearish confluence >= 9/14 and trend/momentum align):
            - Short-term trend is bearish (EMA 5 < EMA 9 < EMA 20 or bearish crossover with price < EMA 20)
            - RSI (14) confirms bearish momentum (32-50, not severely oversold <= 28)
            - MACD (12,26,9) histogram negative or turning negative
            - Pullback toward EMA 9 or EMA 20 with bearish confirmation candle (engulfing, rejection, or solid bearish close)
            - Lower high structure, suitable volatility, no high-impact adverse news, payout passes filter.
            
            When directional confluence is clear, decisively issue CALL (for UP) or PUT (for DOWN). Use NO TRADE only when the market is choppy/sideways, high-impact news is active, payout is below minimum, or signals conflict.
            
            Respond strictly matching the JSON schema.
            """.trimIndent()
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = signalResponseSchema
                temperature = 0.2f
            },
            systemInstruction = masterSystemInstruction
        )
    }

    suspend fun analyzeAndGenerateSignal(
        snapshot: MarketFeedSnapshot,
        selectedExpiry: TradeExpiry
    ): Result<AiSignalResult> {
        return try {
            val effectiveExpirySec = if (snapshot.brokerConfig.strictM1MasterMode) {
                60
            } else {
                selectedExpiry.seconds
            }
            val nowMs = System.currentTimeMillis()
            val executionMs = nowMs + 2_000L
            val expirationMs = executionMs + (effectiveExpirySec * 1_000L)

            val fmtUtc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                .withZone(ZoneOffset.UTC)
            val signalTs = fmtUtc.format(Instant.ofEpochMilli(nowMs))
            val execTs = fmtUtc.format(Instant.ofEpochMilli(executionMs))
            val expTs = fmtUtc.format(Instant.ofEpochMilli(expirationMs))

            val prompt = buildMarketAnalysisPrompt(
                snapshot = snapshot,
                effectiveExpirySec = effectiveExpirySec,
                signalTs = signalTs,
                execTs = execTs,
                expTs = expTs
            )

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("AI engine returned an empty response."))

            val json = JSONObject(jsonText)
            val dirStr = json.optString("signal", "NO TRADE").uppercase(Locale.US)
            val direction = when (dirStr) {
                "CALL" -> SignalDirection.CALL
                "PUT" -> SignalDirection.PUT
                else -> SignalDirection.NO_TRADE
            }

            val reasonsArr = json.optJSONArray("reasonsForSignal")
            val reasons = if (reasonsArr != null) {
                (0 until reasonsArr.length()).map { reasonsArr.getString(it) }
            } else emptyList()

            val warnArr = json.optJSONArray("riskWarnings")
            val warnings = if (warnArr != null) {
                (0 until warnArr.length()).map { warnArr.getString(it) }
            } else emptyList()

            val ind = snapshot.indicators
            val checklistLabel = when (direction) {
                SignalDirection.CALL -> "CALL ${ind.callPassedCount}/14"
                SignalDirection.PUT -> "PUT ${ind.putPassedCount}/14"
                SignalDirection.NO_TRADE -> "CALL ${ind.callPassedCount}/14 | PUT ${ind.putPassedCount}/14"
            }

            val indicatorSummary = String.format(
                Locale.US,
                "RSI %.1f • ADX %.1f • MACD %+.5f • Stoch %.0f/%.0f",
                ind.rsi14,
                ind.adx14,
                ind.macdHistogram,
                ind.stochK,
                ind.stochD
            )

            Result.success(
                AiSignalResult(
                    pairDisplay = snapshot.pair.displayName,
                    chartTimeframe = snapshot.chartTimeframe.label,
                    expirySeconds = effectiveExpirySec,
                    direction = direction,
                    confidenceScore = json.optInt("confidenceScore", 50).coerceIn(0, 100),
                    trendClassification = json.optString("trendClassification", ind.trendClassification.label),
                    entryPrice = snapshot.currentPrice,
                    priceDecimals = snapshot.pair.priceDecimals,
                    signalTimestampUtc = signalTs,
                    executionTimestampUtc = execTs,
                    expirationTimestampUtc = expTs,
                    createdAtEpochMs = nowMs,
                    expirationEpochMs = expirationMs,
                    candlestickPattern = json.optString("candlestickPattern", ind.candlestickPattern),
                    marketStructure = json.optString("marketStructure", ind.marketStructure),
                    volatilityAssessment = json.optString("volatilityAssessment", ind.volatilityRegime),
                    riskRewardVerdict = json.optString("riskRewardVerdict", "Payout ${snapshot.brokerConfig.payoutPct}%"),
                    reasons = reasons,
                    warnings = warnings,
                    executiveSummary = json.optString("executiveSummary", ""),
                    checklistScoreLabel = checklistLabel,
                    indicatorSummary = indicatorSummary,
                    payoutPct = snapshot.brokerConfig.payoutPct,
                    outcome = if (direction == SignalDirection.NO_TRADE) {
                        SignalOutcome.SKIPPED
                    } else {
                        SignalOutcome.PENDING
                    }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI signal generation timed out after 60s", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI signal generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI signal generation failed."))
        }
    }

    private fun buildMarketAnalysisPrompt(
        snapshot: MarketFeedSnapshot,
        effectiveExpirySec: Int,
        signalTs: String,
        execTs: String,
        expTs: String
    ): String {
        val p = snapshot.pair
        val ind = snapshot.indicators
        val cfg = snapshot.brokerConfig
        val prevClosed = snapshot.previousClosedCandle
        val currForming = snapshot.currentFormingCandle
        val recent12Closed = snapshot.closedM1Candles.takeLast(12).joinToString("\n") { c ->
            "O=${p.formatPrice(c.open)} H=${p.formatPrice(c.high)} L=${p.formatPrice(c.low)} C=${p.formatPrice(c.close)} V=${String.format(Locale.US, "%.0f", c.volume)}"
        }

        val vwapText = ind.vwap?.let { p.formatPrice(it) } ?: "Unavailable (spot FX feed without exchange volume)"
        val callFails = ind.callChecklist.filter { !it.passed }.joinToString("; ") { "${it.index}.${it.title}" }
        val putFails = ind.putChecklist.filter { !it.passed }.joinToString("; ") { "${it.index}.${it.title}" }

        return """
            Analyze the following 18 validated Market Data inputs for ${p.displayName} (${snapshot.feedSourceLabel}):
            
            1. Current Asset Price: ${p.formatPrice(snapshot.currentPrice)}
            2. Previous Closed M1 Candle OHLC: O=${p.formatPrice(prevClosed.open)} H=${p.formatPrice(prevClosed.high)} L=${p.formatPrice(prevClosed.low)} C=${p.formatPrice(prevClosed.close)}
            3. Current Forming M1 Candle OHLC (unclosed - do not confirm pattern): O=${p.formatPrice(currForming.open)} H=${p.formatPrice(currForming.high)} L=${p.formatPrice(currForming.low)} C=${p.formatPrice(currForming.close)}
            4. Historical Last 12 Closed M1 Candles:
            $recent12Closed
            5. EMA Values (Closed M1): EMA5=${p.formatPrice(ind.ema5)}, EMA9=${p.formatPrice(ind.ema9)}, EMA20=${p.formatPrice(ind.ema20)}, EMA50=${p.formatPrice(ind.ema50)}
            6. RSI (14): ${String.format(Locale.US, "%.2f", ind.rsi14)}
            7. MACD (12,26,9): Line=${String.format(Locale.US, "%.6f", ind.macdLine)}, Signal=${String.format(Locale.US, "%.6f", ind.macdSignal)}, Hist=${String.format(Locale.US, "%.6f", ind.macdHistogram)} (PrevHist=${String.format(Locale.US, "%.6f", ind.prevMacdHistogram)})
            8. Bollinger Bands (20,2): Upper=${p.formatPrice(ind.bbUpper)}, Middle=${p.formatPrice(ind.bbMiddle)}, Lower=${p.formatPrice(ind.bbLower)}, Bandwidth=${String.format(Locale.US, "%.4f", ind.bbBandwidthPct)}%
            9. ADX (14): ADX=${String.format(Locale.US, "%.2f", ind.adx14)}, +DI=${String.format(Locale.US, "%.2f", ind.plusDi14)}, -DI=${String.format(Locale.US, "%.2f", ind.minusDi14)}
            10. ATR (14): ${p.formatPrice(ind.atr14)} (${String.format(Locale.US, "%.4f", ind.atrPct)}% of price)
            11. Stochastic (14,3,3): %K=${String.format(Locale.US, "%.2f", ind.stochK)}, %D=${String.format(Locale.US, "%.2f", ind.stochD)}
            12. VWAP: $vwapText
            13. Current Spread: ${cfg.spreadPips} pips
            14. Market Volatility & Pattern: Regime=${ind.volatilityRegime}, Pattern=${ind.candlestickPattern}, Structure=${ind.marketStructure}, Pre-Trend=${ind.trendClassification.label}
            15. Economic News Information: ${cfg.newsImpact.label} (${cfg.newsImpact.riskNote})
            16. Broker Payout Percentage: ${cfg.payoutPct}% (Minimum Profitability Filter: ${cfg.minPayoutThresholdPct}%)
            17. Exact Signal Generation Timestamp: $signalTs | Expected Execution: $execTs
            18. Actual Expiration Timestamp (${effectiveExpirySec}s expiry): $expTs | Visual Chart Mode: ${snapshot.chartTimeframe.label} | Strict M1 Master Mode: ${cfg.strictM1MasterMode}
            
            Deterministic Pre-Screen:
            - CALL Checklist Passed: ${ind.callPassedCount}/14 (Failed: ${callFails.ifBlank { "None" }})
            - PUT Checklist Passed: ${ind.putPassedCount}/14 (Failed: ${putFails.ifBlank { "None" }})
            
            Enforce strict capital preservation: generate CALL or PUT only if clear directional multi-indicator confluence and closed-candle confirmation exist; otherwise return NO TRADE.
        """.trimIndent()
    }
}
