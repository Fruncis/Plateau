package com.plateau.app.domain.service

import com.plateau.app.data.local.entity.Exercise
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Domain service for parsing workout import text into structured data.
 * Pure Kotlin implementation with no Android dependencies.
 */
object ImportParser {

    /** JSON instance for parsing aliases from Exercise entity */
    private val json = Json { ignoreUnknownKeys = true }

    /** Regex for standard set format: 3x10, 3×10, 3x 10, 3 x 10 */
    private val standardSetRegex = """(\d+)\s*[x×]\s*(\d+)""".toRegex()

    /** Regex for standard set with weight: Exercise 3x10 80kg or Exercise 3x10 80kg @8 */
    private val standardFormatRegex = """^(.+?)\s+(\d+)\s*[x×]\s*(\d+)\s+(\d+(?:\.\d+)?)kg\s*(?:@(\d+(?:\.\d+)?))?$""".toRegex()

    /** Regex for drop set format: 80kgx10, 70kgx10, 60kgx10 */
    private val dropSetRegex = """(\d+(?:\.\d+)?)kgx(\d+)""".toRegex()

    /**
     * Parses workout import text into structured data.
     *
     * @param input Raw text input from user (e.g., copied from Strong app)
     * @param catalog List of known exercises for fuzzy matching
     * @return ImportResult with parsed workouts or unmatched lines
     */
    fun parse(input: String, catalog: List<Exercise>): ImportResult {
        val lines = input.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ImportResult.Failure("Input is empty")
        }

        val workouts = mutableListOf<ParsedWorkout>()
        val unmatchedLines = mutableListOf<ImportResult.UnmatchedLine>()

        for (line in lines) {
            val parsedLine = parseLine(line, catalog)
            when (parsedLine) {
                is ParseLineResult.Success -> workouts.add(parsedLine.workout)
                is ParseLineResult.Unmatched -> unmatchedLines.add(ImportResult.UnmatchedLine(parsedLine.line, parsedLine.suggestions))
            }
        }

