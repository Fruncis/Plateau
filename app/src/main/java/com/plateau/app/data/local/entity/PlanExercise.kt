package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(entity = PlanDay::class, parentColumns = ["id"], childColumns = ["planDayId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("planDayId"), Index("exerciseId")]
)
@Serializable
data class PlanExercise(
    @PrimaryKey val id: String,
    val planDayId: String,
    val exerciseId: String,
    val order: Int,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val targetWeight: Double?, // kg
    val restSec: Int,
    val notes: String? // superset, amrap, etc.
)