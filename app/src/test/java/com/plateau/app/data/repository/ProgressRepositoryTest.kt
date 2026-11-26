package com.plateau.app.data.repository

import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.dao.ExerciseDao
import com.plateau.app.data.local.dao.MuscleGroupDao
import com.plateau.app.data.local.dao.SetLogDao
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.SetLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.ZoneId

class ProgressRepositoryTest {

    private val rome = ZoneId.of("Europe/Rome")
    private val setLogDao: SetLogDao = mock()
    private val exerciseDao: ExerciseDao = mock()
    private val muscleDao: MuscleGroupDao = mock()
    private val db: PlateauDatabase = mock {
        on { setLogDao() } doReturn setLogDao
        on { exerciseDao() } doReturn exerciseDao
        on { muscleGroupDao() } doReturn muscleDao
    }
    private val repo = ProgressRepository(db)

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(rome).toInstant().toEpochMilli()

    private fun log(id: String, ex: String, w: Double, reps: Int, at: Long) =
        SetLog(id, "s", ex, 0, reps, w, null, at)

    private fun exercise(id: String, name: String) = Exercise(
        id, name, "[]", "chest", null, "push", "barbell", 2, true, "", "catalog"
    )

    // 2026-11-23 is a Monday.
    @Test
    fun `week starts on Monday at midnight`() {
        val monday = millis(2026, 11, 23)
        assertEquals(monday, startOfWeekMillis(monday, zone = rome))
        assertEquals(monday, startOfWeekMillis(millis(2026, 11, 23, 0, 1), zone = rome))
    }

    @Test
    fun `Sunday 23_59 belongs to the previous Monday week`() {
        assertEquals(millis(2026, 11, 23), startOfWeekMillis(millis(2026, 11, 29, 23, 59), zone = rome))
        assertEquals(millis(2026, 11, 30), startOfWeekMillis(millis(2026, 11, 30), zone = rome))
    }

    @Test
    fun `week start is local midnight across the DST change`() {
        // DST starts Sunday 2027-03-28 in Europe/Rome: the week of Mon 22 March is 167 hours long.
        val monday = millis(2027, 3, 22)
        assertEquals(monday, startOfWeekMillis(millis(2027, 3, 28, 23, 59), zone = rome))
        assertEquals(millis(2027, 3, 29), startOfWeekMillis(millis(2027, 3, 29, 0, 30), zone = rome))
        assertEquals(167L * 3_600_000, millis(2027, 3, 29) - monday)
        assertEquals(monday, startOfWeekMillis(millis(2027, 4, 5, 12), weeksBack = 2, zone = rome))
    }

    @Test
    fun `volume buckets logs per week and averages weeks with data`() = runTest {
        val now = millis(2026, 11, 26, 12) // Thursday
        val thisWeek = millis(2026, 11, 23)
        val lastWeek = millis(2026, 11, 16)
        whenever(muscleDao.getById("chest")).thenReturn(MuscleGroup("chest", "Petto", "Chest"))
        val logs = listOf(
            log("a", "bench", 100.0, 5, now - 1000), // 500 this week
            log("b", "bench", 50.0, 10, thisWeek), // 500 this week (boundary)
            log("c", "bench", 80.0, 5, thisWeek - 1), // 400 last week (Sunday 23:59:59.999)
            log("d", "bench", 60.0, 5, lastWeek) // 300 last week (boundary)
        )
        whenever(setLogDao.getByPrimaryMuscleSince(eq("chest"), any())).thenReturn(flowOf(logs))

        val metrics = repo.getVolumeMetrics("chest", weeks = 4, now = now, zone = rome).first()!!

        assertEquals("Petto", metrics.muscleGroupName)
        assertEquals(4, metrics.weeklyData.size)
        assertEquals(thisWeek, metrics.weeklyData.last().weekStart)
        assertEquals(listOf(0.0, 0.0, 700.0, 1000.0), metrics.weeklyData.map { it.volume })
        assertEquals(1000.0, metrics.weeklyVolume, 0.0)
        assertEquals(850.0, metrics.targetVolume, 0.0)
    }

