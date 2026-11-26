package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "photo_analyses",
    foreignKeys = [
        ForeignKey(entity = BodyPhoto::class, parentColumns = ["id"], childColumns = ["photoId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BodyPhoto::class, parentColumns = ["id"], childColumns = ["comparedToPhotoId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("photoId"), Index("comparedToPhotoId")]
)
@Serializable
data class PhotoAnalysis(
    @PrimaryKey val id: String,
    val photoId: String,
    val comparedToPhotoId: String?,
    val regionMetrics: String, // JSON map of region -> metric
    val confidence: Double, // 0-1
    val modelVersion: String
)