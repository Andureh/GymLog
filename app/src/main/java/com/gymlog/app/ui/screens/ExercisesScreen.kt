@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.i18n.Catalog
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.ExerciseAvatar
import com.gymlog.app.util.normalized
import kotlinx.coroutines.launch

@Composable
fun ExercisesScreen(nav: NavController) {
    ExerciseBrowser(title = LocalStrings.current.tabExercises, pickMode = false, onOpen = { nav.navigate("exercise/${it.id}") })
}

@Composable
fun ExercisePickerDialog(onDismiss: () -> Unit, onPicked: (List<Long>) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            ExerciseBrowser(title = LocalStrings.current.addExercises, pickMode = true, onPicked = onPicked, onClose = onDismiss)
        }
    }
}

@Composable
fun ExerciseBrowser(
    title: String,
    pickMode: Boolean,
    onOpen: (Exercise) -> Unit = {},
    onPicked: (List<Long>) -> Unit = {},
    onClose: (() -> Unit)? = null,
) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    val all by remember { app.repo.exercises() }.collectAsStateWithLifecycle(emptyList())
    val usage by remember { app.repo.usageCounts() }.collectAsStateWithLifecycle(emptyMap())
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<Long>() }
    var showAdd by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    val muscles = remember(all) {
        val present = all.map { it.muscle }.toSet()
        Catalog.muscles.keys.filter { it in present } + (present - Catalog.muscles.keys).sorted()
    }
    val filtered = remember(all, query, muscle, usage, s) {
        val q = query.normalized().trim()
        all.filter {
            (muscle == null || it.muscle == muscle) &&
                (q.isEmpty() || s.exName(it).normalized().contains(q) || s.equipment(it.equipment).normalized().contains(q) ||
                    s.muscle(it.muscle).normalized().contains(q))
        }.let { list ->
            // Ordenar pelo nome traduzido; no modo de escolha, os mais usados aparecem primeiro
            val byName = list.sortedBy { s.exName(it).normalized() }
            if (pickMode && q.isEmpty()) byName.sortedByDescending { usage[it.id] ?: 0 } else byName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { if (onClose != null) IconButton(onClick = onClose) { Icon(Icons.Default.Close, s.close) } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, s.createExercise) } },
            )
        },
        bottomBar = {
            if (pickMode && selected.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Button(
                        onClick = { onPicked(selected.toList()) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                    ) { Text(s.addNExercises(selected.size)) }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(s.searchExercise) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, s.clear) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text(s.all) }) }
                items(muscles) { m ->
                    FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(s.muscle(m)) })
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { e ->
                    val isSel = e.id in selected
                    val count = usage[e.id] ?: 0
                    ListItem(
                        headlineContent = { Text(s.exName(e)) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(s.muscle(e.muscle)); append(" · "); append(s.equipment(e.equipment))
                                    if (e.isCustom) append(" · ").append(s.custom)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = { ExerciseAvatar(e) },
                        trailingContent = {
                            if (pickMode) Checkbox(checked = isSel, onCheckedChange = null)
                            else if (count > 0) Text("${count}×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = if (isSel) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.clickable {
                            if (pickMode) {
                                if (isSel) selected.remove(e.id) else selected.add(e.id)
                            } else onOpen(e)
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(32.dp)) {
                            Text(s.noExerciseFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { showAdd = true }) { Text(s.createNamed(query.ifBlank { s.newExercise })) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ExerciseEditDialog(
            existing = null,
            initialName = query,
            onDismiss = { showAdd = false },
            onSave = { e ->
                showAdd = false
                scope.launch {
                    val id = app.repo.addExercise(e)
                    if (pickMode) selected.add(id)
                }
            },
        )
    }
}

@Composable
fun ExerciseEditDialog(
    existing: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    initialName: String = "",
) {
    val s = LocalStrings.current
    val originalName = existing?.let { s.exName(it) }
    var name by remember { mutableStateOf(originalName ?: initialName) }
    var muscle by remember { mutableStateOf(existing?.muscle ?: "chest") }
    var equip by remember { mutableStateOf(existing?.equipment ?: "barbell") }
    var type by remember { mutableStateOf(existing?.type ?: ExerciseType.WEIGHT_REPS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) s.newExercise else s.editExercise) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text(s.name) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(s.muscleGroup, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalog.muscles.keys.forEach { m -> FilterChip(muscle == m, { muscle = m }, label = { Text(s.muscle(m)) }) }
                }
                Spacer(Modifier.height(8.dp))
                Text(s.equipmentLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalog.equipment.keys.forEach { q -> FilterChip(equip == q, { equip = q }, label = { Text(s.equipment(q)) }) }
                }
                Spacer(Modifier.height(8.dp))
                Text(s.trackingType, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseType.entries.forEach { t -> FilterChip(type == t, { type = t }, label = { Text(s.exerciseType(t)) }) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = existing ?: Exercise(name = "", muscle = "", equipment = "", isCustom = true)
                    val renamed = name.trim() != originalName
                    // Se mudares o nome de um exercício incluído, deixa de ser traduzido
                    onSave(
                        base.copy(
                            name = if (renamed || base.nameKey == null) name.trim() else base.name,
                            nameKey = if (renamed) null else base.nameKey,
                            muscle = muscle, equipment = equip, type = type,
                        )
                    )
                },
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
