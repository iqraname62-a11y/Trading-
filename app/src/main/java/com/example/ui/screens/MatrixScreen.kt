package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.MarketFeedSnapshot
import com.example.domain.model.MarketPair
import com.example.domain.model.StrategyCondition
import com.example.domain.model.TrendClassification
import com.example.ui.MarketFeedUiState
import com.example.ui.ScannerUiState
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BullEmerald
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MonoReadoutSmall
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarnAmber
import java.util.Locale

@Composable
fun MatrixScreen(
    marketFeedState: MarketFeedUiState,
    scannerState: ScannerUiState,
    onRunScanner: () -> Unit,
    onJumpToPair: (MarketPair) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCallChecklist by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Technical Matrix & Confluence",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )

        if (marketFeedState is MarketFeedUiState.Ready) {
            val snapshot = marketFeedState.snapshot
            IndicatorsGrid(snapshot = snapshot)

            // 14-Point CALL vs PUT Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(SlateSurface)
                    .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "14-Point Strategy Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = showCallChecklist,
                            onClick = { showCallChecklist = true },
                            label = { Text("CALL (${snapshot.indicators.callPassedCount}/14)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BullEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = BullEmerald,
                                containerColor = ElevatedSurface,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = !showCallChecklist,
                            onClick = { showCallChecklist = false },
                            label = { Text("PUT (${snapshot.indicators.putPassedCount}/14)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BearCrimson.copy(alpha = 0.2f),
                                selectedLabelColor = BearCrimson,
                                containerColor = ElevatedSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                val list = if (showCallChecklist) snapshot.indicators.callChecklist else snapshot.indicators.putChecklist
                val accent = if (showCallChecklist) BullEmerald else BearCrimson
                list.forEach { condition ->
                    ChecklistItemRow(condition = condition, accentColor = accent)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(SlateSurface)
                    .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Load a market pair on the Terminal tab to inspect live EMA, RSI, MACD, Bollinger Bands, ADX, ATR, Stochastic, and VWAP metrics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Multi-Pair Quick Scanner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(SlateSurface)
                .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Favorite Pairs Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ranks starred Live & OTC pairs by 14-point M1 confluence",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onRunScanner,
                    enabled = scannerState !is ScannerUiState.Scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = QuantCyan, contentColor = ObsidianBg),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("run_scanner_button")
                ) {
                    if (scannerState is ScannerUiState.Scanning) {
                        CircularProgressIndicator(
                            color = ObsidianBg,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan")
                    }
                }
            }

            when (scannerState) {
                is ScannerUiState.Idle -> {
                    Text(
                        text = "Tap Scan to evaluate your favorite pairs simultaneously.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                is ScannerUiState.Scanning -> {
                    Text(
                        text = "Fetching M1 candles across your favorite pairs...",
                        style = MaterialTheme.typography.bodySmall,
                        color = QuantCyan
                    )
                }
                is ScannerUiState.Error -> {
                    Text(
                        text = scannerState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = BearCrimson
                    )
                }
                is ScannerUiState.Ready -> {
                    scannerState.items.forEach { item ->
                        val bestIsCall = item.callScore >= item.putScore
                        val badgeColor = when (item.trend) {
                            TrendClassification.STRONG_BULLISH, TrendClassification.BULLISH -> BullEmerald
                            TrendClassification.STRONG_BEARISH, TrendClassification.BEARISH -> BearCrimson
                            else -> WarnAmber
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                                .clickable { onJumpToPair(item.pair) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.pair.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${item.pair.formatPrice(item.price)} • ${item.trend.label}",
                                    style = MonoReadoutSmall,
                                    color = badgeColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (bestIsCall) "CALL ${item.callScore}/14" else "PUT ${item.putScore}/14",
                                    style = MonoReadoutSmall,
                                    color = if (bestIsCall) BullEmerald else BearCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "RSI %.1f • ADX %.1f", item.rsi14, item.adx14),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun IndicatorsGrid(snapshot: MarketFeedSnapshot) {
    val pair = snapshot.pair
    val ind = snapshot.indicators

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IndicatorTile(
                title = "EMA Stack (5/9/20/50)",
                primaryValue = "${pair.formatPrice(ind.ema5)} / ${pair.formatPrice(ind.ema9)}",
                secondaryValue = "20: ${pair.formatPrice(ind.ema20)} • 50: ${pair.formatPrice(ind.ema50)}",
                accentColor = if (ind.ema5 > ind.ema9 && ind.ema9 > ind.ema20) BullEmerald
                else if (ind.ema5 < ind.ema9 && ind.ema9 < ind.ema20) BearCrimson else WarnAmber,
                modifier = Modifier.weight(1f)
            )
            IndicatorTile(
                title = "RSI (14)",
                primaryValue = String.format(Locale.US, "%.2f", ind.rsi14),
                secondaryValue = when {
                    ind.rsi14 >= 70.0 -> "Overbought (>=70)"
                    ind.rsi14 <= 30.0 -> "Oversold (<=30)"
                    ind.rsi14 in 50.0..65.0 -> "Bullish Zone (50-65)"
                    ind.rsi14 in 35.0..50.0 -> "Bearish Zone (35-50)"
                    else -> "Extended Zone"
                },
                accentColor = if (ind.rsi14 >= 50.0 && ind.rsi14 < 70.0) BullEmerald
                else if (ind.rsi14 < 50.0 && ind.rsi14 > 30.0) BearCrimson else WarnAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IndicatorTile(
                title = "MACD (12, 26, 9)",
                primaryValue = String.format(Locale.US, "%+.6f", ind.macdHistogram),
                secondaryValue = "Line: ${String.format(Locale.US, "%.5f", ind.macdLine)} | Sig: ${String.format(Locale.US, "%.5f", ind.macdSignal)}",
                accentColor = if (ind.macdHistogram >= 0) BullEmerald else BearCrimson,
                modifier = Modifier.weight(1f)
            )
            IndicatorTile(
                title = "Bollinger (20, 2)",
                primaryValue = "${pair.formatPrice(ind.bbUpper)} / ${pair.formatPrice(ind.bbLower)}",
                secondaryValue = "Mid: ${pair.formatPrice(ind.bbMiddle)} • Width ${String.format(Locale.US, "%.3f", ind.bbBandwidthPct)}%",
                accentColor = QuantCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IndicatorTile(
                title = "ADX (14)",
                primaryValue = String.format(Locale.US, "%.1f", ind.adx14),
                secondaryValue = String.format(Locale.US, "+DI %.1f • -DI %.1f (%s)", ind.plusDi14, ind.minusDi14, if (ind.adx14 >= 20.0) "Trending" else "Weak"),
                accentColor = if (ind.adx14 >= 20.0) BullEmerald else WarnAmber,
                modifier = Modifier.weight(1f)
            )
            IndicatorTile(
                title = "Stochastic (14, 3, 3)",
                primaryValue = String.format(Locale.US, "%%K %.1f / %%D %.1f", ind.stochK, ind.stochD),
                secondaryValue = if (ind.stochK > ind.stochD) "Bullish Cross" else "Bearish Cross",
                accentColor = if (ind.stochK > ind.stochD) BullEmerald else BearCrimson,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IndicatorTile(
                title = "ATR (14) Volatility",
                primaryValue = pair.formatPrice(ind.atr14),
                secondaryValue = "${ind.volatilityRegime} (${String.format(Locale.US, "%.4f", ind.atrPct)}%)",
                accentColor = if (ind.volatilityRegime == "OPTIMAL SHORT-TERM") QuantCyan else WarnAmber,
                modifier = Modifier.weight(1f)
            )
            IndicatorTile(
                title = "Intraday VWAP",
                primaryValue = ind.vwap?.let { pair.formatPrice(it) } ?: "N/A (Spot)",
                secondaryValue = if (ind.vwap != null) "Volume-weighted anchor" else "No exchange volume ticks",
                accentColor = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IndicatorTile(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SlateSurface)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = primaryValue,
            style = MonoReadoutSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = secondaryValue,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ChecklistItemRow(
    condition: StrategyCondition,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (condition.passed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (condition.passed) accentColor else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${condition.index}. ${condition.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (condition.passed) TextPrimary else TextSecondary,
                    fontWeight = if (condition.passed) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = condition.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (condition.passed) accentColor else TextMuted
                )
            }
        }
    }
}
