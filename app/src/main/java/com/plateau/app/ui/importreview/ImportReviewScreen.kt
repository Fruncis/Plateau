package com.plateau.app.ui.importreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.domain.service.ParsedSet
import com.plateau.app.domain.service.ParsedWorkout
import com.plateau.app.ui.components.MuscleChip
import com.plateau.app.ui.components.PlateauButton
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Spacing

@Composable
fun ImportReviewRoute(
    viewModel: ImportReviewViewModel,
    input: String,
    onCancel: () -> Unit,
    onImported: () -> Unit
) {
    LaunchedEffect(input) { viewModel.parseImportText(input) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.imported) {
        if (state.imported) onImported()
    }
    ImportReviewContent(
        state = state,
        onDismissUnmatched = viewModel::onDismissUnmatched,
        onConfirm = viewModel::onConfirmImport,
        onCancel = onCancel
    )
}

@Composable
fun ImportReviewScreen(
    viewModel: ImportReviewViewModel,
    result: ImportResult,
    onCancel: () -> Unit,
    onImported: () -> Unit
) {
    LaunchedEffect(result) { viewModel.setImportResult(result) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.imported) {
        if (state.imported) onImported()
    }

    ImportReviewContent(
        state = state,
        onDismissUnmatched = viewModel::onDismissUnmatched,
        onConfirm = viewModel::onConfirmImport,
        onCancel = onCancel
    )
}

@Composable
private fun ImportReviewContent(
    state: ImportReviewUiState,
    onDismissUnmatched: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.xl)) {
            Text(
                text = "Rivedi importazione",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Controlla esercizi e serie prima di salvarli.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.lg))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                itemsIndexed(state.editedWorkouts) { index, workout ->
                    WorkoutReviewCard(
                        index = index,
                        workout = workout,
                        primaryMuscle = state.primaryMuscleNames[workout.exerciseName].orEmpty()
                    )
                }
                itemsIndexed(state.unmatched) { index, unmatched ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text("Non riconosciuto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(unmatched.line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                if (unmatched.suggestions.isNotEmpty()) {
                                    Text(
                                        text = "Possibili corrispondenze: ${unmatched.suggestions.joinToString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { onDismissUnmatched(index) }) { Text("Rimuovi") }
                        }
                    }
                }
                if (state.editedWorkouts.isEmpty() && state.unmatched.isEmpty()) {
                    item {
                        Text(
                            text = (state.importResult as? ImportResult.Failure)?.message ?: "Nessun esercizio da importare.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(Spacing.sm))
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(Spacing.md))
            PlateauButton(
                text = if (state.isImporting) "Importazione…" else "Importa ${state.editedWorkouts.size} esercizi",
                onClick = onConfirm,
                enabled = state.editedWorkouts.isNotEmpty() && !state.isImporting && !state.imported,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Annulla")
            }
        }
    }
}

@Composable
private fun WorkoutReviewCard(index: Int, workout: ParsedWorkout, primaryMuscle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(workout.exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("${workout.sets.size} serie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MuscleChip(primaryMuscle.ifBlank { "Importato" }, emphasized = primaryMuscle.isNotBlank())
            }
            workout.sets.forEachIndexed { setIndex, set ->
                Text(
                    text = "${setIndex + 1}. ${set.weight} kg × ${set.reps}${set.rpe?.let { " @ $it" } ?: ""}${if (set.isDropSet) " · drop set" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(name = "Import review · scuro", showBackground = true)
@Composable
private fun ImportReviewDarkPreview() = PlateauTheme(darkTheme = true) {
    ImportReviewContent(
        state = ImportReviewUiState(
            importResult = ImportResult.Success(emptyList()),
            editedWorkouts = listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10), ParsedSet(80.0, 10), ParsedSet(80.0, 10)))),
            unmatched = listOf(ImportResult.UnmatchedLine("Esercizio sconosciuto 3x10 40kg", listOf("Panca piana")))
        ),
        onDismissUnmatched = {}, onConfirm = {}, onCancel = {}
    )
}

@Preview(name = "Import review · chiaro", showBackground = true)
@Composable
private fun ImportReviewLightPreview() = PlateauTheme(darkTheme = false) {
    ImportReviewContent(
        state = ImportReviewUiState(
            importResult = ImportResult.Success(emptyList()),
            editedWorkouts = listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10), ParsedSet(80.0, 10), ParsedSet(80.0, 10))))
        ),
        onDismissUnmatched = {}, onConfirm = {}, onCancel = {}
    )
}
