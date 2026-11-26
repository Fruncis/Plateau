package com.plateau.app.domain.model

import com.plateau.app.data.local.entity.Plan
import com.plateau.app.data.local.entity.PlanDay
import com.plateau.app.data.local.entity.PlanExercise
import com.plateau.app.data.local.entity.Exercise
import kotlinx.serialization.Serializable

@Serializable
data class PlanDetail(
    val plan: Plan,
    val days: List<PlanDayWithExercises>
)

@Serializable
data class PlanDayWithExercises(
    val day: PlanDay,
    val exercises: List<PlanExerciseWithDetails>
)

@Serializable
data class PlanExerciseWithDetails(
    val planExercise: PlanExercise,
    val exercise: Exercise,
    val lastSets: List<SetLogSummary>
)

@Serializable
data class SetLogSummary(
    val weight: Double,
    val reps: Int,
    val date: Long
)