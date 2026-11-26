package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(entity = MuscleGroup::class, parentColumns = ["id"], childColumns = ["primaryMuscleId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MuscleGroup::class, parentColumns = ["id"], childColumns = ["secondaryMuscleId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("primaryMuscleId"),
        Index("secondaryMuscleId"),
        Index("name"),
        Index("source")
    ]
)
@Serializable
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: String, // JSON array of aliases
    val primaryMuscleId: String,
    val secondaryMuscleId: String?,
    val movementPattern: String, // push, pull, hinge, squat, lunge, carry, rotation, core
    val equipment: String, // barbell, dumbbell, machine, cable, bodyweight, band
    val difficulty: Int, // 1=beginner, 2=intermediate, 3=advanced
    val isCompound: Boolean,
    val instructions: String,
    val source: String // catalog, user, imported
)