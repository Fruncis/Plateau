package com.plateau.app.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import com.plateau.app.ui.theme.PlateauTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plateau.app.ui.components.BottomNavBar
import com.plateau.app.ui.components.MuscleChip
import com.plateau.app.ui.components.PlateauButton
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.theme.NumberLarge
import com.plateau.app.ui.theme.Spacing
import com.plateau.app.ui.theme.Size

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    currentDestination: Destination = Destination.Session,
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
                    // Exercise header with muscle chips
                    ExerciseHeader()

                    // Last time info
                    LastTimeInfo()

                    // Sets list
                    SetsList()

                    // e1RM estimate
                    E1RMEstimate()
                }
            }

            // Fixed bottom button
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                    .height(Size.buttonHeight)
                    .align(Alignment.CenterHorizontally)
            ) {
                PlateauButton("Completa serie 1", onClick = {}, modifier = Modifier.fillMaxWidth())
            }

            // Bottom navigation
            BottomNavBar(currentDestination, onNavigate)
        }
    }
}

@Composable
private fun ExerciseHeader() {
    Column(modifier = Modifier.padding(horizontal = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Panca piana", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MuscleChip("Petto", emphasized = true)
            MuscleChip("Spalle")
            MuscleChip("Tricipiti")
        }
    }
}

@Composable
private fun LastTimeInfo() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg).height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Ultima volta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3 giorni fa — 80 kg × 10, 80 kg × 9, 80 kg × 8", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SetsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Current set - highlighted
        SetRow(setNumber = 1, weight = "80", reps = "10", isCurrent = true, isCompleted = false)

        // Completed sets
        SetRow(setNumber = 2, weight = "80", reps = "9", isCurrent = false, isCompleted = true)
        SetRow(setNumber = 3, weight = "80", reps = "8", isCurrent = false, isCompleted = true)
        SetRow(setNumber = 4, weight = "80", reps = "8", isCurrent = false, isCompleted = true)
    }
}

@Composable
private fun SetRow(setNumber: Int, weight: String, reps: String, isCurrent: Boolean, isCompleted: Boolean) {
    val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val weightColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val repsColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#$setNumber", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$weight kg", style = NumberLarge.copy(color = weightColor))
            Text("×$reps", style = NumberLarge.copy(color = repsColor))
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Serie completata",
                    modifier = Modifier.size(24.dp),
                    tint = PlateauTheme.extras.progress
                )
            }
        }
    }
}

@Composable
private fun E1RMEstimate() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("e1RM stimato", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("80 kg × 10", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("107 kg", style = NumberLarge, color = PlateauTheme.extras.progressText)
            }
        }
    }
}

// Rest timer component
@Composable
fun RestTimer(durationSec: Int, onFinish: () -> Unit) {
    // TODO: Implement rest timer with foreground service
}