package com.plateau.app.ui.progress

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plateau.app.domain.model.DoubleProgressionSuggestion
import com.plateau.app.domain.model.E1RMTrend
import com.plateau.app.domain.model.PR
import com.plateau.app.domain.model.VolumeMetrics
import com.plateau.app.ui.components.BottomNavBar
import com.plateau.app.ui.components.MuscleChip
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.progress.chart.CompletionRing
import com.plateau.app.ui.progress.chart.DAY_MS
import com.plateau.app.ui.progress.chart.E1RMChart
import com.plateau.app.ui.progress.chart.PreviewE1RMPoints
import com.plateau.app.ui.progress.chart.PreviewWeeklyVolume
import com.plateau.app.ui.progress.chart.VolumeChart
import com.plateau.app.ui.progress.chart.formatKg
import com.plateau.app.ui.progress.chart.percentLabel
import com.plateau.app.ui.progress.chart.percentOfTarget
import com.plateau.app.ui.progress.chart.ringProgress
import com.plateau.app.ui.theme.NumberLarge
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.theme.Size
import com.plateau.app.ui.theme.Spacing

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    currentDestination: Destination = Destination.Progress,
    onNavigate: (Destination) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ProgressContent(state = state, onExerciseSelected = viewModel::onExerciseSelected)
            }
            BottomNavBar(currentDestination, onNavigate)
        }
    }
}

/** Dashboard senza stato: anelli, selettore esercizio, grafici, record e doppia progressione. */
@Composable
internal fun ProgressContent(
    state: ProgressUiState,
    onExerciseSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis()
) {
    val selected = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
    val muscleName = state.volumeMetrics?.muscleGroupName
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text("Progressi", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            val musclePercent = selected?.let { state.muscleCompletionMap[it.primaryMuscleId] }
            CompletionRing(ringProgress(state.weekCompletionPercent), "Settimana", percentLabel(state.weekCompletionPercent))
            CompletionRing(ringProgress(state.exerciseCompletionPercent), "Esercizio", percentLabel(state.exerciseCompletionPercent))
            CompletionRing(ringProgress(musclePercent), muscleName ?: "Muscolo", percentLabel(musclePercent))
        }
        if (!state.hasActivePlan) {
            Text(
                "Nessuna scheda attiva: attivane una per vedere il completamento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ExerciseSelector(
            options = state.exercises,
            selectedName = state.selectedExerciseName,
            placeholder = if (state.hasActivePlan) "Nessun esercizio nella scheda" else "Nessuna serie registrata",
            muscleName = muscleName,
            onSelected = onExerciseSelected
        )

        E1RMCard(state.e1rmTrend)
        VolumeCard(state.volumeMetrics)
        PRsList(state.prs, now)
        DoubleProgressionCard(state.doubleProgression)
    }
}

@Composable
private fun ExerciseSelector(
    options: List<ProgressExerciseOption>,
    selectedName: String?,
    placeholder: String,
    muscleName: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = options.size > 1
    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Size.minTouch)
                .semantics { role = Role.DropdownList; onClick(label = "Cambia esercizio", action = null) },
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedName ?: placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (muscleName != null) MuscleChip(muscleName, emphasized = true)
                if (enabled) {
                    Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            content()
        }
    }
}

