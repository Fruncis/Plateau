package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "muscle_groups")
@Serializable
data class MuscleGroup(
    @PrimaryKey val id: String,
    val name: String,
    val nameEn: String,
    val displayOrder: Int = 0
)