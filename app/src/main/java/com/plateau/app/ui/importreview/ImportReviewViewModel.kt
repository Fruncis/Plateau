package com.plateau.app.ui.importreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.SeedRepository
import com.plateau.app.data.repository.SessionRepository
import com.plateau.app.domain.service.ImportParser
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.domain.service.ParsedSet
import com.plateau.app.domain.service.ParsedWorkout
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ImportReviewViewModel internal constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val seedRepository: SeedRepository,
    private val parseDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        sessionRepository: SessionRepository,
        exerciseRepository: ExerciseRepository,
        seedRepository: SeedRepository
    ) : this(sessionRepository, exerciseRepository, seedRepository, Dispatchers.Default)

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

    private var loadedInput: String? = null

    fun parseImportText(input: String) {
        // Re-entering composition (rotation, recreation) must not discard the user's edits.
        if (loadedInput == input) return
        loadedInput = input
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            runCatching {
                // Cold start from a share: the catalog may still be seeding.
                seedRepository.seeded.first { it }
                val catalog = exerciseRepository.getAllExercises().first()
                val result = withContext(parseDispatcher) { ImportParser.parse(input, catalog) }
                val exercisesByName = catalog.associateBy { it.name.trim().lowercase() }
                val muscleNames = result.workoutsOrEmpty().mapNotNull { workout ->
                    val exercise = exercisesByName[workout.exerciseName.trim().lowercase()] ?: return@mapNotNull null
                    exercise.name to (exerciseRepository.getMuscleGroupName(exercise.primaryMuscleId) ?: "")
                }.toMap()
                result to muscleNames
            }.onSuccess { (result, muscleNames) -> setImportResult(result, muscleNames) }
                .onFailure { throwable ->
                    loadedInput = null
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = throwable.message ?: "Testo non leggibile"
                    )
                }
        }
    }

    fun loadMuscleGroups() {
        viewModelScope.launch {
            runCatching { exerciseRepository.getMuscleGroups().first() }
                .onSuccess { _uiState.value = _uiState.value.copy(muscleGroups = it) }
        }
    }

    /** Exercise text of an unmatched line: everything before the set notation. */
    fun exerciseTextOf(line: String): String = ImportParser.splitLine(line).exerciseName

    /** Adds an unmatched line as a new exercise, or reuses an existing exercise with the same name. */
    fun onAddAsNew(index: Int, name: String, muscleId: String) = addLine(index, name, muscleId)

    /** Resolves an unmatched line to one of its suggested (existing) exercises. */
    fun onUseSuggestion(index: Int, exerciseName: String) = addLine(index, exerciseName, muscleId = null)

    /** [muscleId] null: the exercise must already exist in the catalog (nothing is inserted). */
    private fun addLine(index: Int, name: String, muscleId: String?) {
        val unmatched = _uiState.value.unmatched.getOrNull(index) ?: return
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val catalog = exerciseRepository.getAllExercises().first()
                val existing = catalog.firstOrNull { it.name.trim().equals(cleanName, ignoreCase = true) }
                if (existing == null && muscleId == null) error("Esercizio non trovato nel catalogo: $cleanName")
                val candidate = existing ?: Exercise(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    aliases = "[]",
                    primaryMuscleId = muscleId!!,
                    secondaryMuscleId = null,
                    movementPattern = "push",
                    equipment = "bodyweight",
                    difficulty = 1,
                    isCompound = false,
                    instructions = "",
                    source = "imported"
                )
                val line = candidate.name + ImportParser.splitLine(unmatched.line).notation
                val parsed = ImportParser.parse(line, if (existing == null) catalog + candidate else catalog)
                    as? ImportResult.Success
                if (parsed != null) {
                    if (existing == null) exerciseRepository.insertExercise(candidate)
                    val muscleName = exerciseRepository.getMuscleGroupName(candidate.primaryMuscleId).orEmpty()
                    Triple(parsed, candidate.name, muscleName)
                } else null
            }.onSuccess { added ->
                val state = _uiState.value
                if (added == null) {
                    _uiState.value = state.copy(error = "Formato serie non riconosciuto: ${unmatched.line}")
                } else {
                    val (parsed, exerciseName, muscleName) = added
                    _uiState.value = state.copy(
                        editedWorkouts = state.editedWorkouts + parsed.workouts,
                        unmatched = state.unmatched.toMutableList().also { it.remove(unmatched) },
                        primaryMuscleNames = state.primaryMuscleNames + (exerciseName to muscleName),
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Impossibile aggiungere l'esercizio")
            }
        }
    }

    /** Replaces the workout at [index]; ignored when any set is invalid (see [isValidSet]). */
    fun onEditWorkout(index: Int, workout: ParsedWorkout) {
        val updated = _uiState.value.editedWorkouts.toMutableList()
        if (index !in updated.indices) return
        if (workout.sets.isEmpty() || !workout.sets.all(::isValidSet)) return
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
                val catalog = exerciseRepository.getAllExercises().first()
                val now = System.currentTimeMillis()
                val session = WorkoutSession(
                    id = UUID.randomUUID().toString(), planDayId = null, date = now, durationSec = 0
                )
                val logs = buildImportLogs(_uiState.value.editedWorkouts, catalog, session.id, now)
                sessionRepository.importSession(session, logs)
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

/** Reps at least 1, weight a finite number of at least 0 kg (0 = bodyweight). */
internal fun isValidSet(set: ParsedSet): Boolean = set.reps >= 1 && set.weight.isFinite() && set.weight >= 0.0

/**
 * One [SetLog] per parsed set. The set index runs per exercise across the whole import, so two lines
 * for the same exercise ("Panca piana 3x10 80kg" + "Panca piana 3x8 85kg") never collide on the
 * unique (sessionId, exerciseId, setIndex) index. Throws when an exercise is not in [catalog].
 */
internal fun buildImportLogs(
    workouts: List<ParsedWorkout>,
    catalog: List<Exercise>,
    sessionId: String,
    completedAt: Long
): List<SetLog> {
    val catalogByName = catalog.associateBy { it.name.trim().lowercase() }
    val nextIndex = mutableMapOf<String, Int>()
    return workouts.flatMap { workout ->
        val exercise = catalogByName[workout.exerciseName.trim().lowercase()]
            ?: error("Esercizio non trovato nel catalogo: ${workout.exerciseName}")
        workout.sets.map { set ->
            val setIndex = nextIndex.getOrDefault(exercise.id, 0)
            nextIndex[exercise.id] = setIndex + 1
            SetLog(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = exercise.id,
                setIndex = setIndex,
                reps = set.reps,
                weight = set.weight,
                rpe = set.rpe,
                completedAt = completedAt
            )
        }
    }
}

data class ImportReviewUiState(
    val importResult: ImportResult? = null,
    val editedWorkouts: List<ParsedWorkout> = emptyList(),
    val unmatched: List<ImportResult.UnmatchedLine> = emptyList(),
    val primaryMuscleNames: Map<String, String> = emptyMap(),
    val muscleGroups: List<MuscleGroup> = emptyList(),
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
