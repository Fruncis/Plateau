package com.plateau.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plateau.app.data.local.PlateauDatabase
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetLogDaoPrimaryMuscleTest {

    private lateinit var db: PlateauDatabase
    private lateinit var dao: SetLogDao

    private fun ex(id: String, primary: String, secondary: String? = null) =
        Exercise(id, id, "[]", primary, secondary, "push", "barbell", 2, true, "", "catalog")

    private var nextIndex = 0

    private fun log(id: String, exerciseId: String, at: Long) =
        SetLog(id, "s1", exerciseId, nextIndex++, 5, 100.0, null, at)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlateauDatabase::class.java)
            .allowMainThreadQueries().build()
        db.muscleGroupDao().insertAll(
            listOf(MuscleGroup("chest", "Petto", "Chest"), MuscleGroup("legs", "Gambe", "Legs"))
        )
        db.exerciseDao().insertAll(
            listOf(ex("bench", "chest"), ex("squat", "legs"), ex("lunge", "legs", secondary = "chest"))
        )
        db.workoutSessionDao().insert(WorkoutSession("s1", null, 0L, 0))
        dao = db.setLogDao()
        dao.insertAll(
            listOf(
                log("old", "bench", 99L),
                log("edge", "bench", 100L),
                log("new", "bench", 200L),
                log("legs", "squat", 150L),
                log("secondary", "lunge", 150L)
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun ids(muscle: String, since: Long) =
        runBlocking { dao.getByPrimaryMuscleSince(muscle, since).first().map { it.id } }

    @Test
    fun filtersByPrimaryMuscleAndStartDateInclusive() {
        assertEquals(listOf("new", "edge"), ids("chest", 100L))
    }

    @Test
    fun secondaryMuscleDoesNotMatchAndOtherMusclesExcluded() {
        assertEquals(setOf("legs", "secondary"), ids("legs", 0L).toSet())
        assertEquals(listOf("new", "edge", "old"), ids("chest", 0L))
    }
}
