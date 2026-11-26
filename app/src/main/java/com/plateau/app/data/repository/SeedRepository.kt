package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.dao.MuscleGroupDao
import com.plateau.app.data.local.seed.CatalogSeed
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val muscleGroupDao: MuscleGroupDao = database.muscleGroupDao()

    suspend fun seedIfEmpty() {
        val existingMuscles = muscleGroupDao.getAllList()
        if (existingMuscles.isEmpty()) {
            muscleGroupDao.insertAll(CatalogSeed.muscleGroups)
            exerciseDao.insertAll(CatalogSeed.exercises)
        }
    }
}