package com.plateau.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Logo di Plateau (Sisifo che spinge il masso verso il plateau), disegnato su una griglia 120x120. */
@Composable
fun PlateauMark(
    modifier: Modifier = Modifier.size(48.dp),
    figureColor: Color = MaterialTheme.colorScheme.onBackground,
    boulderColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier.semantics { contentDescription = "Plateau" }) {
        val s = size.minDimension / 120f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val slope = Path().apply { moveTo(4f * s, 108f * s); lineTo(80f * s, 44f * s); lineTo(114f * s, 44f * s) }
        drawPath(slope, figureColor, style = Stroke(6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(boulderColor, radius = 17f * s, center = p(45f, 49f))
        val body = Path().apply {
            moveTo(32f * s, 60f * s); lineTo(21f * s, 64f * s); lineTo(13f * s, 80f * s); lineTo(25f * s, 90f * s)
            moveTo(13f * s, 80f * s); lineTo(7f * s, 105f * s)
        }
        drawPath(body, figureColor, style = Stroke(5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(figureColor, radius = 5f * s, center = p(24f, 57f))
    }
}
