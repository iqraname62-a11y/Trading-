package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AiSignalResult
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalOutcome

@Entity(tableName = "signal_history")
data class SignalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pairDisplay: String,
    val chartTimeframe: String,
    val expirySeconds: Int,
    val direction: String,
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
    val reasonsJoined: String,
    val warningsJoined: String,
    val executiveSummary: String,
    val checklistScoreLabel: String,
    val indicatorSummary: String,
    val payoutPct: Int,
    val outcome: String
) {
    fun toDomain(): AiSignalResult = AiSignalResult(
        id = id,
        pairDisplay = pairDisplay,
        chartTimeframe = chartTimeframe,
        expirySeconds = expirySeconds,
        direction = runCatching { SignalDirection.valueOf(direction) }.getOrDefault(SignalDirection.NO_TRADE),
        confidenceScore = confidenceScore,
        trendClassification = trendClassification,
        entryPrice = entryPrice,
        priceDecimals = priceDecimals,
        signalTimestampUtc = signalTimestampUtc,
        executionTimestampUtc = executionTimestampUtc,
        expirationTimestampUtc = expirationTimestampUtc,
        createdAtEpochMs = createdAtEpochMs,
        expirationEpochMs = expirationEpochMs,
        candlestickPattern = candlestickPattern,
        marketStructure = marketStructure,
        volatilityAssessment = volatilityAssessment,
        riskRewardVerdict = riskRewardVerdict,
        reasons = reasonsJoined.split("||").filter { it.isNotBlank() },
        warnings = warningsJoined.split("||").filter { it.isNotBlank() },
        executiveSummary = executiveSummary,
        checklistScoreLabel = checklistScoreLabel,
        indicatorSummary = indicatorSummary,
        payoutPct = payoutPct,
        outcome = runCatching { SignalOutcome.valueOf(outcome) }.getOrDefault(SignalOutcome.PENDING)
    )

    companion object {
        fun fromDomain(signal: AiSignalResult): SignalHistoryEntity = SignalHistoryEntity(
            id = signal.id,
            pairDisplay = signal.pairDisplay,
            chartTimeframe = signal.chartTimeframe,
            expirySeconds = signal.expirySeconds,
            direction = signal.direction.name,
            confidenceScore = signal.confidenceScore,
            trendClassification = signal.trendClassification,
            entryPrice = signal.entryPrice,
            priceDecimals = signal.priceDecimals,
            signalTimestampUtc = signal.signalTimestampUtc,
            executionTimestampUtc = signal.executionTimestampUtc,
            expirationTimestampUtc = signal.expirationTimestampUtc,
            createdAtEpochMs = signal.createdAtEpochMs,
            expirationEpochMs = signal.expirationEpochMs,
            candlestickPattern = signal.candlestickPattern,
            marketStructure = signal.marketStructure,
            volatilityAssessment = signal.volatilityAssessment,
            riskRewardVerdict = signal.riskRewardVerdict,
            reasonsJoined = signal.reasons.joinToString("||"),
            warningsJoined = signal.warnings.joinToString("||"),
            executiveSummary = signal.executiveSummary,
            checklistScoreLabel = signal.checklistScoreLabel,
            indicatorSummary = signal.indicatorSummary,
            payoutPct = signal.payoutPct,
            outcome = signal.outcome.name
        )
    }
}
