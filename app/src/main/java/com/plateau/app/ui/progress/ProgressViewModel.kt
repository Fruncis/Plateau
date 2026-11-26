package com.plateau.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.data.repository.ProgressRepository
import com.plateau.app.data.repository.ProgressionTarget
import com.plateau.app.data.repository.startOfWeekMillis
import com.plateau.app.domain.model.DoubleProgressionSuggestion
import com.plateau.app.domain.model.E1RMTrend
import com.plateau.app.domain.model.PR
import com.plateau.app.domain.model.PlanExerciseWithDetails
import com.plateau.app.domain.model.VolumeMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Esercizio selezionabile dal selettore (dal piano attivo o, senza piano, dagli esercizi registrati). */
data class ProgressExerciseOption(
    val id: String,
    val name: String,
    val primaryMuscleId: String
)

/**
 * Le percentuali sono `null` (N/A) quando nel piano non ci sono serie previste o non c'e' un piano attivo.
 * [muscleCompletionMap] contiene solo i muscoli con serie previste (percentuale mai nulla);
 * le chiavi sono gli id dei muscoli primari degli esercizi del piano.
 * Le serie registrate contano solo se appartengono a esercizi del piano attivo e, per ogni esercizio,
 * al massimo quante ne sono previste (le serie extra non compensano quelle saltate).
 * Senza piano attivo il selettore elenca gli esercizi con serie registrate (dal piu' recente), cosi'
 * e1RM, volume e record funzionano anche dopo un'importazione; restano N/D solo gli anelli.
 */
data class ProgressUiState(
    val exercises: List<ProgressExerciseOption> = emptyList(),
    val selectedExerciseId: String? = null,
    val selectedExerciseName: String? = null,
    val e1rmTrend: E1RMTrend? = null,
    val volumeMetrics: VolumeMetrics? = null,
    val prs: List<PR>? = null,
    val doubleProgression: List<DoubleProgressionSuggestion>? = null,
    val weekCompletionPercent: Int? = null,
    val exerciseCompletionPercent: Int? = null,
    val muscleCompletionMap: Map<String, Int> = emptyMap(),
    val hasActivePlan: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    planRepository: PlanRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    private val loggedOptions: Flow<List<ProgressExerciseOption>> = progressRepository.getLoggedExercises()
        .map { exercises -> exercises.map { ProgressExerciseOption(it.id, it.name, it.primaryMuscleId) } }
        .distinctUntilChanged()

    val uiState: StateFlow<ProgressUiState> = planRepository.getActivePlanDetail()
        .map { plan -> plan?.days.orEmpty().flatMap { it.exercises } }
        .distinctUntilChanged()
        .flatMapLatest { planned ->
            if (planned.isEmpty()) {
                combine(loggedOptions, selectedId) { options, selected -> buildState(planned, options, selected) }
            } else {
                selectedId.map { selected -> buildState(planned, planOptions(planned), selected) }
            }
        }
        .flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun onExerciseSelected(exerciseId: String) {
        selectedId.value = exerciseId
    }

    private fun planOptions(planned: List<PlanExerciseWithDetails>) = planned.distinctBy { it.exercise.id }
        .map { ProgressExerciseOption(it.exercise.id, it.exercise.name, it.exercise.primaryMuscleId) }

    private fun buildState(
        planned: List<PlanExerciseWithDetails>,
        options: List<ProgressExerciseOption>,
        selected: String?
    ): Flow<ProgressUiState> {
        val hasPlan = planned.isNotEmpty()
        val current = options.firstOrNull { it.id == selected } ?: options.firstOrNull()
            ?: return progressRepository.getRecentPRs().map { prs -> ProgressUiState(prs = prs, hasActivePlan = hasPlan) }
        val weekStart = startOfWeekMillis(System.currentTimeMillis())
        val plannedByExercise = planned.groupBy { it.exercise.id }
            .mapValues { (_, items) -> items.sumOf { it.planExercise.sets } }
        val muscleOf = options.associate { it.id to it.primaryMuscleId }
        val plannedByMuscle = plannedByExercise.entries
            .groupBy({ muscleOf.getValue(it.key) }, { it.value })
            .mapValues { it.value.sum() }
        val targets = planned.map {
            ProgressionTarget(
                it.exercise.id, it.exercise.name, it.planExercise.sets, it.planExercise.repsMin, it.planExercise.repsMax
            )
        }
        val suggestions = if (hasPlan) progressRepository.getDoubleProgressionSuggestions(targets) else flowOf(emptyList())
        return combine(
            progressRepository.getSetLogsSince(weekStart),
            progressRepository.getE1RMTrend(current.id),
            progressRepository.getVolumeMetrics(current.primaryMuscleId),
            progressRepository.getRecentPRs(),
            suggestions
        ) { logs, trend, volume, prs, progression ->
            // Per esercizio contano al massimo le serie previste: 10 di panca non coprono 5 di squat saltate.
            val doneByExercise = logs.groupingBy { it.exerciseId }.eachCount()
                .filterKeys { it in plannedByExercise }
                .mapValues { (id, done) -> minOf(done, plannedByExercise.getValue(id)) }
            val doneByMuscle = doneByExercise.entries
                .groupBy({ muscleOf.getValue(it.key) }, { it.value })
                .mapValues { it.value.sum() }
            ProgressUiState(
                exercises = options,
                selectedExerciseId = current.id,
                selectedExerciseName = current.name,
                e1rmTrend = trend,
                volumeMetrics = volume,
                prs = prs,
                doubleProgression = progression,
                weekCompletionPercent = completionPercent(doneByExercise.values.sum(), plannedByExercise.values.sum()),
                exerciseCompletionPercent = plannedByExercise[current.id]?.let { completionPercent(doneByExercise[current.id] ?: 0, it) },
                muscleCompletionMap = plannedByMuscle.mapNotNull { (muscle, plannedSets) ->
                    completionPercent(doneByMuscle[muscle] ?: 0, plannedSets)?.let { muscle to it }
                }.toMap(),
                hasActivePlan = hasPlan
            )
        }
    }
}

internal fun completionPercent(done: Int, planned: Int): Int? {
    if (planned <= 0) return null
    if (done >= planned) return 100
    // Arrotonda al piu vicino ma non mostra 100% finche il lavoro non e completo.
    return Math.round(done.coerceAtLeast(0) * 100.0 / planned).toInt().coerceIn(0, 99)
}