        return if (unmatchedLines.isEmpty()) {
            ImportResult.Success(workouts)
        } else {
            ImportResult.Partial(workouts, unmatchedLines)
        }
    }

    /**
     * Parses a single line of input.
     */
    private fun parseLine(line: String, catalog: List<Exercise>): ParseLineResult {
        // Try standard format first: Exercise 3x10 80kg @8
        val standardMatch = standardFormatRegex.matchEntire(line)
        if (standardMatch != null) {
            val exerciseName = standardMatch.groupValues[1].trim()
            val setCount = standardMatch.groupValues[2].toIntOrNull()
            val reps = standardMatch.groupValues[3].toIntOrNull()
            val weight = standardMatch.groupValues[4].toDoubleOrNull()
            val rpe = standardMatch.groupValues[5].takeIf(String::isNotEmpty)?.toIntOrNull()

            if (setCount == null || setCount <= 0 || reps == null || reps <= 0 ||
                weight == null || !weight.isFinite() || weight < 0.0 ||
                (standardMatch.groupValues[5].isNotEmpty() && (rpe == null || rpe !in 1..10))
            ) {
                return ParseLineResult.Unmatched(line, emptyList())
            }

            val matchedExercise = findExercise(exerciseName, catalog)
            return if (matchedExercise != null) {
                ParseLineResult.Success(ParsedWorkout(
                    exerciseName = matchedExercise.name,
                    sets = List(setCount) { ParsedSet(weight = weight, reps = reps, rpe = rpe) }
                ))
            } else {
                ParseLineResult.Unmatched(line, findSuggestions(exerciseName, catalog))
            }
        }

        // Try drop set format: Exercise 80kgx10, 70kgx10, 60kgx10
        val firstDrop = dropSetRegex.find(line)
        if (firstDrop != null && firstDrop.range.first > 0) {
            val exerciseName = line.substring(0, firstDrop.range.first).trim()
            val dropSetMatches = dropSetRegex.findAll(line.substring(firstDrop.range.first)).toList()
            if (exerciseName.isNotEmpty()) {
                val matchedExercise = findExercise(exerciseName, catalog)
                    ?: return ParseLineResult.Unmatched(line, findSuggestions(exerciseName, catalog))
                val sets = dropSetMatches.mapNotNull { match ->
                    val weight = match.groupValues[1].toDoubleOrNull()
                    val reps = match.groupValues[2].toIntOrNull()
                    if (weight == null || !weight.isFinite() || weight < 0.0 || reps == null || reps <= 0) {
                        null
                    } else {
                        ParsedSet(weight = weight, reps = reps, isDropSet = true)
                    }
                }
                if (sets.size != dropSetMatches.size || sets.isEmpty()) {
                    return ParseLineResult.Unmatched(line, emptyList())
                }
                return ParseLineResult.Success(ParsedWorkout(exerciseName = matchedExercise.name, sets = sets))
            }
        }

        // Try LLM fallback as last resort (stub with 5s timeout)
        // The LLM fallback is currently a local no-op stub. Apply a timeout
        // when it becomes a real suspend/network operation.
        val llmResult = parseWithLLMFallback(line, catalog)

        return llmResult ?: ParseLineResult.Unmatched(line, findSuggestions(line, catalog))
    }

    /**
     * Finds an exercise in the catalog by exact name or alias (case-insensitive).
     */
    private fun findExercise(inputName: String, catalog: List<Exercise>): Exercise? {
        val normalizedInput = inputName.lowercase().trim()

        // 1. Exact match on name
        val exactMatch = catalog.find { it.name.lowercase() == normalizedInput }
        if (exactMatch != null) return exactMatch

        // 2. Match on aliases (JSON array)
        for (exercise in catalog) {
            try {
                val aliases = json.decodeFromString<List<String>>(exercise.aliases)
                if (aliases.any { it.lowercase() == normalizedInput }) {
                    return exercise
                }
            } catch (e: Exception) {
                // Ignore malformed JSON
            }
        }

        // Fuzzy (Levenshtein) candidates are intentionally NOT auto-accepted:
        // they surface as suggestions on an unmatched line for user review.
        return null
    }

    /**
     * Finds suggested exercise names for an unmatched input.
     */
    private fun findSuggestions(inputName: String, catalog: List<Exercise>): List<String> {
        val normalizedInput = inputName.lowercase().trim()
        val suggestions = mutableSetOf<String>()

        // Exact and alias matches (shouldn't happen if findExercise worked, but safe fallback)
        for (exercise in catalog) {
            if (exercise.name.lowercase() == normalizedInput) {
                suggestions.add(exercise.name)
            }
            try {
                val aliases = json.decodeFromString<List<String>>(exercise.aliases)
                if (aliases.any { it.lowercase() == normalizedInput }) {
                    suggestions.add(exercise.name)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Fuzzy matches with distance ≤ 2
        for (exercise in catalog) {
            val nameDistance = levenshteinDistance(normalizedInput, exercise.name.lowercase())
            if (nameDistance <= 2) {
                suggestions.add(exercise.name)
            }
            try {
                val aliases = json.decodeFromString<List<String>>(exercise.aliases)
                for (alias in aliases) {
                    val aliasDistance = levenshteinDistance(normalizedInput, alias.lowercase())
                    if (aliasDistance <= 2) {
                        suggestions.add(exercise.name)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return suggestions.toList()
    }

    /**
     * LLM fallback stub - returns null to indicate fallback not implemented.
     * In future, this would call an LLM API to parse ambiguous input.
     */
    private fun parseWithLLMFallback(line: String, catalog: List<Exercise>): ParseLineResult? {
        // TODO: Implement LLM-based parsing for ambiguous inputs
        // For now, return null to indicate fallback unavailable
        return null
    }

    /**
     * Computes Levenshtein distance between two strings.
     * Returns the minimum number of single-character edits (insertions, deletions, substitutions)
     * required to change one string into the other.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val prev = IntArray(s2.length + 1) { it }
        val curr = IntArray(s2.length + 1)

        for (i in 1..s1.length) {
            curr[0] = i
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,        // deletion
                    curr[j - 1] + 1,    // insertion
                    prev[j - 1] + cost  // substitution
                )
            }
            // Swap arrays
            for (j in 0..s2.length) {
                prev[j] = curr[j]
            }
        }

        return prev[s2.length]
    }

    /** Internal sealed class for line parsing results */
    private sealed class ParseLineResult {
        data class Success(val workout: ParsedWorkout) : ParseLineResult()
        data class Unmatched(val line: String, val suggestions: List<String>) : ParseLineResult()
    }
}

/**
 * Sealed class representing the result of an import parse operation.
 */
sealed class ImportResult {
    /** All lines parsed successfully */
    data class Success(val workouts: List<ParsedWorkout>) : ImportResult()

    /** Some lines parsed, others need manual review */
    data class Partial(val workouts: List<ParsedWorkout>, val unmatchedLines: List<UnmatchedLine>) : ImportResult()

    /** No lines could be parsed */
    data class Failure(val message: String) : ImportResult()

    /** A line that couldn't be matched to an exercise */
    data class UnmatchedLine(val line: String, val suggestions: List<String>)
}

/**
 * Represents a parsed workout with its exercise name and sets.
 */
data class ParsedWorkout(
    val exerciseName: String,
    val sets: List<ParsedSet>
)

/**
 * Represents a single parsed set with weight, reps, optional RPE, and drop set flag.
 */
data class ParsedSet(
    val weight: Double,
    val reps: Int,
    val rpe: Int? = null,
    val isDropSet: Boolean = false
)