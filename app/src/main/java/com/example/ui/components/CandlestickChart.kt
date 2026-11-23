package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Candle
import com.example.domain.model.MarketFeedSnapshot
import com.example.ui.theme.BearCrimson
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BullEmerald
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MonoReadoutSmall
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarnAmber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun CandlestickChartCard(
    snapshot: MarketFeedSnapshot,
    modifier: Modifier = Modifier
) {
    var showEmas by remember { mutableStateOf(true) }
    var showBollinger by remember { mutableStateOf(true) }
    var showSubPane by remember { mutableStateOf(true) }
    var inspectedIndex by remember { mutableStateOf<Int?>(null) }

    val candles = remember(snapshot.displayCandles) {
        snapshot.displayCandles.takeLast(44)
    }
    val offsetFromSeries = remember(snapshot.closedM1Candles, candles) {
        (snapshot.closedM1Candles.size - candles.size).coerceAtLeast(0)
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SlateSurface)
            .border(1.dp, BorderSlate, MaterialTheme.shapes.large)
            .padding(12.dp)
            .testTag("candlestick_chart_card")
    ) {
        // Header + overlay toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val inspectedCandle = inspectedIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()
                if (inspectedCandle != null) {
                    val pair = snapshot.pair
                    Text(
                        text = "O ${pair.formatPrice(inspectedCandle.open)}  H ${pair.formatPrice(inspectedCandle.high)}  L ${pair.formatPrice(inspectedCandle.low)}  C ${pair.formatPrice(inspectedCandle.close)}",
                        style = MonoReadoutSmall,
                        color = if (inspectedCandle.isBullish) BullEmerald else BearCrimson
                    )
                    Text(
                        text = "${snapshot.chartTimeframe.label} • ${timeFormatter.format(Instant.ofEpochSecond(inspectedCandle.timestampEpochSec))} UTC",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OverlayToggleChip(
                    label = "EMA",
                    selected = showEmas,
                    accent = QuantCyan,
                    onClick = { showEmas = !showEmas }
                )
                OverlayToggleChip(
                    label = "BB",
                    selected = showBollinger,
                    accent = IndigoAccent,
                    onClick = { showBollinger = !showBollinger }
                )
                OverlayToggleChip(
                    label = "RSI/MACD",
                    selected = showSubPane,
                    accent = WarnAmber,
                    onClick = { showSubPane = !showSubPane }
                )
            }
        }

        if (showEmas) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot("EMA5", QuantCyan)
                LegendDot("EMA9", WarnAmber)
                LegendDot("EMA20", IndigoAccent)
                LegendDot("EMA50", TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Candlestick Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElevatedSurface.copy(alpha = 0.45f))
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        if (candles.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * candles.size)
                                .roundToInt()
                                .coerceIn(0, candles.lastIndex)
                            inspectedIndex = if (inspectedIndex == idx) null else idx
                        }
                    }
                }
                .pointerInput(candles) {
                    detectHorizontalDragGestures(
                        onDragEnd = { inspectedIndex = null },
                        onHorizontalDrag = { change, _ ->
                            if (candles.isNotEmpty()) {
                                val idx = ((change.position.x / size.width) * candles.size)
                                    .roundToInt()
                                    .coerceIn(0, candles.lastIndex)
                                inspectedIndex = idx
                            }
                        }
                    )
                }
        ) {
            CandlestickCanvas(
                candles = candles,
                snapshot = snapshot,
                seriesOffset = offsetFromSeries,
                showEmas = showEmas,
                showBollinger = showBollinger,
                inspectedIndex = inspectedIndex,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showSubPane) {
            Spacer(modifier = Modifier.height(8.dp))
            RsiMacdSubChart(
                snapshot = snapshot,
                visibleCount = candles.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
        }
    }
}

