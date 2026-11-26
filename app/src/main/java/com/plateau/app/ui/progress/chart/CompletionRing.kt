package com.plateau.app.ui.progress.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.plateau.app.ui.theme.NumberDisplay
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface

/**
 * Anello di completamento: traccia in `outline`, arco in `extras.progress`, valore in `NumberDisplay`
 * (`extras.progressText`) con [label] sotto. Per una percentuale assente passa `progress = 0f` e `value = "N/D"`.
 */
@Composable
fun CompletionRing(
    progress: Float, // 0..1
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    size: Dp = Size.ringSize,
    strokeWidth: Dp = Size.ringStroke
) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "ring")
    val track = MaterialTheme.colorScheme.outline
    val fill = PlateauTheme.extras.progress
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = "$label: $value" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = GeometrySize(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = sweepAngle(animated)
            if (sweep > 0f) {
                drawArc(fill, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(
            modifier = Modifier.padding(strokeWidth + Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = NumberDisplay,
                color = if (progress > 0f) PlateauTheme.extras.progressText else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RingPreviewBody() = Surface(color = MaterialTheme.colorScheme.background) {
    Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        CompletionRing(ringProgress(60), "Settimana", percentLabel(60))
        CompletionRing(ringProgress(null), "Esercizio", percentLabel(null))
    }
}

@Preview(name = "Anello · scuro", showBackground = true)
@Composable
private fun RingDarkPreview() = PlateauTheme(darkTheme = true) { RingPreviewBody() }

@Preview(name = "Anello · chiaro", showBackground = true)
@Composable
private fun RingLightPreview() = PlateauTheme(darkTheme = false) { RingPreviewBody() }
