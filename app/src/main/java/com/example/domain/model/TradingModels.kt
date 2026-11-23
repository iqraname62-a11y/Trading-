package com.example.domain.model

import java.util.Locale

enum class PairCategory(val label: String) {
    ALL("All Pairs"),
    LIVE_MAJOR("Live Majors"),
    LIVE_CROSS("Live Crosses"),
    OTC("OTC Market")
}

data class MarketPair(
    val id: String,
    val displayName: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val yahooTicker: String,
    val category: PairCategory,
    val isOtc: Boolean,
    val priceDecimals: Int,
    val pipSize: Double,
    val defaultPayoutPct: Int
) {
    fun formatPrice(price: Double): String =
        String.format(Locale.US, "%.${priceDecimals}f", price)
}

object TradingPairsCatalog {
    val allPairs: List<MarketPair> = listOf(
        // Live Majors & Core Pairs
        livePair("EUR/USD", "EUR", "USD", "EURUSD=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 86),
        livePair("GBP/USD", "GBP", "USD", "GBPUSD=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 85),
        livePair("USD/JPY", "USD", "JPY", "USDJPY=X", PairCategory.LIVE_MAJOR, 3, 0.01, 85),
        livePair("AUD/USD", "AUD", "USD", "AUDUSD=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 84),
        livePair("USD/CAD", "USD", "CAD", "USDCAD=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 84),
        livePair("USD/CHF", "USD", "CHF", "USDCHF=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 83),
        livePair("NZD/USD", "NZD", "USD", "NZDUSD=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 82),
        livePair("EUR/GBP", "EUR", "GBP", "EURGBP=X", PairCategory.LIVE_MAJOR, 5, 0.0001, 84),
        livePair("EUR/JPY", "EUR", "JPY", "EURJPY=X", PairCategory.LIVE_MAJOR, 3, 0.01, 85),
        livePair("GBP/JPY", "GBP", "JPY", "GBPJPY=X", PairCategory.LIVE_MAJOR, 3, 0.01, 84),

        // Live Crosses
        livePair("EUR/AUD", "EUR", "AUD", "EURAUD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 82),
        livePair("GBP/AUD", "GBP", "AUD", "GBPAUD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 81),
        livePair("AUD/JPY", "AUD", "JPY", "AUDJPY=X", PairCategory.LIVE_CROSS, 3, 0.01, 83),
        livePair("AUD/CAD", "AUD", "CAD", "AUDCAD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 81),
        livePair("EUR/CAD", "EUR", "CAD", "EURCAD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 82),
        livePair("GBP/CAD", "GBP", "CAD", "GBPCAD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 81),
        livePair("CHF/JPY", "CHF", "JPY", "CHFJPY=X", PairCategory.LIVE_CROSS, 3, 0.01, 80),
        livePair("CAD/JPY", "CAD", "JPY", "CADJPY=X", PairCategory.LIVE_CROSS, 3, 0.01, 82),
        livePair("NZD/JPY", "NZD", "JPY", "NZDJPY=X", PairCategory.LIVE_CROSS, 3, 0.01, 80),
        livePair("EUR/CHF", "EUR", "CHF", "EURCHF=X", PairCategory.LIVE_CROSS, 5, 0.0001, 81),
        livePair("GBP/CHF", "GBP", "CHF", "GBPCHF=X", PairCategory.LIVE_CROSS, 5, 0.0001, 80),
        livePair("AUD/NZD", "AUD", "NZD", "AUDNZD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 80),
        livePair("EUR/NZD", "EUR", "NZD", "EURNZD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 79),
        livePair("GBP/NZD", "GBP", "NZD", "GBPNZD=X", PairCategory.LIVE_CROSS, 5, 0.0001, 79),

        // OTC Pairs
        otcPair("EUR/USD OTC", "EUR", "USD", "EURUSD=X", 5, 0.0001, 92),
        otcPair("GBP/USD OTC", "GBP", "USD", "GBPUSD=X", 5, 0.0001, 91),
        otcPair("USD/JPY OTC", "USD", "JPY", "USDJPY=X", 3, 0.01, 91),
        otcPair("AUD/USD OTC", "AUD", "USD", "AUDUSD=X", 5, 0.0001, 90),
        otcPair("USD/CAD OTC", "USD", "CAD", "USDCAD=X", 5, 0.0001, 90),
        otcPair("USD/CHF OTC", "USD", "CHF", "USDCHF=X", 5, 0.0001, 89),
        otcPair("NZD/USD OTC", "NZD", "USD", "NZDUSD=X", 5, 0.0001, 88),
        otcPair("EUR/GBP OTC", "EUR", "GBP", "EURGBP=X", 5, 0.0001, 90),
        otcPair("EUR/JPY OTC", "EUR", "JPY", "EURJPY=X", 3, 0.01, 91),
        otcPair("GBP/JPY OTC", "GBP", "JPY", "GBPJPY=X", 3, 0.01, 90),
        otcPair("EUR/AUD OTC", "EUR", "AUD", "EURAUD=X", 5, 0.0001, 89),
        otcPair("GBP/AUD OTC", "GBP", "AUD", "GBPAUD=X", 5, 0.0001, 88),
        otcPair("AUD/JPY OTC", "AUD", "JPY", "AUDJPY=X", 3, 0.01, 89),
        otcPair("USD/PKR OTC", "USD", "PKR", "USDPKR=X", 2, 0.01, 88)
    )

    private fun livePair(
        name: String,
        base: String,
        quote: String,
        ticker: String,
        category: PairCategory,
        decimals: Int,
        pipSize: Double,
        payout: Int
    ) = MarketPair(
        id = name,
        displayName = name,
        baseCurrency = base,
        quoteCurrency = quote,
        yahooTicker = ticker,
        category = category,
        isOtc = false,
        priceDecimals = decimals,
        pipSize = pipSize,
        defaultPayoutPct = payout
    )

    private fun otcPair(
        name: String,
        base: String,
        quote: String,
        ticker: String,
        decimals: Int,
        pipSize: Double,
        payout: Int
    ) = MarketPair(
        id = name,
        displayName = name,
        baseCurrency = base,
        quoteCurrency = quote,
        yahooTicker = ticker,
        category = PairCategory.OTC,
        isOtc = true,
        priceDecimals = decimals,
        pipSize = pipSize,
        defaultPayoutPct = payout
    )

    fun findById(id: String): MarketPair =
        allPairs.firstOrNull { it.id == id } ?: allPairs.first()
}

enum class CandleTimeframe(val label: String, val seconds: Int, val isPrimaryM1: Boolean) {
    M1("1m", 60, true),
    S30("30s", 30, false),
    S15("15s", 15, false),
    S5("5s", 5, false),
    S3("3s", 3, false)
}

enum class TradeExpiry(val label: String, val seconds: Int, val isMasterDefault: Boolean) {
    EXP_1M("1m (60s)", 60, true),
    EXP_30S("30s", 30, false),
    EXP_15S("15s", 15, false),
    EXP_5S("5s", 5, false)
}

enum class NewsImpact(val label: String, val riskNote: String) {
    NONE("No Major News", "Clear economic calendar window"),
    LOW("Low Impact", "Minor data release; normal liquidity"),
    MEDIUM("Medium Impact", "Moderate volatility possible near release"),
    HIGH("High Impact News", "Adverse news event imminent — Capital preservation priority")
}

data class BrokerConfig(
    val payoutPct: Int = 86,
    val spreadPips: Double = 0.8,
    val newsImpact: NewsImpact = NewsImpact.NONE,
    val minPayoutThresholdPct: Int = 75,
    val strictM1MasterMode: Boolean = true
)

data class Candle(
    val timestampEpochSec: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isClosed: Boolean = true
) {
    val isBullish: Boolean get() = close >= open
    val bodySize: Double get() = kotlin.math.abs(close - open)
    val upperWick: Double get() = high - maxOf(open, close)
    val lowerWick: Double get() = minOf(open, close) - low
    val totalRange: Double get() = (high - low).coerceAtLeast(1e-9)
}

enum class TrendClassification(val label: String) {
    STRONG_BULLISH("STRONG BULLISH"),
    BULLISH("BULLISH"),
    STRONG_BEARISH("STRONG BEARISH"),
    BEARISH("BEARISH"),
    SIDEWAYS("SIDEWAYS"),
    HIGH_VOLATILITY("HIGH VOLATILITY"),
    UNCERTAIN("UNCERTAIN")
}

data class StrategyCondition(
    val index: Int,
    val title: String,
    val detail: String,
    val passed: Boolean
)

data class IndicatorSeries(
    val ema5: List<Double?>,
    val ema9: List<Double?>,
    val ema20: List<Double?>,
    val ema50: List<Double?>,
    val bbUpper: List<Double?>,
    val bbMiddle: List<Double?>,
    val bbLower: List<Double?>,
    val rsi14: List<Double?>,
    val macdHistogram: List<Double?>
)

data class IndicatorSnapshot(
    val ema5: Double,
    val ema9: Double,
    val ema20: Double,
    val ema50: Double,
    val rsi14: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val prevMacdHistogram: Double,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val bbBandwidthPct: Double,
    val adx14: Double,
    val plusDi14: Double,
    val minusDi14: Double,
    val atr14: Double,
    val atrPct: Double,
    val stochK: Double,
    val stochD: Double,
    val vwap: Double?,
    val candlestickPattern: String,
    val marketStructure: String,
    val volatilityRegime: String,
    val trendClassification: TrendClassification,
    val callChecklist: List<StrategyCondition>,
    val putChecklist: List<StrategyCondition>,
    val series: IndicatorSeries
) {
    val callPassedCount: Int get() = callChecklist.count { it.passed }
    val putPassedCount: Int get() = putChecklist.count { it.passed }
    val upBiasPct: Int
        get() {
            val total = (callPassedCount + putPassedCount).coerceAtLeast(1)
            return ((callPassedCount * 100.0) / total).toInt().coerceIn(0, 100)
        }
    val downBiasPct: Int get() = 100 - upBiasPct
    val biasLabel: String
        get() = when {
            callPassedCount > putPassedCount -> "UP ▲ (CALL)"
            putPassedCount > callPassedCount -> "DOWN ▼ (PUT)"
            else -> "NEUTRAL"
        }
    val macdTurningBullish: Boolean get() = macdHistogram > 0.0 || macdHistogram > prevMacdHistogram
    val macdTurningBearish: Boolean get() = macdHistogram < 0.0 || macdHistogram < prevMacdHistogram
}

data class MarketFeedSnapshot(
    val pair: MarketPair,
    val chartTimeframe: CandleTimeframe,
    val closedM1Candles: List<Candle>,
    val displayCandles: List<Candle>,
    val previousClosedCandle: Candle,
    val currentFormingCandle: Candle,
    val currentPrice: Double,
    val priceChangePct: Double,
    val feedSourceLabel: String,
    val isLiveExchangeFeed: Boolean,
    val isMarketOpen: Boolean,
    val fetchedAtEpochMs: Long,
    val brokerConfig: BrokerConfig,
    val indicators: IndicatorSnapshot
)

enum class SignalDirection(val label: String) {
    CALL("UP ▲ (CALL)"),
    PUT("DOWN ▼ (PUT)"),
    NO_TRADE("NO TRADE")
}

enum class SignalOutcome(val label: String) {
    PENDING("Pending"),
    WIN("Win"),
    LOSS("Loss"),
    SKIPPED("Capital Preserved")
}

data class AiSignalResult(
    val id: Long = 0L,
    val pairDisplay: String,
    val chartTimeframe: String,
    val expirySeconds: Int,
    val direction: SignalDirection,
    val confidenceScore: Int,
    val trendClassification: String,
    val entryPrice: Double,
    val priceDecimals: Int,
    val signalTimestampUtc: String,
    val executionTimestampUtc: String,
    val expirationTimestampUtc: String,
    val createdAtEpochMs: Long,
    val expirationEpochMs: Long,
    val candlestickPattern: String,
    val marketStructure: String,
    val volatilityAssessment: String,
    val riskRewardVerdict: String,
    val reasons: List<String>,
    val warnings: List<String>,
    val executiveSummary: String,
    val checklistScoreLabel: String,
    val indicatorSummary: String,
    val payoutPct: Int,
    val outcome: SignalOutcome = SignalOutcome.PENDING
)
