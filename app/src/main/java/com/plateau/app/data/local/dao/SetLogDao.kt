package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.SetLog
import kotlinx.coroutines.flow.Flow

data class LastSet(
    val weight: Double,
    val reps: Int
)

@Dao
interface SetLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SetLog>)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun getBySessionId(sessionId: String): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT :limit")
    fun getByExerciseId(exerciseId: String, limit: Int): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId AND completedAt >= :startDate ORDER BY completedAt DESC")
    fun getByExerciseIdSince(exerciseId: String, startDate: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE completedAt >= :startDate ORDER BY completedAt DESC")
    fun getAllSince(startDate: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setIndex")
    suspend fun getBySessionAndExercise(sessionId: String, exerciseId: String): List<SetLog>

    @Query("SELECT MAX(weight) FROM set_logs WHERE exerciseId = :exerciseId")
    suspend fun getMaxWeight(exerciseId: String): Double?

    @Query("SELECT weight, reps FROM set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastSet(exerciseId: String): LastSet?
}