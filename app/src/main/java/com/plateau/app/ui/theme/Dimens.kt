package com.plateau.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Spacing { // griglia da 4/8 dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp // margine laterale delle schermate
    val xxl = 24.dp
    val xxxl = 32.dp
}

object Size {
    val minTouch = 44.dp
    val buttonHeight = 56.dp
    val tabBarHeight = 84.dp
}

val PlateauShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),  // campi di input, riga serie
    medium = RoundedCornerShape(18.dp), // bottoni, card piccole
    large = RoundedCornerShape(24.dp),  // card principali
    extraLarge = RoundedCornerShape(28.dp),
)
val PillShape = RoundedCornerShape(percent = 50) // chip
