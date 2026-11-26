# Task 2 Brief: ImportReviewScreen + ViewModel

## Context
This is Task 2 of 5 in the Phase 2 implementation plan for Plateau fitness tracking app.
Plan: `docs/superpowers/plans/2026-11-26-phase2-import-discover-progress.md`

## Requirements (verbatim from plan)

### Files to Create
- `app/src/main/java/com/plateau/app/ui/import/ImportReviewViewModel.kt`
- `app/src/main/java/com/plateau/app/ui/import/ImportReviewScreen.kt`

### Files to Modify
- `app/src/main/java/com/plateau/app/ui/navigation/Destination.kt` (add ImportReview)
- `app/src/main/java/com/plateau/app/MainActivity.kt` (add route)
- `app/src/main/java/com/plateau/app/di/RepositoryModule.kt` (provide ImportParser)

### Interfaces
- Consumes: `ImportResult` from Intent/navigation args
- Produces: User-confirmed workouts → `SessionRepository.startSession()` + `logSet()`

### Destination Addition
```kotlin
// In Destination.kt
data class ImportReview(val result: ImportResult) : Destination()
```

### ViewModel Requirements
- State: `importResult: ImportResult`, `editedWorkouts: MutableStateFlow<List<ParsedWorkout>>`
- `onConfirmImport()`: For each workout, find/create exercise, start session, log sets
- `onEditWorkout(index)`: Navigate to inline editor (bottom sheet)
- `onDismissUnmatched(index)`: Remove from unmatched list

### Screen Requirements
- Header: "Rivedi importazione" (headlineLarge)
- For each parsed workout: Card with exercise name, MuscleChip(primary), list of sets (weight × reps × RPE)
- Unmatched section: Gray cards with "Non riconosciuto" badge, "Aggiungi come nuovo" button
- Primary action: `PlateauButton("Importa ${count} esercizi")` → confirms
- Secondary: "Annulla" text button
- @Preview dark & light

## Global Constraints
- Use ONLY `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `PlateauTheme.extras`, `Spacing`, `Size`
- Single primary action per screen (`PlateauButton`)
- Reuse `MuscleChip`, `PlateauButton`, `PlateauMark` from `ui/components/`
- Every new screen needs `@Preview` (dark + light)
- Follow existing code patterns in `data/local/`, `data/repository/`, `di/`, `ui/`

## Report File
Write full report to: `.superpowers/sdd/2026-11-26-phase2-import-discover-progress/task-2-report.md`

Report must include:
- Status: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED
- Commits made (if any)
- One-line test summary
- Concerns (if any)

---
Supersedes: nothing (new task brief for Task 2)
Linked from: docs/superpowers/plans/2026-11-26-phase2-import-discover-progress.md (Task 2 section)
User instruction: "continua" (continue with inline execution)