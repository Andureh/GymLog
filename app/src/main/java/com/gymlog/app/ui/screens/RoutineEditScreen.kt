@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.RoutineExercise
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.CompactNumberField
import com.gymlog.app.ui.components.ExerciseAvatar
import com.gymlog.app.ui.components.RestPickerDialog
import com.gymlog.app.ui.components.restLabel
import com.gymlog.app.util.fmtClock
import com.gymlog.app.util.fromDisplayWeight
import com.gymlog.app.util.parseDuration
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.launch

private data class Draft(
    val key: Long,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val rest: Int,
)

@Composable
fun RoutineEditScreen(nav: NavController, routineId: Long) {
    val app = LocalContext.current.app
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val allExercises by remember { repo.exercises() }.collectAsStateWithLifecycle(emptyList())
    val snackbar = remember { SnackbarHostState() }
    val s = LocalStrings.current

    var name by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<Draft>() }
    var nextKey by remember { mutableStateOf(1L) }
    var loaded by remember { mutableStateOf(routineId == 0L) }
    var showPicker by remember { mutableStateOf(false) }
    var restFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(routineId) {
        if (routineId != 0L) {
            repo.routineOnce(routineId)?.let { r ->
                name = r.routine.name
                items.clear()
                r.exercises.forEach {
                    items.add(Draft(nextKey++, it.exercise, it.re.sets, it.re.reps, it.re.weight, it.re.restSeconds))
                }
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == 0L) s.newRoutine else s.editRoutine) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
                actions = {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                scope.launch { snackbar.showSnackbar(s.routineNameRequired) }
                                return@Button
                            }
                            scope.launch {
                                repo.saveRoutine(
                                    routineId, name.trim(), "",
                                    items.map {
                                        RoutineExercise(
                                            routineId = 0, exerciseId = it.exercise.id, position = 0,
                                            sets = it.sets, reps = it.reps, weight = it.weight, restSeconds = it.rest,
                                        )
                                    },
                                )
                                nav.popBackStack()
                            }
                        },
                        enabled = loaded,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(s.save) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(s.routineName) }, placeholder = { Text(s.routineNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            itemsIndexed(items, key = { _, d -> d.key }) { i, d ->
                DraftCard(
                    d = d, lbs = lbs,
                    canUp = i > 0, canDown = i < items.lastIndex,
                    onChange = { nd -> val idx = items.indexOfFirst { it.key == d.key }; if (idx >= 0) items[idx] = nd },
                    onMove = { dir ->
                        val idx = items.indexOfFirst { it.key == d.key }
                        val j = idx + dir
                        if (idx >= 0 && j in items.indices) { val t = items[idx]; items[idx] = items[j]; items[j] = t }
                    },
                    onRemove = { items.removeAll { it.key == d.key } },
                    onRest = { restFor = items.indexOfFirst { it.key == d.key } },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(s.addExercise)
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPicked = { ids ->
                showPicker = false
                ids.mapNotNull { id -> allExercises.find { it.id == id } }.forEach { ex ->
                    val dur = ex.type == ExerciseType.DURATION
                    items.add(Draft(nextKey++, ex, 3, if (dur) 60 else 10, 0.0, settings.defaultRest))
                }
            },
        )
    }
    restFor?.let { idx ->
        val d = items.getOrNull(idx)
        if (d == null) restFor = null
        else RestPickerDialog(d.rest, onPick = { items[idx] = d.copy(rest = it) }, onDismiss = { restFor = null })
    }
}

@Composable
private fun DraftCard(
    d: Draft,
    lbs: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onChange: (Draft) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onRest: () -> Unit,
) {
    val type = d.exercise.type
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseAvatar(d.exercise, 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                s.exName(d.exercise), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (canUp) IconButton(onClick = { onMove(-1) }) { Icon(Icons.Default.ArrowUpward, s.moveUp) }
            if (canDown) IconButton(onClick = { onMove(1) }) { Icon(Icons.Default.ArrowDownward, s.moveDown) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, s.remove, tint = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label(s.colSets)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChange(d.copy(sets = (d.sets - 1).coerceAtLeast(1))) }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, s.less)
                    }
                    Text("${d.sets}", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { onChange(d.copy(sets = (d.sets + 1).coerceAtMost(20))) }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, s.more)
                    }
                }
            }
            key(d.key) {
                if (type == ExerciseType.DURATION) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(s.colTime)
                        CompactNumberField(
                            value = d.reps.toDouble(), onValue = { onChange(d.copy(reps = it.toInt())) },
                            decimal = false, placeholder = "0:00",
                            format = { if (it == 0.0) "" else fmtClock(it.toInt()) }, parse = { parseDuration(it) },
                            modifier = Modifier.width(80.dp),
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(s.colReps)
                        CompactNumberField(
                            value = d.reps.toDouble(), onValue = { onChange(d.copy(reps = it.toInt())) },
                            decimal = false, placeholder = "0", modifier = Modifier.width(56.dp),
                        )
                    }
                }
                if (type == ExerciseType.WEIGHT_REPS) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(unitLabel(lbs).uppercase() + " (${s.optionalShort})")
                        CompactNumberField(
                            value = toDisplayWeight(d.weight, lbs), onValue = { onChange(d.copy(weight = fromDisplayWeight(it, lbs))) },
                            placeholder = "—", modifier = Modifier.width(72.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRest).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Timer, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(s.restValue(restLabel(d.rest)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Label(t: String) {
    Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
