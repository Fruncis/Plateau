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

    private const val WEIGHT = """(\d+(?:[.,]\d+)?)"""

    /**
     * Standard notation after the exercise text: "3x10 80kg", "3 X 10 82,5 KG", "3x10 80kg @8.5".
     * Weight accepts "." or "," decimals and an optional space before a case-insensitive "kg".
     */
    private val standardNotationRegex =
        """(\d+)\s*[x×]\s*(\d+)\s+$WEIGHT\s*kg(?:\s*@\s*$WEIGHT)?""".toRegex(RegexOption.IGNORE_CASE)

    /** One weight-first set: "80kgx10", "80 kg x 10", "72,5KG X 8". */
    private val weightFirstSetRegex = """$WEIGHT\s*kg\s*[x×]\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)

    /** A whole comma-separated list of weight-first sets (a decimal comma stays inside its weight). */
    private val weightFirstListRegex = weightFirstSetRegex.pattern.let { set ->
        """$set(?:\s*,\s*$set)*""".toRegex(RegexOption.IGNORE_CASE)
    }

    private val whitespaceRegex = """\s+""".toRegex()

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

    /** Where the set notation starts in a line: "3x10", "3 x 10" or "80kg x 5" after the exercise text. */
    private val notationStartRegex =
        """\s+(?:\d+\s*[x\u00d7]\s*\d+|\d+(?:[.,]\d+)?\s*kg\s*[x\u00d7]\s*\d+)""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Splits a line into the exercise text and the set notation that follows it.
     * [SplitLine.notation] keeps its leading whitespace, so `exerciseName + notation` rebuilds the line
     * (internal whitespace in the exercise text is collapsed to single spaces).
     * A line without recognisable notation yields an empty notation.
     */
    fun splitLine(line: String): SplitLine {
        val trimmed = line.trim()
        val start = notationStartRegex.find(trimmed)?.range?.first ?: trimmed.length
        val name = trimmed.substring(0, start).trim().replace(whitespaceRegex, " ")
        return SplitLine(name, trimmed.substring(start))
    }

    /**
     * Parses a single line of input. Any line that is not fully understood is unmatched (needs review),
     * with suggestions computed on the exercise text only (see [splitLine]).
     */
    private fun parseLine(line: String, catalog: List<Exercise>): ParseLineResult {
        val split = splitLine(line)
        val exerciseName = split.exerciseName
        val notation = split.notation.trim()
        fun unmatched() = ParseLineResult.Unmatched(line, findSuggestions(exerciseName, catalog))
        if (exerciseName.isEmpty() || notation.isEmpty()) {
            return parseWithLLMFallback(line, catalog) ?: unmatched()
        }

        val sets = standardNotationRegex.matchEntire(notation)?.let(::standardSets)
            ?: weightFirstSets(notation)
            ?: return unmatched()

        val matchedExercise = findExercise(exerciseName, catalog) ?: return unmatched()
        return ParseLineResult.Success(ParsedWorkout(exerciseName = matchedExercise.name, sets = sets))
    }

    /** Sets for "3x10 80kg @8"; null when a value is out of range (rpe must be 1..10). */
    private fun standardSets(match: MatchResult): List<ParsedSet>? {
        val (setsText, repsText, weightText, rpeText) = match.destructured
        val setCount = setsText.toIntOrNull()
        val reps = repsText.toIntOrNull()
        val weight = weightText.toDecimal()
        val rpe = rpeText.takeIf(String::isNotEmpty)?.let { it.toDecimal() ?: return null }
        if (setCount == null || setCount <= 0 || reps == null || reps <= 0 || weight == null || weight < 0.0) return null
        if (rpe != null && rpe !in 1.0..10.0) return null
        return List(setCount) { ParsedSet(weight = weight, reps = reps, rpe = rpe) }
    }

    /**
     * Sets for "80kgx10, 70kgx10" or a single "80kg x 5". The whole notation must be a list of valid
     * sets, so a malformed tail ("80kgx10, 70kgx") rejects the whole line instead of dropping sets.
     * More than one set is flagged as a drop set.
     */
    private fun weightFirstSets(notation: String): List<ParsedSet>? {
        if (!weightFirstListRegex.matches(notation)) return null
        val sets = weightFirstSetRegex.findAll(notation).toList().map { match ->
            val weight = match.groupValues[1].toDecimal()
            val reps = match.groupValues[2].toIntOrNull()
            if (weight == null || weight < 0.0 || reps == null || reps <= 0) return null
            ParsedSet(weight = weight, reps = reps)
        }
        return if (sets.size > 1) sets.map { it.copy(isDropSet = true) } else sets
    }

    private fun String.toDecimal(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

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

/** A workout line split into exercise text and set notation (see [ImportParser.splitLine]). */
data class SplitLine(val exerciseName: String, val notation: String)

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
    val rpe: Double? = null,
    val isDropSet: Boolean = false
)