package com.plateau.app.domain.model

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDetail(
    val exercise: Exercise,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscle: MuscleGroup?,
    val lastSet: SetLogSummary?,
    val e1rm: Double?,
    val volumeTrend: VolumeTrend?
)

@Serializable
data class SetLogSummary(
    val weight: Double,
    val reps: Int,
    val date: Long
)

@Serializable
data class VolumeTrend(
    val weeklyVolume: Double,
    val targetVolume: Double,
    val trend: String // up, down, stable
)