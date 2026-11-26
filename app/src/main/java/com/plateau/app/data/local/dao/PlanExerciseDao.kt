package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.PlanExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<PlanExercise>)

    @Query("SELECT * FROM plan_exercises WHERE planDayId = :planDayId ORDER BY \"order\"")
    fun getByPlanDayId(planDayId: String): Flow<List<PlanExercise>>

    @Query("SELECT * FROM plan_exercises WHERE id = :id")
    suspend fun getById(id: String): PlanExercise?

    @Query("DELETE FROM plan_exercises WHERE planDayId = :planDayId")
    suspend fun deleteByPlanDayId(planDayId: String)
}