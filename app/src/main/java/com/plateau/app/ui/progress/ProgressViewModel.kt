package com.plateau.app.ui.progress

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** La schermata Progressi mostra ancora dati segnaposto: il ViewModel non ha stato. */
@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel()

internal fun completionPercent(done: Int, planned: Int): Int? {
    if (planned <= 0) return null
    return ((done.coerceAtLeast(0) * 100.0) / planned).toInt().coerceIn(0, 100)
}
