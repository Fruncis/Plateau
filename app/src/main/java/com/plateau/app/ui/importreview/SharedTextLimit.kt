package com.plateau.app.ui.importreview

/** Shared text after [limitSharedText]; [truncated] is true when lines were dropped. */
data class LimitedSharedText(val text: String, val truncated: Boolean)

/**
 * Max characters of shared text passed to ImportReview. The text travels in the navigation route,
 * which is kept in saved state: an unbounded share risks TransactionTooLargeException.
 */
const val MAX_SHARED_TEXT_CHARS = 20_000

/**
 * Keeps [text] whole when it fits in [limit] characters; otherwise cuts it at the last line break
 * before the limit, so no line is imported half-written. A single line longer than the limit is cut
 * at the limit (it will surface as an unmatched line).
 */
fun limitSharedText(text: String, limit: Int = MAX_SHARED_TEXT_CHARS): LimitedSharedText {
    if (text.length <= limit) return LimitedSharedText(text, truncated = false)
    val lastBreak = text.lastIndexOf('\n', startIndex = limit)
    val end = if (lastBreak > 0) lastBreak else limit
    return LimitedSharedText(text.substring(0, end).trimEnd(), truncated = true)
}
