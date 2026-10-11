@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.domain.Session
import com.gymlog.app.data.ExerciseMedia
import com.gymlog.app.ui.components.AnimatedExerciseImages
import com.gymlog.app.i18n.Catalog
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.OutlinedButton
import com.gymlog.app.domain.buildSessions
import com.gymlog.app.domain.e1rm
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.i18n.Strings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ChartPoint
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.EmptyState
import com.gymlog.app.ui.components.LineChart
import com.gymlog.app.ui.components.Loaded
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.ui.theme.Gold
import com.gymlog.app.util.fmtClock
import com.gymlog.app.util.fmtDate
import com.gymlog.app.util.fmtDateShort
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.fmtVolume
import com.gymlog.app.util.fmtWeight
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class Metric(val isWeight: Boolean = false, val isDuration: Boolean = false) {
    E1RM(isWeight = true),
    MAX_WEIGHT(isWeight = true),
    BEST_SET(isWeight = true),
    VOLUME(isWeight = true),
    MAX_REPS,
    TOTAL_REPS,
    MAX_DUR(isDuration = true),
    TOTAL_DUR(isDuration = true),
}

fun Strings.metric(m: Metric) = when (m) {
    Metric.E1RM -> est1rm
    Metric.MAX_WEIGHT -> maxWeight
    Metric.BEST_SET -> bestSet
    Metric.VOLUME -> sessionVolume
    Metric.MAX_REPS -> maxReps
    Metric.TOTAL_REPS -> totalReps
    Metric.MAX_DUR -> longestSet
    Metric.TOTAL_DUR -> totalDuration
}

fun metricsFor(type: ExerciseType) = when (type) {
    ExerciseType.WEIGHT_REPS -> listOf(Metric.E1RM, Metric.MAX_WEIGHT, Metric.BEST_SET, Metric.VOLUME, Metric.TOTAL_REPS)
    ExerciseType.REPS_ONLY -> listOf(Metric.MAX_REPS, Metric.TOTAL_REPS)
    ExerciseType.DURATION -> listOf(Metric.MAX_DUR, Metric.TOTAL_DUR)
}

fun Session.metric(m: Metric): Double = when (m) {
    Metric.E1RM -> bestE1rm
    Metric.MAX_WEIGHT -> maxWeight
    Metric.BEST_SET -> bestSetVolume
    Metric.VOLUME -> volume
    Metric.MAX_REPS -> maxReps.toDouble()
    Metric.TOTAL_REPS -> totalReps.toDouble()
    Metric.MAX_DUR -> maxDuration.toDouble()
    Metric.TOTAL_DUR -> totalDuration.toDouble()
}

enum class Range(val days: Int?) {
    M1(30), M3(91), M6(182), Y1(365), ALL(null)
}

fun Strings.range(r: Range) = when (r) {
    Range.M1 -> "1$monthAbbr"
    Range.M3 -> "3$monthAbbr"
    Range.M6 -> "6$monthAbbr"
    Range.Y1 -> "1$yearAbbr"
    Range.ALL -> rangeAll
}

