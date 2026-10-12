@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.RoutineFull
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.ui.components.StatItem
import com.gymlog.app.util.fmtVolume
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Devolve uma função que inicia um treino; se já existir um em curso,
 * pergunta se é para retomar ou descartar.
 */
@Composable
fun rememberWorkoutStarter(nav: NavController): (suspend () -> Long) -> Unit {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<(suspend () -> Long)?>(null) }
    var activeId by remember { mutableStateOf(0L) }
    val s = LocalStrings.current

    pending?.let { create ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(s.workoutInProgress) },
            text = { Text(s.activeWorkoutText) },
            confirmButton = {
                TextButton(onClick = { pending = null; nav.navigate("workout/$activeId") }) { Text(s.resume) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        app.repo.deleteWorkout(activeId)
                        app.restTimer.stop()
                        val id = create()
                        nav.navigate("workout/$id")
                    }
                }) { Text(s.discardAndStart, color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    return { create ->
        scope.launch {
            val a = app.repo.activeWorkoutOnce()
            if (a != null) {
                activeId = a.id
                pending = create
            } else {
                val id = create()
                nav.navigate("workout/$id")
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavController) {
    val app = LocalContext.current.app
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val routines by remember { repo.routines() }.collectAsStateWithLifecycle(emptyList())
    val workouts by remember { repo.finishedWorkouts() }.collectAsStateWithLifecycle(emptyList())
    val active by remember { repo.activeWorkout() }.collectAsStateWithLifecycle(null)
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val start = rememberWorkoutStarter(nav)
    var toDelete by remember { mutableStateOf<RoutineFull?>(null) }
    val s = LocalStrings.current

    val weekStart = remember {
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val thisWeek = workouts.filter { it.workout.startTime >= weekStart }

    Scaffold(topBar = { TopAppBar(title = { Text(s.tabWorkout, fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card {
                    Column {
                        Text(s.thisWeek, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            StatItem(s.workouts, thisWeek.size.toString(), Modifier.weight(1f))
                            StatItem(s.volume, fmtVolume(thisWeek.sumOf { it.volumeKg() }, settings.useLbs), Modifier.weight(1f))
                            StatItem(s.total, workouts.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                SectionTitle(s.quickStart)
                Button(
                    onClick = {
                        val a = active
                        if (a != null) nav.navigate("workout/${a.workout.id}") else start { repo.startEmptyWorkout() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (active != null) s.resumeWorkout else s.startEmptyWorkout)
                }
            }
            if (settings.aiEnabled) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Card(onClick = { nav.navigate("coach") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.aiCoach, fontWeight = FontWeight.SemiBold)
                                Text(s.aiCoachDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    }
                }
            }
            item {
                SectionTitle(s.routines) {
                    TextButton(onClick = { nav.navigate("routine/0") }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.newRoutine)
                    }
                }
            }
            if (routines.isEmpty()) {
                item {
                    Card(onClick = { nav.navigate("routine/0") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(s.firstRoutineTitle, fontWeight = FontWeight.SemiBold)
                                Text(
                                    s.firstRoutineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            items(routines, key = { it.routine.id }) { r ->
                RoutineCard(
                    r,
                    onStart = { start { repo.startFromRoutine(r.routine.id) } },
                    onEdit = { nav.navigate("routine/${r.routine.id}") },
                    onDelete = { toDelete = r },
                    onProgress = { nav.navigate("routineProgress?name=" + android.net.Uri.encode(r.routine.name)) },
                )
            }
        }
    }

    toDelete?.let { r ->
        ConfirmDialog(
            title = s.deleteRoutineTitle,
            text = s.deleteRoutineText(r.routine.name),
            confirmText = s.delete,
            destructive = true,
            onConfirm = { scope.launch { repo.deleteRoutine(r.routine.id) } },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun RoutineCard(r: RoutineFull, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onProgress: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val s = LocalStrings.current
    Card(onClick = onEdit) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, s.options) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(s.viewProgress) }, leadingIcon = { Icon(Icons.Default.ShowChart, null) },
                            onClick = { menu = false; onProgress() },
                        )
                        DropdownMenuItem(
                            text = { Text(s.edit) }, leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text(s.delete) }, leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Text(
                if (r.exercises.isEmpty()) s.noExercises
                else r.exercises.joinToString(", ") { s.exName(it.exercise) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text(s.startRoutine)
                }
                OutlinedButton(onClick = onEdit) { Text(s.edit) }
            }
        }
    }
}