@Composable
private fun OverlayToggleChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.18f),
            selectedLabelColor = accent,
            containerColor = ElevatedSurface,
            labelColor = TextMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BorderSlate,
            selectedBorderColor = accent.copy(alpha = 0.6f)
        ),
        modifier = Modifier.height(30.dp)
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun CandlestickCanvas(
    candles: List<Candle>,
    snapshot: MarketFeedSnapshot,
    seriesOffset: Int,
    showEmas: Boolean,
    showBollinger: Boolean,
    inspectedIndex: Int?,
    modifier: Modifier = Modifier
) {
    val isM1Aligned = snapshot.chartTimeframe.isPrimaryM1
    val series = snapshot.indicators.series

    Canvas(modifier = modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
        if (candles.isEmpty()) return@Canvas

        var minPrice = candles.minOf { it.low }
        var maxPrice = candles.maxOf { it.high }

        if (showBollinger && isM1Aligned) {
            for (i in candles.indices) {
                val sIdx = seriesOffset + i
                series.bbUpper.getOrNull(sIdx)?.let { maxPrice = maxOf(maxPrice, it) }
                series.bbLower.getOrNull(sIdx)?.let { minPrice = minOf(minPrice, it) }
            }
        }

        val pad = (maxPrice - minPrice).coerceAtLeast(snapshot.pair.pipSize * 5.0) * 0.10
        minPrice -= pad
        maxPrice += pad
        val priceSpan = (maxPrice - minPrice).coerceAtLeast(1e-9)

        fun yFor(price: Double): Float {
            val norm = ((price - minPrice) / priceSpan).toFloat()
            return size.height * (1f - norm)
        }

        // Subtle horizontal grid lines
        val gridCount = 4
        for (g in 0..gridCount) {
            val gy = size.height * (g.toFloat() / gridCount)
            drawLine(
                color = BorderSlate.copy(alpha = 0.45f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1f
            )
        }

        val stepX = size.width / candles.size.toFloat()
        val bodyWidth = (stepX * 0.62f).coerceIn(3f, 18f)

        // Bollinger Band fill & lines (on M1 series)
        if (showBollinger && isM1Aligned && candles.size > 2) {
            drawIndicatorLine(candles.indices, seriesOffset, series.bbUpper, stepX, ::yFor, IndigoAccent.copy(alpha = 0.45f), 1.5f)
            drawIndicatorLine(candles.indices, seriesOffset, series.bbMiddle, stepX, ::yFor, IndigoAccent.copy(alpha = 0.30f), 1.2f)
            drawIndicatorLine(candles.indices, seriesOffset, series.bbLower, stepX, ::yFor, IndigoAccent.copy(alpha = 0.45f), 1.5f)
        }

        // Candles
        candles.forEachIndexed { idx, candle ->
            val cx = idx * stepX + stepX / 2f
            val color = if (candle.isBullish) BullEmerald else BearCrimson

            val yHigh = yFor(candle.high)
            val yLow = yFor(candle.low)
            val yOpen = yFor(candle.open)
            val yClose = yFor(candle.close)

            // Wick
            drawLine(
                color = color.copy(alpha = if (candle.isClosed) 0.9f else 0.6f),
                start = Offset(cx, yHigh),
                end = Offset(cx, yLow),
                strokeWidth = 2f
            )

            // Body
            val top = minOf(yOpen, yClose)
            val bodyH = kotlin.math.abs(yOpen - yClose).coerceAtLeast(2.5f)
            drawRect(
                color = color.copy(alpha = if (candle.isClosed) 0.95f else 0.55f),
                topLeft = Offset(cx - bodyWidth / 2f, top),
                size = Size(bodyWidth, bodyH)
            )
        }

        // EMAs on M1 series
        if (showEmas && isM1Aligned) {
            drawIndicatorLine(candles.indices, seriesOffset, series.ema50, stepX, ::yFor, TextSecondary.copy(alpha = 0.7f), 2.2f)
            drawIndicatorLine(candles.indices, seriesOffset, series.ema20, stepX, ::yFor, IndigoAccent, 2.4f)
            drawIndicatorLine(candles.indices, seriesOffset, series.ema9, stepX, ::yFor, WarnAmber, 2.4f)
            drawIndicatorLine(candles.indices, seriesOffset, series.ema5, stepX, ::yFor, QuantCyan, 2.6f)
        }

        // Current live price dashed line
        val liveY = yFor(snapshot.currentPrice)
        drawLine(
            color = QuantCyan.copy(alpha = 0.75f),
            start = Offset(0f, liveY),
            end = Offset(size.width, liveY),
            strokeWidth = 1.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )

        // Crosshair if user is inspecting a candle
        if (inspectedIndex != null && inspectedIndex in candles.indices) {
            val ix = inspectedIndex * stepX + stepX / 2f
            val iy = yFor(candles[inspectedIndex].close)
            drawLine(
                color = TextPrimary.copy(alpha = 0.5f),
                start = Offset(ix, 0f),
                end = Offset(ix, size.height),
                strokeWidth = 1.4f
            )
            drawCircle(
                color = TextPrimary,
                radius = 5f,
                center = Offset(ix, iy)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIndicatorLine(
    indices: IntRange,
    seriesOffset: Int,
    values: List<Double?>,
    stepX: Float,
    yFor: (Double) -> Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    var started = false
    for (i in indices) {
        val v = values.getOrNull(seriesOffset + i) ?: continue
        val x = i * stepX + stepX / 2f
        val y = yFor(v)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    if (started) {
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
    }
}

@Composable
private fun RsiMacdSubChart(
    snapshot: MarketFeedSnapshot,
    visibleCount: Int,
    modifier: Modifier = Modifier
) {
    val ind = snapshot.indicators
    val rsiSlice = ind.series.rsi14.takeLast(visibleCount)
    val macdSlice = ind.series.macdHistogram.takeLast(visibleCount)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RSI(14): ${String.format(java.util.Locale.US, "%.1f", ind.rsi14)}",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    ind.rsi14 >= 70.0 -> BearCrimson
                    ind.rsi14 <= 30.0 -> BullEmerald
                    else -> QuantCyan
                }
            )
            Text(
                text = "MACD Hist: ${String.format(java.util.Locale.US, "%+.5f", ind.macdHistogram)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (ind.macdHistogram >= 0) BullEmerald else BearCrimson
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (rsiSlice.isEmpty()) return@Canvas
            val stepX = size.width / rsiSlice.size.toFloat()

            // RSI 70 and 30 lines
            val y70 = size.height * 0.30f
            val y30 = size.height * 0.70f
            drawLine(BorderSlate, Offset(0f, y70), Offset(size.width, y70), 1f)
            drawLine(BorderSlate, Offset(0f, y30), Offset(size.width, y30), 1f)

            // MACD Histogram bars centered vertically
            val maxHist = macdSlice.filterNotNull().maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1e-7) ?: 1e-5
            val midY = size.height / 2f
            macdSlice.forEachIndexed { idx, h ->
                if (h != null) {
                    val barH = ((kotlin.math.abs(h) / maxHist) * (size.height * 0.42f)).toFloat()
                    val cx = idx * stepX + stepX / 2f
                    val barW = (stepX * 0.45f).coerceAtLeast(2f)
                    val top = if (h >= 0) midY - barH else midY
                    drawRect(
                        color = (if (h >= 0) BullEmerald else BearCrimson).copy(alpha = 0.38f),
                        topLeft = Offset(cx - barW / 2f, top),
                        size = Size(barW, barH.coerceAtLeast(1.5f))
                    )
                }
            }

            // RSI Line
            val rsiPath = Path()
            var started = false
            rsiSlice.forEachIndexed { idx, r ->
                if (r != null) {
                    val x = idx * stepX + stepX / 2f
                    val y = size.height * (1f - (r.toFloat().coerceIn(0f, 100f) / 100f))
                    if (!started) {
                        rsiPath.moveTo(x, y)
                        started = true
                    } else {
                        rsiPath.lineTo(x, y)
                    }
                }
            }
            if (started) {
                drawPath(rsiPath, color = QuantCyan, style = Stroke(width = 2.2f))
            }
        }
    }
}
