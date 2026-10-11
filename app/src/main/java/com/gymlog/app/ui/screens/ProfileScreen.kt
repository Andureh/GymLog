@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.Backup
import com.gymlog.app.data.HevyImport
import androidx.compose.material.icons.filled.FileOpen
import com.gymlog.app.ai.Coach
import com.gymlog.app.ai.GeminiException
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.domain.durationMs
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.i18n.I18n
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Bar
import com.gymlog.app.ui.components.BarChart
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.HBar
import com.gymlog.app.ui.components.RestPickerDialog
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.ui.components.StatItem
import com.gymlog.app.ui.components.muscleColor
import com.gymlog.app.ui.components.restLabel
import com.gymlog.app.ui.theme.Gold
import com.gymlog.app.util.fmtSkeleton
import com.gymlog.app.util.fmtBig
import com.gymlog.app.util.fmtDate
import com.gymlog.app.util.fmtVolume
import com.gymlog.app.util.fmtWeight
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.time.temporal.TemporalAdjusters
import java.util.Date

private fun weekOf(ms: Long): LocalDate =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private class Stats(workouts: List<WorkoutFull>) {
    val count = workouts.size
    val totalMs = workouts.sumOf { it.durationMs() }
    val totalVolume = workouts.sumOf { it.volumeKg() }
    val weeks: List<Pair<LocalDate, List<WorkoutFull>>>
    val streak: Int
    val muscles: List<Pair<String, Int>>

    init {
        val thisWeek = weekOf(System.currentTimeMillis())
        val byWeek = workouts.groupBy { weekOf(it.workout.startTime) }
        weeks = (11 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }.map { it to byWeek[it].orEmpty() }
        // Semanas consecutivas com pelo menos um treino (a semana atual pode ainda estar em curso)
        var s = 0
        var wk = if (byWeek.containsKey(thisWeek)) thisWeek else thisWeek.minusWeeks(1)
        while (byWeek.containsKey(wk)) { s++; wk = wk.minusWeeks(1) }
        streak = s
        val cutoff = System.currentTimeMillis() - 30L * 86_400_000L
        muscles = workouts.filter { it.workout.startTime >= cutoff }
            .flatMap { w -> w.exercises.map { it.exercise.muscle to it.sets.count { s -> s.completed } } }
            .groupBy({ it.first }, { it.second })
            .map { (m, l) -> m to l.sum() }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }
}

