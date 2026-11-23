package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AiSignalResult
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalOutcome
import com.example.ui.SignalUiState
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BearCrimsonMuted
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BullEmerald
import com.example.ui.theme.BullEmeraldMuted
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MonoPriceMedium
import com.example.ui.theme.MonoReadoutSmall
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarnAmber
import com.example.ui.theme.WarnAmberMuted
import java.util.Locale

@Composable
fun SignalResultSection(
    signalState: SignalUiState,
    remainingExpirySec: Int,
    onRetryAnalyze: () -> Unit,
    onMarkOutcome: (Long, SignalOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    when (signalState) {
        is SignalUiState.Idle -> SignalReadyBanner(modifier = modifier)
        is SignalUiState.Analyzing -> SignalAnalyzingCard(modifier = modifier)
        is SignalUiState.Error -> SignalErrorCard(
            message = signalState.message,
            onRetry = onRetryAnalyze,
            modifier = modifier
        )
        is SignalUiState.Success -> AiSignalVerdictCard(
            signal = signalState.signal,
            remainingExpirySec = remainingExpirySec,
            onMarkOutcome = onMarkOutcome,
            modifier = modifier
        )
    }
}

@Composable
private fun SignalReadyBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_market_pulse_banner_1795442145733),
            contentDescription = "Market analysis engine ready backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.94f),
                            ObsidianBg.copy(alpha = 0.78f),
                            ObsidianBg.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = QuantCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI SIGNAL ENGINE STANDBY",
                        style = MaterialTheme.typography.labelLarge,
                        color = QuantCyan
                    )
                }
                Text(
                    text = "Closed M1 Candle Verification Ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Tap Analyze & Generate Signal to evaluate all 18 market inputs, multi-indicator confluence, and 1-minute expiry discipline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SignalAnalyzingCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SlateSurface)
            .border(1.dp, QuantCyan.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            color = QuantCyan,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "Evaluating Closed M1 Structure & Multi-Indicator Confluence...",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = "Checking EMA 5/9/20/50, RSI(14), MACD, Bollinger Bands, ADX, ATR, Stochastic, Spread & News Filter",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SignalErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BearCrimsonMuted.copy(alpha = 0.35f))
            .border(1.dp, BearCrimson.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Signal error",
                tint = BearCrimson
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Signal Analysis Error",
                style = MaterialTheme.typography.titleSmall,
                color = BearCrimson
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        OutlinedButton(onClick = onRetry) {
            Text("Retry Analysis", color = TextPrimary)
        }
    }
}

@Composable
fun AiSignalVerdictCard(
    signal: AiSignalResult,
    remainingExpirySec: Int,
    onMarkOutcome: (Long, SignalOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (signal.direction) {
        SignalDirection.CALL -> BullEmerald
        SignalDirection.PUT -> BearCrimson
        SignalDirection.NO_TRADE -> WarnAmber
    }
    val mutedBg = when (signal.direction) {
        SignalDirection.CALL -> BullEmeraldMuted.copy(alpha = 0.32f)
        SignalDirection.PUT -> BearCrimsonMuted.copy(alpha = 0.32f)
        SignalDirection.NO_TRADE -> WarnAmberMuted.copy(alpha = 0.35f)
    }
    val directionIcon = when (signal.direction) {
        SignalDirection.CALL -> Icons.AutoMirrored.Filled.TrendingUp
        SignalDirection.PUT -> Icons.AutoMirrored.Filled.TrendingDown
        SignalDirection.NO_TRADE -> Icons.Default.Shield
    }

    val progressTarget = if (signal.expirySeconds > 0 && remainingExpirySec > 0) {
        remainingExpirySec.toFloat() / signal.expirySeconds.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(),
        label = "expiryProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SlateSurface)
            .border(1.5.dp, accentColor.copy(alpha = 0.75f), MaterialTheme.shapes.large)
            .padding(16.dp)
            .testTag("ai_signal_verdict_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Verdict Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(mutedBg)
                        .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = signal.direction.label,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = signal.direction.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${signal.pairDisplay} • ${signal.trendClassification}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${signal.confidenceScore}%",
                    style = MonoPriceMedium,
                    color = accentColor
                )
                Text(
                    text = "CONFIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Live Expiry Countdown when active CALL or PUT
        AnimatedVisibility(visible = signal.direction != SignalDirection.NO_TRADE) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (remainingExpirySec > 0) "EXPIRY WINDOW ACTIVE" else "EXPIRY WINDOW COMPLETED",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (remainingExpirySec > 0) "${remainingExpirySec}s / ${signal.expirySeconds}s" else "${signal.expirySeconds}s Closed",
                        style = MonoReadoutSmall,
                        color = accentColor
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = accentColor,
                    trackColor = BorderSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // Key Execution Telemetry Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ElevatedSurface.copy(alpha = 0.65f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCell(
                label = "ENTRY PRICE",
                value = String.format(Locale.US, "%.${signal.priceDecimals}f", signal.entryPrice),
                valueColor = TextPrimary
            )
            MetricCell(
                label = "EXPIRY",
                value = "${signal.expirySeconds}s (${signal.chartTimeframe})",
                valueColor = QuantCyan
            )
            MetricCell(
                label = "CHECKLIST",
                value = signal.checklistScoreLabel,
                valueColor = accentColor
            )
        }

        // Timestamps
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianBg.copy(alpha = 0.65f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TimestampRow("Signal Generated", signal.signalTimestampUtc)
            TimestampRow("Expected Entry", signal.executionTimestampUtc)
            TimestampRow("Trade Expiration", signal.expirationTimestampUtc)
        }

        // Executive Summary & Pattern
        Text(
            text = signal.executiveSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Pattern: ${signal.candlestickPattern}",
                style = MaterialTheme.typography.bodySmall,
                color = QuantCyan
            )
            Text(
                text = "Structure: ${signal.marketStructure} • Volatility: ${signal.volatilityAssessment}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "Risk/Payout: ${signal.riskRewardVerdict}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Reasons
        if (signal.reasons.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CONFIRMATION FACTORS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                signal.reasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = accentColor, style = MaterialTheme.typography.bodySmall)
                        Text(reason, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Warnings
        if (signal.warnings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "RISK & INVALIDATION NOTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarnAmber
                )
                signal.warnings.forEach { warn ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("! ", color = WarnAmber, style = MaterialTheme.typography.bodySmall)
                        Text(warn, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Outcome Audit Buttons for CALL / PUT
        if (signal.direction != SignalDirection.NO_TRADE && signal.id > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onMarkOutcome(signal.id, SignalOutcome.WIN) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (signal.outcome == SignalOutcome.WIN) BullEmerald else BullEmeraldMuted,
                        contentColor = if (signal.outcome == SignalOutcome.WIN) ObsidianBg else BullEmerald
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Win")
                }
                Button(
                    onClick = { onMarkOutcome(signal.id, SignalOutcome.LOSS) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (signal.outcome == SignalOutcome.LOSS) BearCrimson else BearCrimsonMuted,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Loss")
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MonoReadoutSmall, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimestampRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MonoReadoutSmall, color = TextSecondary)
    }
}
