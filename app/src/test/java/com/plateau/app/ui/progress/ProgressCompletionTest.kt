package com.plateau.app.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressCompletionTest {
    @Test
    fun `completion percentage rounds completed sets over planned sets`() {
        assertEquals(60, completionPercent(done = 3, planned = 5))
    }

    @Test
    fun `completion percentage is unavailable when no sets are planned`() {
        assertNull(completionPercent(done = 0, planned = 0))
    }

    @Test
    fun `completion percentage does not exceed one hundred`() {
        assertEquals(100, completionPercent(done = 6, planned = 5))
    }

    @Test
    fun `completion percentage rounds to nearest`() {
        assertEquals(67, completionPercent(done = 8, planned = 12))
    }

    @Test
    fun `completion percentage is never one hundred while incomplete`() {
        assertEquals(99, completionPercent(done = 199, planned = 200))
        assertEquals(100, completionPercent(done = 200, planned = 200))
    }

    @Test
    fun `completion percentage of a tiny fraction rounds down to zero`() {
        assertEquals(0, completionPercent(done = 1, planned = 1000))
        assertEquals(1, completionPercent(done = 1, planned = 200))
    }
}
