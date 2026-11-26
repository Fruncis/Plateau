package com.plateau.app.ui.progress.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.plateau.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Area di disegno (px) dentro il Canvas, al netto delle etichette degli assi. */
internal data class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Stato vuoto dei grafici: nessun disegno e nessun tocco, solo una riga di testo. */
@Composable
internal fun EmptyChart(text: String, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Tooltip con i valori esatti, ancorato ad [anchor] (px nel grafico) e tenuto dentro [bounds].
 * Va messo come figlio del Box che contiene il Canvas.
 */
@Composable
internal fun ChartTooltip(anchor: Offset, bounds: IntSize, lines: List<String>, emphasis: String) {
    var tipSize by remember { mutableStateOf(IntSize.Zero) }
    val gap = with(LocalDensity.current) { Spacing.sm.roundToPx() }
    val position = tooltipOffset(anchor.x, anchor.y, tipSize.width, tipSize.height, bounds.width, bounds.height, gap)
    Surface(
        modifier = Modifier
            .offset { IntOffset(position.x, position.y) }
            .onSizeChanged { tipSize = it }
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(emphasis, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            lines.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val ShortDate = DateTimeFormatter.ofPattern("d/M", Locale.ITALIAN)
private val LongDate = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

internal fun shortDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(ShortDate)

internal fun longDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(LongDate)
