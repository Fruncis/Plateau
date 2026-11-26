package com.plateau.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoComparison(
    val currentPhotoId: String,
    val previousPhotoId: String,
    val overallChangePercent: Double,
    val confidence: Double,
    val regionMetrics: Map<String, RegionMetric>
)

@Serializable
data class RegionMetric(
    val regionName: String,
    val changePercent: Double,
    val confidence: Double,
    val isPositive: Boolean
)

@Serializable
data class WeakArea(
    val regionName: String,
    val growthPercent: Double,
    val targetGrowthPercent: Double,
    val weeklyVolume: Int,
    val targetWeeklyVolume: Int,
    val suggestion: String,
    val recommendedExercises: List<String>
)

@Serializable
data class KcalTarget(
    val bmr: Double,
    val tdee: Double,
    val targetKcal: Double,
    val proteinGrams: Double,
    val carbGrams: Double,
    val fatGrams: Double,
    val goalType: String
)