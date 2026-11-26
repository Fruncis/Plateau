package com.plateau.app.ui.progress

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.Plan
import com.plateau.app.data.local.entity.PlanDay
import com.plateau.app.data.local.entity.PlanExercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.data.repository.ProgressRepository
import com.plateau.app.domain.model.E1RMDataPoint
import com.plateau.app.domain.model.E1RMTrend
import com.plateau.app.domain.model.PlanDayWithExercises
import com.plateau.app.domain.model.PlanDetail
import com.plateau.app.domain.model.PlanExerciseWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val progressRepo: ProgressRepository = mock()
    private val planRepo: PlanRepository = mock()
    private val logs = MutableStateFlow<List<SetLog>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(progressRepo.getSetLogsSince(any())).thenReturn(logs)
        whenever(progressRepo.getRecentPRs(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(progressRepo.getDoubleProgressionSuggestions()).thenReturn(flowOf(emptyList()))
        whenever(progressRepo.getVolumeMetrics(any(), any(), any(), any())).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun exercise(id: String, name: String, muscle: String) = Exercise(
        id = id, name = name, aliases = "[]", primaryMuscleId = muscle, secondaryMuscleId = null,
        movementPattern = "push", equipment = "barbell", difficulty = 2, isCompound = true,
        instructions = "", source = "catalog"
    )

    private fun planned(id: String, name: String, muscle: String, sets: Int) = PlanExerciseWithDetails(
        PlanExercise("pe_$id", "day1", id, 0, sets, 6, 10, null, 90, null), exercise(id, name, muscle), emptyList()
    )

    private fun plan(vararg items: PlanExerciseWithDetails) = PlanDetail(
        Plan("plan", "Piano", 2, "BULK", 0L, "active", "generated", null, 0L, 0L),
        listOf(PlanDayWithExercises(PlanDay("day1", "plan", null, "Giorno 1", 0), items.toList()))
    )

    private fun log(exerciseId: String, i: Int) = SetLog("l_${exerciseId}_$i", "s", exerciseId, i, 8, 80.0, null, 1L)

    private fun trend(id: String, name: String) =
        E1RMTrend(id, name, 100.0, 105.0, 5.0, listOf(E1RMDataPoint(1L, 100.0, 80.0, 8)))

    private fun viewModel(detail: PlanDetail?): ProgressViewModel {
        whenever(planRepo.getActivePlanDetail()).thenReturn(flowOf(detail))
        return ProgressViewModel(progressRepo, planRepo)
    }

    private fun stubTrends() {
        whenever(progressRepo.getE1RMTrend(any(), any())).thenAnswer { inv ->
            val id = inv.getArgument<String>(0)
            flowOf(trend(id, if (id == "bench") "Panca piana" else "Squat"))
        }
    }

    @Test
    fun `calculates week completion percentage`() = runTest {
        stubTrends()
        logs.value = (0 until 3).map { log("bench", it) }
        val vm = viewModel(plan(planned("bench", "Panca piana", "chest", 5)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(60, vm.uiState.value.weekCompletionPercent)
    }

    @Test
    fun `calculates exercise completion percentage`() = runTest {
        stubTrends()
        logs.value = (0 until 8).map { log("bench", it) } + (0 until 2).map { log("squat", it) }
        val vm = viewModel(
            plan(planned("bench", "Panca piana", "chest", 12), planned("squat", "Squat", "legs", 8))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.value
        assertEquals("bench", state.selectedExerciseId)
        assertEquals(67, state.exerciseCompletionPercent)
        assertEquals(50, state.weekCompletionPercent)
        assertEquals(mapOf("chest" to 67, "legs" to 25), state.muscleCompletionMap)
    }

    @Test
    fun `selecting an exercise updates name and e1rm trend`() = runTest {
        stubTrends()
        val vm = viewModel(
            plan(planned("squat", "Squat", "legs", 4), planned("bench", "Panca piana", "chest", 4))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals("Squat", vm.uiState.value.selectedExerciseName)

        vm.onExerciseSelected("bench")

        assertEquals("Panca piana", vm.uiState.value.selectedExerciseName)
        assertNotNull(vm.uiState.value.e1rmTrend)
        assertEquals("bench", vm.uiState.value.e1rmTrend?.exerciseId)
        assertEquals(listOf("squat", "bench"), vm.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `completion is null when no sets are planned`() = runTest {
        stubTrends()
        logs.value = listOf(log("bench", 0))
        val vm = viewModel(plan(planned("bench", "Panca piana", "chest", 0)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.value
        assertNull(state.weekCompletionPercent)
        assertNull(state.exerciseCompletionPercent)
        assertEquals(emptyMap<String, Int>(), state.muscleCompletionMap)
    }

    @Test
    fun `no active plan gives empty state`() = runTest {
        val vm = viewModel(null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertNull(vm.uiState.value.selectedExerciseId)
        assertNull(vm.uiState.value.weekCompletionPercent)
    }
}
