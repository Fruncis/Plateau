package com.plateau.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Palette grezza del brand. Non usarla nelle schermate: usa MaterialTheme.colorScheme e PlateauTheme.extras. */
internal object Brand {
    val Basalt = Color(0xFF111318)
    val Stone = Color(0xFF1C1F26)
    val Slate = Color(0xFF2A2E37)
    val Mist = Color(0xFFA3AAB8)
    val Bone = Color(0xFFF4F1EA)
    val Ember = Color(0xFFFF7A3D)
    val Moss = Color(0xFF7DD3A8)
    val Paper = Color(0xFFF7F5F0)
    val White = Color(0xFFFFFFFF)
    val Sand = Color(0xFFE4E0D6)
    val Graphite = Color(0xFF5B6270)
    val EmberInk = Color(0xFFB93F08)
    val MossInk = Color(0xFF17744D)
}

val PlateauDarkColors = darkColorScheme(
    primary = Brand.Ember, onPrimary = Brand.Basalt,
    secondary = Brand.Moss, onSecondary = Brand.Basalt,
    background = Brand.Basalt, onBackground = Brand.Bone,
    surface = Brand.Stone, onSurface = Brand.Bone,
    surfaceVariant = Brand.Slate, onSurfaceVariant = Brand.Mist,
    outline = Brand.Slate, outlineVariant = Brand.Slate,
)

val PlateauLightColors = lightColorScheme(
    primary = Brand.Ember, onPrimary = Brand.Basalt,
    secondary = Brand.Moss, onSecondary = Brand.Basalt,
    background = Brand.Paper, onBackground = Brand.Basalt,
    surface = Brand.White, onSurface = Brand.Basalt,
    surfaceVariant = Brand.Sand, onSurfaceVariant = Brand.Graphite,
    outline = Brand.Sand, outlineVariant = Brand.Sand,
)

/** Colori semantici che Material non prevede. */
@Immutable
data class PlateauExtras(
    val progress: Color,     // riempimenti: serie completate, indicatori positivi
    val onProgress: Color,   // contenuto sopra "progress"
    val progressText: Color, // testo/numeri positivi (record, +kg), leggibile nel tema
    val accentText: Color,   // arancione usato COME TESTO (tab attivo, serie in corso)
)

internal val DarkExtras = PlateauExtras(Brand.Moss, Brand.Basalt, Brand.Moss, Brand.Ember)
internal val LightExtras = PlateauExtras(Brand.Moss, Brand.Basalt, Brand.MossInk, Brand.EmberInk)
