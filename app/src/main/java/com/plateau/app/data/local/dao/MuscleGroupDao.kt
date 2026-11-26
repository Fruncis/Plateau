package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(muscleGroups: List<MuscleGroup>)

    @Query("SELECT * FROM muscle_groups ORDER BY displayOrder")
    fun getAll(): Flow<List<MuscleGroup>>

    @Query("SELECT * FROM muscle_groups ORDER BY displayOrder")
    suspend fun getAllList(): List<MuscleGroup>

    @Query("SELECT * FROM muscle_groups WHERE id = :id")
    suspend fun getById(id: String): MuscleGroup?

    @Query("SELECT * FROM muscle_groups WHERE name = :name OR nameEn = :name")
    suspend fun getByName(name: String): MuscleGroup?
}