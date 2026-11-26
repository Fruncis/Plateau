package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index(value = ["sessionId", "exerciseId", "setIndex"], unique = true)]
)
@Serializable
data class SetLog(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weight: Double, // kg
    val rpe: Double?,
    val completedAt: Long // epoch millis
)