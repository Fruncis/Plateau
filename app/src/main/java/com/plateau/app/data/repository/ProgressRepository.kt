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
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Flow<VolumeMetrics?> {
        val currentWeekStart = startOfWeekMillis(now, zone = zone)
        val firstWeekStart = startOfWeekMillis(now, weeksBack = weeks - 1, zone = zone)
        return setLogDao.getByPrimaryMuscleSince(muscleGroupId, firstWeekStart).map { logs ->
            val muscleName = muscleGroupDao.getById(muscleGroupId)?.name ?: return@map null
            val weekStarts = (weeks - 1 downTo 0).map { startOfWeekMillis(now, weeksBack = it, zone = zone) }
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

    /** Esercizi con almeno una serie registrata, dal piu' recente (per il selettore senza scheda attiva). */
    fun getLoggedExercises(): Flow<List<Exercise>> =
        combine(setLogDao.getAllSince(0L), exerciseDao.getAll()) { logs, exercises ->
            val byId = exercises.associateBy { it.id }
            logs.sortedByDescending { it.completedAt }
                .map { it.exerciseId }
                .distinct()
                .mapNotNull { byId[it] }
        }

    /**
     * Doppia progressione per gli esercizi della scheda attiva ([targets], passati dal chiamante perche'
     * il repository non conosce la scheda): vedi [doubleProgressionSuggestions].
     */
    fun getDoubleProgressionSuggestions(
        targets: List<ProgressionTarget>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Flow<List<DoubleProgressionSuggestion>> =
        setLogDao.getAllSince(0L).map { logs -> doubleProgressionSuggestions(targets, logs, zone) }

    private fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1 + reps / 30.0)
    }

    private companion object {
        const val VOLUME_WEEKS = 8
        const val NEW_PR_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** Esercizio della scheda attiva con il suo range: [sets] serie da [repsMin]-[repsMax] ripetizioni. */
data class ProgressionTarget(
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int
)

/** Incremento di carico quando il range e' chiuso in alto. */
internal const val PROGRESSION_STEP_KG = 2.5

/**
 * Doppia progressione. Per ogni [ProgressionTarget] (senza serie registrate: saltato) guarda le serie
 * della sessione piu' recente:
 * - in alto: le prime [ProgressionTarget.sets] serie (per setIndex) sono tutte allo stesso peso con
 *   almeno [ProgressionTarget.repsMax] ripetizioni -> peso + 2,5 kg, obiettivo [ProgressionTarget.repsMin];
 * - altrimenti: stesso peso (il peso massimo della sessione), obiettivo min(ripetizioni + 1, repsMax),
 *   dove "ripetizioni" sono le minime fatte a quel peso (la serie da recuperare).
 * [DoubleProgressionSuggestion.weeksAtTop] conta le settimane consecutive (da lunedi', fuso [zone]),
 * a partire da quella della sessione piu' recente, la cui ultima sessione era in alto; 0 se non lo e'.
 */
internal fun doubleProgressionSuggestions(
    targets: List<ProgressionTarget>,
    logs: List<SetLog>,
    zone: ZoneId
): List<DoubleProgressionSuggestion> {
    val logsByExercise = logs.groupBy { it.exerciseId }
    return targets.distinctBy { it.exerciseId }.mapNotNull { target ->
        val sessions = logsByExercise[target.exerciseId].orEmpty()
            .groupBy { it.sessionId }.values
            .map { sets -> sets.sortedBy { it.setIndex } }
            .sortedByDescending { sets -> sets.maxOf { it.completedAt } }
        val last = sessions.firstOrNull() ?: return@mapNotNull null
        val atTop = isAtTop(last, target)
        val workingWeight = last.maxOf { it.weight }
        val lastReps = last.filter { it.weight == workingWeight }.minOf { it.reps }
        DoubleProgressionSuggestion(
            exerciseId = target.exerciseId,
            exerciseName = target.exerciseName,
            currentWeight = workingWeight,
            currentReps = lastReps,
            targetReps = if (atTop) target.repsMin else minOf(lastReps + 1, target.repsMax),
            suggestedWeight = if (atTop) workingWeight + PROGRESSION_STEP_KG else workingWeight,
            weeksAtTop = if (atTop) weeksAtTop(sessions, target, zone) else 0
        )
    }
}

private fun isAtTop(sessionSets: List<SetLog>, target: ProgressionTarget): Boolean {
    if (target.sets <= 0) return false
    val planned = sessionSets.take(target.sets)
    return planned.size == target.sets &&
        planned.all { it.reps >= target.repsMax } &&
        planned.map { it.weight }.distinct().size == 1
}

/** [sessions] dalla piu' recente; conta le settimane consecutive la cui ultima sessione e' in alto. */
private fun weeksAtTop(sessions: List<List<SetLog>>, target: ProgressionTarget, zone: ZoneId): Int {
    val latestPerWeek = sessions
        .groupBy { sets -> startOfWeekMillis(sets.maxOf { it.completedAt }, zone = zone) }
        .mapValues { (_, weekSessions) -> weekSessions.first() } // sessions are most recent first
        .toSortedMap(compareByDescending { it })
    var expectedWeek = latestPerWeek.firstKey()
    var count = 0
    for ((week, session) in latestPerWeek) {
        if (week != expectedWeek || !isAtTop(session, target)) break
        count++
        expectedWeek = startOfWeekMillis(week, weeksBack = 1, zone = zone)
    }
    return count
}

/** Lunedi' 00:00 (fuso locale) della settimana di [timeMillis], spostato indietro di [weeksBack] settimane. */
internal fun startOfWeekMillis(
    timeMillis: Long,
    weeksBack: Int = 0,
    zone: ZoneId = ZoneId.systemDefault()
): Long = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate()
    .with(DayOfWeek.MONDAY).minusWeeks(weeksBack.toLong())
    .atStartOfDay(zone).toInstant().toEpochMilli()
