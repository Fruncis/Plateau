package com.plateau.app.ui.importreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTextLimitTest {

    @Test
    fun `text within the limit is kept whole`() {
        val text = "Panca piana 3x10 80kg\nSquat 4x8 100kg"
        assertEquals(LimitedSharedText(text, truncated = false), limitSharedText(text, limit = text.length))
    }

    @Test
    fun `long text is cut at the last line break before the limit`() {
        val text = "aaaa\nbbbb\ncccc" // breaks at 4 and 9
        val limited = limitSharedText(text, limit = 12)
        assertTrue(limited.truncated)
        assertEquals("aaaa\nbbbb", limited.text)
    }

    @Test
    fun `a break exactly at the limit keeps the lines before it`() {
        assertEquals("aaaa", limitSharedText("aaaa\nbbbb", limit = 4).text)
    }

    @Test
    fun `a single line over the limit is cut at the limit`() {
        val limited = limitSharedText("x".repeat(30), limit = 10)
        assertTrue(limited.truncated)
        assertEquals(10, limited.text.length)
    }

    @Test
    fun `default limit is twenty thousand characters`() {
        val line = "Panca piana 3x10 80kg\n" // 22 chars
        val text = line.repeat(1_000) // 22 000 chars
        val limited = limitSharedText(text)
        assertTrue(limited.truncated)
        assertTrue(limited.text.length <= MAX_SHARED_TEXT_CHARS)
        assertEquals(909, limited.text.lines().size) // 909 * 22 - 1 = 19 997
        assertFalse(limitSharedText(line.repeat(900)).truncated)
    }

    @Test
    fun `edit sheet inputs accept zero weight and reject invalid values`() {
        assertEquals(0.0, parseWeightInput("0")!!, 0.0)
        assertEquals(82.5, parseWeightInput(" 82,5 ")!!, 0.0)
        assertNull(parseWeightInput("-1"))
        assertNull(parseWeightInput(""))
        assertNull(parseWeightInput("NaN"))
        assertEquals(1, parseRepsInput("1"))
        assertNull(parseRepsInput("0"))
        assertNull(parseRepsInput("2.5"))
        assertNull(parseRepsInput(""))
    }
}
