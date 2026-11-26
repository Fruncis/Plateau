package com.plateau.app.ui.progress.chart

import com.plateau.app.domain.model.E1RMDataPoint
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Funzioni pure dei grafici di Progressi: niente Compose qui, cosi' sono testabili in JVM.

/** Posizione (px, in alto a sinistra) del tooltip dentro l'area del grafico. */
data class TooltipPosition(val x: Int, val y: Int)

/** Percentuale -> riempimento 0..1 dell'anello. `null` (N/D) = anello vuoto. */
internal fun ringProgress(percent: Int?): Float = ((percent ?: 0).coerceIn(0, 100)) / 100f

/** Testo al centro dell'anello: "N/D" quando non c'e' nulla di previsto, mai "0%". */
internal fun percentLabel(percent: Int?): String = percent?.let { "$it%" } ?: "N/D"

/** Riempimento 0..1 -> angolo dell'arco in gradi. */
internal fun sweepAngle(progress: Float): Float = progress.coerceIn(0f, 1f) * 360f

/**
 * Indice del punto piu' vicino al tocco (distanza euclidea), oppure `null` se non ci sono punti
 * o se il piu' vicino e' oltre [maxDistance]. A parita' vince il primo.
 */
internal fun nearestIndex(xs: List<Float>, ys: List<Float>, tapX: Float, tapY: Float, maxDistance: Float): Int? {
    val count = minOf(xs.size, ys.size)
    var best: Int? = null
    var bestSq = Float.MAX_VALUE
    for (i in 0 until count) {
        val dx = xs[i] - tapX
        val dy = ys[i] - tapY
        val sq = dx * dx + dy * dy
        if (sq < bestSq) {
            bestSq = sq
            best = i
        }
    }
    return best?.takeIf { bestSq <= maxDistance * maxDistance }
}

/** Barra sotto il tocco: il grafico e' diviso in [count] colonne uguali su [width]. */
internal fun barIndexAt(tapX: Float, width: Float, count: Int): Int? {
    if (count <= 0 || width <= 0f || tapX < 0f || tapX >= width) return null
    return (tapX / (width / count)).toInt().coerceIn(0, count - 1)
}

/** Volume in percentuale del target, `null` quando il target non c'e' (nessuna settimana con dati). */
internal fun percentOfTarget(volume: Double, target: Double): Int? =
    if (target <= 0.0) null else (volume * 100.0 / target).roundToInt()

/** Intervallo dell'asse Y con un 5% di margine; mai alto zero, `null` senza dati. */
internal fun paddedRange(values: List<Double>): ClosedFloatingPointRange<Double>? {
    if (values.isEmpty()) return null
    val min = values.min()
    val max = values.max()
    val span = max - min
    val pad = if (span > 0.0) span * 0.05 else maxOf(abs(max) * 0.05, 1.0)
    return (min - pad)..(max + pad)
}

/** Punti degli ultimi [days] giorni, contati dal punto piu' recente. */
internal fun windowPoints(points: List<E1RMDataPoint>, days: Int): List<E1RMDataPoint> {
    val last = points.maxOfOrNull { it.date } ?: return emptyList()
    val start = last - days * DAY_MS
    return points.filter { it.date >= start }.sortedBy { it.date }
}

/**
 * Tooltip centrato sopra il punto, a [gap] px; se sopra non c'e' spazio va sotto.
 * Resta sempre dentro [boundsWidth] x [boundsHeight] (allineato a sinistra/in alto se piu' grande).
 */
internal fun tooltipOffset(
    anchorX: Float, anchorY: Float, tipWidth: Int, tipHeight: Int, boundsWidth: Int, boundsHeight: Int, gap: Int
): TooltipPosition {
    val x = (anchorX - tipWidth / 2f).roundToInt().coerceAtMost(boundsWidth - tipWidth).coerceAtLeast(0)
    val above = (anchorY - gap - tipHeight).roundToInt()
    val y = if (above >= 0) above else (anchorY + gap).roundToInt()
    return TooltipPosition(x, y.coerceAtMost(boundsHeight - tipHeight).coerceAtLeast(0))
}

/** Chili in formato italiano: "8.400", "82,5"; al massimo un decimale. */
internal fun formatKg(kg: Double): String =
    DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.ITALIAN)).format(kg)

internal const val DAY_MS = 24L * 60 * 60 * 1000
