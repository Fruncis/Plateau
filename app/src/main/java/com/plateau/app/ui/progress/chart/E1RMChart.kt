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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.plateau.app.domain.model.E1RMDataPoint
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing

private const val WINDOW_DAYS = 30
private const val H_LINES = 4
private const val V_LINES = 6

/**
 * Andamento dell'e1RM (Epley) negli ultimi 30 giorni: X = data, Y = e1RM.
 * Tocca o tieni premuto vicino a un punto per vedere data, e1RM e peso x ripetizioni.
 * Senza dati mostra uno stato vuoto e ignora i tocchi.
 */
@Composable
fun E1RMChart(
    dataPoints: List<E1RMDataPoint>,
    onPointTap: (E1RMDataPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember(dataPoints) { windowPoints(dataPoints, WINDOW_DAYS) }
    val range = remember(points) { paddedRange(points.map { it.e1rm }) }
    if (points.isEmpty() || range == null) {
        EmptyChart("Nessuna serie negli ultimi 30 giorni", Size.chartHeight, modifier)
        return
    }
    val end = points.last().date
    val start = end - WINDOW_DAYS * DAY_MS

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outline
    val lineColor = PlateauTheme.extras.progress
    val pointColor = PlateauTheme.extras.progressText
    val selectedRing = MaterialTheme.colorScheme.onSurface

    val maxLabel = measurer.measure(formatKg(range.endInclusive), labelStyle)
    val minLabel = measurer.measure(formatKg(range.start), labelStyle)
    val xLabels = (0 until V_LINES).map { i -> measurer.measure(shortDate(start + (end - start) * i / (V_LINES - 1)), labelStyle) }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val currentOnTap by rememberUpdatedState(onPointTap)

    val gapPx = with(density) { Spacing.sm.toPx() }
    val area = PlotArea(
        left = maxOf(maxLabel.size.width, minLabel.size.width) + gapPx,
        top = maxLabel.size.height / 2f,
        right = size.width - xLabels.last().size.width / 2f,
        bottom = size.height - xLabels.first().size.height - gapPx
    )
    val span = (range.endInclusive - range.start)
    val xs = points.map { area.left + area.width * (it.date - start).toFloat() / (end - start).toFloat() }
    val ys = points.map { area.bottom - area.height * ((it.e1rm - range.start) / span).toFloat() }
    val touchRadius = with(density) { Size.minTouch.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Size.chartHeight)
            .onSizeChanged { size = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Grafico e1RM: ${points.size} serie, ultima ${formatKg(points.last().e1rm)} kg"
                }
                .pointerInput(points, size) {
                    val select: (Offset) -> Unit = { tap ->
                        selected = nearestIndex(xs, ys, tap.x, tap.y, touchRadius)
                        selected?.let { currentOnTap(points[it]) }
                    }
                    detectTapGestures(onTap = select, onLongPress = select)
                }
        ) {
            if (area.width <= 0f || area.height <= 0f) return@Canvas
            val grid = Size.chartGrid.toPx()
            for (i in 0 until H_LINES) {
                val y = area.top + area.height * i / (H_LINES - 1)
                drawLine(gridColor, Offset(area.left, y), Offset(area.right, y), grid)
            }
            for (i in 0 until V_LINES) {
                val x = area.left + area.width * i / (V_LINES - 1)
                drawLine(gridColor, Offset(x, area.top), Offset(x, area.bottom), grid)
                val label = xLabels[i]
                val lx = (x - label.size.width / 2f).coerceIn(0f, this.size.width - label.size.width)
                drawText(label, topLeft = Offset(lx, area.bottom + gapPx))
            }
            drawText(maxLabel, topLeft = Offset(0f, area.top - maxLabel.size.height / 2f))
            drawText(minLabel, topLeft = Offset(0f, area.bottom - minLabel.size.height / 2f))

            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(xs[0], ys[0])
                    for (i in 1 until points.size) lineTo(xs[i], ys[i])
                }
                drawPath(path, lineColor, style = Stroke(Size.chartLine.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val radius = Size.chartPoint.toPx()
            points.indices.forEach { i -> drawCircle(pointColor, radius, Offset(xs[i], ys[i])) }
            selected?.let { i ->
                drawCircle(selectedRing, radius * 2, Offset(xs[i], ys[i]), style = Stroke(Size.chartLine.toPx()))
            }
        }
        selected?.let { i ->
            val p = points[i]
            ChartTooltip(
                anchor = Offset(xs[i], ys[i]),
                bounds = size,
                emphasis = "${formatKg(p.e1rm)} kg e1RM",
                lines = listOf(longDate(p.date), "${formatKg(p.weight)} kg × ${p.reps}")
            )
        }
    }
}

internal val PreviewE1RMPoints: List<E1RMDataPoint> = run {
    val base = 1_763_000_000_000L
    listOf(80.0 to 8, 80.0 to 9, 82.5 to 8, 82.5 to 9, 85.0 to 8, 85.0 to 10).mapIndexed { i, (w, r) ->
        E1RMDataPoint(base + i * 5 * DAY_MS, w * (1 + r / 30.0), w, r)
    }
}

@Composable
private fun E1RMPreviewBody() = Surface(color = MaterialTheme.colorScheme.surface) {
    Box(Modifier.padding(Spacing.lg)) { E1RMChart(PreviewE1RMPoints, onPointTap = {}) }
}

@Preview(name = "e1RM · scuro", showBackground = true)
@Composable
private fun E1RMDarkPreview() = PlateauTheme(darkTheme = true) { E1RMPreviewBody() }

@Preview(name = "e1RM · chiaro", showBackground = true)
@Composable
private fun E1RMLightPreview() = PlateauTheme(darkTheme = false) { E1RMPreviewBody() }
