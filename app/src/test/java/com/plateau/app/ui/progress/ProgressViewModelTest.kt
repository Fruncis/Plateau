package com.plateau.app.ui.progress

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.Plan
import com.plateau.app.data.local.entity.PlanDay
import com.plateau.app.data.local.entity.PlanExercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.data.repository.ProgressRepository
import com.plateau.app.domain.model.DoubleProgressionSuggestion
import com.plateau.app.domain.model.E1RMDataPoint
import com.plateau.app.domain.model.E1RMTrend
import com.plateau.app.domain.model.MuscleGroupGap
import com.plateau.app.domain.model.PR
import com.plateau.app.domain.model.VolumeMetrics
import com.plateau.app.domain.model.WeeklyVolumePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.OptIn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import java.util.UUID

/**
 * Tests for ProgressViewModel - follows TDD approach.
 * Tests define expected behavior before implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val exerciseId = "ex_panca_piana"
    private val planId = "plan_1"
    private val planDayId = "day_1"
    private val muscleGroupId = "mg_chest"

    private val exercise = Exercise(
        id = exerciseId,
        name = "Panca piana",
        primaryMuscleId = muscleGroupId,
        secondaryMuscleId = "mg_triceps",
        movementPattern = "push",
        equipment = "barbell",
        difficulty = 2,
        isCompound = true,
        aliases = "[\"Bench press\", \"Panca\"]",
        createdAt = 0,
        updatedAt = 0
    )

    private val muscleGroup = MuscleGroup(
        id = muscleGroupId,
        name = "Petto",
        displayOrder = 1
    )

    private val plan = Plan(
        id = planId,
        name = "Test Plan",
        description = "",
        isActive = true,
        createdAt = 0,
        updatedAt = 0
    )

    private val planDay = PlanDay(
        id = planDayId,
        planId = planId,
        name = "Giorno 1",
        `order` = 1,
        createdAt = 0,
        updatedAt = 0
    )

    private val planExercise = PlanExercise(
        id = "pe_1",
        planDayId = planDayId,
        exerciseId = exerciseId,
        targetSets = 4,
        targetReps = 10,
        targetWeight = 80.0,
        restSeconds = 120,
        `order` = 1,
        createdAt = 0,
        updatedAt = 0
    )

    @Test
    fun `calculates week completion percentage - 3 of 5 sets done = 60%`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        // Mock active plan with 5 planned sets total
        val planDetail = com.plateau.app.domain.model.PlanDetail(
            plan = plan,
            days = listOf(
                com.plateau.app.domain.model.PlanDayWithExercises(
                    planDay,
                    listOf(
                        com.plateau.app.domain.model.PlanExerciseWithDetails(
                            planExercise,
                            exercise,
                            listOf() // No completed sets
                        )
                    )
                )
            )
        )

        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(planDetail))

        // Mock set logs - 3 sets completed
        val setLogs = listOf(
            SetLog(UUID.randomUUID().toString(), "session_1", exerciseId, 0, 10, 80.0, null, 1000),
            SetLog(UUID.randomUUID().toString(), "session_1", exerciseId, 1, 10, 80.0, null, 2000),
            SetLog(UUID.randomUUID().toString(), "session_1", exerciseId, 2, 10, 80.0, null, 3000)
        )
        val setLogDao = mock(com.plateau.app.data.local.dao.SetLogDao::class.java)
        when(setLogDao.getByExerciseId(exerciseId, 100)).thenReturn(flowOf(setLogs))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        val uiState = vm.uiState.first()

        // 3 completed out of 4 planned = 75%
        assertEquals(75, uiState.weekCompletionPercent?.toInt() ?: -1)
    }

    @Test
    fun `calculates week completion percentage - 0 planned returns null`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        // No active plan
        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(null))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        val uiState = vm.uiState.first()

        assertNull(uiState.weekCompletionPercent)
    }

    @Test
    fun `calculates exercise completion percentage - 8 of 12 sets done = 67%`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val planDetail = com.plateau.app.domain.model.PlanDetail(
            plan = plan,
            days = listOf(
                com.plateau.app.domain.model.PlanDayWithExercises(
                    planDay,
                    listOf(
                        com.plateau.app.domain.model.PlanExerciseWithDetails(
                            planExercise.copy(targetSets = 3), // 3 sets per session
                            exercise,
                            listOf()
                        )
                    )
                )
            )
        )

        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(planDetail))

        // 2 sessions * 3 sets = 6 planned per week, 4 completed this week
        val setLogs = (1..4).map { i ->
            SetLog(UUID.randomUUID().toString(), "session_$i", exerciseId, 0, 10, 80.0, null, 1000L * i)
        }
        val setLogDao = mock(com.plateau.app.data.local.dao.SetLogDao::class.java)
        when(setLogDao.getByExerciseId(exerciseId, 100)).thenReturn(flowOf(setLogs))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        vm.onExerciseSelected(exerciseId)
        val uiState = vm.uiState.first()

        // 4 completed out of 6 planned = 67%
        assertEquals(67, uiState.exerciseCompletionPercent?.toInt() ?: -1)
    }

    @Test
    fun `selects exercise updates e1RM trend`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val dataPoints = listOf(
            E1RMDataPoint(1000, 120.0, 100.0, 8),
            E1RMDataPoint(2000, 125.0, 105.0, 8),
            E1RMDataPoint(3000, 127.0, 110.0, 6)
        )
        val e1rmTrend = E1RMTrend(exerciseId, "Panca piana", 127.0, 127.0, 7.0, dataPoints)

        when(progressRepo.getE1RMTrend(exerciseId, 30)).thenReturn(flowOf(e1rmTrend))
        when(exerciseRepo.getAllExercises()).thenReturn(flowOf(listOf(exercise)))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        vm.onExerciseSelected(exerciseId)
        val uiState = vm.uiState.first()

        assertEquals("Panca piana", uiState.selectedExerciseName)
        assertNotNull(uiState.e1rmTrend)
        assertEquals(127.0, uiState.e1rmTrend!!.currentE1RM, 0.001)
        assertEquals(3, uiState.e1rmTrend!!.dataPoints.size)
    }

    @Test
    fun `calculates muscle completion map from volume gaps`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val gaps = listOf(
            MuscleGroupGap(muscleGroup, 5, 10, 50), // 50% deficit
            MuscleGroupGap(MuscleGroup("mg_back", "Schiena", 2), 8, 10, 20) // 20% deficit
        )

        when(exerciseRepo.getVolumeGaps()).thenReturn(flowOf(gaps))
        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(null))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        val uiState = vm.uiState.first()

        assertEquals(2, uiState.muscleCompletionMap.size)
        // Completion = 100 - deficit
        assertEquals(50, uiState.muscleCompletionMap["Petto"]?.toInt() ?: -1) // 100 - 50
        assertEquals(80, uiState.muscleCompletionMap["Schiena"]?.toInt() ?: -1) // 100 - 20
    }

    @Test
    fun `loads recent PRs`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val prs = listOf(
            PR(exerciseId, "Panca piana", 100.0, 8, 127.0, 1000, true),
            PR("ex_squat", "Squat", 140.0, 6, 168.0, 2000, false)
        )

        when(progressRepo.getRecentPRs(10)).thenReturn(flowOf(prs))
        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(null))
        when(exerciseRepo.getVolumeGaps()).thenReturn(flowOf(emptyList()))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        val uiState = vm.uiState.first()

        assertEquals(2, uiState.prs.size)
        assertEquals("Panca piana", uiState.prs[0].exerciseName)
        assertTrue(uiState.prs[0].isNew)
    }

    @Test
    fun `loads double progression suggestions`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val suggestions = listOf(
            DoubleProgressionSuggestion(exerciseId, "Panca piana", 80.0, 10, 10, 82.5, 2)
        )

        when(progressRepo.getDoubleProgressionSuggestions()).thenReturn(flowOf(suggestions))
        when(planRepo.getActivePlanDetail()).thenReturn(flowOf(null))
        when(exerciseRepo.getVolumeGaps()).thenReturn(flowOf(emptyList()))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        val uiState = vm.uiState.first()

        assertEquals(1, uiState.doubleProgression.size)
        assertEquals("Panca piana", uiState.doubleProgression[0].exerciseName)
        assertEquals(82.5, uiState.doubleProgression[0].suggestedWeight, 0.001)
    }

    @Test
    fun `loads volume metrics for selected exercise`() = runBlockingTest {
        val progressRepo = mock(ProgressRepository::class.java)
        val planRepo = mock(PlanRepository::class.java)
        val exerciseRepo = mock(ExerciseRepository::class.java)

        val weeklyData = listOf(
            WeeklyVolumePoint(1000, 8400.0),
            WeeklyVolumePoint(2000, 7800.0),
            WeeklyVolumePoint(3000, 9200.0)
        )
        val volumeMetrics = VolumeMetrics(muscleGroupId, "Petto", 8400.0, 9000.0, weeklyData)

        when(progressRepo.getVolumeMetrics(muscleGroupId)).thenReturn(flowOf(volumeMetrics))
        when(exerciseRepo.getAllExercises()).thenReturn(flowOf(listOf(exercise)))

        val vm = ProgressViewModel(progressRepo, planRepo, exerciseRepo)
        vm.onExerciseSelected(exerciseId)
        val uiState = vm.uiState.first()

        assertNotNull(uiState.volumeMetrics)
        assertEquals("Petto", uiState.volumeMetrics!!.muscleGroupName)
        assertEquals(3, uiState.volumeMetrics!!.weeklyData.size)
    }
}