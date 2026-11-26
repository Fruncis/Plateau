package com.plateau.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plateau.app.domain.model.PlanDetail
import com.plateau.app.domain.usecase.GetActivePlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.plateau.app.domain.model.PlanDayWithExercises
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePlanUseCase: GetActivePlanUseCase
) : ViewModel() {

    val activePlan: StateFlow<PlanDetail?> = getActivePlanUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 1=Lun ... 7=Dom, come PlanDay.weekday
    val currentWeekday: Int =
        (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1

    fun getTodaysWorkout(plan: PlanDetail?): PlanDayWithExercises? {
        return plan?.days?.find { day ->
            day.day.weekday == currentWeekday
        }
    }
}