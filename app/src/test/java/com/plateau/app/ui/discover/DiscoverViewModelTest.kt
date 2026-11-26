package com.plateau.app.ui.discover

import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.MuscleGroupGap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    private val repo: ExerciseRepository = mock()

    private val chest = MuscleGroup("chest", "Petto", "Chest")
    private val legs = MuscleGroup("legs", "Gambe", "Legs")

    private val bench = exercise("bench", "Panca piana", "chest", "barbell", 2)
    private val fly = exercise("fly", "Croci", "chest", "cable", 1)
    private val squat = exercise("squat", "Squat", "legs", "barbell", 2)
    private val catalog = listOf(bench, fly, squat)

    private val gaps = MutableStateFlow(
        listOf(
            MuscleGroupGap(legs, 2, 10, 80),
            MuscleGroupGap(chest, 7, 10, 30)
        )
    )

    private fun exercise(id: String, name: String, muscle: String, equipment: String, difficulty: Int) = Exercise(
        id = id, name = name, aliases = "[]", primaryMuscleId = muscle, secondaryMuscleId = null,
        movementPattern = "push", equipment = equipment, difficulty = difficulty, isCompound = true,
        instructions = "", source = "catalog"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(repo.getMuscleGroups()).thenReturn(MutableStateFlow(listOf(chest, legs)))
        whenever(repo.getAllExercises()).thenReturn(MutableStateFlow(catalog))
        whenever(repo.getVolumeGaps()).thenReturn(gaps)
        whenever(repo.getFilteredExercises(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).doAnswer {
            val muscle = it.getArgument<String?>(0)
            val equipment = it.getArgument<String?>(1)
            val difficulty = it.getArgument<Int?>(2)
            MutableStateFlow(
                catalog.filter { e ->
                    (muscle == null || e.primaryMuscleId == muscle) &&
                        (equipment == null || e.equipment == equipment) &&
                        (difficulty == null || e.difficulty <= difficulty)
                }
            )
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `filters by muscle`() = runTest(UnconfinedTestDispatcher()) {
        val vm = DiscoverViewModel(repo)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals(catalog, vm.uiState.value.filteredExercises)

        vm.onMuscleSelected("chest")

        assertEquals(listOf(bench, fly), vm.uiState.value.filteredExercises)
        assertEquals("chest", vm.uiState.value.activeFilters.muscleId)
        job.cancel()
    }

    @Test
    fun `combined filters use AND logic and reach the repository together`() = runTest(UnconfinedTestDispatcher()) {
        val vm = DiscoverViewModel(repo)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.onMuscleSelected("chest")
        vm.onEquipmentSelected("barbell")
        vm.onDifficultySelected(2)

        verify(repo).getFilteredExercises(
            eq("chest"), eq("barbell"), eq(2), isNull(), isNull()
        )
        assertEquals(listOf(bench), vm.uiState.value.filteredExercises)
        job.cancel()
    }

    @Test
    fun `blank search is passed as null and real search is trimmed`() = runTest(UnconfinedTestDispatcher()) {
        val vm = DiscoverViewModel(repo)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.onSearchChanged("   ")
        verify(repo, org.mockito.kotlin.atLeastOnce()).getFilteredExercises(isNull(), isNull(), isNull(), isNull(), isNull())
        assertFalse(vm.uiState.value.activeFilters.hasActiveFilters)

        vm.onSearchChanged(" panca ")
        verify(repo).getFilteredExercises(isNull(), isNull(), isNull(), isNull(), eq("panca"))
        job.cancel()
    }

    @Test
    fun `recommendations come from volume gaps ordered by deficit`() = runTest(UnconfinedTestDispatcher()) {
        val vm = DiscoverViewModel(repo)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val recs = vm.uiState.value.recommendations
        assertEquals(listOf("squat", "bench", "fly"), recs.map { it.exercise.id })
        assertTrue(recs.any { it.exercise.primaryMuscleId == "chest" })
        assertEquals("legs", recs.first().volumeGap.muscleGroup.id)
        assertTrue(recs[0].score > recs[1].score)
        job.cancel()
    }

    @Test
    fun `clearFilters resets everything`() = runTest(UnconfinedTestDispatcher()) {
        val vm = DiscoverViewModel(repo)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.onMuscleSelected("chest")
        vm.onEquipmentSelected("cable")
        vm.onSearchChanged("cro")
        assertTrue(vm.uiState.value.activeFilters.hasActiveFilters)

        vm.clearFilters()

        assertEquals(DiscoverFilters(), vm.uiState.value.activeFilters)
        assertFalse(vm.uiState.value.activeFilters.hasActiveFilters)
        assertEquals(catalog, vm.uiState.value.filteredExercises)
        job.cancel()
    }
}
