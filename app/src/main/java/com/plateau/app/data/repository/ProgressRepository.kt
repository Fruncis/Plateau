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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val setLogDao: SetLogDao = database.setLogDao()
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val muscleGroupDao = database.muscleGroupDao()

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

    /** Serie registrate da [startMillis] in poi (tutti gli esercizi). */
    fun getSetLogsSince(startMillis: Long): Flow<List<SetLog>> = setLogDao.getAllSince(startMillis)

    /**
     * Volume load (somma peso x ripetizioni, in kg) dei set degli esercizi con [muscleGroupId] come
     * muscolo primario. [VolumeMetrics.weeklyVolume] e' la settimana corrente (da lunedi' locale),
     * [VolumeMetrics.weeklyData] le ultime [weeks] settimane (dalla piu' vecchia, corrente inclusa).
     * [VolumeMetrics.targetVolume] e' la media del volume delle settimane con dati, 0.0 se non ce ne sono.
     */
    fun getVolumeMetrics(
        muscleGroupId: String,
        weeks: Int = VOLUME_WEEKS,
        now: Long = System.currentTimeMillis()
    ): Flow<VolumeMetrics?> {
        val currentWeekStart = startOfWeekMillis(now)
        val firstWeekStart = startOfWeekMillis(now, weeksBack = weeks - 1)
        return setLogDao.getByPrimaryMuscleSince(muscleGroupId, firstWeekStart).map { logs ->
            val muscleName = muscleGroupDao.getById(muscleGroupId)?.name ?: return@map null
            val weekStarts = (weeks - 1 downTo 0).map { startOfWeekMillis(now, weeksBack = it) }
            val weeklyData = weekStarts.mapIndexed { i, start ->
                val end = weekStarts.getOrNull(i + 1) ?: Long.MAX_VALUE
                WeeklyVolumePoint(start, logs.filter { it.completedAt in start until end }.sumOf { it.weight * it.reps })
            }
            val withData = weeklyData.filter { it.volume > 0.0 }
            val target = if (withData.isEmpty()) 0.0 else withData.map { it.volume }.average()
            val current = weeklyData.firstOrNull { it.weekStart == currentWeekStart }?.volume ?: 0.0
            VolumeMetrics(muscleGroupId, muscleName, current, target, weeklyData)
        }
    }

    /** Miglior set per e1RM (Epley) di ogni esercizio, dal piu' recente; isNew = set degli ultimi 7 giorni. */
    fun getRecentPRs(limit: Int = 10, now: Long = System.currentTimeMillis()): Flow<List<PR>> =
        combine(setLogDao.getAllSince(0L), exerciseDao.getAll()) { logs, exercises ->
            val names = exercises.associate { it.id to it.name }
            logs.groupBy { it.exerciseId }.mapNotNull { (exerciseId, exerciseLogs) ->
                val best = exerciseLogs.maxByOrNull { calculateE1RM(it.weight, it.reps) } ?: return@mapNotNull null
                PR(
                    exerciseId, names[exerciseId] ?: return@mapNotNull null, best.weight, best.reps,
                    calculateE1RM(best.weight, best.reps), best.completedAt, best.completedAt >= now - NEW_PR_WINDOW_MS
                )
            }.sortedByDescending { it.date }.take(limit)
        }

    /** Ancora un segnaposto: la progressione doppia non e' ancora calcolata. */
    fun getDoubleProgressionSuggestions(): Flow<List<DoubleProgressionSuggestion>> = exerciseDao.getAll()
        .map { exercises ->
            // Simplified - would check recent sets for each exercise
            emptyList()
        }

    private fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1 + reps / 30.0)
    }

    private companion object {
        const val VOLUME_WEEKS = 8
        const val NEW_PR_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** Lunedi' 00:00 (fuso locale) della settimana di [timeMillis], spostato indietro di [weeksBack] settimane. */
internal fun startOfWeekMillis(
    timeMillis: Long,
    weeksBack: Int = 0,
    zone: ZoneId = ZoneId.systemDefault()
): Long = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate()
    .with(DayOfWeek.MONDAY).minusWeeks(weeksBack.toLong())
    .atStartOfDay(zone).toInstant().toEpochMilli()