    @Test
    fun `volume target is zero without data and metrics null for unknown muscle`() = runTest {
        val now = millis(2026, 11, 26, 12)
        whenever(muscleDao.getById("chest")).thenReturn(MuscleGroup("chest", "Petto", "Chest"))
        whenever(setLogDao.getByPrimaryMuscleSince(eq("chest"), any())).thenReturn(flowOf(emptyList()))
        val empty = repo.getVolumeMetrics("chest", weeks = 3, now = now, zone = rome).first()!!
        assertEquals(0.0, empty.targetVolume, 0.0)
        assertEquals(0.0, empty.weeklyVolume, 0.0)

        whenever(muscleDao.getById("ghost")).thenReturn(null)
        whenever(setLogDao.getByPrimaryMuscleSince(eq("ghost"), any())).thenReturn(flowOf(emptyList()))
        assertNull(repo.getVolumeMetrics("ghost", weeks = 3, now = now, zone = rome).first())
    }

    @Test
    fun `recent PRs pick the best Epley set per exercise`() = runTest {
        val now = millis(2026, 11, 26, 12)
        // 100x5 -> 116.67, 90x10 -> 120.0, 120x1 -> 124.0 (best)
        val logs = listOf(
            log("a", "bench", 100.0, 5, now - 1000),
            log("b", "bench", 90.0, 10, now - 2000),
            log("c", "bench", 120.0, 1, now - 3000),
            log("d", "squat", 100.0, 3, now - 4000)
        )
        whenever(setLogDao.getAllSince(0L)).thenReturn(flowOf(logs))
        whenever(exerciseDao.getAll()).thenReturn(flowOf(listOf(exercise("bench", "Panca"), exercise("squat", "Squat"))))

        val prs = repo.getRecentPRs(limit = 10, now = now).first()

        val bench = prs.first { it.exerciseId == "bench" }
        assertEquals(120.0, bench.weight, 0.0)
        assertEquals(1, bench.reps)
        assertEquals(124.0, bench.e1rm, 1e-9)
        assertEquals("Panca", bench.exerciseName)
        assertEquals(110.0, prs.first { it.exerciseId == "squat" }.e1rm, 1e-9)
        assertEquals(listOf("bench", "squat"), prs.map { it.exerciseId }) // most recent first
        assertEquals(1, repo.getRecentPRs(limit = 1, now = now).first().size)
    }

    @Test
    fun `PR isNew is true up to exactly seven days`() = runTest {
        val now = millis(2026, 11, 26, 12)
        val week = 7L * 24 * 60 * 60 * 1000
        val logs = listOf(
            log("e1", "bench", 100.0, 5, now - week),
            log("e2", "squat", 100.0, 5, now - week - 1),
            log("e3", "row", 100.0, 5, now - week + 1)
        )
        whenever(setLogDao.getAllSince(0L)).thenReturn(flowOf(logs))
        whenever(exerciseDao.getAll()).thenReturn(
            flowOf(listOf(exercise("bench", "Panca"), exercise("squat", "Squat"), exercise("row", "Rematore")))
        )

        val prs = repo.getRecentPRs(now = now).first().associateBy { it.exerciseId }

        assertTrue(prs.getValue("bench").isNew)
        assertTrue(prs.getValue("row").isNew)
        assertFalse(prs.getValue("squat").isNew)
    }

    @Test
    fun `PR for an exercise missing from the catalog is skipped`() = runTest {
        val now = millis(2026, 11, 26, 12)
        whenever(setLogDao.getAllSince(0L)).thenReturn(
            flowOf(listOf(log("a", "ghost", 100.0, 5, now), log("b", "bench", 50.0, 5, now)))
        )
        whenever(exerciseDao.getAll()).thenReturn(flowOf(listOf(exercise("bench", "Panca"))))

        assertEquals(listOf("bench"), repo.getRecentPRs(now = now).first().map { it.exerciseId })
    }

    // --- double progression ---

    private val benchTarget = ProgressionTarget("bench", "Panca", sets = 3, repsMin = 6, repsMax = 10)

    private fun session(id: String, at: Long, vararg sets: Pair<Double, Int>) =
        sets.mapIndexed { i, (w, r) -> SetLog("${id}_$i", id, "bench", i, r, w, null, at + i) }

    @Test
    fun `all planned sets at repsMax at the same weight suggests plus 2_5 kg and repsMin`() {
        val now = millis(2026, 11, 26, 18)
        val logs = session("s1", now, 80.0 to 10, 80.0 to 10, 80.0 to 11)

        val s = doubleProgressionSuggestions(listOf(benchTarget), logs, rome).single()

        assertEquals(80.0, s.currentWeight, 0.0)
        assertEquals(10, s.currentReps)
        assertEquals(82.5, s.suggestedWeight, 0.0)
        assertEquals(6, s.targetReps)
        assertEquals(1, s.weeksAtTop)
    }

