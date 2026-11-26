package com.plateau.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoFilterTest {

    private lateinit var db: PlateauDatabase
    private lateinit var dao: ExerciseDao

    private fun ex(
        id: String, name: String, primary: String, equipment: String, difficulty: Int,
        secondary: String? = null, pattern: String = "push", aliases: String = "[]"
    ) = Exercise(id, name, aliases, primary, secondary, pattern, equipment, difficulty, true, "", "catalog")

    // target matches chest + barbell + difficulty <= 2
    private val target = ex("target", "Panca piana", "chest", "barbell", 2, aliases = "[\"bench press\"]")
    private val wrongEquipment = ex("d1", "Croci", "chest", "cable", 2)
    private val wrongMuscle = ex("d2", "Squat", "legs", "barbell", 2, pattern = "squat")
    private val tooHard = ex("d3", "Panca pesante", "chest", "barbell", 3)
    private val secondaryHit = ex("sec", "Military press", "shoulders", "dumbbell", 1, secondary = "chest")
    private val all = listOf(target, wrongEquipment, wrongMuscle, tooHard, secondaryHit)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlateauDatabase::class.java)
            .allowMainThreadQueries().build()
        db.muscleGroupDao().insertAll(
            listOf(
                MuscleGroup("chest", "Petto", "Chest"),
                MuscleGroup("legs", "Gambe", "Legs"),
                MuscleGroup("shoulders", "Spalle", "Shoulders")
            )
        )
        dao = db.exerciseDao()
        dao.insertAll(all)
    }

    @After
    fun tearDown() = db.close()

    private fun ids(
        muscle: String? = null, equipment: String? = null, difficulty: Int? = null,
        pattern: String? = null, search: String? = null
    ) = runBlocking { dao.getFiltered(muscle, equipment, difficulty, pattern, search).first().map { it.id }.toSet() }

    @Test
    fun muscleEquipmentDifficultyAreCombinedWithAnd() {
        assertEquals(setOf("target"), ids(muscle = "chest", equipment = "barbell", difficulty = 2))
    }

    @Test
    fun secondaryMuscleMatches() {
        assertEquals(setOf("d1", "target", "d3", "sec"), ids(muscle = "chest"))
        assertEquals(setOf("sec"), ids(muscle = "chest", equipment = "dumbbell"))
    }

    @Test
    fun searchMatchesNameAndAlias() {
        assertEquals(setOf("target", "d3"), ids(search = "panca"))
        assertEquals(setOf("target"), ids(search = "bench press"))
        assertEquals(setOf("target"), ids(muscle = "chest", equipment = "barbell", difficulty = 2, search = "panca"))
    }

    @Test
    fun allNullReturnsEverything() {
        assertEquals(all.map { it.id }.toSet(), ids())
    }
}
