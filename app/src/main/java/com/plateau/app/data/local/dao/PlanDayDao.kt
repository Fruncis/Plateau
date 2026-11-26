package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.PlanDay
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<PlanDay>)

    @Query("SELECT * FROM plan_days WHERE planId = :planId ORDER BY \"order\"")
    fun getByPlanId(planId: String): Flow<List<PlanDay>>

    @Query("SELECT * FROM plan_days WHERE id = :id")
    suspend fun getById(id: String): PlanDay?

    @Query("SELECT * FROM plan_days WHERE planId = :planId AND weekday = :weekday")
    suspend fun getByWeekday(planId: String, weekday: Int): PlanDay?

    @Query("DELETE FROM plan_days WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: String)
}