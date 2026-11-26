package com.plateau.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plateau.app.ui.components.BottomNavBar
import com.plateau.app.ui.components.MuscleChip
import com.plateau.app.ui.components.PlateauButton
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.theme.Spacing
import com.plateau.app.ui.theme.Size

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    currentDestination: Destination = Destination.Discover,
    onNavigate: (Destination) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Main content
            androidx.compose.foundation.layout.Box(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Size.tabBarHeight + Spacing.xl)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    // Header with filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scopri esercizi", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Filtri", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    // Filter chips row
                    FilterChips()

                    // Recommended section (based on gaps)
                    RecommendedSection()

                    // All exercises list
                    AllExercisesSection()
                }
            }

            // Bottom navigation
            BottomNavBar(currentDestination, onNavigate)
        }
    }
}

@Composable
private fun FilterChips() {
    val filters = listOf("Tutti", "Petto", "Dorso", "Gambe", "Spalle", "Braccia", "Core")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        filters.forEach { filter ->
            Surface(
                modifier = Modifier.height(36.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = com.plateau.app.ui.theme.PillShape
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(filter, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun RecommendedSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Consigliati per te", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Buchi nella scheda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        RecommendedExerciseCard("Face pull", "Rear delts, Trapezi", "Cavo", "Manca lavoro rear delts", 0.9)
        RecommendedExerciseCard("Bulgarian split squat", "Quad, Glutei", "Manubri", "Unilaterale per gambe", 0.8)
        RecommendedExerciseCard("Ab wheel rollout", "Core", "Bodyweight", "Anti-estensione", 0.7)
    }
}

@Composable
private fun RecommendedExerciseCard(name: String, muscles: String, equipment: String, reason: String, score: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(crossAxisSize = androidx.compose.ui.layout.LayoutWeight(fill = true)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(muscles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.progressText)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                MuscleChip(equipment)
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AllExercisesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text("Catalogo completo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

        AllExerciseCard("Panca piana", "Petto", "Spalle, Tricipiti", "Bilanciere", true)
        AllExerciseCard("Squat", "Quad", "Glutei, Core", "Bilanciere", true)
        AllExerciseCard("Stacco da terra", "Dorso", "Femorali, Glutei, Core", "Bilanciere", true)
        AllExerciseCard("Military press", "Spalle", "Tricipiti, Core", "Bilanciere", true)
        AllExerciseCard("Rematore con bilanciere", "Dorso", "Bicipiti, Rear delts", "Bilanciere", true)
        AllExerciseCard("Dip alle parallele", "Petto", "Tricipiti, Spalle", "Bodyweight", true)
        AllExerciseCard("Pull-up", "Dorso", "Bicipiti, Rear delts", "Bodyweight", true)
        AllExerciseCard("Leg press", "Quad", "Glutei", "Macchina", false)
        AllExerciseCard("Curl bicipiti", "Bicipiti", "", "Manubri", false)
        AllExerciseCard("Estensioni tricipiti", "Tricipiti", "", "Cavo", false)
    }
}

@Composable
private fun AllExerciseCard(name: String, primaryMuscle: String, secondaryMuscles: String, equipment: String, isCompound: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(crossAxisSize = androidx.compose.ui.layout.LayoutWeight(fill = true)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (isCompound) {
                        Text("COMPOSTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.progressText)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MuscleChip(primaryMuscle, emphasized = true)
                    if (secondaryMuscles.isNotBlank()) {
                        Text(secondaryMuscles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(equipment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}