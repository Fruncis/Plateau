package com.plateau.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.plateau.app.ui.theme.PillShape

/** Gruppo muscolare dell'esercizio. emphasized = true per il gruppo primario. */
@Composable
fun MuscleChip(label: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val bg = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier.clip(PillShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
