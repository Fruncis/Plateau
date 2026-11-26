package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetric)

    @Query("SELECT * FROM body_metrics WHERE date = :date")
    suspend fun getByDate(date: Long): BodyMetric?

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<BodyMetric>>

    @Query("SELECT weightKg FROM body_metrics WHERE date >= :startDate ORDER BY date")
    suspend fun getWeightsSince(startDate: Long): List<Double>

    @Query("SELECT MAX(date) FROM body_metrics")
    suspend fun getLastDate(): Long?
}