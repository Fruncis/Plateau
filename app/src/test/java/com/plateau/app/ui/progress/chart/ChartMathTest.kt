package com.plateau.app.ui.progress.chart

import com.plateau.app.domain.model.E1RMDataPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartMathTest {

    private val day = 24L * 60 * 60 * 1000

    // --- anello di completamento ---

    @Test
    fun `null percent gives empty ring and N-D label`() {
        assertEquals(0f, ringProgress(null), 0f)
        assertEquals("N/D", percentLabel(null))
    }

    @Test
    fun `zero percent is a real 0 percent, not N-D`() {
        assertEquals(0f, ringProgress(0), 0f)
        assertEquals("0%", percentLabel(0))
    }

    @Test
    fun `percent maps to progress and clamps out of range values`() {
        assertEquals(0.6f, ringProgress(60), 0.0001f)
        assertEquals(1f, ringProgress(140), 0f)
        assertEquals(0f, ringProgress(-5), 0f)
    }

    @Test
    fun `progress maps to sweep angle and clamps`() {
        assertEquals(0f, sweepAngle(0f), 0f)
        assertEquals(216f, sweepAngle(0.6f), 0.001f)
        assertEquals(360f, sweepAngle(1f), 0f)
        assertEquals(360f, sweepAngle(1.7f), 0f)
        assertEquals(0f, sweepAngle(-0.2f), 0f)
    }

    // --- punto piu' vicino (e1RM) ---

    @Test
    fun `nearest index is null on empty data`() {
        assertNull(nearestIndex(emptyList(), emptyList(), 10f, 10f, 50f))
    }

    @Test
    fun `nearest index picks the closest point in 2D`() {
        val xs = listOf(0f, 100f, 200f)
        val ys = listOf(50f, 50f, 50f)
        assertEquals(1, nearestIndex(xs, ys, 120f, 60f, 50f))
        assertEquals(2, nearestIndex(xs, ys, 190f, 40f, 50f))
    }

    @Test
    fun `nearest index separates points stacked on the same day`() {
        val xs = listOf(100f, 100f)
        val ys = listOf(20f, 80f)
        assertEquals(1, nearestIndex(xs, ys, 102f, 75f, 50f))
        assertEquals(0, nearestIndex(xs, ys, 98f, 25f, 50f))
    }

    @Test
    fun `nearest index ignores taps beyond the touch radius`() {
        assertNull(nearestIndex(listOf(0f, 100f), listOf(0f, 0f), 50f, 60f, 40f))
    }

    // --- barra toccata (volume) ---

    @Test
    fun `bar index is null without bars or outside the chart`() {
        assertNull(barIndexAt(10f, 300f, 0))
        assertNull(barIndexAt(-1f, 300f, 3))
        assertNull(barIndexAt(300f, 300f, 3))
        assertNull(barIndexAt(10f, 0f, 3))
    }

    @Test
    fun `bar index maps tap to equal width slots`() {
        assertEquals(0, barIndexAt(0f, 300f, 3))
        assertEquals(0, barIndexAt(99.9f, 300f, 3))
        assertEquals(1, barIndexAt(100f, 300f, 3))
        assertEquals(2, barIndexAt(299f, 300f, 3))
    }

    // --- valori del tooltip e scale ---

    @Test
    fun `percent of target is null when target is missing`() {
        assertNull(percentOfTarget(500.0, 0.0))
        assertEquals(93, percentOfTarget(8400.0, 9000.0))
        assertEquals(120, percentOfTarget(1200.0, 1000.0))
    }

    @Test
    fun `padded range is null on empty data and never zero height`() {
        assertNull(paddedRange(emptyList()))
        val single = paddedRange(listOf(100.0))!!
        assertEquals(true, single.start < 100.0 && single.endInclusive > 100.0)
        val range = paddedRange(listOf(100.0, 120.0))!!
        assertEquals(99.0, range.start, 0.0001)
        assertEquals(121.0, range.endInclusive, 0.0001)
    }

    @Test
    fun `window keeps only the last N days ending at the latest point`() {
        val points = listOf(0L, 10 * day, 35 * day, 40 * day).map { E1RMDataPoint(it, 100.0, 80.0, 8) }
        assertEquals(listOf(10 * day, 35 * day, 40 * day), windowPoints(points, 30).map { it.date })
        assertEquals(emptyList<E1RMDataPoint>(), windowPoints(emptyList(), 30))
    }

    @Test
    fun `tooltip sits above the anchor and stays inside the bounds`() {
        // centrato sopra il punto
        assertEquals(TooltipPosition(80, 40), tooltipOffset(100f, 100f, 40, 50, 300, 200, 10))
        // bordo sinistro e destro
        assertEquals(TooltipPosition(0, 40), tooltipOffset(5f, 100f, 40, 50, 300, 200, 10))
        assertEquals(TooltipPosition(260, 40), tooltipOffset(295f, 100f, 40, 50, 300, 200, 10))
        // senza spazio sopra va sotto il punto
        assertEquals(TooltipPosition(80, 30), tooltipOffset(100f, 20f, 40, 50, 300, 200, 10))
        // tooltip piu' largo del grafico: allineato a sinistra
        assertEquals(TooltipPosition(0, 40), tooltipOffset(100f, 100f, 400, 50, 300, 200, 10))
    }

    @Test
    fun `kg formatting drops useless decimals and uses the Italian comma`() {
        assertEquals("100", formatKg(100.0))
        assertEquals("82,5", formatKg(82.5))
        assertEquals("126,7", formatKg(126.66666))
        assertEquals("8.400", formatKg(8400.0))
    }
}
