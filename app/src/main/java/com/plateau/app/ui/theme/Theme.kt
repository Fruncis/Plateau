package com.plateau.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtras = staticCompositionLocalOf { DarkExtras }

object PlateauTheme {
    val extras: PlateauExtras
        @Composable @ReadOnlyComposable get() = LocalExtras.current
}

/** Avvolgi tutta l'app: setContent { PlateauTheme { ... } } */
@Composable
fun PlateauTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) PlateauDarkColors else PlateauLightColors
    val extras = if (darkTheme) DarkExtras else LightExtras
    CompositionLocalProvider(LocalExtras provides extras) {
        MaterialTheme(colorScheme = colors, typography = PlateauTypography, shapes = PlateauShapes, content = content)
    }
}
