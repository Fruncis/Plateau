package com.plateau.app.ui.navigation

import android.net.Uri

sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Oggi")
    data object Session : Destination("session", "Sessione")
    data object Plans : Destination("plans", "Schede")
    data object Discover : Destination("discover", "Scopri")
    data object Progress : Destination("progress", "Progressi")
    data object Photo : Destination("photo", "Foto")
    data class ImportReview(val input: String) : Destination("import-review/${Uri.encode(input)}", "Importa")

    companion object {
        const val importReviewPattern = "import-review/{input}"
    }
}

// Called from MainActivity navigation and BottomNavBar destination mappings.
// No prior Destination implementation exists in the navigation package.
// User instruction: "continua"
