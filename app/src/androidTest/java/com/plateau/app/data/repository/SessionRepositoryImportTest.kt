package com.plateau.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.domain.service.ImportParser
import com.plateau.app.domain.service.ImportResult
import com.plateau.app.ui.importreview.buildImportLogs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRepositoryImportTest {

    private lateinit var db: PlateauDatabase
    private lateinit var repo: SessionRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlateauDatabase::class.java).build()
        SeedRepository(db).seedIfEmpty()
        repo = SessionRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun session(id: String) = WorkoutSession(id = id, planDayId = null, date = 1L, durationSec = 0)

    @Test
    fun repeatedExerciseKeepsEverySetThroughTheRealDao() = runBlocking {
        val catalog = db.exerciseDao().getAll().first()
        val parsed = ImportParser.parse(
            "Panca piana 3x10 80kg\nSquat 4x8 100KG @8.5\nPanca piana 3x8 85 kg",
            catalog
        ) as ImportResult.Success

        val logs = buildImportLogs(parsed.workouts, catalog, "s1", 1L)
        repo.importSession(session("s1"), logs)

        val saved = repo.getSetsForSession("s1").first()
        assertEquals(10, saved.size)
        val benchId = catalog.first { it.name == "Panca piana" }.id
        val bench = saved.filter { it.exerciseId == benchId }.sortedBy { it.setIndex }
        assertEquals((0..5).toList(), bench.map { it.setIndex })
        assertEquals(listOf(80.0, 80.0, 80.0, 85.0, 85.0, 85.0), bench.map { it.weight })
        assertTrue(saved.filter { it.exerciseId != benchId }.all { it.rpe == 8.5 })
    }

    @Test
    fun failedImportLeavesNoSessionAndNoSets() = runBlocking {
        val exerciseId = db.exerciseDao().getAll().first().first().id
        val logs = listOf(
            SetLog("a", "ignored", exerciseId, 0, 10, 80.0, null, 1L),
            SetLog("b", "ignored", "does-not-exist", 0, 10, 80.0, null, 1L) // FK violation
        )

        try {
            repo.importSession(session("s2"), logs)
            fail("Expected the foreign key violation to abort the import")
        } catch (expected: Exception) {
            // rolled back
        }

        assertNull(db.workoutSessionDao().getById("s2"))
        assertTrue(repo.getSetsForSession("s2").first().isEmpty())
    }
}
