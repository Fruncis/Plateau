package com.plateau.app.ui.photo

import com.plateau.app.ui.theme.NumberLarge
import com.plateau.app.ui.theme.NumberDisplay
import com.plateau.app.ui.theme.PlateauTheme
import androidx.compose.foundation.layout.height
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
fun PhotoScreen(
    viewModel: PhotoViewModel,
    currentDestination: Destination = Destination.Photo,
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
                .weight(1f)
                .fillMaxWidth()
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
                        Text("Foto e report", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        PlateauButton("Scatta foto", onClick = {})
                    }

                    // Current goal toggle
                    GoalToggle()

                    // Photo comparison
                    PhotoComparisonCard()

                    // Region metrics
                    RegionMetricsCard()

                    // Weak areas
                    WeakAreasCard()

                    // Kcal recommendation
                    KcalRecommendationCard()
                }
            }

            // Bottom navigation
            BottomNavBar(currentDestination, onNavigate)
        }
    }
}

@Composable
private fun GoalToggle() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Obiettivo attuale", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                GoalOption("Bulk", "Surplus +200–300 kcal", true)
                GoalOption("Cut", "Deficit −300–500 kcal", false)
                GoalOption("Mantenimento", "TDEE", false)
            }
        }
    }
}

@Composable
private fun GoalOption(label: String, detail: String, isSelected: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoComparisonCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Confronto progressi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            // Photo placeholders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                PhotoPlaceholder("Precedente", "2 settimane fa", true)
                PhotoPlaceholder("Attuale", "Oggi", false)
            }

            // Overall progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Crescita stimata", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("+2–4% (confidenza media)", style = NumberLarge, color = PlateauTheme.extras.progressText)
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder(label: String, date: String, isPrevious: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📷", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isPrevious) {
                Text("Pose guidata ✓", style = MaterialTheme.typography.labelSmall, color = PlateauTheme.extras.progressText)
            }
        }
    }
}

@Composable
private fun RegionMetricsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Metriche per regione", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            RegionMetricRow("Spalle", "+5%", "Alta", true)
            RegionMetricRow("Petto", "+3%", "Media", true)
            RegionMetricRow("Braccia", "+1%", "Bassa", false)
            RegionMetricRow("Vita", "−2%", "Media", true)
            RegionMetricRow("Cosce", "+4%", "Media", true)
            RegionMetricRow("Polpacci", "0%", "Bassa", false)
        }
    }
}

@Composable
private fun RegionMetricRow(region: String, change: String, confidence: String, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(region, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(change, style = NumberLarge, color = if (isPositive) PlateauTheme.extras.progressText else MaterialTheme.colorScheme.onSurface)
            Text(confidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeakAreasCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zone carenti", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Volume < target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            WeakAreaRow("Braccia", "Crescita +1% vs +4% target", "Volume sett. 8 vs 12 target", "Aggiungi curl e pushdown")
            WeakAreaRow("Polpacci", "Crescita 0% vs +3% target", "Volume sett. 4 vs 8 target", "Aggiungi calf raise")
        }
    }
}

@Composable
private fun WeakAreaRow(region: String, growthIssue: String, volumeIssue: String, suggestion: String) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                MuscleChip(region, emphasized = true)
                Column(modifier = Modifier.weight(1f)) {
                    Text(growthIssue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(volumeIssue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💡", style = MaterialTheme.typography.labelLarge)
            Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            PlateauButton("Vai a Scopri", onClick = {})
        }
    }
}

@Composable
private fun KcalRecommendationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Target kcal giornaliero", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BMR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text("1.750 kcal", style = NumberLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TDEE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text("2.625 kcal", style = NumberLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target (Bulk)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text("2.900 kcal", style = NumberDisplay, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Text("Proteine: 1,8 g/kg = 144 g  |  Carbo: 3,5 g/kg = 280 g  |  Grassi: 0,9 g/kg = 72 g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))

            Text("⚠ Basato su trend peso (media 7gg). Non è consiglio medico. Inserisci peso per correzione adattiva.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
        }
    }
}