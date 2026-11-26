package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.UserProfileDao
import com.plateau.app.data.local.dao.BodyMetricDao
import com.plateau.app.data.local.dao.GoalDao
import com.plateau.app.data.local.entity.UserProfile
import com.plateau.app.data.local.entity.BodyMetric
import com.plateau.app.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val database: PlateauDatabase
) {
    private val profileDao: UserProfileDao = database.userProfileDao()
    private val bodyMetricDao: BodyMetricDao = database.bodyMetricDao()
    private val goalDao: GoalDao = database.goalDao()

    fun getProfile(): Flow<UserProfile?> = profileDao.getProfile()

    suspend fun getProfileOnce(): UserProfile? = profileDao.getProfileOnce()

    suspend fun saveProfile(profile: UserProfile) = profileDao.insert(profile)

    suspend fun updateProfile(profile: UserProfile) = profileDao.update(profile)

    suspend fun addBodyMetric(metric: BodyMetric) = bodyMetricDao.insert(metric)

    fun getRecentBodyMetrics(limit: Int): Flow<List<BodyMetric>> = bodyMetricDao.getRecent(limit)

    fun getBodyMetricsRange(startDate: Long, endDate: Long): Flow<List<BodyMetric>> = bodyMetricDao.getByDateRange(startDate, endDate)

    suspend fun getWeightsSince(startDate: Long): List<Double> = bodyMetricDao.getWeightsSince(startDate)

    fun getCurrentGoal(): Flow<Goal?> = goalDao.getCurrentGoal()

    suspend fun setGoal(goal: Goal) = goalDao.insert(goal)

    suspend fun updateGoal(goal: Goal) = goalDao.update(goal)
}