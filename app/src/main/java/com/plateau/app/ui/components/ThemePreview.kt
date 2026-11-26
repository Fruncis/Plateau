package com.plateau.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Spacing

@Composable
private fun Sample() = Surface(color = MaterialTheme.colorScheme.background) {
    Column(Modifier.padding(Spacing.xl), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.lg)) {
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md)) {
            PlateauMark()
            Text("Panca piana", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
            MuscleChip("Petto", emphasized = true); MuscleChip("Spalle"); MuscleChip("Tricipiti")
        }
        PlateauButton("Inizia allenamento", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Scuro", showBackground = true) @Composable private fun DarkPreview() = PlateauTheme(darkTheme = true) { Sample() }
@Preview(name = "Chiaro", showBackground = true) @Composable private fun LightPreview() = PlateauTheme(darkTheme = false) { Sample() }