@Composable
fun ProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.app
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val workouts by remember { app.repo.finishedWorkouts() }.collectAsStateWithLifecycle(emptyList())
    val weights by remember { app.repo.bodyWeights() }.collectAsStateWithLifecycle(emptyList())
    val stats = remember(workouts) { Stats(workouts) }
    var restDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var langDialog by remember { mutableStateOf(false) }
    var keyDialog by remember { mutableStateOf(false) }
    var modelDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val s = LocalStrings.current
    fun weekLabel(wk: LocalDate) = fmtSkeleton("dM", wk.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val json = Backup.export(app.db)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(ctx, if (ok) s.exported else s.exportError, Toast.LENGTH_SHORT).show()
        }
    }
    var hevyData by remember { mutableStateOf<HevyImport.Parsed?>(null) }
    val hevyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val parsed = runCatching {
                withContext(Dispatchers.IO) {
                    val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    HevyImport.parse(text)
                }
            }.getOrNull()
            if (parsed == null || (parsed.workouts.isEmpty() && parsed.bodyWeights.isEmpty())) {
                Toast.makeText(ctx, s.hevyError, Toast.LENGTH_LONG).show()
            } else hevyData = parsed
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Scaffold(topBar = { TopAppBar(title = { Text(s.tabProfile, fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Card {
                    Column {
                        Row {
                            StatItem(s.workouts, stats.count.toString(), Modifier.weight(1f))
                            StatItem(s.totalTime, "${fmtBig(stats.totalMs / 3_600_000.0)} h", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row {
                            StatItem(s.totalVolume, fmtVolume(stats.totalVolume, lbs), Modifier.weight(1f))
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = Gold)
                                Spacer(Modifier.width(4.dp))
                                StatItem(s.streak, s.weeks(stats.streak))
                            }
                        }
                    }
                }
            }

            item { SectionTitle(s.workoutsPerWeek) }
            item {
                Card {
                    BarChart(
                        bars = stats.weeks.map { (wk, l) -> Bar(weekLabel(wk), l.size.toDouble()) },
                        yFormat = { s.workoutsN(it.toInt()) },
                        caption = { s.weekOf(it.label) },
                    )
                }
            }

            item { SectionTitle(s.volumePerWeek) }
            item {
                Card {
                    BarChart(
                        bars = stats.weeks.map { (wk, l) -> Bar(weekLabel(wk), toDisplayWeight(l.sumOf { it.volumeKg() }, lbs)) },
                        yFormat = { "${fmtBig(it)} ${unitLabel(lbs)}" },
                        caption = { s.weekOf(it.label) },
                    )
                }
            }

            if (stats.muscles.isNotEmpty()) {
                item { SectionTitle(s.setsPerMuscle) }
                item {
                    Card {
                        Column {
                            val max = stats.muscles.first().second.toDouble()
                            stats.muscles.forEach { (m, n) -> HBar(s.muscle(m), n.toDouble(), max, s.setsN(n), muscleColor(m)) }
                        }
                    }
                }
            }

            item { SectionTitle(s.body) }
            item {
                val last = weights.firstOrNull()
                SettingRow(
                    icon = { Icon(Icons.Default.MonitorWeight, null) },
                    title = s.bodyWeight,
                    subtitle = last?.let { "${fmtWeight(it.weightKg, lbs, 1)} · ${fmtDate(it.date)}" } ?: s.logYourWeight,
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    onClick = { nav.navigate("bodyweight") },
                )
            }

            item { SectionTitle(s.settings) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Default.Language, null) },
                    title = s.language,
                    subtitle = if (settings.language == I18n.SYSTEM) "${s.systemLanguage} · ${s.nativeName}" else s.nativeName,
                    onClick = { langDialog = true },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.Scale, null) },
                    title = s.weightUnit,
                    subtitle = if (lbs) s.pounds else s.kilograms,
                    trailing = {
                        Row {
                            FilterChip(!lbs, { app.settings.update { it.copy(useLbs = false) } }, label = { Text("kg") })
                            Spacer(Modifier.width(6.dp))
                            FilterChip(lbs, { app.settings.update { it.copy(useLbs = true) } }, label = { Text("lb") })
                        }
                    },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.Timer, null) },
                    title = s.defaultRest,
                    subtitle = restLabel(settings.defaultRest),
                    onClick = { restDialog = true },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.ScreenLockPortrait, null) },
                    title = s.keepScreenOn,
                    subtitle = s.keepScreenOnDesc,
                    trailing = {
                        Switch(settings.keepScreenOn, { v -> app.settings.update { it.copy(keepScreenOn = v) } })
                    },
                    onClick = { app.settings.update { it.copy(keepScreenOn = !it.keepScreenOn) } },
                )
            }

            item { SectionTitle(s.assistants) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Default.Lightbulb, null) },
                    title = s.suggestionsSetting,
                    subtitle = s.suggestionsDesc,
                    trailing = { Switch(settings.suggestions, { v -> app.settings.update { it.copy(suggestions = v) } }) },
                    onClick = { app.settings.update { it.copy(suggestions = !it.suggestions) } },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.Image, null) },
                    title = s.showImages,
                    subtitle = s.showImagesDesc,
                    trailing = { Switch(settings.showImages, { v -> app.settings.update { it.copy(showImages = v) } }) },
                    onClick = { app.settings.update { it.copy(showImages = !it.showImages) } },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.Psychology, null) },
                    title = s.aiEnable,
                    subtitle = s.aiCoachDesc,
                    trailing = { Switch(settings.aiEnabled, { v -> app.settings.update { it.copy(aiEnabled = v) } }) },
                    onClick = { app.settings.update { it.copy(aiEnabled = !it.aiEnabled) } },
                )
                if (settings.aiEnabled) {
                    SettingRow(
                        icon = { Icon(Icons.Default.Key, null) },
                        title = s.aiKey,
                        subtitle = if (settings.aiKey.isBlank()) s.aiKeyNotSet else s.aiKeySet + " · ••••" + settings.aiKey.takeLast(4),
                        onClick = { keyDialog = true },
                    )
                    SettingRow(
                        icon = { Icon(Icons.Default.Memory, null) },
                        title = s.aiModel,
                        subtitle = settings.aiModel.ifBlank {
                            s.aiModelAuto + (settings.aiAutoModel.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "")
                        },
                        onClick = { modelDialog = true },
                    )
                    SettingRow(
                        icon = { Icon(Icons.Default.NetworkCheck, null) },
                        title = s.aiTest,
                        subtitle = if (testing) s.aiTesting else testResult ?: "",
                        onClick = {
                            if (!testing) {
                                testing = true
                                scope.launch {
                                    testResult = try {
                                        s.aiTestOk(app.coach.testConnection())
                                    } catch (e: GeminiException) {
                                        Coach.errorText(s, e)
                                    }
                                    testing = false
                                }
                            }
                        },
                    )
                    Text(
                        s.aiPrivacy,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            item { SectionTitle(s.data) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Default.Upload, null) },
                    title = s.exportBackup,
                    subtitle = s.exportBackupDesc,
                    onClick = {
                        val name = "gymlog-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"
                        exportLauncher.launch(name)
                    },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.FileOpen, null) },
                    title = s.hevyImport,
                    subtitle = s.hevyImportDesc,
                    onClick = { hevyLauncher.launch("*/*") },
                )
                SettingRow(
                    icon = { Icon(Icons.Default.Download, null) },
                    title = s.importBackup,
                    subtitle = s.importBackupDesc,
                    onClick = { importLauncher.launch("*/*") },
                )
                Text(
                    s.dataNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "GymLog 1.4",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }

    if (restDialog) {
        RestPickerDialog(
            settings.defaultRest,
            onPick = { s -> app.settings.update { it.copy(defaultRest = s) } },
            onDismiss = { restDialog = false },
            title = s.defaultRest,
        )
    }
    pendingImport?.let { uri ->
        ConfirmDialog(
            title = s.importTitle,
            text = s.importText,
            confirmText = s.importAction, destructive = true,
            onConfirm = {
                scope.launch {
                    val res = runCatching {
                        val json = withContext(Dispatchers.IO) {
                            ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        }
                        app.restTimer.stop()
                        Backup.import(app.db, json)
                    }
                    Toast.makeText(
                        ctx,
                        if (res.isSuccess) s.imported else s.importError,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
    hevyData?.let { data ->
        val titles = data.exerciseTitles
        val matched = titles.count { HevyImport.matchKey(it) != null }
        ConfirmDialog(
            title = s.hevyConfirmTitle,
            text = buildString {
                if (data.workouts.isNotEmpty()) append(s.hevySummary(data.workouts.size, data.setCount, matched, titles.size - matched))
                if (data.bodyWeights.isNotEmpty()) { if (isNotEmpty()) append("\n"); append(s.hevyWeights(data.bodyWeights.size)) }
            },
            confirmText = s.importAction,
            onConfirm = {
                scope.launch {
                    val res = runCatching { HevyImport.import(app.db, data, settings.defaultRest) }
                    Toast.makeText(
                        ctx,
                        res.getOrNull()?.let { s.hevyDone(it.imported, it.skipped) } ?: s.hevyError,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onDismiss = { hevyData = null },
        )
    }
    if (keyDialog) {
        var key by remember { mutableStateOf(settings.aiKey) }
        AlertDialog(
            onDismissRequest = { keyDialog = false },
            title = { Text(s.aiKey) },
            text = {
                Column {
                    Text(s.aiKeyHelp, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }
                    }) {
                        Icon(Icons.Default.OpenInNew, null)
                        Spacer(Modifier.width(6.dp))
                        Text(s.aiGetKey)
                    }
                    OutlinedTextField(
                        value = key, onValueChange = { key = it.trim() },
                        label = { Text(s.aiKey) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    keyDialog = false
                    testResult = null
                    app.settings.update { it.copy(aiKey = key, aiAutoModel = "") }
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { keyDialog = false }) { Text(s.cancel) } },
        )
    }
    if (modelDialog) {
        var model by remember { mutableStateOf(settings.aiModel) }
        AlertDialog(
            onDismissRequest = { modelDialog = false },
            title = { Text(s.aiModel) },
            text = {
                Column {
                    Text(s.aiModelHelp, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = model, onValueChange = { model = it.trim() },
                        placeholder = { Text(settings.aiAutoModel.ifBlank { "gemini-…-flash" }) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    modelDialog = false
                    app.settings.update { it.copy(aiModel = model) }
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { modelDialog = false }) { Text(s.cancel) } },
        )
    }
    if (langDialog) {
        val options = listOf(I18n.SYSTEM to s.systemLanguage) + I18n.all.map { it.code to it.nativeName }
        AlertDialog(
            onDismissRequest = { langDialog = false },
            title = { Text(s.language) },
            text = {
                Column {
                    options.forEach { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                langDialog = false
                                if (code != settings.language) {
                                    app.setLanguage(code)
                                    (ctx as? Activity)?.recreate()
                                }
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = code == settings.language, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { langDialog = false }) { Text(s.close) } },
        )
    }
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
