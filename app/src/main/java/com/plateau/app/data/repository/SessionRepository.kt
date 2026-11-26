package com.plateau.app.data.repository

import androidx.room.withTransaction
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.WorkoutSessionDao
import com.plateau.app.data.local.dao.SetLogDao
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.data.local.entity.SetLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val sessionDao: WorkoutSessionDao = database.workoutSessionDao()
    private val setLogDao: SetLogDao = database.setLogDao()

    suspend fun startSession(planDayId: String?): WorkoutSession {
        val session = WorkoutSession(
            id = java.util.UUID.randomUUID().toString(),
            planDayId = planDayId,
            date = System.currentTimeMillis(),
            durationSec = 0
        )
        sessionDao.insert(session)
        return session
    }

    /**
     * Saves an imported workout atomically: the session and all of its [logs] are written in one
     * transaction, so a failure leaves nothing behind and a retry cannot duplicate a partial import.
     * Each log's sessionId is set to [session]'s id.
     */
    suspend fun importSession(session: WorkoutSession, logs: List<SetLog>) {
        database.withTransaction {
            sessionDao.insert(session)
            setLogDao.insertAll(logs.map { it.copy(sessionId = session.id) })
        }
    }

    suspend fun endSession(sessionId: String, durationSec: Int) {
        // Duration would be updated, in reality we'd fetch and update
    }

    fun getSession(sessionId: String): Flow<WorkoutSession?> = flow { emit(sessionDao.getById(sessionId)) }

    suspend fun logSet(setLog: SetLog) = setLogDao.insertAll(listOf(setLog))

    fun getSetsForSession(sessionId: String): Flow<List<SetLog>> = setLogDao.getBySessionId(sessionId)

    suspend fun getLastSetForExercise(exerciseId: String): Pair<Double, Int>? =
        setLogDao.getLastSet(exerciseId)?.let { it.weight to it.reps }
}