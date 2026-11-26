package com.plateau.app.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.ui.components.BottomNavBar
import com.plateau.app.ui.components.MuscleChip
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    currentDestination: Destination = Destination.Discover,
    onNavigate: (Destination) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val muscles by viewModel.muscleGroups.collectAsState(initial = emptyList())
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        DiscoverContent(
            state = state,
            muscles = muscles,
            equipmentOptions = viewModel.equipmentOptions,
            onSearch = viewModel::onSearchChanged,
            onMuscle = viewModel::onMuscleSelected,
            onEquipment = viewModel::onEquipmentSelected,
            onDifficulty = viewModel::onDifficultySelected,
            onMovement = viewModel::onMovementPatternSelected,
            onClear = viewModel::clearFilters,
            modifier = Modifier.weight(1f)
        )
        BottomNavBar(currentDestination, onNavigate)
    }
}

@Composable
private fun DiscoverContent(
    state: DiscoverUiState,
    muscles: List<MuscleGroup>,
    equipmentOptions: List<String>,
    onSearch: (String) -> Unit,
    onMuscle: (String?) -> Unit,
    onEquipment: (String?) -> Unit,
    onDifficulty: (Int?) -> Unit,
    onMovement: (String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Text("Scopri esercizi", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            OutlinedTextField(
                value = state.activeFilters.searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cerca esercizio") },
                singleLine = true
            )
        }
        item {
            FilterRow("Muscoli", muscles.map { it.name to it.id }, state.activeFilters.muscleId, onMuscle)
        }
        item {
            FilterRow("Attrezzi", equipmentOptions.map { it.toEquipmentLabel() to it }, state.activeFilters.equipment, onEquipment)
        }
        item {
            FilterRow("Difficoltà massima", listOf("Principiante" to "1", "Intermedio" to "2", "Avanzato" to "3"), state.activeFilters.maxDifficulty?.toString()) {
                onDifficulty(it?.toIntOrNull())
            }
        }
        item {
            FilterRow("Schema motorio", MOVEMENT_PATTERNS.map { it.toMovementLabel() to it }, state.activeFilters.movementPattern, onMovement)
            if (state.activeFilters.hasActiveFilters) {
                Text(
                    text = "Azzera filtri",
                    modifier = Modifier.padding(top = Spacing.xs).clickable(onClick = onClear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (state.recommendations.isNotEmpty()) {
            item {
                Text("Consigliati per te", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Spacing.xs))
            }
            items(state.recommendations.take(5), key = { "recommend-${it.exercise.id}" }) { recommendation ->
                ExerciseCard(recommendation.exercise, "${recommendation.volumeGap.muscleGroup.name} · ${recommendation.volumeGap.deficitPercent}% del volume settimanale mancante")
            }
        }
        item { Text("Catalogo completo", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) }
        if (state.filteredExercises.isEmpty()) {
            item { Text("Nessun esercizio trovato", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.filteredExercises, key = { it.id }) { exercise -> ExerciseCard(exercise) }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(if (selected == value) null else value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, note: String? = null) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(note ?: "${exercise.movementPattern.toMovementLabel()} · ${exercise.difficulty.toDifficultyLabel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MuscleChip(exercise.equipment.toEquipmentLabel(), emphasized = exercise.isCompound)
        }
    }
}

private val MOVEMENT_PATTERNS = listOf("push", "pull", "hinge", "squat", "lunge", "carry", "rotation", "core")
private fun String.toEquipmentLabel() = replaceFirstChar { it.uppercase() }
private fun String.toMovementLabel() = replaceFirstChar { it.uppercase() }
private fun Int.toDifficultyLabel() = when (this) { 1 -> "Principiante"; 2 -> "Intermedio"; else -> "Avanzato" }

@Preview(name = "Discover · scuro", showBackground = true)
@Composable
private fun DiscoverDarkPreview() = PlateauTheme(darkTheme = true) {
    DiscoverContent(DiscoverUiState(), emptyList(), emptyList(), {}, {}, {}, {}, {}, {}, Modifier)
}

@Preview(name = "Discover · chiaro", showBackground = true)
@Composable
private fun DiscoverLightPreview() = PlateauTheme(darkTheme = false) {
    DiscoverContent(DiscoverUiState(), emptyList(), emptyList(), {}, {}, {}, {}, {}, {}, Modifier)
}
