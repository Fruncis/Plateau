package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "user_profile")
@Serializable
data class UserProfile(
    @PrimaryKey val id: String = "profile",
    val sex: String, // M, F
    val birthYear: Int,
    val heightCm: Double,
    val level: Int, // 1=beginner, 2=intermediate, 3=advanced
    val equipment: String // JSON array of available equipment
)