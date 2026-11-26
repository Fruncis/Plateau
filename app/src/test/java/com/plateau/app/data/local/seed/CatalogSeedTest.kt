package com.plateau.app.data.local.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSeedTest {

    private val muscleIds = CatalogSeed.muscleGroups.map { it.id }.toSet()

    @Test
    fun everyPrimaryMuscleExists() {
        val bad = CatalogSeed.exercises.filter { it.primaryMuscleId !in muscleIds }.map { it.id }
        assertTrue("Unknown primaryMuscleId in: $bad", bad.isEmpty())
    }

    @Test
    fun everyNonNullSecondaryMuscleExists() {
        val bad = CatalogSeed.exercises
            .filter { it.secondaryMuscleId != null && it.secondaryMuscleId !in muscleIds }
            .map { it.id }
        assertTrue("Unknown secondaryMuscleId in: $bad", bad.isEmpty())
    }

    @Test
    fun idsAreUnique() {
        assertEquals(CatalogSeed.muscleGroups.size, muscleIds.size)
        assertEquals(CatalogSeed.exercises.size, CatalogSeed.exercises.map { it.id }.toSet().size)
    }

    @Test
    fun namesAreNotBlank() {
        assertTrue(CatalogSeed.muscleGroups.all { it.name.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(CatalogSeed.exercises.all { it.name.isNotBlank() })
    }
}
