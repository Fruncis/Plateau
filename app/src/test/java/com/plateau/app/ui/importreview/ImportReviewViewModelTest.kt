package com.plateau.app.ui.importreview

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.SessionRepository
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.domain.service.ParsedSet
import com.plateau.app.domain.service.ParsedWorkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ImportReviewViewModelTest {

    private val sessionRepository: SessionRepository = mock()
    private val exerciseRepository: ExerciseRepository = mock()

    private val bench = Exercise(
        id = "bench", name = "Panca piana", aliases = "[]", primaryMuscleId = "chest",
        secondaryMuscleId = null, movementPattern = "push", equipment = "barbell",
        difficulty = 1, isCompound = true, instructions = "", source = "catalog"
    )
    private val squat = bench.copy(id = "squat", name = "Squat", primaryMuscleId = "legs")

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(exerciseRepository.getAllExercises()).thenReturn(flowOf(listOf(bench, squat)))
        whenever(exerciseRepository.getMuscleGroupName(any())).thenReturn("Petto")
        whenever(sessionRepository.startSession(null))
            .thenReturn(WorkoutSession(id = "s1", planDayId = null, date = 0L, durationSec = 0))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ImportReviewViewModel(sessionRepository, exerciseRepository)

    @Test
    fun `confirm import starts one session and logs one set per parsed set`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Success(
                listOf(
                    ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10), ParsedSet(80.0, 8, rpe = 9))),
                    ParsedWorkout("Squat", listOf(ParsedSet(100.0, 5)))
                )
            )
        )

        vm.onConfirmImport()

        verify(sessionRepository, times(1)).startSession(null)
        val captor = argumentCaptor<SetLog>()
        verify(sessionRepository, times(3)).logSet(captor.capture())
        assertEquals(listOf("bench", "bench", "squat"), captor.allValues.map { it.exerciseId })
        assertTrue(captor.allValues.all { it.sessionId == "s1" })
        assertTrue(vm.uiState.value.imported)
    }

    @Test
    fun `dismissing an unmatched line removes it`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Partial(
                emptyList(),
                listOf(
                    ImportResult.UnmatchedLine("foo 3x10 40kg", emptyList()),
                    ImportResult.UnmatchedLine("bar 3x10 40kg", listOf("Squat"))
                )
            )
        )

        vm.onDismissUnmatched(0)

        assertEquals(listOf("bar 3x10 40kg"), vm.uiState.value.unmatched.map { it.line })
    }

    @Test
    fun `failure input yields error state`() = runTest {
        val vm = viewModel()

        vm.parseImportText("   ")

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertTrue(state.editedWorkouts.isEmpty())
        assertTrue(state.unmatched.isEmpty())
        assertTrue(!state.isImporting)
    }

    @Test
    fun `edit workout replaces sets at index`() = runTest {
        val vm = viewModel()
        vm.setImportResult(ImportResult.Success(listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10))))))

        vm.onEditWorkout(0, ParsedWorkout("Panca piana", listOf(ParsedSet(85.0, 8))))

        assertEquals(85.0, vm.uiState.value.editedWorkouts[0].sets[0].weight, 0.0)
    }

    @Test
    fun `adding unmatched line as new exercise moves it into workouts`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Partial(emptyList(), listOf(ImportResult.UnmatchedLine("Esercizio Sconosciuto 3x10 50kg", emptyList())))
        )
        assertEquals("Esercizio Sconosciuto", vm.exerciseTextOf("Esercizio Sconosciuto 3x10 50kg"))

        vm.onAddAsNew(0, "Esercizio Sconosciuto", "chest")

        val captor = argumentCaptor<Exercise>()
        verify(exerciseRepository, times(1)).insertExercise(captor.capture())
        assertEquals("imported", captor.firstValue.source)
        val state = vm.uiState.value
        assertTrue(state.unmatched.isEmpty())
        assertEquals(1, state.editedWorkouts.size)
        assertEquals(3, state.editedWorkouts[0].sets.size)
    }
}
