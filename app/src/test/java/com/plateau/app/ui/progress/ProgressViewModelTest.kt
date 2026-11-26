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
import com.plateau.app.domain.model.PR
import com.plateau.app.domain.model.DoubleProgressionSuggestion
import com.plateau.app.domain.model.VolumeMetrics
import com.plateau.app.data.repository.ProgressionTarget
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
    private val logged = MutableStateFlow<List<Exercise>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(progressRepo.getSetLogsSince(any())).thenReturn(logs)
        whenever(progressRepo.getRecentPRs(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(progressRepo.getDoubleProgressionSuggestions(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(progressRepo.getLoggedExercises()).thenReturn(logged)
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

    private val benchPr = PR("bench", "Panca piana", 85.0, 8, 107.7, 1L, isNew = true)

    @Test
    fun `no active plan and no logs still emits recent PRs`() = runTest {
        whenever(progressRepo.getRecentPRs(any(), any())).thenReturn(flowOf(listOf(benchPr)))
        val vm = viewModel(null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.value
        assertNull(state.selectedExerciseId)
        assertNull(state.weekCompletionPercent)
        assertEquals(listOf(benchPr), state.prs)
        assertFalse(state.hasActivePlan)
    }

    @Test
    fun `no active plan with logs builds the selector from logged exercises`() = runTest {
        stubTrends()
        val volume = VolumeMetrics("legs", "Gambe", 800.0, 0.0, emptyList())
        whenever(progressRepo.getVolumeMetrics(eq("legs"), any(), any(), any())).thenReturn(flowOf(volume))
        whenever(progressRepo.getRecentPRs(any(), any())).thenReturn(flowOf(listOf(benchPr)))
        logged.value = listOf(exercise("squat", "Squat", "legs"), exercise("bench", "Panca piana", "chest"))
        logs.value = listOf(log("squat", 0), log("bench", 0))
        val vm = viewModel(null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value
        assertEquals(listOf("squat", "bench"), state.exercises.map { it.id }) // most recent first
        assertEquals("squat", state.selectedExerciseId)
        assertEquals("squat", state.e1rmTrend?.exerciseId)
        assertEquals(volume, state.volumeMetrics)
        assertEquals(listOf(benchPr), state.prs)
        assertNull(state.weekCompletionPercent)
        assertNull(state.exerciseCompletionPercent)
        assertTrue(state.muscleCompletionMap.isEmpty())
        assertEquals(emptyList<DoubleProgressionSuggestion>(), state.doubleProgression)
        verify(progressRepo, never()).getDoubleProgressionSuggestions(any(), any())

        vm.onExerciseSelected("bench")
        assertEquals("Panca piana", vm.uiState.value.selectedExerciseName)
        assertEquals("bench", vm.uiState.value.e1rmTrend?.exerciseId)
    }

    @Test
    fun `a newly logged exercise appears in the selector without a plan`() = runTest {
        stubTrends()
        val vm = viewModel(null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.uiState.value.exercises.isEmpty())

        logged.value = listOf(exercise("bench", "Panca piana", "chest"))

        assertEquals("bench", vm.uiState.value.selectedExerciseId)
    }

    @Test
    fun `extra sets on one exercise do not compensate skipped ones`() = runTest {
        stubTrends()
        logs.value = (0 until 10).map { log("bench", it) }
        val vm = viewModel(plan(planned("bench", "Panca piana", "chest", 5), planned("squat", "Squat", "chest", 5)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.value
        assertEquals(50, state.weekCompletionPercent)
        assertEquals(100, state.exerciseCompletionPercent)
        assertEquals(mapOf("chest" to 50), state.muscleCompletionMap)
    }

    @Test
    fun `double progression gets the plan targets`() = runTest {
        stubTrends()
        val suggestion = DoubleProgressionSuggestion("bench", "Panca piana", 80.0, 10, 6, 82.5, 1)
        whenever(progressRepo.getDoubleProgressionSuggestions(any(), any())).thenReturn(flowOf(listOf(suggestion)))
        val vm = viewModel(plan(planned("bench", "Panca piana", "chest", 3)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(listOf(suggestion), vm.uiState.value.doubleProgression)
        assertTrue(vm.uiState.value.hasActivePlan)
        val targets = argumentCaptor<List<ProgressionTarget>>()
        verify(progressRepo).getDoubleProgressionSuggestions(targets.capture(), any())
        assertEquals(listOf(ProgressionTarget("bench", "Panca piana", 3, 6, 10)), targets.firstValue)
    }
}
