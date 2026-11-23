package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiSignalResult
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalOutcome
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BullEmerald
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
import java.util.Locale

@Composable
fun JournalScreen(
    history: List<AiSignalResult>,
    onMarkOutcome: (Long, SignalOutcome) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wins = history.count { it.outcome == SignalOutcome.WIN }
    val losses = history.count { it.outcome == SignalOutcome.LOSS }
    val noTrades = history.count { it.direction == SignalDirection.NO_TRADE }
    val resolved = wins + losses
    val winRate = if (resolved > 0) (wins * 100.0) / resolved else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Signal Audit Journal",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_journal_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", color = TextSecondary)
                }
            }
        }

        // KPI Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(SlateSurface)
                .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KpiStat("WIN RATE", if (resolved > 0) String.format(Locale.US, "%.0f%%", winRate) else "--", BullEmerald)
            KpiStat("W / L", "$wins / $losses", TextPrimary)
            KpiStat("NO TRADE", "$noTrades", WarnAmber)
            KpiStat("TOTAL", "${history.size}", QuantCyan)
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(SlateSurface)
                    .border(1.dp, BorderSlate, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = QuantCyan,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No Signal Audits Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Every CALL, PUT, and NO TRADE analysis generated by the AI engine is automatically logged here with timestamps and indicator snapshots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    JournalHistoryCard(
                        item = item,
                        onMarkOutcome = onMarkOutcome,
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun KpiStat(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MonoPriceMedium, color = color)
    }
}

@Composable
private fun JournalHistoryCard(
    item: AiSignalResult,
    onMarkOutcome: (Long, SignalOutcome) -> Unit,
    onDelete: () -> Unit
) {
    val dirColor = when (item.direction) {
        SignalDirection.CALL -> BullEmerald
        SignalDirection.PUT -> BearCrimson
        SignalDirection.NO_TRADE -> WarnAmber
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SlateSurface)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dirColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.direction.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = dirColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${item.pairDisplay} • ${item.expirySeconds}s (${item.confidenceScore}%)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = item.signalTimestampUtc,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = TextMuted)
            }
        }

        Text(
            text = "Entry ${String.format(Locale.US, "%.${item.priceDecimals}f", item.entryPrice)} • ${item.trendClassification} • ${item.checklistScoreLabel}",
            style = MonoReadoutSmall,
            color = QuantCyan
        )
        Text(
            text = item.indicatorSummary,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        if (item.executiveSummary.isNotBlank()) {
            Text(
                text = item.executiveSummary,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Status: ${item.outcome.label}",
                style = MaterialTheme.typography.labelMedium,
                color = when (item.outcome) {
                    SignalOutcome.WIN -> BullEmerald
                    SignalOutcome.LOSS -> BearCrimson
                    SignalOutcome.SKIPPED -> WarnAmber
                    SignalOutcome.PENDING -> TextSecondary
                }
            )
            if (item.direction != SignalDirection.NO_TRADE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onMarkOutcome(item.id, SignalOutcome.WIN) }) {
                        Text("Win", color = BullEmerald)
                    }
                    TextButton(onClick = { onMarkOutcome(item.id, SignalOutcome.LOSS) }) {
                        Text("Loss", color = BearCrimson)
                    }
                }
            }
        }
    }
}
