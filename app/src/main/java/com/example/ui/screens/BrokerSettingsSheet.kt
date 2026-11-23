package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.BrokerConfig
import com.example.domain.model.NewsImpact
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BullEmerald
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarnAmber
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerSettingsBottomSheet(
    currentConfig: BrokerConfig,
    onSave: (BrokerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var payoutPct by remember(currentConfig) { mutableIntStateOf(currentConfig.payoutPct) }
    var minThreshold by remember(currentConfig) { mutableIntStateOf(currentConfig.minPayoutThresholdPct) }
    var spreadPips by remember(currentConfig) { mutableDoubleStateOf(currentConfig.spreadPips) }
    var newsImpact by remember(currentConfig) { mutableStateOf(currentConfig.newsImpact) }
    var strictM1 by remember(currentConfig) { mutableStateOf(currentConfig.strictM1MasterMode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlateSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Broker & Risk Filter Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            // Broker Payout %
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Broker Payout %", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("$payoutPct%", style = MaterialTheme.typography.titleSmall, color = QuantCyan)
                }
                Slider(
                    value = payoutPct.toFloat(),
                    onValueChange = { payoutPct = it.roundToInt() },
                    valueRange = 60f..96f,
                    colors = SliderDefaults.colors(thumbColor = QuantCyan, activeTrackColor = QuantCyan)
                )
            }

            // Min Profitability Filter
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min Profitability Filter", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("$minThreshold%", style = MaterialTheme.typography.titleSmall, color = BullEmerald)
                }
                Slider(
                    value = minThreshold.toFloat(),
                    onValueChange = { minThreshold = it.roundToInt() },
                    valueRange = 65f..90f,
                    colors = SliderDefaults.colors(thumbColor = BullEmerald, activeTrackColor = BullEmerald)
                )
            }

            // Current Spread (pips)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Broker Spread", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(String.format(Locale.US, "%.1f pips", spreadPips), style = MaterialTheme.typography.titleSmall, color = WarnAmber)
                }
                Slider(
                    value = spreadPips.toFloat(),
                    onValueChange = { spreadPips = (it * 10f).roundToInt() / 10.0 },
                    valueRange = 0f..5f,
                    colors = SliderDefaults.colors(thumbColor = WarnAmber, activeTrackColor = WarnAmber)
                )
            }

            // Economic News Impact
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Economic News Risk Window", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NewsImpact.entries.forEach { impact ->
                        val selected = impact == newsImpact
                        val color = if (impact == NewsImpact.HIGH) BearCrimson else QuantCyan
                        FilterChip(
                            selected = selected,
                            onClick = { newsImpact = impact },
                            label = {
                                Text(
                                    text = when (impact) {
                                        NewsImpact.NONE -> "Clear"
                                        NewsImpact.LOW -> "Low"
                                        NewsImpact.MEDIUM -> "Med"
                                        NewsImpact.HIGH -> "High"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                                containerColor = ElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = newsImpact.riskNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (newsImpact == NewsImpact.HIGH) BearCrimson else TextSecondary
                )
            }

            // Strict M1 Master Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Strict 1m (60s) Master Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Enforces exact 1-minute chart & 60s expiry discipline",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = strictM1,
                    onCheckedChange = { strictM1 = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = BullEmerald
                    )
                )
            }

            Button(
                onClick = {
                    onSave(
                        BrokerConfig(
                            payoutPct = payoutPct,
                            spreadPips = spreadPips,
                            newsImpact = newsImpact,
                            minPayoutThresholdPct = minThreshold,
                            strictM1MasterMode = strictM1
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = QuantCyan, contentColor = ObsidianBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_broker_settings_button")
            ) {
                Text("Apply Risk Rules", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
