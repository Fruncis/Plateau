package com.plateau.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.seed.CatalogSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedRepositoryTest {

    private lateinit var db: PlateauDatabase
    private lateinit var repo: SeedRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlateauDatabase::class.java).build()
        repo = SeedRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun muscleCount() = db.muscleGroupDao().getAllList().size
    private suspend fun exerciseCount() = db.exerciseDao().getAll().first().size

    @Test
    fun seedsEveryMuscleAndExerciseOnEmptyDb() = runBlocking {
        repo.seedIfEmpty()
        assertEquals(CatalogSeed.muscleGroups.size, muscleCount())
        assertEquals(CatalogSeed.exercises.size, exerciseCount())
    }

    @Test
    fun secondCallDoesNotDuplicate() = runBlocking {
        repo.seedIfEmpty()
        repo.seedIfEmpty()
        assertEquals(CatalogSeed.muscleGroups.size, muscleCount())
        assertEquals(CatalogSeed.exercises.size, exerciseCount())
    }

    @Test
    fun musclesWithoutExercisesGetExercisesSeeded() = runBlocking {
        db.muscleGroupDao().insertAll(CatalogSeed.muscleGroups)
        assertEquals(0, exerciseCount())
        repo.seedIfEmpty()
        assertEquals(CatalogSeed.muscleGroups.size, muscleCount())
        assertEquals(CatalogSeed.exercises.size, exerciseCount())
    }
}
