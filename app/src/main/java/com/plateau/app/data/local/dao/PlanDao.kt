package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plateau.app.data.local.entity.Plan
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: Plan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<Plan>)

    @Update
    suspend fun update(plan: Plan)

    @Query("SELECT * FROM plans WHERE status = 'active' ORDER BY startDate DESC LIMIT 1")
    fun getActivePlan(): Flow<Plan?>

    @Query("SELECT * FROM plans WHERE status = :status ORDER BY startDate DESC")
    fun getByStatus(status: String): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getById(id: String): Plan?

    @Query("SELECT * FROM plans ORDER BY startDate DESC")
    fun getAll(): Flow<List<Plan>>

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun delete(id: String)
}