package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BrokerConfig
import com.example.domain.model.CandleTimeframe
import com.example.domain.model.MarketFeedSnapshot
import com.example.domain.model.MarketPair
import com.example.domain.model.SignalOutcome
import com.example.domain.model.TradeExpiry
import com.example.domain.model.TradingPairsCatalog
import com.example.domain.model.TrendClassification
import com.example.ui.MarketFeedUiState
import com.example.ui.SignalUiState
import com.example.ui.components.CandlestickChartCard
import com.example.ui.components.SignalResultSection
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BullEmerald
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MonoPriceLarge
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
fun TerminalScreen(
    selectedPair: MarketPair,
    selectedTimeframe: CandleTimeframe,
    selectedExpiry: TradeExpiry,
    brokerConfig: BrokerConfig,
    favoritePairIds: Set<String>,
    marketFeedState: MarketFeedUiState,
    signalState: SignalUiState,
    remainingExpirySec: Int,
    onOpenPairPicker: () -> Unit,
    onOpenBrokerSettings: () -> Unit,
    onSelectPair: (MarketPair) -> Unit,
    onSelectTimeframe: (CandleTimeframe) -> Unit,
    onSelectExpiry: (TradeExpiry) -> Unit,
    onRefreshFeed: () -> Unit,
    onGenerateSignal: () -> Unit,
    onMarkOutcome: (Long, SignalOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isWide = maxWidth >= 820.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TerminalTopHeader(
                        selectedPair = selectedPair,
                        brokerConfig = brokerConfig,
                        marketFeedState = marketFeedState,
                        onOpenPairPicker = onOpenPairPicker,
                        onOpenBrokerSettings = onOpenBrokerSettings,
                        onRefreshFeed = onRefreshFeed
                    )
                    QuickFavoritePairsRow(
                        selectedPair = selectedPair,
                        favoritePairIds = favoritePairIds,
                        onSelectPair = onSelectPair
                    )
                    TimeframeAndExpiryBar(
                        selectedTimeframe = selectedTimeframe,
                        selectedExpiry = selectedExpiry,
                        strictM1Mode = brokerConfig.strictM1MasterMode,
                        onSelectTimeframe = onSelectTimeframe,
                        onSelectExpiry = onSelectExpiry
                    )
                    ChartSection(
                        marketFeedState = marketFeedState,
                        onRetry = onRefreshFeed
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (marketFeedState is MarketFeedUiState.Ready) {
                        QuickTelemetryStrip(snapshot = marketFeedState.snapshot)
                    }
                    AnalyzeSignalButton(
                        isAnalyzing = signalState is SignalUiState.Analyzing,
                        onClick = onGenerateSignal
                    )
                    SignalResultSection(
                        signalState = signalState,
                        remainingExpirySec = remainingExpirySec,
                        onRetryAnalyze = onGenerateSignal,
                        onMarkOutcome = onMarkOutcome
                    )
                    DisclaimerFooter()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerminalTopHeader(
                    selectedPair = selectedPair,
                    brokerConfig = brokerConfig,
                    marketFeedState = marketFeedState,
                    onOpenPairPicker = onOpenPairPicker,
                    onOpenBrokerSettings = onOpenBrokerSettings,
                    onRefreshFeed = onRefreshFeed
                )
                QuickFavoritePairsRow(
                    selectedPair = selectedPair,
                    favoritePairIds = favoritePairIds,
                    onSelectPair = onSelectPair
                )
                TimeframeAndExpiryBar(
                    selectedTimeframe = selectedTimeframe,
                    selectedExpiry = selectedExpiry,
                    strictM1Mode = brokerConfig.strictM1MasterMode,
                    onSelectTimeframe = onSelectTimeframe,
                    onSelectExpiry = onSelectExpiry
                )
                ChartSection(
                    marketFeedState = marketFeedState,
                    onRetry = onRefreshFeed
                )
                if (marketFeedState is MarketFeedUiState.Ready) {
                    QuickTelemetryStrip(snapshot = marketFeedState.snapshot)
                }
                AnalyzeSignalButton(
                    isAnalyzing = signalState is SignalUiState.Analyzing,
                    onClick = onGenerateSignal
                )
                SignalResultSection(
                    signalState = signalState,
                    remainingExpirySec = remainingExpirySec,
                    onRetryAnalyze = onGenerateSignal,
                    onMarkOutcome = onMarkOutcome
                )
                DisclaimerFooter()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TerminalTopHeader(
    selectedPair: MarketPair,
    brokerConfig: BrokerConfig,
    marketFeedState: MarketFeedUiState,
    onOpenPairPicker: () -> Unit,
    onOpenBrokerSettings: () -> Unit,
    onRefreshFeed: () -> Unit
) {
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
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenPairPicker)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("pair_selector_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selectedPair.isOtc) WarnAmber.copy(alpha = 0.2f) else BullEmerald.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (selectedPair.isOtc) "OTC" else "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedPair.isOtc) WarnAmber else BullEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedPair.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Choose pair",
                    tint = QuantCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefreshFeed,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh_feed),
                        tint = QuantCyan
                    )
                }
                IconButton(
                    onClick = onOpenBrokerSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("broker_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.action_broker_settings),
                        tint = TextPrimary
                    )
                }
            }
        }

        // Price + Feed Status Row
        when (marketFeedState) {
            is MarketFeedUiState.Ready -> {
                val snap = marketFeedState.snapshot
                val up = snap.priceChangePct >= 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = snap.pair.formatPrice(snap.currentPrice),
                            style = MonoPriceLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = snap.feedSourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (snap.isLiveExchangeFeed) BullEmerald else WarnAmber
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%+.3f%%", snap.priceChangePct),
                            style = MonoReadoutSmall,
                            color = if (up) BullEmerald else BearCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Payout ${brokerConfig.payoutPct}% • Spread ${brokerConfig.spreadPips}p",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            is MarketFeedUiState.Loading -> {
                Text(
                    text = "Syncing live market OHLC candles...",
                    style = MaterialTheme.typography.bodySmall,
                    color = QuantCyan
                )
            }
            is MarketFeedUiState.Error -> {
                Text(
                    text = "Feed offline — tap Refresh",
                    style = MaterialTheme.typography.bodySmall,
                    color = BearCrimson
                )
            }
        }
    }
}

