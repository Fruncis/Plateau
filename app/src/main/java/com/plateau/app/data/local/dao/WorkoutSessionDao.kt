package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: String): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE planDayId = :planDayId ORDER BY date DESC")
    fun getByPlanDayId(planDayId: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>>

    @Query("SELECT MAX(date) FROM workout_sessions")
    suspend fun getLastWorkoutDate(): Long?
}