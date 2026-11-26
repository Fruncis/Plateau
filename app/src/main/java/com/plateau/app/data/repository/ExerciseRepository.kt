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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

 data class MuscleGroupGap(
    val muscleGroup: MuscleGroup,
    val currentVolume: Int,
    val targetVolume: Int,
    val deficitPercent: Int
)

@Singleton
class ExerciseRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val muscleGroupDao: MuscleGroupDao = database.muscleGroupDao()
    private val setLogDao: SetLogDao = database.setLogDao()

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAll()

    fun getMuscleGroups() = muscleGroupDao.getAll()

    suspend fun getMuscleGroupName(muscleId: String): String? = muscleGroupDao.getById(muscleId)?.name

    fun getExercisesByMuscle(muscleId: String): Flow<List<Exercise>> = exerciseDao.getByMuscle(muscleId)

    fun getExercisesByDifficultyAndEquipment(maxDifficulty: Int, equipment: List<String>): Flow<List<Exercise>> =
        exerciseDao.getByDifficultyAndEquipment(maxDifficulty, equipment)

    fun searchExercises(query: String): Flow<List<Exercise>> = exerciseDao.search("%$query%")

    fun getFilteredExercises(
        muscleId: String?,
        equipment: String?,
        difficulty: Int?,
        movementPattern: String?,
        searchQuery: String?
    ): Flow<List<Exercise>> = exerciseDao.getFiltered(
        muscleId, equipment, difficulty, movementPattern, searchQuery
    )

    fun getVolumeGaps(): Flow<List<MuscleGroupGap>> {
        val startOfWeek = System.currentTimeMillis() - WEEK_MILLIS
        return combine(exerciseDao.getAll(), setLogDao.getAllSince(startOfWeek), muscleGroupDao.getAll()) {
                exercises, logs, muscleGroups ->
            val muscleByExercise = exercises.associate { it.id to it.primaryMuscleId }
            val setsByMuscle = logs.mapNotNull { log -> muscleByExercise[log.exerciseId] }
                .groupingBy { it }
                .eachCount()
            muscleGroups.map { muscle ->
                val current = setsByMuscle[muscle.id] ?: 0
                val target = WEEKLY_SET_TARGET
                MuscleGroupGap(
                    muscleGroup = muscle,
                    currentVolume = current,
                    targetVolume = target,
                    deficitPercent = (((target - current).coerceAtLeast(0) * 100.0) / target).toInt()
                )
            }.filter { it.deficitPercent > 0 }.sortedByDescending { it.deficitPercent }
        }
    }

    fun getExerciseDetail(exerciseId: String): Flow<ExerciseDetail?> = flow {
        emit(exerciseDao.getById(exerciseId)?.let { buildExerciseDetail(it) })
    }

    private suspend fun buildExerciseDetail(exercise: Exercise): ExerciseDetail {
        val primaryMuscle = muscleGroupDao.getById(exercise.primaryMuscleId)!!
        val secondaryMuscle = exercise.secondaryMuscleId?.let { muscleGroupDao.getById(it) }

        val lastSet = setLogDao.getLastSet(exercise.id)?.let { last ->
            SetLogSummary(last.weight, last.reps, System.currentTimeMillis())
        }

        val e1rm = lastSet?.let { calculateE1RM(it.weight, it.reps) }

        val volumeTrend = VolumeTrend(weeklyVolume = 0.0, targetVolume = 10.0, trend = "stable")

        return ExerciseDetail(exercise, primaryMuscle, secondaryMuscle, lastSet, e1rm, volumeTrend)
    }

    private fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1 + reps / 30.0) // Epley formula
    }

    fun getCompoundExercises(): Flow<List<Exercise>> = exerciseDao.getCompoundExercises()

    suspend fun insertExercise(exercise: Exercise) = exerciseDao.insertAll(listOf(exercise))

    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertAll(exercises)

    suspend fun insertMuscleGroups(groups: List<MuscleGroup>) = muscleGroupDao.insertAll(groups)

    private companion object {
        const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val WEEKLY_SET_TARGET = 10
    }
}