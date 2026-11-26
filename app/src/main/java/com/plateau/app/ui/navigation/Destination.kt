package com.plateau.app.ui.navigation

import android.net.Uri

sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Oggi")
    data object Session : Destination("session", "Sessione")
    data object Plans : Destination("plans", "Schede")
    data object Discover : Destination("discover", "Scopri")
    data object Progress : Destination("progress", "Progressi")
    data object Photo : Destination("photo", "Foto")
    data class ImportReview(val input: String, val truncated: Boolean = false) :
        Destination("import-review/${Uri.encode(input)}?truncated=$truncated", "Importa")

    companion object {
        const val importReviewPattern = "import-review/{input}?truncated={truncated}"
    }
}

// Called from MainActivity navigation and BottomNavBar destination mappings.
// No prior Destination implementation exists in the navigation package.
// User instruction: "continua"
