package com.plateau.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class E1RMTrend(
    val exerciseId: String,
    val exerciseName: String,
    val currentE1RM: Double,
    val bestE1RM: Double,
    val change30Days: Double,
    val dataPoints: List<E1RMDataPoint>
)

@Serializable
data class E1RMDataPoint(
    val date: Long,
    val e1rm: Double,
    val weight: Double,
    val reps: Int
)

@Serializable
data class VolumeMetrics(
    val muscleGroupId: String,
    val muscleGroupName: String,
    val weeklyVolume: Double,
    val targetVolume: Double,
    val weeklyData: List<WeeklyVolumePoint>
)

@Serializable
data class WeeklyVolumePoint(
    val weekStart: Long,
    val volume: Double
)

@Serializable
data class PR(
    val exerciseId: String,
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val e1rm: Double,
    val date: Long,
    val isNew: Boolean
)

@Serializable
data class DoubleProgressionSuggestion(
    val exerciseId: String,
    val exerciseName: String,
    val currentWeight: Double,
    val currentReps: Int,
    val targetReps: Int,
    val suggestedWeight: Double,
    val weeksAtTop: Int
)