@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.domain.PrType
import com.gymlog.app.domain.analyzePrs
import com.gymlog.app.domain.completedSets
import com.gymlog.app.domain.durationMs
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.EmptyState
import com.gymlog.app.ui.components.Loaded
import com.gymlog.app.ui.components.StatItem
import com.gymlog.app.ui.theme.Gold
import com.gymlog.app.util.fmtDate
import com.gymlog.app.util.fmtDateTime
import com.gymlog.app.util.fmtDuration
import com.gymlog.app.util.fmtMonthYear
import com.gymlog.app.util.fmtVolume
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(nav: NavController) {
    val app = LocalContext.current.app
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val list by remember { app.repo.finishedWorkouts() }.collectAsStateWithLifecycle(null)
    val prMarks = remember(list) { analyzePrs(list.orEmpty().reversed()) }
    val s = LocalStrings.current

    Scaffold(topBar = { TopAppBar(title = { Text(s.tabHistory, fontWeight = FontWeight.Bold) }) }) { pad ->
        val workouts = list
        if (workouts == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (workouts.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Default.History, s.noWorkoutsTitle, s.noWorkoutsText)
            }
            return@Scaffold
        }
        val grouped = remember(workouts, s) { workouts.groupBy { fmtMonthYear(it.workout.startTime) } }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            grouped.forEach { (month, ws) ->
                item(key = "h_$month") {
                    Text(
                        s.monthCount(month, ws.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(ws, key = { it.workout.id }) { w ->
                    WorkoutCard(w, prMarks, settings.useLbs) { nav.navigate("workoutDetail/${w.workout.id}") }
                }
            }
        }
    }
}

fun WorkoutFull.prCount(prMarks: Map<Long, List<PrType>>): Int =
    exercises.sumOf { e -> e.sets.sumOf { prMarks[it.id]?.size ?: 0 } }

@Composable
private fun WorkoutCard(w: WorkoutFull, prMarks: Map<Long, List<PrType>>, lbs: Boolean, onClick: () -> Unit) {
    val prs = w.prCount(prMarks)
    val s = LocalStrings.current
    Card(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(w.workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(fmtDate(w.workout.startTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (prs > 0) PrBadge(prs)
            }
            Spacer(Modifier.height(10.dp))
            Row {
                StatItem(s.duration, fmtDuration(w.durationMs()), Modifier.weight(1f))
                StatItem(s.volume, fmtVolume(w.volumeKg(), lbs), Modifier.weight(1f))
                StatItem(s.sets, w.completedSets().toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            w.exercises.take(5).forEach { e ->
                Text(
                    "${e.sets.size} × ${s.exName(e.exercise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (w.exercises.size > 5) {
                Text(s.moreExercises(w.exercises.size - 5), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PrBadge(count: Int) {
    Surface(shape = RoundedCornerShape(50), color = Gold.copy(alpha = 0.15f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(16.dp), tint = Gold)
            Spacer(Modifier.width(4.dp))
            Text(LocalStrings.current.prCount(count), color = Gold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WorkoutDetailScreen(nav: NavController, workoutId: Long, isNew: Boolean) {
    val app = LocalContext.current.app
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val loaded by remember(workoutId) { repo.workout(workoutId).map { Loaded(it) } }.collectAsStateWithLifecycle(null)
    val all by remember { repo.finishedWorkouts() }.collectAsStateWithLifecycle(emptyList())
    val prMarks = remember(all) { analyzePrs(all.reversed()) }
    val start = rememberWorkoutStarter(nav)
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    val state = loaded
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val w = state.value
    if (w == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val prs = w.prCount(prMarks)
    val number = all.size - all.indexOfFirst { it.workout.id == workoutId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(w.workout.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
                actions = {
                    IconButton(onClick = { nav.navigate("workout/$workoutId") }) { Icon(Icons.Default.Edit, s.edit) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, s.options) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (settings.aiEnabled) DropdownMenuItem(
                                text = { Text(s.aiAnalyzeWorkout) }, leadingIcon = { Icon(Icons.Default.Psychology, null) },
                                onClick = { menu = false; nav.navigate("coach?workout=$workoutId") },
                            )
                            DropdownMenuItem(
                                text = { Text(s.repeatWorkout) }, leadingIcon = { Icon(Icons.Default.Replay, null) },
                                onClick = { menu = false; start { repo.repeatWorkout(workoutId) } },
                            )
                            DropdownMenuItem(
                                text = { Text(s.saveAsRoutine) }, leadingIcon = { Icon(Icons.Default.BookmarkAdd, null) },
                                onClick = {
                                    menu = false
                                    scope.launch {
                                        repo.saveWorkoutAsRoutine(workoutId)
                                        snackbar.showSnackbar(s.routineCreated)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.deleteWorkout, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (isNew) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 40.sp)
                            Text(s.workoutDone, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                s.workoutNumber(number) + if (prs > 0) " " + s.prsBeaten(prs) else "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(fmtDateTime(w.workout.startTime), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        StatItem(s.duration, fmtDuration(w.durationMs()), Modifier.weight(1f))
                        StatItem(s.volume, fmtVolume(w.volumeKg(), lbs), Modifier.weight(1f))
                        StatItem(s.sets, w.completedSets().toString(), Modifier.weight(0.7f))
                        StatItem(s.prsShort, prs.toString(), Modifier.weight(0.5f), valueColor = if (prs > 0) Gold else MaterialTheme.colorScheme.onSurface)
                    }
                    if (w.workout.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(w.workout.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(w.exercises, key = { it.we.id }) { e ->
                Card(onClick = { nav.navigate("exercise/${e.exercise.id}") }) {
                    Column {
                        Text(s.exName(e.exercise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        if (e.we.notes.isNotBlank()) {
                            Text(e.we.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.padding(bottom = 2.dp)) {
                            Text(s.colSet, Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (e.exercise.type == ExerciseType.DURATION) s.colTime else s.colWeightReps,
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        var n = 0
                        e.sets.forEach { set ->
                            val label = when (set.type) {
                                SetType.WARMUP -> s.warmupShort
                                SetType.DROP -> "D"
                                SetType.FAILURE -> "F"
                                SetType.NORMAL -> (++n).toString()
                            }
                            if (set.type == SetType.DROP || set.type == SetType.FAILURE) n++
                            val types = prMarks[set.id].orEmpty()
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.width(52.dp), fontWeight = FontWeight.Bold)
                                Text(setSummary(e.exercise.type, set.weight, set.reps, set.durationSec, lbs), Modifier.weight(1f))
                                if (types.isNotEmpty()) {
                                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(16.dp), tint = Gold)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        types.joinToString(" · ") { s.prType(it) },
                                        style = MaterialTheme.typography.labelSmall, color = Gold,
                                        maxLines = 2, modifier = Modifier.width(120.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = s.deleteWorkoutTitle,
            text = s.deleteWorkoutText,
            confirmText = s.delete, destructive = true,
            onConfirm = { scope.launch { repo.deleteWorkout(workoutId) } },
            onDismiss = { confirmDelete = false },
        )
    }
}
