package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "body_metrics")
@Serializable
data class BodyMetric(
    @PrimaryKey val date: Long, // epoch millis (day start)
    val weightKg: Double
)