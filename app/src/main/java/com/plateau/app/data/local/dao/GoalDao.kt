package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plateau.app.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Update
    suspend fun update(goal: Goal)

    @Query("SELECT * FROM goals WHERE toDate IS NULL ORDER BY fromDate DESC LIMIT 1")
    fun getCurrentGoal(): Flow<Goal?>

    @Query("SELECT * FROM goals ORDER BY fromDate DESC")
    fun getAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): Goal?
}