package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(entity = PlanDay::class, parentColumns = ["id"], childColumns = ["planDayId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("planDayId"), Index("date")]
)
@Serializable
data class WorkoutSession(
    @PrimaryKey val id: String,
    val planDayId: String?,
    val date: Long, // epoch millis
    val durationSec: Int
)