@Composable
private fun CardHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, positive: Boolean = false, align: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = NumberLarge, color = if (positive) PlateauTheme.extras.progressText else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun E1RMCard(trend: E1RMTrend?) {
    DashboardCard {
        CardHeader("e1RM", "30 giorni")
        E1RMChart(dataPoints = trend?.dataPoints.orEmpty(), onPointTap = {})
        if (trend != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Stat("Attuale", "${formatKg(trend.currentE1RM)} kg", Modifier.weight(1f))
                Stat("Migliore", "${formatKg(trend.bestE1RM)} kg", Modifier.weight(1f), positive = true, align = Alignment.CenterHorizontally)
                val change = trend.change30Days
                val sign = if (change > 0) "+" else if (change < 0) "−" else ""
                Stat(
                    "Variazione", "$sign${formatKg(kotlin.math.abs(change))} kg", Modifier.weight(1f),
                    positive = change > 0, align = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun VolumeCard(volume: VolumeMetrics?) {
    DashboardCard {
        CardHeader(volume?.let { "Volume settimanale · ${it.muscleGroupName}" } ?: "Volume settimanale")
        VolumeChart(weeklyData = volume?.weeklyData.orEmpty(), targetVolume = volume?.targetVolume ?: 0.0)
        if (volume != null) {
            val hasTarget = volume.targetVolume > 0.0
            Row(modifier = Modifier.fillMaxWidth()) {
                Stat("Questa settimana", "${formatKg(volume.weeklyVolume)} kg", Modifier.weight(1f))
                Stat(
                    "Target", if (hasTarget) "${formatKg(volume.targetVolume)} kg" else "N/D", Modifier.weight(1f),
                    align = Alignment.CenterHorizontally
                )
                val percent = percentOfTarget(volume.weeklyVolume, volume.targetVolume)
                Stat(
                    "Del target", percentLabel(percent), Modifier.weight(1f),
                    positive = percent != null && percent >= 100, align = Alignment.End
                )
            }
            if (hasTarget) {
                Text(
                    "Linea tratteggiata: target (media delle settimane con dati). Tocca una barra per i dettagli.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PRsList(prs: List<PR>?, now: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text("Record personali", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (prs.isNullOrEmpty()) {
            Text(
                "Nessun record ancora: registra una serie per iniziare.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            prs.forEach { PRCard(it, now) }
        }
    }
}

@Composable
private fun PRCard(pr: PR, now: Long) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pr.exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (pr.isNew) {
                        Text("NUOVO", style = MaterialTheme.typography.labelSmall, color = PlateauTheme.extras.progressText)
                    }
                }
                Text(
                    "${formatKg(pr.weight)} kg × ${pr.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${formatKg(pr.e1rm)} kg", style = NumberLarge, color = PlateauTheme.extras.progressText)
                Text(
                    "e1RM · ${relativeDay(pr.date, now)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DoubleProgressionCard(suggestions: List<DoubleProgressionSuggestion>?) {
    DashboardCard {
        CardHeader("Doppia progressione")
        if (suggestions.isNullOrEmpty()) {
            Text(
                "Nessun suggerimento per ora: registra le serie di un esercizio della scheda attiva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            suggestions.forEach { s ->
                val atTop = s.suggestedWeight > s.currentWeight
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(s.exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (atTop) {
                            val weeks = if (s.weeksAtTop == 1) "1 settimana" else "${s.weeksAtTop} settimane"
                            "Range chiuso: ${formatKg(s.currentWeight)} kg × ${s.currentReps} da $weeks"
                        } else {
                            "Ultima volta: ${formatKg(s.currentWeight)} kg × ${s.currentReps}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (atTop) "Prossima seduta: ${formatKg(s.suggestedWeight)} kg × ${s.targetReps} (+${formatKg(s.suggestedWeight - s.currentWeight)} kg)"
                        else "Prossima seduta: ${formatKg(s.suggestedWeight)} kg × ${s.targetReps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (atTop) PlateauTheme.extras.progressText else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** "oggi", "ieri", "N giorni fa", "N settimane fa". */
internal fun relativeDay(date: Long, now: Long): String {
    val days = ((now - date) / DAY_MS).coerceAtLeast(0)
    return when {
        days == 0L -> "oggi"
        days == 1L -> "ieri"
        days < 7 -> "$days giorni fa"
        days < 14 -> "1 settimana fa"
        else -> "${days / 7} settimane fa"
    }
}

private val PreviewNow = PreviewE1RMPoints.last().date + DAY_MS

private val PreviewState = ProgressUiState(
    exercises = listOf(
        ProgressExerciseOption("ex_panca_piana", "Panca piana", "chest"),
        ProgressExerciseOption("ex_squat", "Squat", "quads")
    ),
    selectedExerciseId = "ex_panca_piana",
    selectedExerciseName = "Panca piana",
    e1rmTrend = E1RMTrend(
        "ex_panca_piana", "Panca piana",
        currentE1RM = PreviewE1RMPoints.last().e1rm,
        bestE1RM = PreviewE1RMPoints.maxOf { it.e1rm },
        change30Days = PreviewE1RMPoints.last().e1rm - PreviewE1RMPoints.first().e1rm,
        dataPoints = PreviewE1RMPoints
    ),
    volumeMetrics = VolumeMetrics("chest", "Petto", PreviewWeeklyVolume.last().volume, 7600.0, PreviewWeeklyVolume),
    prs = listOf(
        PR("ex_panca_piana", "Panca piana", 85.0, 10, 85.0 * (1 + 10 / 30.0), PreviewNow - DAY_MS, isNew = true),
        PR("ex_squat", "Squat", 140.0, 6, 140.0 * (1 + 6 / 30.0), PreviewNow - 9 * DAY_MS, isNew = false)
    ),
    doubleProgression = listOf(
        DoubleProgressionSuggestion("ex_panca_piana", "Panca piana", 80.0, 10, 6, 82.5, 2),
        DoubleProgressionSuggestion("ex_squat", "Squat", 140.0, 6, 7, 140.0, 0)
    ),
    hasActivePlan = true,
    weekCompletionPercent = 60,
    exerciseCompletionPercent = null,
    muscleCompletionMap = mapOf("chest" to 67)
)

@Composable
private fun ProgressPreviewBody() = Surface(color = MaterialTheme.colorScheme.background) {
    ProgressContent(state = PreviewState, onExerciseSelected = {}, now = PreviewNow)
}

@Preview(name = "Progressi · scuro", showBackground = true, heightDp = 1600)
@Composable
private fun ProgressDarkPreview() = PlateauTheme(darkTheme = true) { ProgressPreviewBody() }

@Preview(name = "Progressi · chiaro", showBackground = true, heightDp = 1600)
@Composable
private fun ProgressLightPreview() = PlateauTheme(darkTheme = false) { ProgressPreviewBody() }

@Preview(name = "Progressi · vuoto", showBackground = true)
@Composable
private fun ProgressEmptyPreview() = PlateauTheme(darkTheme = true) {
    Surface(color = MaterialTheme.colorScheme.background) { ProgressContent(state = ProgressUiState(), onExerciseSelected = {}) }
}
