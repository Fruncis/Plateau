package com.plateau.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.MuscleGroupGap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    private val _filters = MutableStateFlow(DiscoverFilters())
    val filters: StateFlow<DiscoverFilters> = _filters.asStateFlow()
    val muscleGroups = exerciseRepository.getMuscleGroups()
    val equipmentOptions = listOf("barbell", "dumbbell", "machine", "cable", "bodyweight", "band")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredExercises = _filters.flatMapLatest { filters ->
        exerciseRepository.getFilteredExercises(
            muscleId = filters.muscleId,
            equipment = filters.equipment,
            difficulty = filters.maxDifficulty,
            movementPattern = filters.movementPattern,
            searchQuery = filters.searchQuery.trim().takeIf(String::isNotEmpty)
        )
    }

    private val recommendations = combine(
        exerciseRepository.getVolumeGaps(),
        exerciseRepository.getAllExercises()
    ) { gaps, exercises -> recommendExercises(gaps, exercises) }

    val uiState: StateFlow<DiscoverUiState> = combine(
        _filters,
        filteredExercises,
        recommendations
    ) { activeFilters, exercises, recommended ->
        DiscoverUiState(
            filteredExercises = exercises,
            recommendations = recommended,
            activeFilters = activeFilters,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun updateFilters(transform: (DiscoverFilters) -> DiscoverFilters) {
        _filters.value = transform(_filters.value)
    }

    fun onMuscleSelected(muscleId: String?) = updateFilters { it.copy(muscleId = muscleId) }
    fun onEquipmentSelected(equipment: String?) = updateFilters { it.copy(equipment = equipment) }
    fun onDifficultySelected(maxDifficulty: Int?) = updateFilters { it.copy(maxDifficulty = maxDifficulty) }
    fun onMovementPatternSelected(pattern: String?) = updateFilters { it.copy(movementPattern = pattern) }
    fun onSearchChanged(query: String) = updateFilters { it.copy(searchQuery = query) }
    fun clearFilters() { _filters.value = DiscoverFilters() }

    private fun recommendExercises(gaps: List<MuscleGroupGap>, exercises: List<Exercise>): List<ExerciseRecommendation> {
        val deficitByMuscle = gaps.associate { it.muscleGroup.id to it }
        return exercises.mapNotNull { exercise ->
            val gap = deficitByMuscle[exercise.primaryMuscleId] ?: return@mapNotNull null
            ExerciseRecommendation(exercise, gap, gap.deficitPercent / 100f)
        }.sortedByDescending { it.score }
    }
}

data class DiscoverFilters(
    val muscleId: String? = null,
    val equipment: String? = null,
    val maxDifficulty: Int? = null,
    val movementPattern: String? = null,
    val searchQuery: String = ""
) {
    val hasActiveFilters: Boolean
        get() = muscleId != null || equipment != null || maxDifficulty != null || movementPattern != null || searchQuery.isNotBlank()
}

data class ExerciseRecommendation(
    val exercise: Exercise,
    val volumeGap: MuscleGroupGap,
    val score: Float
)

data class DiscoverUiState(
    val filteredExercises: List<Exercise> = emptyList(),
    val recommendations: List<ExerciseRecommendation> = emptyList(),
    val activeFilters: DiscoverFilters = DiscoverFilters(),
    val isLoading: Boolean = true
)
