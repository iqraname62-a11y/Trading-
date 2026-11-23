package com.example

import com.example.domain.engine.TechnicalIndicatorEngine
import com.example.domain.model.BrokerConfig
import com.example.domain.model.Candle
import com.example.domain.model.TradingPairsCatalog
import com.example.domain.model.TrendClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalIndicatorEngineTest {

    @Test
    fun bullishUptrendProducesAlignedEmasAndBullishTrend() {
        val pair = TradingPairsCatalog.findById("EUR/USD")
        val candles = (0 until 65).map { idx ->
            val base = 1.0850 + idx * 0.00005
            val pullback = if (idx == 62) -0.00008 else 0.0
            val open = base + pullback
            val close = base + pullback + 0.00006
            Candle(
                timestampEpochSec = 1_700_000_000L + idx * 60L,
                open = open,
                high = close + 0.00003,
                low = open - 0.00004,
                close = close,
                volume = 120.0,
                isClosed = true
            )
        }

        val snapshot = TechnicalIndicatorEngine.analyzeClosedCandles(
            closedCandles = candles,
            pair = pair,
            brokerConfig = BrokerConfig(payoutPct = 86),
            dataReliable = true
        )

        assertTrue("Expected EMA5 > EMA9", snapshot.ema5 > snapshot.ema9)
        assertTrue("Expected EMA9 > EMA20", snapshot.ema9 > snapshot.ema20)
        assertTrue("Expected EMA20 > EMA50", snapshot.ema20 > snapshot.ema50)
        assertTrue("Expected RSI > 50, got ${snapshot.rsi14}", snapshot.rsi14 > 50.0)
        assertEquals(14, snapshot.callChecklist.size)
        assertEquals(14, snapshot.putChecklist.size)
        assertTrue(
            "Expected BULLISH or STRONG_BULLISH, got ${snapshot.trendClassification}",
            snapshot.trendClassification == TrendClassification.BULLISH ||
                snapshot.trendClassification == TrendClassification.STRONG_BULLISH
        )
        assertTrue("Expected >= 10 CALL conditions passed, got ${snapshot.callPassedCount}", snapshot.callPassedCount >= 10)
    }

    @Test
    fun bearishDowntrendProducesBearishEmasAndPutChecklist() {
        val pair = TradingPairsCatalog.findById("GBP/USD")
        val candles = (0 until 65).map { idx ->
            val base = 1.2900 - idx * 0.00006
            val open = base + 0.00003
            val close = base - 0.00005
            Candle(
                timestampEpochSec = 1_700_000_000L + idx * 60L,
                open = open,
                high = open + 0.00004,
                low = close - 0.00003,
                close = close,
                volume = 95.0,
                isClosed = true
            )
        }

        val snapshot = TechnicalIndicatorEngine.analyzeClosedCandles(
            closedCandles = candles,
            pair = pair,
            brokerConfig = BrokerConfig(payoutPct = 85),
            dataReliable = true
        )

        assertTrue("Expected EMA5 < EMA9", snapshot.ema5 < snapshot.ema9)
        assertTrue("Expected EMA9 < EMA20", snapshot.ema9 < snapshot.ema20)
        assertTrue("Expected RSI < 50, got ${snapshot.rsi14}", snapshot.rsi14 < 50.0)
        assertTrue(
            "Expected BEARISH or STRONG_BEARISH, got ${snapshot.trendClassification}",
            snapshot.trendClassification == TrendClassification.BEARISH ||
                snapshot.trendClassification == TrendClassification.STRONG_BEARISH
        )
        assertTrue("Expected >= 10 PUT conditions passed, got ${snapshot.putPassedCount}", snapshot.putPassedCount >= 10)
    }
}
