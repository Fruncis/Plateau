package com.plateau.app.ui.progress

internal fun completionPercent(done: Int, planned: Int): Int? {
    if (planned <= 0) return null
    return ((done.coerceAtLeast(0) * 100.0) / planned).toInt().coerceIn(0, 100)
}
