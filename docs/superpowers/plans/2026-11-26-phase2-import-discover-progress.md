# Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 2 features: Import Parser (hybrid rules+fuzzy+LLM fallback), Discover screen with real filters (muscle, equipment, difficulty, movement pattern) + gap-based recommendations, and Progress visualization with percentage completion + interactive dashboard.

**Architecture:** Clean MVVM with Hilt DI. ImportParser as pure Kotlin domain service (testable). DiscoverViewModel wraps ExerciseRepository with filter state. ProgressViewModel uses ProgressRepository for real data with composable charts using Canvas.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material3, Hilt, Room, Kotlinx Serialization, Kotlinx Coroutines Flow, kotlinx-datetime for dates.

**Spec:** `DESIGN.md`, `design/tokens.json`, original project spec (4 phases)

## Global Constraints

- Use ONLY `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `PlateauTheme.extras`, `Spacing`, `Size` — never hardcoded colors/dimensions
- Single primary action per screen (`PlateauButton`)
- Reuse `MuscleChip`, `PlateauButton`, `PlateauMark` from `ui/components/`
- Every new screen needs `@Preview` (dark + light)
- Epley formula: `weight × (1 + reps/30)` for e1RM
- Room entities use UUIDs, `updatedAt`, proper FKs for future sync
- Follow existing code patterns in `data/local/`, `data/repository/`, `di/`, `ui/`

## Review Focus

1. **Import parser edge cases**: Empty lines, unknown exercise names, malformed set notation (e.g., "3x10", "3×10", "3x 10", "3 x 10") — must not crash, show review screen with suggestions
2. **Filter combinations**: Multiple simultaneous filters (muscle + equipment + difficulty) — must combine correctly with AND logic
3. **Progress percentage math**: Week completion = sets done / sets planned; handles 0 planned (show N/A not 0%)
4. **Interactive chart touch**: Tap/hold on data point shows tooltip with exact values; doesn't crash on empty data
5. **LLM fallback timeout**: 5s max, graceful degradation to "needs manual review" if LLM fails

---

### Task 1: ImportParser Domain Service (Pure Kotlin)

**Files:**
- Create: `app/src/main/java/com/plateau/app/domain/service/ImportParser.kt`
- Create: `app/src/test/java/com/plateau/app/domain/service/ImportParserTest.kt`

**Interfaces:**
- Consumes: `ExerciseRepository.getAllExercises()` for fuzzy matching catalog
- Produces: `ImportResult` (sealed class: `Success(List<ParsedWorkout>)`, `Partial(List<ParsedWorkout>, List<UnmatchedLine>)`, `Failure(String)`)

- [ ] **Step 1: Write failing tests for ImportParser**

```kotlin
@Test
fun `parses standard Strong format: Exercise 3x10 80kg`() {
    val input = "Panca piana 3x10 80kg\nSquat 4x8 100kg"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Success)
    assertEquals(2, result.workouts.size)
    assertEquals("Panca piana", result.workouts[0].exerciseName)
    assertEquals(3, result.workouts[0].sets.size)
    assertEquals(80.0, result.workouts[0].sets[0].weight)
    assertEquals(10, result.workouts[0].sets[0].reps)
}

@Test
fun `parses multiple set formats: 3x10, 3×10, 3x 10, 3 x 10`() {
    val input = "Panca piana 3x10 80kg\nPanca piana 3×10 80kg\nPanca piana 3x 10 80kg\nPanca piana 3 x 10 80kg"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Success)
    assertEquals(4, result.workouts.size)
    assertAll { result.workouts.forEach { assertEquals(3, it.sets.size) } }
}

@Test
fun `fuzzy matches exercise aliases: "Bench press" -> "Panca piana"`() {
    val input = "Bench press 3x10 80kg"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Success)
    assertEquals("Panca piana", result.workouts[0].exerciseName)
}

@Test
fun `returns Partial with unmatched lines for unknown exercises`() {
    val input = "Panca piana 3x10 80kg\nEsercizio Sconosciuto 3x10 50kg"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Partial)
    assertEquals(1, result.workouts.size)
    assertEquals(1, result.unmatched.size)
    assertTrue(result.unmatched[0].line.contains("Esercizio Sconosciuto"))
}

@Test
fun `handles RPE notation: 3x10 80kg @8`() {
    val input = "Panca piana 3x10 80kg @8"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Success)
    assertEquals(8, result.workouts[0].sets[0].rpe)
}

