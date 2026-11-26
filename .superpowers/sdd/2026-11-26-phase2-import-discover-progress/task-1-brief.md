# Task 1 Brief: ImportParser Domain Service (Pure Kotlin)

## Context
This is Task 1 of 5 in the Phase 2 implementation plan for Plateau fitness tracking app.
Plan: `docs/superpowers/plans/2026-11-26-phase2-import-discover-progress.md`

## Requirements (verbatim from plan)

### Files to Create
- `app/src/main/java/com/plateau/app/domain/service/ImportParser.kt`
- `app/src/test/java/com/plateau/app/domain/service/ImportParserTest.kt`

### Interfaces
- Consumes: `ExerciseRepository.getAllExercises()` for fuzzy matching catalog (passed as `List<Exercise>` to parse function)
- Produces: `ImportResult` (sealed class):
  - `Success(List<ParsedWorkout>)`
  - `Partial(List<ParsedWorkout>, List<UnmatchedLine>)`
  - `Failure(String)`
  - `UnmatchedLine(line: String, suggestions: List<String>)`

### Data Classes
- `ParsedWorkout(exerciseName: String, sets: List<ParsedSet>)`
- `ParsedSet(weight: Double, reps: Int, rpe: Int? = null, isDropSet: Boolean = false)`

### Parse Function Signature
```kotlin
fun parse(input: String, catalog: List<Exercise>): ImportResult
```

### Parsing Rules (must handle)
1. **Standard format**: `Exercise 3x10 80kg` or `Exercise 4x8 100kg`
2. **Multiple set formats**: `3x10`, `3×10`, `3x 10`, `3 x 10`
3. **RPE notation**: `3x10 80kg @8`
4. **Drop sets**: `80kgx10, 70kgx10, 60kgx10`
5. **Fuzzy matching**: exact name → aliases (JSON array) → Levenshtein distance ≤ 2
6. **LLM fallback stub**: 5s timeout, returns empty on failure

### Test Cases (must pass)
```kotlin
@Test
fun `parses standard Strong format: Exercise 3x10 80kg`()

@Test
fun `parses multiple set formats: 3x10, 3×10, 3x 10, 3 x 10`()

@Test
fun `fuzzy matches exercise aliases: "Bench press" -> "Panca piana"`()

@Test
fun `returns Partial with unmatched lines for unknown exercises`()

@Test
fun `handles RPE notation: 3x10 80kg @8`()

@Test
fun `handles drop sets: 80kgx10, 70kgx10, 60kgx10`()
```

## Global Constraints (from plan)
- Pure Kotlin domain service (no Android dependencies)
- No hardcoded colors/dimensions (not applicable here)
- Follow existing code patterns in `data/local/`, `data/repository/`, `di/`, `ui/`
- Epley formula: `weight × (1 + reps/30)` for e1RM (used elsewhere, not here)
- Room entities use UUIDs, `updatedAt`, proper FKs for future sync

## Review Focus (from plan)
1. Import parser edge cases: Empty lines, unknown exercise names, malformed set notation — must not crash, show review screen with suggestions
2. LLM fallback timeout: 5s max, graceful degradation to "needs manual review" if LLM fails

## Report File
Write full report to: `.superpowers/sdd/2026-11-26-phase2-import-discover-progress/task-1-report.md`

Report must include:
- Status: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED
- Commits made (if any)
- One-line test summary
- Concerns (if any)

---
Supersedes: nothing (new task brief for Task 1)
Linked from: docs/superpowers/plans/2026-11-26-phase2-import-discover-progress.md (Task 1 section)
User instruction: "subagent-driven" (chose subagent-driven-development execution approach)