    @Test
    fun `not at top keeps the weight and adds one rep up to repsMax`() {
        val now = millis(2026, 11, 26, 18)
        val logs = session("s1", now, 80.0 to 10, 80.0 to 9, 80.0 to 8)

        val s = doubleProgressionSuggestions(listOf(benchTarget), logs, rome).single()

        assertEquals(80.0, s.suggestedWeight, 0.0)
        assertEquals(8, s.currentReps)
        assertEquals(9, s.targetReps)
        assertEquals(0, s.weeksAtTop)

        val nearTop = session("s2", now, 80.0 to 10, 80.0 to 10)
        val capped = doubleProgressionSuggestions(listOf(benchTarget), nearTop, rome).single() // only 2 of 3 sets
        assertEquals(80.0, capped.suggestedWeight, 0.0)
        assertEquals(10, capped.targetReps) // min(10 + 1, repsMax)
        assertEquals(0, capped.weeksAtTop)
    }

    @Test
    fun `mixed weights are not at top and use the heaviest weight`() {
        val now = millis(2026, 11, 26, 18)
        val logs = session("s1", now, 80.0 to 10, 85.0 to 10, 80.0 to 10)

        val s = doubleProgressionSuggestions(listOf(benchTarget), logs, rome).single()

        assertEquals(85.0, s.currentWeight, 0.0)
        assertEquals(85.0, s.suggestedWeight, 0.0)
        assertEquals(10, s.targetReps)
        assertEquals(0, s.weeksAtTop)
    }

    @Test
    fun `only the most recent session counts`() {
        val older = session("old", millis(2026, 11, 24, 18), 80.0 to 10, 80.0 to 10, 80.0 to 10)
        val latest = session("new", millis(2026, 11, 26, 18), 82.5 to 7, 82.5 to 6, 82.5 to 6)

        val s = doubleProgressionSuggestions(listOf(benchTarget), older + latest, rome).single()

        assertEquals(82.5, s.suggestedWeight, 0.0)
        assertEquals(7, s.targetReps)
        assertEquals(0, s.weeksAtTop)
    }

    @Test
    fun `weeksAtTop counts consecutive recent weeks at top and stops at a gap or a miss`() {
        val top = arrayOf(80.0 to 10, 80.0 to 10, 80.0 to 10)
        val thisWeek = session("w0", millis(2026, 11, 26, 18), *top)
        val lastWeek = session("w1", millis(2026, 11, 19, 18), *top)
        val twoWeeksAgo = session("w2", millis(2026, 11, 12, 18), *top)
        val threeWeeksAgoMiss = session("w3", millis(2026, 11, 5, 18), 80.0 to 8, 80.0 to 8, 80.0 to 8)
        assertEquals(3, doubleProgressionSuggestions(listOf(benchTarget), thisWeek + lastWeek + twoWeeksAgo + threeWeeksAgoMiss, rome).single().weeksAtTop)

        // A week without sessions breaks the streak.
        assertEquals(1, doubleProgressionSuggestions(listOf(benchTarget), thisWeek + twoWeeksAgo, rome).single().weeksAtTop)

        // Within a week only the latest session counts.
        val earlierThisWeekMiss = session("w0a", millis(2026, 11, 23, 18), 80.0 to 8, 80.0 to 8, 80.0 to 8)
        assertEquals(2, doubleProgressionSuggestions(listOf(benchTarget), thisWeek + earlierThisWeekMiss + lastWeek, rome).single().weeksAtTop)
    }

    @Test
    fun `exercises without logs are skipped and duplicates in the plan counted once`() {
        val logs = session("s1", millis(2026, 11, 26, 18), 80.0 to 10)
        val squat = ProgressionTarget("squat", "Squat", 3, 5, 8)

        val result = doubleProgressionSuggestions(listOf(benchTarget, squat, benchTarget.copy(sets = 5)), logs, rome)

        assertEquals(listOf("bench"), result.map { it.exerciseId })
    }

    @Test
    fun `logged exercises are listed most recent first and unknown ids skipped`() = runTest {
        whenever(setLogDao.getAllSince(0L)).thenReturn(
            flowOf(listOf(log("a", "bench", 50.0, 5, 1L), log("b", "squat", 50.0, 5, 3L), log("c", "ghost", 1.0, 1, 4L), log("d", "bench", 50.0, 5, 2L)))
        )
        whenever(exerciseDao.getAll()).thenReturn(flowOf(listOf(exercise("bench", "Panca"), exercise("squat", "Squat"))))

        assertEquals(listOf("squat", "bench"), repo.getLoggedExercises().first().map { it.id })
    }
}
