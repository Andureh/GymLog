@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.gymlog.app.domain.RoutineSession
import com.gymlog.app.domain.RoutineTemplate
import com.gymlog.app.domain.groupByRoutine
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ChartPoint
import com.gymlog.app.ui.components.EmptyState
import com.gymlog.app.ui.components.LineChart
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.ui.components.StatItem
import com.gymlog.app.util.fmtBig
import com.gymlog.app.util.fmtDate
import com.gymlog.app.util.fmtDateShort
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.fmtVolume
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Evolução entre a primeira e a última sessão do período, em %. */
private fun changeText(values: List<Double>): String {
    if (values.size < 2 || values.first() <= 0) return "—"
    val pct = ((values.last() - values.first()) / values.first() * 100).roundToInt()
    return (if (pct > 0) "+" else "") + "$pct%"
}

/**
 * Progresso de cada rotina ao longo do tempo (volume total e séries por sessão).
 * [initialKey]: "r:<id>" para abrir numa rotina guardada; vazio = a rotina feita mais recentemente.
 */
@Composable
fun RoutineProgressScreen(nav: NavController, initialKey: String) {
    val app = LocalContext.current.app
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val workouts by remember { app.repo.finishedWorkouts() }.collectAsStateWithLifecycle(null)
    val routines by remember { app.repo.routines() }.collectAsStateWithLifecycle(emptyList())
    val groups = remember(workouts, routines) {
        groupByRoutine(
            workouts.orEmpty(),
            routines.map { r -> RoutineTemplate(r.routine.id, r.routine.name, r.exercises.map { it.exercise.id }.toSet()) },
        )
    }
    var selectedKey by rememberSaveable { mutableStateOf(initialKey) }
    var range by rememberSaveable { mutableStateOf(Range.ALL) }

    val group = groups.find { it.key == selectedKey } ?: groups.firstOrNull()
    val sessions: List<RoutineSession> = remember(group, range) {
        val cutoff = range.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: Long.MIN_VALUE
        group?.sessions.orEmpty().filter { it.date >= cutoff }
    }
    val unit = unitLabel(lbs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.routineProgress) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
            )
        },
    ) { pad ->
        if (workouts != null && groups.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(Icons.Default.ShowChart, s.routineProgress, s.noRoutineData)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.key }) { g ->
                        FilterChip(
                            selected = g.key == group?.key,
                            onClick = { selectedKey = g.key },
                            label = { Text("${g.name} (${g.sessions.size})") },
                            leadingIcon = if (g.isAuto) {
                                { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }

            // Grupo detetado automaticamente: explicar e permitir guardá-lo como rotina
            if (group != null && group.isAuto) {
                item {
                    Card {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(s.autoDetected, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = {
                                val last = group.sessions.last()
                                scope.launch {
                                    val rid = app.repo.saveWorkoutAsRoutine(last.workoutId, group.name, group.sessions.map { it.workoutId })
                                    if (rid != null) selectedKey = "r:$rid"
                                }
                            }) {
                                Icon(Icons.Default.BookmarkAdd, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.saveAsRoutine)
                            }
                        }
                    }
                }
            }

            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { RangeSelector(range) { range = it } }
            }

            // ---------- Volume total ----------
            item { SectionTitle(s.totalVolume) }
            item {
                val vols = sessions.map { toDisplayWeight(it.volumeKg, lbs) }
                Card {
                    Column {
                        Row {
                            StatItem(s.average, if (vols.isEmpty()) "—" else "${fmtBig(vols.average())} $unit", Modifier.weight(1f))
                            StatItem(s.best, if (vols.isEmpty()) "—" else "${fmtBig(vols.max())} $unit", Modifier.weight(1f))
                            StatItem(s.change, changeText(vols), Modifier.weight(0.7f))
                        }
                        Spacer(Modifier.height(12.dp))
                        LineChart(
                            points = sessions.map { ChartPoint(it.date, toDisplayWeight(it.volumeKg, lbs)) },
                            yFormat = { "${fmtBig(it)} $unit" },
                            xFormat = ::fmtDateShort,
                        )
                    }
                }
            }

            // ---------- Total de séries ----------
            item { SectionTitle(s.totalSets) }
            item {
                val sets = sessions.map { it.sets.toDouble() }
                Card {
                    Column {
                        Row {
                            StatItem(s.average, if (sets.isEmpty()) "—" else fmtNum(sets.average(), 1), Modifier.weight(1f))
                            StatItem(s.best, if (sets.isEmpty()) "—" else sets.max().toInt().toString(), Modifier.weight(1f))
                            StatItem(s.change, changeText(sets), Modifier.weight(0.7f))
                        }
                        Spacer(Modifier.height(12.dp))
                        LineChart(
                            points = sessions.map { ChartPoint(it.date, it.sets.toDouble()) },
                            yFormat = { s.setsN(it.roundToInt()) },
                            xFormat = ::fmtDateShort,
                        )
                    }
                }
            }

            // ---------- Sessões ----------
            item { SectionTitle(s.historySessions(sessions.size)) }
            items(sessions.reversed(), key = { it.workoutId }) { r ->
                Card(onClick = { nav.navigate("workoutDetail/${r.workoutId}") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fmtDate(r.date), fontWeight = FontWeight.SemiBold)
                            Text(
                                r.title, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            // Explica saltos no volume: exercícios a mais/menos que na sessão anterior
                            if (r.added > 0 || r.removed > 0) {
                                Text(
                                    s.exercisesDiff(r.added, r.removed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(fmtVolume(r.volumeKg, lbs))
                            Text(s.setsN(r.sets), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text(
                    s.routineGroupingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
