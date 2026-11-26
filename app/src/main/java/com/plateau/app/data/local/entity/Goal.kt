package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "goals", indices = [Index("fromDate")])
@Serializable
data class Goal(
    @PrimaryKey val id: String,
    val type: String, // BULK, CUT, MAINTENANCE
    val fromDate: Long, // epoch millis
    val toDate: Long? // null if current
)