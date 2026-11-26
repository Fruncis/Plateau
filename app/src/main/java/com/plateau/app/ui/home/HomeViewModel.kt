package com.plateau.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.domain.model.PlanDetail
import com.plateau.app.domain.usecase.GetActivePlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePlanUseCase: GetActivePlanUseCase
) : ViewModel() {

    val activePlan: StateFlow<PlanDetail?> = getActivePlanUseCase()
        .map { it }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), null)

    val currentWeekday: Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 6=Sat

    fun getTodaysWorkout(plan: PlanDetail?): PlanDetail.PlanDayWithExercises? {
        return plan?.days?.find { day ->
            day.day.weekday?.let { it == currentWeekday } ?: false
        }
    }
}