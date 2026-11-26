package com.plateau.app.ui.progress

import com.plateau.app.ui.progress.chart.DAY_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDayTest {
    private val now = 100 * DAY_MS

    @Test
    fun `relative day labels`() {
        assertEquals("oggi", relativeDay(now - 1000, now))
        assertEquals("ieri", relativeDay(now - DAY_MS, now))
        assertEquals("3 giorni fa", relativeDay(now - 3 * DAY_MS, now))
        assertEquals("1 settimana fa", relativeDay(now - 9 * DAY_MS, now))
        assertEquals("3 settimane fa", relativeDay(now - 22 * DAY_MS, now))
        assertEquals("oggi", relativeDay(now + DAY_MS, now))
    }
}