@Composable
private fun QuickFavoritePairsRow(
    selectedPair: MarketPair,
    favoritePairIds: Set<String>,
    onSelectPair: (MarketPair) -> Unit
) {
    val quickPairs = TradingPairsCatalog.allPairs.filter { it.id in favoritePairIds || it.id == selectedPair.id }
    if (quickPairs.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPairs.forEach { pair ->
            val selected = pair.id == selectedPair.id
            FilterChip(
                selected = selected,
                onClick = { onSelectPair(pair) },
                label = {
                    Text(
                        text = pair.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QuantCyan.copy(alpha = 0.18f),
                    selectedLabelColor = QuantCyan,
                    containerColor = SlateSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = BorderSlate,
                    selectedBorderColor = QuantCyan
                )
            )
        }
    }
}

@Composable
private fun TimeframeAndExpiryBar(
    selectedTimeframe: CandleTimeframe,
    selectedExpiry: TradeExpiry,
    strictM1Mode: Boolean,
    onSelectTimeframe: (CandleTimeframe) -> Unit,
    onSelectExpiry: (TradeExpiry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SlateSurface)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CANDLE TIMEFRAME",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = if (strictM1Mode) "MASTER M1 (60s) LOCKED" else "CUSTOM SCALP MODE",
                style = MaterialTheme.typography.labelSmall,
                color = if (strictM1Mode) BullEmerald else WarnAmber
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CandleTimeframe.entries.forEach { tf ->
                val isSelected = tf == selectedTimeframe
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTimeframe(tf) },
                    label = {
                        Text(
                            text = if (tf.isPrimaryM1) "1m (Primary)" else tf.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QuantCyan.copy(alpha = 0.2f),
                        selectedLabelColor = QuantCyan,
                        containerColor = ElevatedSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Text(
            text = "TRADE EXPIRATION",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TradeExpiry.entries.forEach { exp ->
                val isSelected = exp == selectedExpiry
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectExpiry(exp) },
                    label = {
                        Text(
                            text = exp.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BullEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = BullEmerald,
                        containerColor = ElevatedSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ChartSection(
    marketFeedState: MarketFeedUiState,
    onRetry: () -> Unit
) {
    when (marketFeedState) {
        is MarketFeedUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(SlateSurface)
                    .border(1.dp, BorderSlate, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = QuantCyan)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading 1m Market Candles...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        is MarketFeedUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(SlateSurface)
                    .border(1.dp, BearCrimson.copy(alpha = 0.6f), MaterialTheme.shapes.large)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = marketFeedState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BearCrimson
                )
                OutlinedButton(onClick = onRetry) {
                    Text("Retry Live Feed", color = TextPrimary)
                }
            }
        }
        is MarketFeedUiState.Ready -> {
            CandlestickChartCard(snapshot = marketFeedState.snapshot)
        }
    }
}

@Composable
private fun QuickTelemetryStrip(snapshot: MarketFeedSnapshot) {
    val ind = snapshot.indicators
    val trendColor = when (ind.trendClassification) {
        TrendClassification.STRONG_BULLISH, TrendClassification.BULLISH -> BullEmerald
        TrendClassification.STRONG_BEARISH, TrendClassification.BEARISH -> BearCrimson
        else -> WarnAmber
    }
    val biasColor = when {
        ind.callPassedCount > ind.putPassedCount -> BullEmerald
        ind.putPassedCount > ind.callPassedCount -> BearCrimson
        else -> WarnAmber
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SlateSurface)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryMini("TREND", ind.trendClassification.label, trendColor)
            TelemetryMini("RSI(14)", String.format(Locale.US, "%.1f", ind.rsi14), QuantCyan)
            TelemetryMini("ADX(14)", String.format(Locale.US, "%.1f", ind.adx14), if (ind.adx14 >= 20) BullEmerald else WarnAmber)
            TelemetryMini("BIAS", ind.biasLabel, biasColor)
        }

        // Live UP vs DOWN directional pressure bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UP ▲ ${ind.upBiasPct}% (${ind.callPassedCount}/14)",
                    style = MaterialTheme.typography.labelSmall,
                    color = BullEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DOWN ▼ ${ind.downBiasPct}% (${ind.putPassedCount}/14)",
                    style = MaterialTheme.typography.labelSmall,
                    color = BearCrimson,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BearCrimson.copy(alpha = 0.75f))
            ) {
                val upFrac = (ind.upBiasPct / 100f).coerceIn(0.05f, 0.95f)
                Box(
                    modifier = Modifier
                        .weight(upFrac)
                        .height(6.dp)
                        .background(BullEmerald)
                )
                Box(
                    modifier = Modifier
                        .weight((1f - upFrac).coerceAtLeast(0.05f))
                        .height(6.dp)
                        .background(BearCrimson)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMini(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MonoReadoutSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnalyzeSignalButton(
    isAnalyzing: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isAnalyzing,
        colors = ButtonDefaults.buttonColors(
            containerColor = QuantCyan,
            contentColor = ObsidianBg,
            disabledContainerColor = QuantCyan.copy(alpha = 0.45f),
            disabledContentColor = ObsidianBg
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(QuantCyan, BullEmerald)),
                shape = MaterialTheme.shapes.medium
            )
            .testTag("generate_signal_button")
    ) {
        if (isAnalyzing) {
            CircularProgressIndicator(
                color = ObsidianBg,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Analyzing 18 Market Inputs...",
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_analyze_signal),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DisclaimerFooter() {
    Text(
        text = stringResource(R.string.label_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
