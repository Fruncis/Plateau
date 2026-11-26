package com.plateau.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.data.repository.ProgressRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Esercizio selezionabile dal selettore (derivato dal piano attivo). */
data class ProgressExerciseOption(
    val id: String,
    val name: String,
    val primaryMuscleId: String
)

/**
 * Le percentuali sono `null` (N/A) quando nel piano non ci sono serie previste.
 * [muscleCompletionMap] contiene solo i muscoli con serie previste (percentuale mai nulla);
 * le chiavi sono gli id dei muscoli primari degli esercizi del piano.
 * Le serie registrate contano solo se appartengono a esercizi del piano attivo.
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
    val muscleCompletionMap: Map<String, Int> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    planRepository: PlanRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProgressUiState> = combine(
        planRepository.getActivePlanDetail(),
        selectedId
    ) { plan, selected -> plan?.days.orEmpty().flatMap { it.exercises } to selected }
        .flatMapLatest { (planned, selected) -> buildState(planned, selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun onExerciseSelected(exerciseId: String) {
        selectedId.value = exerciseId
    }

    private fun buildState(planned: List<PlanExerciseWithDetails>, selected: String?): Flow<ProgressUiState> {
        if (planned.isEmpty()) return flowOf(ProgressUiState())
        val options = planned.distinctBy { it.exercise.id }
            .map { ProgressExerciseOption(it.exercise.id, it.exercise.name, it.exercise.primaryMuscleId) }
        val current = options.firstOrNull { it.id == selected } ?: options.first()
        val weekStart = startOfWeekMillis(System.currentTimeMillis())
        val plannedByExercise = planned.groupBy { it.exercise.id }
            .mapValues { (_, items) -> items.sumOf { it.planExercise.sets } }
        val muscleOf = options.associate { it.id to it.primaryMuscleId }
        val plannedByMuscle = plannedByExercise.entries
            .groupBy({ muscleOf.getValue(it.key) }, { it.value })
            .mapValues { it.value.sum() }
        return combine(
            progressRepository.getSetLogsSince(weekStart),
            progressRepository.getE1RMTrend(current.id),
            progressRepository.getVolumeMetrics(current.primaryMuscleId),
            progressRepository.getRecentPRs(),
            progressRepository.getDoubleProgressionSuggestions()
        ) { logs, trend, volume, prs, suggestions ->
            val doneByExercise = logs.groupingBy { it.exerciseId }.eachCount()
                .filterKeys { it in plannedByExercise }
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
                doubleProgression = suggestions,
                weekCompletionPercent = completionPercent(doneByExercise.values.sum(), plannedByExercise.values.sum()),
                exerciseCompletionPercent = completionPercent(
                    doneByExercise[current.id] ?: 0, plannedByExercise.getValue(current.id)
                ),
                muscleCompletionMap = plannedByMuscle.mapNotNull { (muscle, plannedSets) ->
                    completionPercent(doneByMuscle[muscle] ?: 0, plannedSets)?.let { muscle to it }
                }.toMap()
            )
        }
    }
}

internal fun completionPercent(done: Int, planned: Int): Int? {
    if (planned <= 0) return null
    return Math.round(done.coerceAtLeast(0) * 100.0 / planned).toInt().coerceIn(0, 100)
}
