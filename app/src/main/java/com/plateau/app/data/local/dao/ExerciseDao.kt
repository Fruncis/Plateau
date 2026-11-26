package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises ORDER BY name")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE primaryMuscleId = :muscleId ORDER BY name")
    fun getByPrimaryMuscle(muscleId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE :muscleId IN (primaryMuscleId, secondaryMuscleId) ORDER BY isCompound DESC, name")
    fun getByMuscle(muscleId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE difficulty <= :maxDifficulty AND equipment IN (:equipment) ORDER BY isCompound DESC, name")
    fun getByDifficultyAndEquipment(maxDifficulty: Int, equipment: List<String>): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE source = :source ORDER BY name")
    fun getBySource(source: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    @Query("SELECT * FROM exercises WHERE name LIKE :query OR aliases LIKE :query ORDER BY CASE WHEN name LIKE :query THEN 0 ELSE 1 END, name LIMIT 20")
    fun search(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE isCompound = 1 ORDER BY name")
    fun getCompoundExercises(): Flow<List<Exercise>>

    @Query("""
        SELECT * FROM exercises
        WHERE (:muscleId IS NULL OR primaryMuscleId = :muscleId OR secondaryMuscleId = :muscleId)
        AND (:equipment IS NULL OR equipment = :equipment)
        AND (:difficulty IS NULL OR difficulty <= :difficulty)
        AND (:movementPattern IS NULL OR movementPattern = :movementPattern)
        AND (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%' OR aliases LIKE '%' || :searchQuery || '%')
        ORDER BY isCompound DESC, name
    """)
    fun getFiltered(
        muscleId: String?,
        equipment: String?,
        difficulty: Int?,
        movementPattern: String?,
        searchQuery: String?
    ): Flow<List<Exercise>>
}