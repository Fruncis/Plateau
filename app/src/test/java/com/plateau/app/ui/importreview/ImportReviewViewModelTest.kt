package com.plateau.app.ui.importreview

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.SeedRepository
import com.plateau.app.data.repository.SessionRepository
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.domain.service.ParsedSet
import com.plateau.app.domain.service.ParsedWorkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class ImportReviewViewModelTest {

    private val sessionRepository: SessionRepository = mock()
    private val exerciseRepository: ExerciseRepository = mock()
    private val seeded = MutableStateFlow(true)
    private val seedRepository: SeedRepository = mock { on { seeded } doReturn seeded }

    private val bench = Exercise(
        id = "bench", name = "Panca piana", aliases = "[]", primaryMuscleId = "chest",
        secondaryMuscleId = null, movementPattern = "push", equipment = "barbell",
        difficulty = 1, isCompound = true, instructions = "", source = "catalog"
    )
    private val squat = bench.copy(id = "squat", name = "Squat", primaryMuscleId = "legs")
    private val catalog = MutableStateFlow(listOf(bench, squat))

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(exerciseRepository.getAllExercises()).thenReturn(catalog)
        doAnswer { catalog.value = catalog.value + (it.arguments[0] as Exercise); Unit }
            .whenever(exerciseRepository).insertExercise(any())
        whenever(exerciseRepository.getMuscleGroupName(any())).thenReturn("Petto")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(dispatcher: CoroutineDispatcher = Dispatchers.Main) =
        ImportReviewViewModel(sessionRepository, exerciseRepository, seedRepository, dispatcher)

    /** Session and logs passed to the single atomic importSession call. */
    private suspend fun importedLogs(): Pair<WorkoutSession, List<SetLog>> {
        val session = argumentCaptor<WorkoutSession>()
        val logs = argumentCaptor<List<SetLog>>()
        verify(sessionRepository, times(1)).importSession(session.capture(), logs.capture())
        verify(sessionRepository, never()).logSet(any())
        verify(sessionRepository, never()).startSession(any())
        return session.firstValue to logs.firstValue
    }

    @Test
    fun `confirm import starts one session and logs one set per parsed set`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Success(
                listOf(
                    ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10), ParsedSet(80.0, 8, rpe = 9.0))),
                    ParsedWorkout("Squat", listOf(ParsedSet(100.0, 5)))
                )
            )
        )

        vm.onConfirmImport()

        val (session, logs) = importedLogs()
        assertEquals(listOf("bench", "bench", "squat"), logs.map { it.exerciseId })
        assertTrue(logs.all { it.sessionId == session.id })
        assertNull(session.planDayId)
        assertEquals(listOf(null, 9.0, null), logs.map { it.rpe })
        assertTrue(vm.uiState.value.imported)
    }

    @Test
    fun `two lines for the same exercise get distinct set indexes`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Success(
                listOf(
                    ParsedWorkout("Panca piana", List(3) { ParsedSet(80.0, 10) }),
                    ParsedWorkout("Squat", listOf(ParsedSet(100.0, 5))),
                    ParsedWorkout("Panca piana", List(3) { ParsedSet(85.0, 8) })
                )
            )
        )

        vm.onConfirmImport()

        val (_, logs) = importedLogs()
        assertEquals(7, logs.size)
        val bench = logs.filter { it.exerciseId == "bench" }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), bench.map { it.setIndex })
        assertEquals(listOf(80.0, 80.0, 80.0, 85.0, 85.0, 85.0), bench.map { it.weight })
        assertEquals(listOf(0), logs.filter { it.exerciseId == "squat" }.map { it.setIndex })
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

    @Test
    fun `adding a line whose name already exists reuses it without inserting`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Partial(emptyList(), listOf(ImportResult.UnmatchedLine("Panca pianaa 3x10 50kg", emptyList())))
        )

        vm.onAddAsNew(0, "panca PIANA", "chest")

        verify(exerciseRepository, never()).insertExercise(any())
        val state = vm.uiState.value
        assertEquals(listOf("Panca piana"), state.editedWorkouts.map { it.exerciseName })
        assertEquals(1, catalog.value.count { it.name.equals("Panca piana", ignoreCase = true) })
    }

    @Test
    fun `adding a line with unparsable sets keeps it unmatched, shows error and does not insert`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Partial(emptyList(), listOf(ImportResult.UnmatchedLine("Nuovo 3x10", emptyList())))
        )

        vm.onAddAsNew(0, "Nuovo", "chest")

        verify(exerciseRepository, never()).insertExercise(any())
        val state = vm.uiState.value
        assertEquals(1, state.unmatched.size)
        assertTrue(state.editedWorkouts.isEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `parsing the same input again keeps user edits`() = runTest {
        val vm = viewModel()
        vm.parseImportText("Panca piana 3x10 80kg\nIgnoto 3x10 40kg")
        assertEquals(1, vm.uiState.value.unmatched.size)
        vm.onDismissUnmatched(0)

        vm.parseImportText("Panca piana 3x10 80kg\nIgnoto 3x10 40kg")

        assertTrue(vm.uiState.value.unmatched.isEmpty())
    }

    @Test
    fun `failed atomic import shows error and can be retried`() = runTest {
        val vm = viewModel()
        vm.setImportResult(ImportResult.Success(listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10))))))
        whenever(sessionRepository.importSession(any(), any())).thenThrow(IllegalStateException("disk full"))

        vm.onConfirmImport()

        assertEquals("disk full", vm.uiState.value.error)
        assertFalse(vm.uiState.value.imported)
        assertFalse(vm.uiState.value.isImporting)
    }

    @Test
    fun `workout not in catalog fails before anything is written`() = runTest {
        val vm = viewModel()
        vm.setImportResult(
            ImportResult.Success(
                listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10))), ParsedWorkout("Fantasma", listOf(ParsedSet(10.0, 10))))
            )
        )

        vm.onConfirmImport()

        verify(sessionRepository, never()).importSession(any(), any())
        assertTrue(vm.uiState.value.error!!.contains("Fantasma"))
    }

    @Test
    fun `tapping a suggestion resolves the line to that existing exercise`() = runTest {
        val vm = viewModel()
        vm.parseImportText("Squat 4x8 100kg\nPnca piana 3x8 85 kg")
        val unmatched = vm.uiState.value.unmatched.single()
        assertTrue(unmatched.suggestions.contains("Panca piana"))

        vm.onUseSuggestion(0, "Panca piana")

        verify(exerciseRepository, never()).insertExercise(any())
        val state = vm.uiState.value
        assertTrue(state.unmatched.isEmpty())
        assertEquals(listOf("Squat", "Panca piana"), state.editedWorkouts.map { it.exerciseName })
        assertEquals(List(3) { 85.0 }, state.editedWorkouts[1].sets.map { it.weight })
        assertNull(state.error)
    }

    @Test
    fun `a suggestion that is not in the catalog never inserts an exercise`() = runTest {
        val vm = viewModel()
        vm.setImportResult(ImportResult.Partial(emptyList(), listOf(ImportResult.UnmatchedLine("Xyz 3x10 50kg", listOf("Sparito")))))

        vm.onUseSuggestion(0, "Sparito")

        verify(exerciseRepository, never()).insertExercise(any())
        assertEquals(1, vm.uiState.value.unmatched.size)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `edit with invalid sets is rejected`() = runTest {
        val vm = viewModel()
        vm.setImportResult(ImportResult.Success(listOf(ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 10))))))

        vm.onEditWorkout(0, ParsedWorkout("Panca piana", listOf(ParsedSet(80.0, 0))))
        vm.onEditWorkout(0, ParsedWorkout("Panca piana", listOf(ParsedSet(-5.0, 8))))
        vm.onEditWorkout(0, ParsedWorkout("Panca piana", listOf(ParsedSet(Double.NaN, 8))))
        vm.onEditWorkout(0, ParsedWorkout("Panca piana", emptyList()))
        assertEquals(ParsedSet(80.0, 10), vm.uiState.value.editedWorkouts[0].sets.single())

        vm.onEditWorkout(0, ParsedWorkout("Panca piana", listOf(ParsedSet(0.0, 12))))
        assertEquals(ParsedSet(0.0, 12), vm.uiState.value.editedWorkouts[0].sets.single())
    }

    @Test
    fun `parsing waits for the catalog seed on a cold start`() = runTest {
        seeded.value = false
        val vm = viewModel()

        vm.parseImportText("Panca piana 3x10 80kg")
        assertTrue(vm.uiState.value.isImporting)
        assertTrue(vm.uiState.value.editedWorkouts.isEmpty())

        seeded.value = true
        assertFalse(vm.uiState.value.isImporting)
        assertEquals(listOf("Panca piana"), vm.uiState.value.editedWorkouts.map { it.exerciseName })
    }

    @Test
    fun `parsing runs on the parse dispatcher`() = runTest {
        val parse = StandardTestDispatcher(testScheduler)
        val vm = viewModel(parse)

        vm.parseImportText("Panca piana 3x10 80kg")
        assertTrue(vm.uiState.value.editedWorkouts.isEmpty()) // parse is queued on the other dispatcher

        advanceUntilIdle()
        assertEquals(listOf("Panca piana"), vm.uiState.value.editedWorkouts.map { it.exerciseName })
    }
}