@Composable
fun ExerciseDetailScreen(nav: NavController, exerciseId: Long) {
    val app = LocalContext.current.app
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val s = LocalStrings.current
    val loaded by remember(exerciseId) { repo.exercise(exerciseId).map { Loaded(it) } }.collectAsStateWithLifecycle(null)
    val rows by remember(exerciseId) { repo.exerciseHistory(exerciseId) }.collectAsStateWithLifecycle(emptyList())

    val state = loaded
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val ex = state.value
    if (ex == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    val sessions = remember(rows) { buildSessions(rows) }
    val metrics = metricsFor(ex.type)
    var metric by rememberSaveable(ex.type) { mutableStateOf(metrics.first()) }
    var range by rememberSaveable { mutableStateOf(Range.ALL) }
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun fmtMetric(v: Double): String = when {
        metric.isWeight -> "${fmtNum(v, 1)} ${unitLabel(lbs)}"
        metric.isDuration -> fmtClock(v.toInt())
        else -> fmtNum(v, 0)
    }

    val points = remember(sessions, metric, range, lbs) {
        val cutoff = range.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: Long.MIN_VALUE
        sessions.filter { it.date >= cutoff }.map {
            val v = it.metric(metric)
            ChartPoint(it.date, if (metric.isWeight) toDisplayWeight(v, lbs) else v)
        }.filter { it.y > 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.exName(ex), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, s.options) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(s.edit) }, leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { menu = false; edit = true },
                            )
                            DropdownMenuItem(
                                text = { Text(s.delete, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text(
                    "${s.muscle(ex.muscle)} · ${s.equipment(ex.equipment)} · ${s.exerciseType(ex.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                val ctx = LocalContext.current
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val images = if (settings.showImages) ExerciseMedia.images(ex.nameKey) else emptyList()
                    if (images.isNotEmpty()) {
                        AnimatedExerciseImages(images)
                        Text(
                            s.imagesCredit, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            // Pesquisa no YouTube: nome na língua atual + nome em inglês dá melhores resultados
                            val en = ex.nameKey?.let { Catalog.exerciseName(it, 1) }
                            val q = listOfNotNull(s.exName(ex), en?.takeIf { it != s.exName(ex) }, s.videoQuery).joinToString(" ")
                            val url = "https://www.youtube.com/results?search_query=" + Uri.encode(q)
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PlayCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(s.watchVideo)
                    }
                }
            }
            if (sessions.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.ShowChart, s.noHistoryTitle, s.noHistoryText,
                    )
                }
                return@LazyColumn
            }

            // ---------- Gráfico ----------
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(metrics) { m -> FilterChip(metric == m, { metric = m }, label = { Text(s.metric(m)) }) }
                }
                Card {
                    Column {
                        LineChart(points = points, yFormat = ::fmtMetric, xFormat = ::fmtDateShort)
                        Spacer(Modifier.height(8.dp))
                        RangeSelector(range) { range = it }
                    }
                }
            }

            // ---------- Recordes ----------
            item { SectionTitle(s.personalRecords) }
            item { RecordsCard(ex.type, sessions, lbs) }

            if (ex.type == ExerciseType.WEIGHT_REPS) {
                item { SectionTitle(s.bestWeightByReps) }
                item { RepRecords(sessions, lbs) }
            }

            // ---------- Histórico ----------
            item { SectionTitle(s.historySessions(sessions.size)) }
            items(sessions.reversed(), key = { it.workoutId }) { session ->
                Card(onClick = { nav.navigate("workoutDetail/${session.workoutId}") }) {
                    Column {
                        Row {
                            Text(session.workoutName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(fmtDate(session.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        var n = 0
                        session.sets.forEach { set ->
                            val label = if (set.type == SetType.WARMUP) s.warmupShort else (++n).toString()
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text(label, Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(setSummary(ex.type, set.weight, set.reps, set.durationSec, lbs), Modifier.weight(1f))
                                if (ex.type == ExerciseType.WEIGHT_REPS && set.reps > 0) {
                                    Text(
                                        "1RM ${fmtNum(toDisplayWeight(e1rm(set.weight, set.reps), lbs), 1)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (edit) {
        ExerciseEditDialog(
            existing = ex,
            onDismiss = { edit = false },
            onSave = { e -> edit = false; scope.launch { repo.updateExercise(e) } },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = s.deleteExerciseTitle,
            text = s.deleteExerciseText(s.exName(ex)),
            confirmText = s.delete, destructive = true,
            onConfirm = { scope.launch { repo.deleteExercise(ex) } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun RecordRow(label: String, value: String, date: Long?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.EmojiEvents, null, Modifier.size(20.dp), tint = Gold)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            date?.let { Text(fmtDate(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecordsCard(type: ExerciseType, sessions: List<Session>, lbs: Boolean) {
    val s = LocalStrings.current
    Card {
        Column {
            when (type) {
                ExerciseType.WEIGHT_REPS -> {
                    val bw = sessions.maxBy { it.maxWeight }
                    val be = sessions.maxBy { it.bestE1rm }
                    val bs = sessions.maxBy { it.bestSetVolume }
                    val bv = sessions.maxBy { it.volume }
                    RecordRow(s.maxWeight, fmtWeight(bw.maxWeight, lbs), bw.date)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecordRow(s.est1rm, fmtWeight(be.bestE1rm, lbs, 1), be.date)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecordRow(s.bestSet, fmtWeight(bs.bestSetVolume, lbs, 1), bs.date)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecordRow(s.bestSessionVolume, fmtVolume(bv.volume, lbs), bv.date)
                }
                ExerciseType.REPS_ONLY -> {
                    val br = sessions.maxBy { it.maxReps }
                    val bt = sessions.maxBy { it.totalReps }
                    RecordRow(s.mostRepsSet, "${br.maxReps}", br.date)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecordRow(s.mostRepsSession, "${bt.totalReps}", bt.date)
                }
                ExerciseType.DURATION -> {
                    val bd = sessions.maxBy { it.maxDuration }
                    val bt = sessions.maxBy { it.totalDuration }
                    RecordRow(s.longestSet, fmtClock(bd.maxDuration), bd.date)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecordRow(s.longestSession, fmtClock(bt.totalDuration), bt.date)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RecordRow(s.sessionsLogged, "${sessions.size}", null)
        }
    }
}

@Composable
private fun RepRecords(sessions: List<Session>, lbs: Boolean) {
    // Para cada nº de repetições (1–15), o maior peso alguma vez feito
    val best = remember(sessions) {
        val map = sortedMapOf<Int, Pair<Double, Long>>()
        sessions.forEach { s ->
            s.sets.filter { it.type != SetType.WARMUP && it.reps in 1..15 && it.weight > 0 }.forEach { set ->
                val cur = map[set.reps]
                if (cur == null || set.weight > cur.first) map[set.reps] = set.weight to s.date
            }
        }
        map
    }
    if (best.isEmpty()) return
    val s = LocalStrings.current
    Card {
        Column {
            Row(Modifier.padding(bottom = 4.dp)) {
                Text(s.colReps, Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.colWeight, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.colDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            best.forEach { (reps, p) ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("$reps", Modifier.width(56.dp), fontWeight = FontWeight.SemiBold)
                    Text(fmtWeight(p.first, lbs), Modifier.weight(1f))
                    Text(fmtDate(p.second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RangeSelector(range: Range, onChange: (Range) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)
    ) {
        Range.entries.forEach { r ->
            val sel = r == range
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(r) }.padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    LocalStrings.current.range(r), style = MaterialTheme.typography.labelLarge,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
