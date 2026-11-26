package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.SetLogDao
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.domain.model.E1RMTrend
import com.plateau.app.domain.model.E1RMDataPoint
import com.plateau.app.domain.model.VolumeMetrics
import com.plateau.app.domain.model.WeeklyVolumePoint
import com.plateau.app.domain.model.PR
import com.plateau.app.domain.model.DoubleProgressionSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val setLogDao: SetLogDao = database.setLogDao()
    private val exerciseDao: ExerciseDao = database.exerciseDao()

    fun getE1RMTrend(exerciseId: String, days: Int = 30): Flow<E1RMTrend?> = setLogDao.getByExerciseId(exerciseId, 100)
        .map { logs ->
            if (logs.isEmpty()) return@map null
            val exercise = exerciseDao.getById(exerciseId)!!
            val dataPoints = logs.reversed().map { log ->
                E1RMDataPoint(log.completedAt, calculateE1RM(log.weight, log.reps), log.weight, log.reps)
            }
            val currentE1RM = dataPoints.last().e1rm
            val bestE1RM = dataPoints.maxByOrNull { it.e1rm }?.e1rm ?: currentE1RM
            val change30Days = currentE1RM - (dataPoints.firstOrNull()?.e1rm ?: currentE1RM)

            E1RMTrend(exerciseId, exercise.name, currentE1RM, bestE1RM, change30Days, dataPoints)
        }

    fun getVolumeMetrics(muscleGroupId: String): Flow<VolumeMetrics?> = setLogDao.getByExerciseId(muscleGroupId, 200)
        .map { logs ->
            // Simplified - in reality would join with exercise to get muscle group
            VolumeMetrics(muscleGroupId, muscleGroupId, 0.0, 10.0, emptyList())
        }

    fun getRecentPRs(limit: Int = 10): Flow<List<PR>> = setLogDao.getByExerciseId("", limit)
        .map { logs ->
            // Simplified implementation
            emptyList()
        }

    fun getDoubleProgressionSuggestions(): Flow<List<DoubleProgressionSuggestion>> = exerciseDao.getAll()
        .map { exercises ->
            // Simplified - would check recent sets for each exercise
            emptyList()
        }

    private fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1 + reps / 30.0)
    }
}