package com.plateau.app.ui.progress.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.plateau.app.domain.model.WeeklyVolumePoint
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing

/**
 * Volume load settimanale: una barra per settimana, linea tratteggiata `primary` al target.
 * Barre in `extras.chartProgress` se raggiungono il target, in `primary` se sotto.
 * Tocca o tieni premuto una barra per vedere settimana, volume e % del target.
 * Senza volume registrato mostra uno stato vuoto e ignora i tocchi.
 */
@Composable
fun VolumeChart(
    weeklyData: List<WeeklyVolumePoint>,
    targetVolume: Double,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(weeklyData, targetVolume) {
        maxOf(weeklyData.maxOfOrNull { it.volume } ?: 0.0, targetVolume)
    }
    if (weeklyData.isEmpty() || weeklyData.all { it.volume <= 0.0 } || maxValue <= 0.0) {
        EmptyChart("Nessun volume registrato", Size.chartHeightCompact, modifier)
        return
    }
    val top = maxValue * 1.1

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outline
    val reached = PlateauTheme.extras.chartProgress
    val below = MaterialTheme.colorScheme.primary
    val selectedRing = MaterialTheme.colorScheme.onSurface

    val topLabel = measurer.measure(formatKg(top), labelStyle)
    val zeroLabel = measurer.measure("0", labelStyle)
    val labelEvery = if (weeklyData.size > 6) 2 else 1
    val xLabels = weeklyData.map { measurer.measure(shortDate(it.weekStart), labelStyle) }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember(weeklyData) { mutableStateOf<Int?>(null) }

    val gapPx = with(density) { Spacing.sm.toPx() }
    val area = PlotArea(
        left = maxOf(topLabel.size.width, zeroLabel.size.width) + gapPx,
        top = topLabel.size.height / 2f,
        right = size.width.toFloat(),
        bottom = size.height - xLabels.first().size.height - gapPx
    )
    val slot = if (weeklyData.isEmpty()) 0f else area.width / weeklyData.size
    fun yOf(value: Double) = area.bottom - area.height * (value / top).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Size.chartHeightCompact)
            .onSizeChanged { size = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Grafico volume: ${weeklyData.size} settimane, " +
                        "ultima ${formatKg(weeklyData.last().volume)} kg"
                }
                .pointerInput(weeklyData, size) {
                    val select: (Offset) -> Unit = { tap ->
                        selected = barIndexAt(tap.x - area.left, area.width, weeklyData.size)
                    }
                    detectTapGestures(onTap = select, onLongPress = select)
                }
        ) {
            if (area.width <= 0f || area.height <= 0f) return@Canvas
            val grid = Size.chartGrid.toPx()
            drawLine(gridColor, Offset(area.left, area.top), Offset(area.right, area.top), grid)
            drawLine(gridColor, Offset(area.left, area.bottom), Offset(area.right, area.bottom), grid)
            drawText(topLabel, topLeft = Offset(0f, area.top - topLabel.size.height / 2f))
            drawText(zeroLabel, topLeft = Offset(0f, area.bottom - zeroLabel.size.height / 2f))

            val barGap = Spacing.xs.toPx()
            weeklyData.forEachIndexed { i, week ->
                val x = area.left + slot * i
                if (week.volume > 0.0) {
                    val y = yOf(week.volume)
                    val color = if (targetVolume > 0.0 && week.volume >= targetVolume) reached else below
                    drawRect(color, Offset(x + barGap, y), GeometrySize(slot - barGap * 2, area.bottom - y))
                }
                if (i % labelEvery == (weeklyData.size - 1) % labelEvery) {
                    val label = xLabels[i]
                    val lx = (x + slot / 2f - label.size.width / 2f).coerceIn(0f, this.size.width - label.size.width)
                    drawText(label, topLeft = Offset(lx, area.bottom + gapPx))
                }
            }
            if (targetVolume > 0.0) {
                val dash = Size.chartDash.toPx()
                val ty = yOf(targetVolume)
                drawLine(
                    below, Offset(area.left, ty), Offset(area.right, ty), Size.chartLine.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                )
            }
            selected?.let { i ->
                val x = area.left + slot * i
                val y = yOf(weeklyData[i].volume)
                drawRect(
                    selectedRing, Offset(x + barGap, y), GeometrySize(slot - barGap * 2, area.bottom - y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(Size.chartLine.toPx())
                )
            }
        }
        selected?.let { i ->
            val week = weeklyData[i]
            val percent = percentOfTarget(week.volume, targetVolume)
            ChartTooltip(
                anchor = Offset(area.left + slot * i + slot / 2f, yOf(week.volume)),
                bounds = size,
                emphasis = "${formatKg(week.volume)} kg",
                lines = listOf(
                    "Settimana dal ${longDate(week.weekStart)}",
                    percent?.let { "$it% del target" } ?: "Target N/D"
                )
            )
        }
    }
}

internal val PreviewWeeklyVolume: List<WeeklyVolumePoint> = run {
    val base = 1_758_492_000_000L
    listOf(6200.0, 7100.0, 0.0, 7800.0, 8100.0, 6900.0, 9000.0, 8400.0).mapIndexed { i, v ->
        WeeklyVolumePoint(base + i * 7 * DAY_MS, v)
    }
}

@Composable
private fun VolumePreviewBody() = Surface(color = MaterialTheme.colorScheme.surface) {
    Box(Modifier.padding(Spacing.lg)) { VolumeChart(PreviewWeeklyVolume, targetVolume = 7600.0) }
}

@Preview(name = "Volume · scuro", showBackground = true)
@Composable
private fun VolumeDarkPreview() = PlateauTheme(darkTheme = true) { VolumePreviewBody() }

@Preview(name = "Volume · chiaro", showBackground = true)
@Composable
private fun VolumeLightPreview() = PlateauTheme(darkTheme = false) { VolumePreviewBody() }
