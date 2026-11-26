package com.plateau.app.ui.importreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.SessionRepository
import com.plateau.app.domain.service.ImportParser
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.domain.service.ParsedWorkout
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ImportReviewViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportReviewUiState())
    val uiState: StateFlow<ImportReviewUiState> = _uiState.asStateFlow()

    fun setImportResult(result: ImportResult, muscleNames: Map<String, String> = emptyMap()) {
        _uiState.value = ImportReviewUiState(
            importResult = result,
            editedWorkouts = result.workoutsOrEmpty(),
            unmatched = result.unmatchedOrEmpty(),
            primaryMuscleNames = muscleNames,
            error = (result as? ImportResult.Failure)?.message
        )
    }

    fun parseImportText(input: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            runCatching {
                val catalog = exerciseRepository.getAllExercises().first()
                val result = ImportParser.parse(input, catalog)
                val exercisesByName = catalog.associateBy { it.name.trim().lowercase() }
                val muscleNames = result.workoutsOrEmpty().mapNotNull { workout ->
                    val exercise = exercisesByName[workout.exerciseName.trim().lowercase()] ?: return@mapNotNull null
                    exercise.name to (exerciseRepository.getMuscleGroupName(exercise.primaryMuscleId) ?: "")
                }.toMap()
                result to muscleNames
            }.onSuccess { (result, muscleNames) -> setImportResult(result, muscleNames) }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = throwable.message ?: "Testo non leggibile"
                    )
                }
        }
    }

    fun onEditWorkout(index: Int, workout: ParsedWorkout) {
        val updated = _uiState.value.editedWorkouts.toMutableList()
        if (index !in updated.indices) return
        updated[index] = workout
        _uiState.value = _uiState.value.copy(editedWorkouts = updated)
    }

    fun onDismissUnmatched(index: Int) {
        val unmatched = _uiState.value.unmatched.toMutableList()
        if (index !in unmatched.indices) return
        unmatched.removeAt(index)
        _uiState.value = _uiState.value.copy(unmatched = unmatched)
    }

    fun onConfirmImport() {
        if (_uiState.value.isImporting || _uiState.value.imported) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            runCatching {
                val catalogByName = exerciseRepository.getAllExercises().first()
                    .associateBy { it.name.trim().lowercase() }
                val session = sessionRepository.startSession(planDayId = null)
                _uiState.value.editedWorkouts.forEach { workout ->
                    val exercise = catalogByName[workout.exerciseName.trim().lowercase()]
                        ?: error("Esercizio non trovato nel catalogo: ${workout.exerciseName}")
                    workout.sets.forEachIndexed { setIndex, set ->
                        sessionRepository.logSet(
                            SetLog(
                                id = UUID.randomUUID().toString(),
                                sessionId = session.id,
                                exerciseId = exercise.id,
                                setIndex = setIndex,
                                reps = set.reps,
                                weight = set.weight,
                                rpe = set.rpe?.toDouble(),
                                completedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isImporting = false, imported = true)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = throwable.message ?: "Importazione non riuscita"
                )
            }
        }
    }
}

data class ImportReviewUiState(
    val importResult: ImportResult? = null,
    val editedWorkouts: List<ParsedWorkout> = emptyList(),
    val unmatched: List<ImportResult.UnmatchedLine> = emptyList(),
    val primaryMuscleNames: Map<String, String> = emptyMap(),
    val isImporting: Boolean = false,
    val imported: Boolean = false,
    val error: String? = null
)

private fun ImportResult.workoutsOrEmpty(): List<ParsedWorkout> = when (this) {
    is ImportResult.Success -> workouts
    is ImportResult.Partial -> workouts
    is ImportResult.Failure -> emptyList()
}

private fun ImportResult.unmatchedOrEmpty(): List<ImportResult.UnmatchedLine> = when (this) {
    is ImportResult.Partial -> unmatchedLines
    else -> emptyList()
}
