package com.plateau.app.ui.plans

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
fun PlansScreen(
    viewModel: PlansViewModel,
    currentDestination: Destination = Destination.Plans,
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Le mie schede", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        PlateauButton("Nuova scheda", onClick = {})
                    }

                    // Active plan card
                    ActivePlanCard()

                    // Archived plans placeholder
                    ArchivedPlansSection()
                }
            }

            // Bottom navigation
            BottomNavBar(currentDestination, onNavigate)
        }
    }
}

@Composable
private fun ActivePlanCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Scheda attiva", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PPL 6 giorni", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                MuscleChip("Attiva", emphasized = true)
            }

            // Day preview
            PlanDayRow("Push A", "Petto, Spalle, Tricipiti", true)
            PlanDayRow("Pull A", "Dorso, Bicipiti, Rear delts", false)
            PlanDayRow("Legs A", "Quad, Femorali, Glutei, Polpacci", false)
            PlanDayRow("Push B", "Petto, Spalle, Tricipiti", false)
            PlanDayRow("Pull B", "Dorso, Bicipiti, Rear delts", false)
            PlanDayRow("Legs B", "Quad, Femorali, Glutei, Polpacci", false)
            PlanDayRow("Riposo", "", false)

            PlateauButton("Vai all'allenamento di oggi", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlanDayRow(label: String, muscles: String, isToday: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(crossAxisSize = androidx.compose.ui.layout.LayoutWeight(fill = true)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (muscles.isNotBlank()) {
                Text(muscles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isToday) {
            Text("OGGI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ArchivedPlansSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text("Archivio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ArchivedPlanCard("Upper/Lower 4 giorni", "Gen–Mar 2026", "Completata")
        ArchivedPlanCard("Full Body 3 giorni", "Ott–Dic 2025", "Completata")
    }
}

@Composable
private fun ArchivedPlanCard(name: String, period: String, status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(period, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.progressText)
        }
    }
}