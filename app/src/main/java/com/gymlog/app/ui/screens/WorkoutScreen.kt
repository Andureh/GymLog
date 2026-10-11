@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.RestTimer
import com.gymlog.app.app
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.data.WorkoutExerciseFull
import com.gymlog.app.data.WorkoutSet
import com.gymlog.app.domain.Bests
import com.gymlog.app.domain.PrType
import com.gymlog.app.domain.completedSets
import com.gymlog.app.domain.durationMs
import com.gymlog.app.domain.e1rm
import com.gymlog.app.domain.markPrs
import com.gymlog.app.domain.prsFor
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.domain.Session
import com.gymlog.app.domain.SuggestKind
import com.gymlog.app.domain.Suggestion
import com.gymlog.app.domain.suggestNext
import androidx.compose.material.icons.filled.Lightbulb
import com.gymlog.app.i18n.I18n
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.CompactNumberField
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.ExerciseAvatar
import com.gymlog.app.ui.components.Loaded
import com.gymlog.app.ui.components.RestPickerDialog
import com.gymlog.app.ui.components.StatItem
import com.gymlog.app.ui.components.rememberNow
import com.gymlog.app.ui.components.restLabel
import com.gymlog.app.ui.theme.DropColor
import com.gymlog.app.ui.theme.FailureColor
import com.gymlog.app.ui.theme.Gold
import com.gymlog.app.ui.theme.Success
import com.gymlog.app.ui.theme.WarmupColor
import com.gymlog.app.util.fmtClock
import com.gymlog.app.util.fmtDateTime
import com.gymlog.app.util.fmtDuration
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.fmtVolume
import com.gymlog.app.util.fmtWeight
import com.gymlog.app.util.fromDisplayWeight
import com.gymlog.app.util.parseDuration
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import com.gymlog.app.util.vibrateCelebrate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class PrAlert(val exercise: Exercise, val prs: List<PrType>, val w: Double, val r: Int, val d: Int)

fun prValue(t: PrType, w: Double, r: Int, d: Int, lbs: Boolean): String = when (t) {
    PrType.WEIGHT -> fmtWeight(w, lbs)
    PrType.E1RM -> fmtWeight(e1rm(w, r), lbs, 1)
    PrType.VOLUME -> fmtWeight(w * r, lbs, 1)
    PrType.REPS -> I18n.current.reps(r)
    PrType.DURATION -> fmtClock(d)
}

fun setSummary(type: ExerciseType, w: Double, r: Int, d: Int, lbs: Boolean): String = when (type) {
    ExerciseType.WEIGHT_REPS -> "${fmtWeight(w, lbs)} × $r"
    ExerciseType.REPS_ONLY -> if (w > 0) "+${fmtWeight(w, lbs)} × $r" else I18n.current.reps(r)
    ExerciseType.DURATION -> fmtClock(d)
}

