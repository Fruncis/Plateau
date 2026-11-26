package com.plateau.app.domain.service

import com.plateau.app.data.local.entity.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportParserTest {

    private val catalog = listOf(
        Exercise("ex_panca_piana", "Panca piana", "[\"Bench press\", \"Panca bilanciere\"]", "chest", "triceps", "push", "barbell", 2, true, "Sdraiato su panca, bilanciere al petto, spingi verso l'alto", "catalog"),
        Exercise("ex_stacco", "Stacco da terra", "[\"Deadlift\"]", "hamstrings", "glutes", "hinge", "barbell", 3, true, "Pattern hinge fondamentale", "catalog"),
        Exercise("ex_squat", "Squat", "[\"Back squat\"]", "quads", "glutes", "squat", "barbell", 3, true, "Re dei esercizi per gambe", "catalog"),
        Exercise("ex_military_press", "Military press", "[\"Overhead press\", \"OHP\"]", "shoulders", "triceps", "push", "barbell", 2, true, "Spinta verticale base", "catalog"),
    )

    @Test
    fun `parses standard Strong format Exercise 3x10 80kg`() {
        val input = "Panca piana 3x10 80kg"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.workouts.size)
        val workout = success.workouts[0]
        assertEquals("Panca piana", workout.exerciseName)
        assertEquals(3, workout.sets.size)
        workout.sets.forEach { set ->
            assertEquals(80.0, set.weight, 0.001)
            assertEquals(10, set.reps)
            assertEquals(null, set.rpe)
        }
    }

    @Test
    fun `parses multiple set formats 3x10 and 3 times 10 and spaced variants`() {
        val inputs = listOf(
            "Panca piana 3x10 80kg",
            "Panca piana 3×10 80kg",
            "Panca piana 3x 10 80kg",
            "Panca piana 3 x 10 80kg"
        )

        for (input in inputs) {
            val result = ImportParser.parse(input, catalog)
            assertTrue("Failed for input: $input", result is ImportResult.Success)
            val success = result as ImportResult.Success
            assertEquals(1, success.workouts.size)
            val set = success.workouts[0].sets[0]
            assertEquals(80.0, set.weight, 0.001)
            assertEquals(10, set.reps)
        }
    }

    @Test
    fun `matches exercise alias Bench press to Panca piana`() {
        val input = "Bench press 3x10 80kg"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.workouts.size)
        assertEquals("Panca piana", success.workouts[0].exerciseName)
    }

    @Test
    fun `returns Partial with unmatched lines for unknown exercises`() {
        val input = "Panca piana 3x10 80kg\nUnknown exercise 3x10 60kg"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Partial)
        val partial = result as ImportResult.Partial
        assertEquals(1, partial.workouts.size)
        assertEquals("Panca piana", partial.workouts[0].exerciseName)
        assertEquals(1, partial.unmatchedLines.size)
        val unmatched = partial.unmatchedLines[0]
        assertEquals("Unknown exercise 3x10 60kg", unmatched.line)
        assertTrue(unmatched.suggestions.isEmpty()) // no catalog entry is a close match to this unrelated name
    }

    @Test
    fun `handles RPE notation 3x10 80kg at 8`() {
        val input = "Panca piana 3x10 80kg @8"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        val set = success.workouts[0].sets[0]
        assertEquals(80.0, set.weight, 0.001)
        assertEquals(10, set.reps)
        assertEquals(8, set.rpe)
    }

    @Test
    fun `handles drop sets 80kgx10 70kgx10 60kgx10`() {
        val input = "Panca piana 80kgx10, 70kgx10, 60kgx10"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(3, success.workouts[0].sets.size)
        val sets = success.workouts[0].sets
        assertEquals(80.0, sets[0].weight, 0.001)
        assertEquals(10, sets[0].reps)
        assertTrue(sets[0].isDropSet)
        assertEquals(70.0, sets[1].weight, 0.001)
        assertEquals(10, sets[1].reps)
        assertTrue(sets[1].isDropSet)
        assertEquals(60.0, sets[2].weight, 0.001)
        assertEquals(10, sets[2].reps)
        assertTrue(sets[2].isDropSet)
    }

    @Test
    fun `parses multiple exercises in one input`() {
        val input = "Panca piana 3x10 80kg\nStacco da terra 4x5 140kg"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(2, success.workouts.size)
        assertEquals("Panca piana", success.workouts[0].exerciseName)
        assertEquals("Stacco da terra", success.workouts[1].exerciseName)
    }

    @Test
    fun `handles empty input gracefully`() {
        val input = ""
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Failure)
    }

    @Test
    fun `handles input with only whitespace`() {
        val input = "   \n\t  \n"
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Failure)
    }

    @Test
    fun `handles malformed set notation gracefully`() {
        val input = "Panca piana invalid format\nStacco da terra 3x10 100kg"
        val result = ImportParser.parse(input, catalog)

        // Should parse the valid line and return Partial for the invalid one
        assertTrue(result is ImportResult.Partial)
        val partial = result as ImportResult.Partial
        assertEquals(1, partial.workouts.size)
        assertEquals("Stacco da terra", partial.workouts[0].exerciseName)
        assertEquals(1, partial.unmatchedLines.size)
    }

    @Test
    fun `Levenshtein distance suggests similar exercises for typos`() {
        val input = "Squat 4x8 100kg\nPnca piana 3x10 80kg" // typo in "Panca"; valid first line keeps result Partial
        val result = ImportParser.parse(input, catalog)

        assertTrue(result is ImportResult.Partial)
        val partial = result as ImportResult.Partial
        assertEquals(1, partial.unmatchedLines.size)
        val suggestions = partial.unmatchedLines[0].suggestions
        assertTrue(suggestions.contains("Panca piana"))
    }

    @Test
    fun `blank lines between exercises are skipped`() {
        val result = ImportParser.parse("\n\nPanca piana 3x10 80kg\n   \n\nSquat 4x8 100kg\n\n", catalog)

        assertTrue(result is ImportResult.Success)
        assertEquals(2, (result as ImportResult.Success).workouts.size)
    }

    @Test
    fun `malformed set notation never crashes and surfaces as unmatched`() {
        val inputs = listOf("Panca piana 3x", "Panca piana x10 80kg", "Panca piana 3x10 kg", "3x10 80kg", "Panca piana 0x10 80kg", "Panca piana 3x10 80kg @99", "x", "@8")
        for (input in inputs) {
            val result = ImportParser.parse("Squat 4x8 100kg\n$input", catalog)
            assertTrue("Input: $input -> $result", result is ImportResult.Partial)
            assertEquals(1, (result as ImportResult.Partial).unmatchedLines.size)
        }
    }

    @Test
    fun `single unmatched line returns Partial with no workouts and keeps suggestions`() {
        val result = ImportParser.parse("Pnca piana 3x10 80kg", catalog)

        assertTrue("Got $result", result is ImportResult.Partial)
        val partial = result as ImportResult.Partial
        assertEquals(0, partial.workouts.size)
        assertEquals(1, partial.unmatchedLines.size)
        assertTrue(partial.unmatchedLines[0].suggestions.contains("Panca piana"))
    }

    @Test
    fun `splitLine separates exercise text from standard notation`() {
        val split = ImportParser.splitLine("Esercizio Sconosciuto 3x10 50kg")
        assertEquals("Esercizio Sconosciuto", split.exerciseName)
        assertEquals(" 3x10 50kg", split.notation)
    }

    @Test
    fun `splitLine handles spaced and weight-first notation`() {
        assertEquals("Curl", ImportParser.splitLine("Curl 3 x 10 @ 40").exerciseName)
        assertEquals("Row", ImportParser.splitLine("Row 80kg x 5").exerciseName)
        assertEquals("Row", ImportParser.splitLine("Row 80kgx10, 70kgx10").exerciseName)
    }

    @Test
    fun `splitLine without notation keeps whole line as name`() {
        val split = ImportParser.splitLine("Solo testo")
        assertEquals("Solo testo", split.exerciseName)
        assertEquals("", split.notation)
    }

    @Test
    fun `splitLine rebuilds a parsable line with a new name`() {
        val split = ImportParser.splitLine("Esercizio Sconosciuto 3x10 50kg")
        val result = ImportParser.parse("Squat" + split.notation, catalog)
        assertTrue(result is ImportResult.Success)
    }
}
