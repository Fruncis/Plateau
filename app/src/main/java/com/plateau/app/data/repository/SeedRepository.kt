package com.plateau.app.data.repository

import androidx.room.withTransaction
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.dao.MuscleGroupDao
import com.plateau.app.data.local.seed.CatalogSeed
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val muscleGroupDao: MuscleGroupDao = database.muscleGroupDao()

    /**
     * Seeds the catalog when either table is empty. Both inserts run in one transaction
     * (all or nothing) and use OnConflictStrategy.REPLACE, so a reseed is idempotent.
     */
    suspend fun seedIfEmpty() {
        database.withTransaction {
            val musclesEmpty = muscleGroupDao.getAllList().isEmpty()
            val exercisesEmpty = exerciseDao.getAll().first().isEmpty()
            if (musclesEmpty || exercisesEmpty) {
                muscleGroupDao.insertAll(CatalogSeed.muscleGroups)
                exerciseDao.insertAll(CatalogSeed.exercises)
            }
        }
    }
}