/** Ecrã do treino: em curso (endTime == null) ou edição de um treino já terminado. */
@Composable
fun WorkoutScreen(nav: NavController, workoutId: Long) {
    val app = LocalContext.current.app
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val str = LocalStrings.current
    val loaded by remember(workoutId) { repo.workout(workoutId).map { Loaded(it) } }.collectAsStateWithLifecycle(null)

    val state = loaded
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val wf = state.value
    if (wf == null) {
        // Treino eliminado/descartado
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    val isActive = wf.workout.endTime == null
    val start = wf.workout.startTime
    val exIds = wf.exercises.map { it.exercise.id }.distinct()
    val prevMap by produceState(emptyMap<Long, List<WorkoutSet>>(), exIds) {
        value = exIds.associateWith { repo.previousSets(it, start) }
    }
    val bestsMap by produceState(emptyMap<Long, Bests>(), exIds) {
        value = exIds.associateWith { repo.bestsBefore(it, start) }
    }
    val sessionsMap by produceState(emptyMap<Long, List<Session>>(), exIds) {
        value = exIds.associateWith { repo.sessionsBefore(it, start) }
    }
    val prMarks = remember(wf, bestsMap) {
        buildMap {
            wf.exercises.groupBy { it.exercise.id }.forEach { (exId, list) ->
                putAll(markPrs(list.first().exercise.type, bestsMap[exId] ?: Bests(), list.flatMap { it.sets }))
            }
        }
    }

    // Manter o ecrã ligado durante o treino
    val view = LocalView.current
    DisposableEffect(isActive, settings.keepScreenOn) {
        view.keepScreenOn = isActive && settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val snackbar = remember { SnackbarHostState() }
    var prAlert by remember { mutableStateOf<PrAlert?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    fun toggle(e: WorkoutExerciseFull, s: WorkoutSet, prev: WorkoutSet?) {
        focus.clearFocus()
        scope.launch {
            if (s.completed) {
                repo.setCompleted(s.id, false)
                return@launch
            }
            val type = e.exercise.type
            var w = s.weight
            var r = s.reps
            var d = s.durationSec
            // Campos vazios usam os valores da última vez (como no Hevy)
            if (prev != null) {
                if (w == 0.0 && type != ExerciseType.DURATION) w = prev.weight
                if (r == 0 && type != ExerciseType.DURATION) r = prev.reps
                if (d == 0 && type == ExerciseType.DURATION) d = prev.durationSec
            }
            val valid = if (type == ExerciseType.DURATION) d > 0 else r > 0
            if (!valid) {
                snackbar.showSnackbar(if (type == ExerciseType.DURATION) str.enterDuration else str.enterReps)
                return@launch
            }
            repo.completeSet(s.id, w, r, d)

            val history = bestsMap[e.exercise.id] ?: Bests()
            if (s.type != SetType.WARMUP && history.count > 0) {
                val before = wf.exercises.filter { it.exercise.id == e.exercise.id }
                    .flatMap { it.sets }
                    .filter { it.completed && it.id != s.id && it.type != SetType.WARMUP }
                    .fold(history) { b, x -> b.add(x.weight, x.reps, x.durationSec) }
                val prs = prsFor(type, before, w, r, d)
                if (prs.isNotEmpty()) {
                    prAlert = PrAlert(e.exercise, prs, w, r, d)
                    app.vibrateCelebrate()
                }
            }
            if (isActive && e.we.restSeconds > 0) app.restTimer.start(e.we.restSeconds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(if (isActive) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.ArrowBack, str.back)
                    }
                },
                title = {
                    Column {
                        Text(if (isActive) str.workoutInProgress else str.editWorkout, style = MaterialTheme.typography.titleMedium)
                        if (isActive) ElapsedText(start)
                    }
                },
                actions = {
                    Button(onClick = { focus.clearFocus(); showFinish = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (isActive) str.finish else str.save)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (isActive) RestTimerBar(app.restTimer) },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item(key = "header") {
                WorkoutHeader(
                    name = wf.workout.name,
                    notes = wf.workout.notes,
                    subtitle = if (isActive) null else fmtDateTime(start),
                    duration = if (isActive) null else fmtDuration(wf.durationMs()),
                    volume = fmtVolume(wf.volumeKg(), lbs),
                    sets = wf.completedSets(),
                    onName = { n -> scope.launch { repo.renameWorkout(workoutId, n) } },
                    onNotes = { n -> scope.launch { repo.setWorkoutNotes(workoutId, n) } },
                    isActive = isActive,
                    startTime = start,
                )
            }
            if (wf.exercises.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timeline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(str.startByAdding, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(wf.exercises, key = { it.we.id }) { e ->
                val suggestion = if (isActive && settings.suggestions && e.sets.any { !it.completed && it.type != SetType.WARMUP }) {
                    sessionsMap[e.exercise.id]?.let { suggestNext(e.exercise.type, e.exercise.equipment, it, lbs) }
                } else null
                ExerciseBlock(
                    e = e,
                    suggestion = suggestion,
                    onApplySuggestion = { sug ->
                        focus.clearFocus()
                        scope.launch { repo.applySuggestion(e.we.id, sug.weight, sug.reps, sug.duration) }
                    },
                    prev = prevMap[e.exercise.id].orEmpty(),
                    prMarks = prMarks,
                    lbs = lbs,
                    canMoveUp = e != wf.exercises.first(),
                    canMoveDown = e != wf.exercises.last(),
                    onOpen = { nav.navigate("exercise/${e.exercise.id}") },
                    onMove = { dir -> scope.launch { repo.moveExercise(workoutId, e.we.id, dir) } },
                    onRemove = { scope.launch { repo.removeExercise(e.we.id) } },
                    onRest = { sec -> scope.launch { repo.setRest(e.we.id, sec) } },
                    onNotes = { n -> scope.launch { repo.setExerciseNotes(e.we.id, n) } },
                    onAddSet = { scope.launch { repo.addSet(e.we.id) } },
                    onToggle = { s, p -> toggle(e, s, p) },
                    onWeight = { id, kg -> scope.launch { repo.setWeight(id, kg) } },
                    onReps = { id, r -> scope.launch { repo.setReps(id, r) } },
                    onDuration = { id, d -> scope.launch { repo.setDuration(id, d) } },
                    onSetType = { id, t -> scope.launch { repo.setSetType(id, t) } },
                    onDeleteSet = { id -> scope.launch { repo.deleteSet(id) } },
                )
            }
            item(key = "actions") {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(str.addExercise)
                    }
                    if (isActive) {
                        TextButton(onClick = { showDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(str.discardWorkout, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPicked = { ids -> showPicker = false; scope.launch { repo.addExercises(workoutId, ids) } },
        )
    }

    if (showDiscard) {
        ConfirmDialog(
            title = str.discardWorkoutTitle,
            text = str.discardWorkoutText,
            confirmText = str.discard,
            destructive = true,
            onConfirm = {
                app.restTimer.stop()
                scope.launch { repo.deleteWorkout(workoutId) }
            },
            onDismiss = { showDiscard = false },
        )
    }

    if (showFinish) {
        val completed = wf.completedSets()
        val incomplete = wf.exercises.sumOf { e -> e.sets.count { !it.completed } }
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text(if (isActive) str.finishWorkoutTitle else str.saveChangesTitle) },
            text = {
                Text(
                    when {
                        completed == 0 -> str.noCompletedSets
                        incomplete > 0 -> str.incompleteSets(incomplete)
                        else -> if (isActive) str.goodJob else str.changesWillBeSaved
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinish = false
                    scope.launch {
                        if (isActive) app.restTimer.stop()
                        val ok = repo.finishWorkout(workoutId, setEnd = isActive)
                        if (ok) {
                            if (isActive) {
                                nav.navigate("workoutDetail/$workoutId?new=true") {
                                    popUpTo("workout/{id}") { inclusive = true }
                                }
                            } else {
                                nav.popBackStack()
                            }
                        }
                    }
                }) { Text(if (isActive) str.finish else str.save) }
            },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text(str.keepGoing) } },
        )
    }

    prAlert?.let { a ->
        AlertDialog(
            onDismissRequest = { prAlert = null },
            icon = { Text("🏆", fontSize = 48.sp) },
            title = { Text(str.newPr, textAlign = TextAlign.Center) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(str.exName(a.exercise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(setSummary(a.exercise.type, a.w, a.r, a.d, lbs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    a.prs.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, null, Modifier.size(18.dp), tint = Gold)
                            Spacer(Modifier.width(6.dp))
                            Text("${str.prType(t)}: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(prValue(t, a.w, a.r, a.d, lbs), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { prAlert = null }) { Text(str.nice) } },
        )
    }
}

@Composable
private fun ElapsedText(start: Long) {
    val now = rememberNow()
    Text(fmtClock((now - start) / 1000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun WorkoutHeader(
    name: String,
    notes: String,
    subtitle: String?,
    duration: String?,
    volume: String,
    sets: Int,
    onName: (String) -> Unit,
    onNotes: (String) -> Unit,
    isActive: Boolean,
    startTime: Long,
) {
    var nameText by remember { mutableStateOf(name) }
    var notesText by remember { mutableStateOf(notes) }
    val s = LocalStrings.current
    val textColor = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        BasicTextField(
            value = nameText,
            onValueChange = { nameText = it; onName(it) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(12.dp))
        Row {
            if (isActive) {
                val now = rememberNow()
                StatItem(s.duration, fmtClock((now - startTime) / 1000), Modifier.weight(1f))
            } else {
                StatItem(s.duration, duration ?: "-", Modifier.weight(1f))
            }
            StatItem(s.volume, volume, Modifier.weight(1f))
            StatItem(s.sets, sets.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = notesText,
            onValueChange = { notesText = it; onNotes(it) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            decorationBox = { inner ->
                Box {
                    if (notesText.isEmpty()) {
                        Text(s.workoutNotesHint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun setLabel(sets: List<WorkoutSet>, index: Int): String {
    val s = sets[index]
    return when (s.type) {
        SetType.WARMUP -> I18n.current.warmupShort
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
        SetType.NORMAL -> (sets.take(index + 1).count { it.type != SetType.WARMUP }).toString()
    }
}

private fun setTypeColor(t: SetType, default: Color) = when (t) {
    SetType.NORMAL -> default
    SetType.WARMUP -> WarmupColor
    SetType.DROP -> DropColor
    SetType.FAILURE -> FailureColor
}

@Composable
private fun ExerciseBlock(
    e: WorkoutExerciseFull,
    suggestion: Suggestion?,
    onApplySuggestion: (Suggestion) -> Unit,
    prev: List<WorkoutSet>,
    prMarks: Map<Long, List<PrType>>,
    lbs: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onRest: (Int) -> Unit,
    onNotes: (String) -> Unit,
    onAddSet: () -> Unit,
    onToggle: (WorkoutSet, WorkoutSet?) -> Unit,
    onWeight: (Long, Double) -> Unit,
    onReps: (Long, Int) -> Unit,
    onDuration: (Long, Int) -> Unit,
    onSetType: (Long, SetType) -> Unit,
    onDeleteSet: (Long) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var restDialog by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(e.we.notes) }
    val type = e.exercise.type
    val s = LocalStrings.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp)
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseAvatar(e.exercise, 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(
                    s.exName(e.exercise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(s.muscle(e.exercise.muscle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, s.options) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (canMoveUp) DropdownMenuItem(
                        text = { Text(s.moveUp) }, leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                        onClick = { menu = false; onMove(-1) },
                    )
                    if (canMoveDown) DropdownMenuItem(
                        text = { Text(s.moveDown) }, leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                        onClick = { menu = false; onMove(1) },
                    )
                    DropdownMenuItem(
                        text = { Text(s.restTime) }, leadingIcon = { Icon(Icons.Default.Timer, null) },
                        onClick = { menu = false; restDialog = true },
                    )
                    DropdownMenuItem(
                        text = { Text(s.removeExercise, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmRemove = true },
                    )
                }
            }
        }

        BasicTextField(
            value = notes,
            onValueChange = { notes = it; onNotes(it) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            decorationBox = { inner ->
                Box {
                    if (notes.isEmpty()) Text(
                        s.exerciseNoteHint, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    inner()
                }
            },
        )

        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp).clip(RoundedCornerShape(8.dp)).clickable { restDialog = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Timer, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(s.restValue(restLabel(e.we.restSeconds)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }

        suggestion?.let { sug ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp), tint = Gold)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${s.suggestion} · " + when (sug.kind) {
                            SuggestKind.UP -> s.suggestUp
                            SuggestKind.REPS -> s.suggestMore
                            SuggestKind.DELOAD -> s.suggestDeload
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        setSummary(type, sug.weight, sug.reps, sug.duration, lbs),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                TextButton(onClick = { onApplySuggestion(sug) }) { Text(s.apply) }
            }
        }

        // Cabeçalho da tabela
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            val hs = MaterialTheme.typography.labelSmall
            val hc = MaterialTheme.colorScheme.onSurfaceVariant
            Text(s.colSet, style = hs, color = hc, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            Text(s.colPrevious, style = hs, color = hc, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            when (type) {
                ExerciseType.WEIGHT_REPS -> {
                    Text(unitLabel(lbs).uppercase(), style = hs, color = hc, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(6.dp))
                    Text(s.colReps, style = hs, color = hc, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                }
                ExerciseType.REPS_ONLY -> Text(s.colReps, style = hs, color = hc, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                ExerciseType.DURATION -> Text(s.colTime, style = hs, color = hc, modifier = Modifier.width(88.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Check, null, Modifier.width(40.dp).size(16.dp), tint = hc)
        }

        e.sets.forEachIndexed { i, s ->
            key(s.id) {
                SetRow(
                    label = setLabel(e.sets, i),
                    set = s,
                    prev = prev.getOrNull(i),
                    type = type,
                    lbs = lbs,
                    prs = prMarks[s.id].orEmpty(),
                    onToggle = { onToggle(s, prev.getOrNull(i)) },
                    onWeight = { onWeight(s.id, it) },
                    onReps = { onReps(s.id, it) },
                    onDuration = { onDuration(s.id, it) },
                    onType = { onSetType(s.id, it) },
                    onDelete = { onDeleteSet(s.id) },
                )
            }
        }

        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.addSet)
        }
    }

    if (restDialog) RestPickerDialog(e.we.restSeconds, onPick = onRest, onDismiss = { restDialog = false })
    if (confirmRemove) ConfirmDialog(
        title = s.removeExerciseTitle,
        text = s.removeExerciseText(s.exName(e.exercise)),
        confirmText = s.remove, destructive = true,
        onConfirm = onRemove, onDismiss = { confirmRemove = false },
    )
}

@Composable
private fun SetRow(
    label: String,
    set: WorkoutSet,
    prev: WorkoutSet?,
    type: ExerciseType,
    lbs: Boolean,
    prs: List<PrType>,
    onToggle: () -> Unit,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onDuration: (Int) -> Unit,
    onType: (SetType) -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val done = set.completed
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (done) Success.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { menu = true }.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label, fontWeight = FontWeight.Bold,
                    color = setTypeColor(set.type, MaterialTheme.colorScheme.onSurface),
                )
                if (prs.isNotEmpty()) {
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.EmojiEvents, s.record, Modifier.size(14.dp), tint = Gold)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                SetType.entries.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(s.setType(t), color = setTypeColor(t, MaterialTheme.colorScheme.onSurface)) },
                        trailingIcon = { if (t == set.type) Icon(Icons.Default.Check, null) },
                        onClick = { menu = false; onType(t) },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(s.deleteSet, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
        Text(
            prev?.let { setSummary(type, it.weight, it.reps, it.durationSec, lbs) } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        when (type) {
            ExerciseType.WEIGHT_REPS -> {
                CompactNumberField(
                    value = toDisplayWeight(set.weight, lbs),
                    onValue = { onWeight(fromDisplayWeight(it, lbs)) },
                    placeholder = prev?.let { fmtNum(toDisplayWeight(it.weight, lbs)) } ?: "0",
                    modifier = Modifier.width(72.dp),
                    highlight = done,
                )
                Spacer(Modifier.width(6.dp))
                CompactNumberField(
                    value = set.reps.toDouble(),
                    onValue = { onReps(it.toInt()) },
                    placeholder = prev?.reps?.toString() ?: "0",
                    decimal = false,
                    modifier = Modifier.width(56.dp),
                    highlight = done,
                )
            }
            ExerciseType.REPS_ONLY -> CompactNumberField(
                value = set.reps.toDouble(),
                onValue = { onReps(it.toInt()) },
                placeholder = prev?.reps?.toString() ?: "0",
                decimal = false,
                modifier = Modifier.width(72.dp),
                highlight = done,
            )
            ExerciseType.DURATION -> CompactNumberField(
                value = set.durationSec.toDouble(),
                onValue = { onDuration(it.toInt()) },
                placeholder = prev?.let { fmtClock(it.durationSec) } ?: "0:00",
                decimal = false,
                format = { if (it == 0.0) "" else fmtClock(it.toInt()) },
                parse = { parseDuration(it) },
                modifier = Modifier.width(88.dp),
                highlight = done,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp, 34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (done) Success else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check, if (done) s.done else s.markDone,
                tint = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RestTimerBar(timer: RestTimer) {
    val st by timer.state.collectAsStateWithLifecycle()
    val s = st ?: return
    val now = rememberNow(200)
    val remaining = ((s.endAt - now + 999) / 1000).coerceAtLeast(0).toInt()
    val str = LocalStrings.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
        Column {
            LinearProgressIndicator(
                progress = { (remaining.toFloat() / s.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(str.rest, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fmtClock(remaining), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { timer.add(-15) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("−15") }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = { timer.add(15) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+15") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = { timer.stop() }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(str.skip) }
            }
        }
    }
}
