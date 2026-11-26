package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.dao.MuscleGroupDao
import com.plateau.app.data.local.dao.SetLogDao
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.domain.model.ExerciseDetail
import com.plateau.app.domain.model.SetLogSummary
import com.plateau.app.domain.model.VolumeTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val muscleGroupDao: MuscleGroupDao = database.muscleGroupDao()
    private val setLogDao: SetLogDao = database.setLogDao()

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAll()

    fun getExercisesByMuscle(muscleId: String): Flow<List<Exercise>> = exerciseDao.getByMuscle(muscleId)

    fun getExercisesByDifficultyAndEquipment(maxDifficulty: Int, equipment: List<String>): Flow<List<Exercise>> =
        exerciseDao.getByDifficultyAndEquipment(maxDifficulty, equipment)

    fun searchExercises(query: String): Flow<List<Exercise>> = exerciseDao.search("%$query%")

    fun getExerciseDetail(exerciseId: String): Flow<ExerciseDetail?> = exerciseDao.getById(exerciseId).map { exercise ->
        exercise?.let { buildExerciseDetail(it) }
    }.distinctUntilChanged()

    private fun buildExerciseDetail(exercise: Exercise): ExerciseDetail {
        val primaryMuscle = muscleGroupDao.getById(exercise.primaryMuscleId)!!
        val secondaryMuscle = exercise.secondaryMuscleId?.let { muscleGroupDao.getById(it) }

        val lastSet = setLogDao.getLastSet(exercise.id)?.let { (weight, reps) ->
            SetLogSummary(weight, reps, System.currentTimeMillis())
        }

        val e1rm = lastSet?.let { calculateE1RM(it.weight, it.reps) }

        val volumeTrend = VolumeTrend(weeklyVolume = 0.0, targetVolume = 10.0, trend = "stable")

        return ExerciseDetail(exercise, primaryMuscle, secondaryMuscle, lastSet, e1rm, volumeTrend)
    }

    private fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1 + reps / 30.0) // Epley formula
    }

    fun getCompoundExercises(): Flow<List<Exercise>> = exerciseDao.getCompoundExercises()

    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertAll(exercises)

    suspend fun insertMuscleGroups(groups: List<MuscleGroup>) = muscleGroupDao.insertAll(groups)
}