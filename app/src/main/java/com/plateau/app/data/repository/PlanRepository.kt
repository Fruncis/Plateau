package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.PlanDao
import com.plateau.app.data.local.dao.PlanDayDao
import com.plateau.app.data.local.dao.PlanExerciseDao
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.dao.SetLogDao
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.Plan
import com.plateau.app.data.local.entity.PlanDay
import com.plateau.app.data.local.entity.PlanExercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.domain.model.PlanDetail
import com.plateau.app.domain.model.PlanDayWithExercises
import com.plateau.app.domain.model.PlanExerciseWithDetails
import com.plateau.app.domain.model.SetLogSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val planDao: PlanDao = database.planDao()
    private val planDayDao: PlanDayDao = database.planDayDao()
    private val planExerciseDao: PlanExerciseDao = database.planExerciseDao()
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val setLogDao: SetLogDao = database.setLogDao()

    fun getActivePlanDetail(): Flow<PlanDetail?> = planDao.getActivePlan().map { plan ->
        plan?.let { buildPlanDetail(it) }
    }.distinctUntilChanged()

    private fun buildPlanDetail(plan: Plan): PlanDetail {
        val days = planDayDao.getByPlanId(plan.id).map { planDays ->
            planDays.map { day ->
                val exercises = planExerciseDao.getByPlanDayId(day.id).map { planExercises ->
                    planExercises.map { pe ->
                        val exercise = exerciseDao.getById(pe.exerciseId)!!
                        val lastSets = setLogDao.getByExerciseId(pe.exerciseId, 3).map { logs ->
                            logs.map { SetLogSummary(it.weight, it.reps, it.completedAt) }
                        }
                        PlanExerciseWithDetails(pe, exercise, lastSets)
                    }
                }
                PlanDayWithExercises(day, exercises)
            }
        }
        return PlanDetail(plan, days)
    }

    suspend fun insertPlan(plan: Plan) = planDao.insert(plan)

    suspend fun insertPlanDays(days: List<PlanDay>) = planDayDao.insertAll(days)

    suspend fun insertPlanExercises(exercises: List<PlanExercise>) = planExerciseDao.insertAll(exercises)

    suspend fun updatePlan(plan: Plan) = planDao.update(plan)

    fun getAllPlans(): Flow<List<Plan>> = planDao.getAll()

    suspend fun getPlanById(id: String): Plan? = planDao.getById(id)
}