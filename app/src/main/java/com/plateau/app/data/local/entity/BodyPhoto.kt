package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "body_photos", indices = [Index("date")])
@Serializable
data class BodyPhoto(
    @PrimaryKey val id: String,
    val date: Long, // epoch millis
    val pose: String, // front, side, back
    val filePath: String, // internal storage path
    val qualityScore: Double, // 0-1
    val goalAtCapture: String // BULK, CUT, MAINTENANCE
)