@Test
fun `handles drop sets: 80kgx10, 70kgx10, 60kgx10`() {
    val input = "Panca piana 80kgx10, 70kgx10, 60kgx10"
    val result = ImportParser.parse(input, catalogExercises)
    assertTrue(result is ImportResult.Success)
    assertEquals(3, result.workouts[0].sets.size)
    assertEquals(80.0, result.workouts[0].sets[0].weight)
    assertEquals(70.0, result.workouts[0].sets[1].weight)
    assertEquals(60.0, result.workouts[0].sets[2].weight)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.plateau.app.domain.service.ImportParserTest"`
Expected: FAIL - ImportParser not found

- [ ] **Step 3: Implement ImportParser with sealed result classes**

Create `ImportParser.kt` with:
- `sealed class ImportResult { data class Success(...), data class Partial(...), data class Failure(...), data class UnmatchedLine(...) }`
- `data class ParsedWorkout(exerciseName, sets: List<ParsedSet>)`
- `data class ParsedSet(weight, reps, rpe?, isDropSet?)`
- `fun parse(input: String, catalog: List<Exercise>): ImportResult`
- Regex patterns for: standard sets (`(\d+)x(\d+)\s*(\d+(?:\.\d+)?)kg`), RPE (`@(\d+(?:\.\d+)?)`), drop sets (`(\d+(?:\.\d+)?)kgx(\d+)`)
- Fuzzy matching: exact name → aliases → Levenshtein distance ≤ 2 → return suggestions
- LLM fallback stub: `private suspend fun llmFallback(unmatched: List<String>): List<ExerciseMatch>` (throws timeout after 5s, returns empty)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.plateau.app.domain.service.ImportParserTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/plateau/app/domain/service/ImportParser.kt app/src/test/java/com/plateau/app/domain/service/ImportParserTest.kt
git commit -m "feat: add ImportParser domain service with hybrid rules+fuzzy matching"
```

---

### Task 2: ImportReviewScreen + ViewModel

**Files:**
- Create: `app/src/main/java/com/plateau/app/ui/import/ImportReviewViewModel.kt`
- Create: `app/src/main/java/com/plateau/app/ui/import/ImportReviewScreen.kt`
- Modify: `app/src/main/java/com/plateau/app/ui/navigation/Destination.kt` (add ImportReview)
- Modify: `app/src/main/java/com/plateau/app/MainActivity.kt` (add route)
- Modify: `app/src/main/java/com/plateau/app/di/RepositoryModule.kt` (provide ImportParser)

**Interfaces:**
- Consumes: `ImportResult` from Intent/navigation args
- Produces: User-confirmed workouts → `SessionRepository.startSession()` + `logSet()`

- [ ] **Step 1: Add ImportReview to Destination sealed class**

```kotlin
// In Destination.kt
data class ImportReview(val result: ImportResult) : Destination()
```

- [ ] **Step 2: Write failing tests for ImportReviewViewModel**

```kotlin
@Test
fun `confirms import saves workouts to session`() {
    val viewModel = ImportReviewViewModel(importResult, sessionRepository, exerciseRepository)
    viewModel.onConfirmImport()
    verify(sessionRepository).startSession(null)
    verify(sessionRepository, times(totalSets)).logSet(any())
}
```

- [ ] **Step 3: Implement ImportReviewViewModel**

- State: `importResult: ImportResult`, `editedWorkouts: MutableStateFlow<List<ParsedWorkout>>`
- `onConfirmImport()`: For each workout, find/create exercise, start session, log sets
- `onEditWorkout(index)`: Navigate to inline editor (bottom sheet)
- `onDismissUnmatched(index)`: Remove from unmatched list

- [ ] **Step 4: Implement ImportReviewScreen**

- Header: "Rivedi importazione" (headlineLarge)
- For each parsed workout: Card with exercise name, MuscleChip(primary), list of sets (weight × reps × RPE)
- Unmatched section: Gray cards with "Non riconosciuto" badge, "Aggiungi come nuovo" button
- Primary action: `PlateauButton("Importa ${count} esercizi")` → confirms
- Secondary: "Annulla" text button
- @Preview dark & light

- [ ] **Step 5: Run tests + verify UI compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/plateau/app/ui/import/ app/src/main/java/com/plateau/app/ui/navigation/Destination.kt app/src/main/java/com/plateau/app/MainActivity.kt app/src/main/java/com/plateau/app/di/RepositoryModule.kt
git commit -m "feat: add ImportReview screen with confirmation flow"
```

---

### Task 3: DiscoverViewModel with Real Filters

**Files:**
- Modify: `app/src/main/java/com/plateau/app/ui/discover/DiscoverViewModel.kt`
- Modify: `app/src/main/java/com/plateau/app/data/repository/ExerciseRepository.kt` (add filter methods)
- Modify: `app/src/main/java/com/plateau/app/data/local/dao/ExerciseDao.kt` (add filter queries)

**Interfaces:**
- Consumes: `ExerciseRepository` flows
- Produces: `StateFlow<DiscoverUiState>` with `filteredExercises`, `recommendations`, `activeFilters`

- [ ] **Step 1: Add filter query methods to ExerciseDao**

```kotlin
@Query("""
    SELECT * FROM exercises 
    WHERE (:muscleId IS NULL OR primaryMuscleId = :muscleId OR secondaryMuscleId = :muscleId)
    AND (:equipment IS NULL OR equipment = :equipment)
    AND (:difficulty IS NULL OR difficulty <= :difficulty)
    AND (:movementPattern IS NULL OR movementPattern = :movementPattern)
    AND (:searchQuery IS NULL OR name LIKE :searchQuery OR aliases LIKE :searchQuery)
    ORDER BY isCompound DESC, name
""")
fun getFiltered(
    muscleId: String?, equipment: String?, difficulty: Int?, 
    movementPattern: String?, searchQuery: String?
): Flow<List<Exercise>>
```

- [ ] **Step 2: Add getVolumeGaps to ExerciseRepository**

```kotlin
// Returns muscle groups with volume < target (from ProgressRepository)
fun getVolumeGaps(): Flow<List<MuscleGroupGap>> 
// MuscleGroupGap(muscleGroup, currentVolume, targetVolume, deficitPercent)
```

- [ ] **Step 3: Write failing tests for DiscoverViewModel**

```kotlin
@Test
fun `filters by muscle group`() {
    val vm = DiscoverViewModel(exerciseRepo, progressRepo)
    vm.onFilterChanged(muscle = "chest")
    assertEquals(chestExercises, vm.uiState.filteredExercises.first())
}

@Test
fun `recommends exercises for volume gaps`() {
    // Mock progressRepo returning chest deficit
    val vm = DiscoverViewModel(exerciseRepo, progressRepo)
    assertTrue(vm.uiState.recommendations.any { it.exercise.primaryMuscleId == "chest" })
}
```

- [ ] **Step 4: Implement DiscoverViewModel**

- `data class DiscoverUiState(filteredExercises, recommendations, activeFilters, isLoading)`
- `data class FilterState(muscleId?, equipment?, difficulty?, movementPattern?, searchQuery?)`
- `onFilterChanged(...)` updates filter state, triggers repo query
- `recommendations`: combine `getVolumeGaps()` + `getExercisesByMuscle()` → score by deficit%
- Expose `muscleGroups: Flow<List<MuscleGroup>>` for filter chips
- Expose `equipmentOptions = listOf("barbell","dumbbell","machine","cable","bodyweight","band")`

- [ ] **Step 5: Update DiscoverScreen to use real ViewModel state**

- Replace static `FilterChips` with dynamic chips from `viewModel.muscleGroups` + equipment list
- Show `viewModel.uiState.filteredExercises` in `AllExercisesSection`
- Show `viewModel.uiState.recommendations` in `RecommendedSection`
- Add search field at top
- Add "Reset filters" when any filter active
- @Preview dark & light with mock data

- [ ] **Step 6: Run tests + verify**

Run: `./gradlew test --tests "com.plateau.app.ui.discover.DiscoverViewModelTest"`
Run: `./gradlew assembleDebug`
Expected: PASS + BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/plateau/app/ui/discover/DiscoverViewModel.kt app/src/main/java/com/plateau/app/ui/discover/DiscoverScreen.kt app/src/main/java/com/plateau/app/data/repository/ExerciseRepository.kt app/src/main/java/com/plateau/app/data/local/dao/ExerciseDao.kt
git commit -m "feat: implement Discover real filters and gap-based recommendations"
```

---

### Task 4: Progress Dashboard - Percentage Completion + Interactive Charts

**Files:**
- Modify: `app/src/main/java/com/plateau/app/ui/progress/ProgressViewModel.kt`
- Modify: `app/src/main/java/com/plateau/app/ui/progress/ProgressScreen.kt`
- Create: `app/src/main/java/com/plateau/app/ui/progress/chart/E1RMChart.kt`
- Create: `app/src/main/java/com/plateau/app/ui/progress/chart/VolumeChart.kt`
- Create: `app/src/main/java/com/plateau/app/ui/progress/chart/CompletionRing.kt`
- Create: `app/src/test/java/com/plateau/app/ui/progress/ProgressViewModelTest.kt`

**Interfaces:**
- Consumes: `ProgressRepository` (e1RM trend, volume metrics, PRs, double progression)
- Produces: `StateFlow<ProgressUiState>` with all metrics + selected exercise

- [ ] **Step 1: Write failing tests for ProgressViewModel**

```kotlin
@Test
fun `calculates week completion percentage`() {
    val vm = ProgressViewModel(progressRepo, planRepo)
    // 3 of 5 sets done
    assertEquals(60, vm.uiState.weekCompletionPercent)
}

@Test
fun `calculates exercise completion percentage`() {
    // 8 of 12 sets done for selected exercise
    assertEquals(67, vm.uiState.exerciseCompletionPercent)
}

@Test
fun `selects exercise updates all charts`() {
    vm.onExerciseSelected("ex_panca_piana")
    assertEquals("Panca piana", vm.uiState.selectedExerciseName)
    assertNotNull(vm.uiState.e1rmTrend)
}
```

- [ ] **Step 2: Implement ProgressViewModel**

- `data class ProgressUiState(selectedExerciseId?, e1rmTrend?, volumeMetrics?, prs?, doubleProgression?, weekCompletionPercent, exerciseCompletionPercent, muscleCompletionMap: Map<String, Int>)`
- `weekCompletionPercent`: sets done this week / sets planned this week (from active plan)
- `exerciseCompletionPercent`: sets done for selected exercise / sets planned for it
- `muscleCompletionMap`: per-muscle completion for ring dashboard
- `onExerciseSelected(id)` triggers repo queries

- [ ] **Step 3: Implement CompletionRing composable**

```kotlin
@Composable
fun CompletionRing(
    progress: Float, // 0..1
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp
)
```
- Draw with `Canvas`, `drawArc` for background + progress
- Center: `NumberDisplay` value + `label` below
- Color: `MaterialTheme.colorScheme.progress` for ring, `progressText` for value
- Animate progress with `animateFloatAsState`

- [ ] **Step 4: Implement E1RMChart composable (interactive)**

```kotlin
@Composable
fun E1RMChart(
    dataPoints: List<E1RMDataPoint>,
    onPointTap: (E1RMDataPoint) -> Unit,
    modifier: Modifier = Modifier
)
```
- `Canvas` drawing: X=date (30 days), Y=e1RM range
- Line path + circles at points
- Touch handling: `pointerInput` + `detectTapGestures` → find nearest point → callback
- Tooltip: `Box` overlay at tap position with date, e1RM, weight×reps
- Grid lines (4 horizontal, 6 vertical) in `outline` color
- Y-axis labels (min/max), X-axis labels (week markers)

- [ ] **Step 5: Implement VolumeChart composable (bar chart)**

```kotlin
@Composable
fun VolumeChart(
    weeklyData: List<WeeklyVolumePoint>,
    targetVolume: Double,
    modifier: Modifier = Modifier
)
```
- Bar per week, height proportional to volume
- Target line across chart (dashed, `primary` color)
- Bar color: `progress` if ≥ target, `primary` if < target
- Tap bar → tooltip with week, volume, % of target

- [ ] **Step 6: Redesign ProgressScreen with Dashboard Layout**

- Top: 3 `CompletionRing` in horizontal scroll (Settimana, Esercizio, Muscolo selezionato)
- Middle: Exercise selector dropdown (from active plan exercises)
- e1RMChart (full width, 200dp tall) - interactive
- VolumeChart (full width, 160dp tall)
- PRsList (unchanged but real data)
- DoubleProgressionCard (unchanged but real data)
- All cards use `MaterialTheme.shapes.large`, `Spacing.lg` padding
- @Preview dark & light with mock data

- [ ] **Step 7: Run tests + verify**

Run: `./gradlew test --tests "com.plateau.app.ui.progress.ProgressViewModelTest"`
Run: `./gradlew assembleDebug`
Expected: PASS + BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/plateau/app/ui/progress/ app/src/test/java/com/plateau/app/ui/progress/ProgressViewModelTest.kt
git commit -m "feat: add Progress dashboard with completion rings and interactive charts"
```

---

### Task 5: Integration & Polish

**Files:**
- Modify: `app/src/main/java/com/plateau/app/MainActivity.kt` (verify all routes)
- Modify: `app/src/main/java/com/plateau/app/di/RepositoryModule.kt` (verify all provides)

- [ ] **Step 1: Verify all navigation routes work**

- Home → Session (start workout)
- Home → Plans
- Home → Discover (filters work)
- Home → Progress (charts interactive)
- Home → Photo
- Import flow: Share text → ImportReview → Session

- [ ] **Step 2: Run full build + lint**

Run: `./gradlew build`
Run: `./gradlew lint`
Expected: BUILD SUCCESSFUL, no new lint errors

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: Phase 2 integration - import parser, discover filters, progress dashboard"
```