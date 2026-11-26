package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "plans",
    foreignKeys = [
        ForeignKey(entity = Plan::class, parentColumns = ["id"], childColumns = ["parentPlanId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("status"), Index("startDate"), Index("parentPlanId")]
)
@Serializable
data class Plan(
    @PrimaryKey val id: String,
    val name: String,
    val level: Int, // 1=beginner, 2=intermediate, 3=advanced
    val goal: String, // BULK, CUT, MAINTENANCE
    val startDate: Long, // epoch millis
    val status: String, // active, draft, archived
    val origin: String, // imported, generated, rotated
    val parentPlanId: String?,
    val createdAt: Long,
    val updatedAt: Long
)