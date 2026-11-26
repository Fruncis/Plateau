package com.plateau.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "plan_days",
    foreignKeys = [
        ForeignKey(entity = Plan::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("planId"), Index("weekday")]
)
@Serializable
data class PlanDay(
    @PrimaryKey val id: String,
    val planId: String,
    val weekday: Int?, // 1=Mon ... 7=Sun, null for labeled days
    val label: String, // "Giorno A", "Push", "Day 1"
    val order: Int
)