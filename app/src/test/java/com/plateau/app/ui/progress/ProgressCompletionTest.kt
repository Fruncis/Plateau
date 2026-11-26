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
}
