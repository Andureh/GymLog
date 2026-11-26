@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.gymlog.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.InputChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.data.BarSpec
import com.gymlog.app.data.Gym
import com.gymlog.app.data.GymStore
import com.gymlog.app.data.PlateSpec
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ConfirmDialog
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.parseDecimal
import com.gymlog.app.util.unitLabel
import java.util.UUID

@Composable
fun GymsScreen(nav: NavController) {
    val app = LocalContext.current.app
    val s = LocalStrings.current
    val state by app.gyms.flow.collectAsStateWithLifecycle()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Gym?>(null) }
    var calc by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.gymsAndPlates) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
                actions = { IconButton(onClick = { calc = true }) { Icon(Icons.Default.Calculate, s.plateCalculator) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = GymStore.defaultGym(settings.useLbs).copy(name = "") },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text(s.addGym) },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Card(onClick = { app.gyms.setDumbbellSum(!state.dumbbellSum) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.dumbbellSum, fontWeight = FontWeight.SemiBold)
                            Text(s.dumbbellSumDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(state.dumbbellSum, { app.gyms.setDumbbellSum(it) })
                    }
                }
            }
            items(state.gyms, key = { it.id }) { g ->
                val u = unitLabel(g.lbs)
                Card(onClick = { editing = g }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (g.id == state.activeId) {
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(s.activeGym, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                TextButton(onClick = { app.gyms.setActive(g.id) }) { Text(s.useThisGym) }
                            }
                        }
                        Text(
                            "${s.plates}: " + g.plates.sortedByDescending { it.weight }.joinToString(", ") { "${fmtNum(it.weight)}×${it.pairs}" } + " $u",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${s.bars}: " + g.bars.joinToString(", ") { "${it.name} ${fmtNum(it.weight)} $u" },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${s.dumbbells}: " + g.dumbbells.sorted().joinToString(", ") { fmtNum(it) } + " $u",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    editing?.let { g ->
        GymEditor(
            initial = g,
            canDelete = state.gyms.size > 1 && state.gyms.any { it.id == g.id },
            onSave = { app.gyms.upsertGym(it); editing = null },
            onDelete = { app.gyms.deleteGym(g.id); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (calc) {
        PlateCalculatorDialog(initialKg = null, exerciseId = null, equipment = "barbell", onDismiss = { calc = false }, onManageGyms = {})
    }
}

/** Editor de um ginásio: nome, unidade, discos (peso + pares) e barras (nome + peso). */
@Composable
private fun GymEditor(initial: Gym, canDelete: Boolean, onSave: (Gym) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(initial.name) }
    var lbs by remember { mutableStateOf(initial.lbs) }
    val plates = remember { mutableStateListOf<PlateSpec>().apply { addAll(initial.plates.sortedByDescending { it.weight }) } }
    val bars = remember { mutableStateListOf<Pair<BarSpec, String>>().apply { addAll(initial.bars.map { it to fmtNum(it.weight) }) } }
    var newPlate by remember { mutableStateOf("") }
    val dumbbells = remember { mutableStateListOf<Double>().apply { addAll(initial.dumbbells.sorted()) } }
    var newDumbbell by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val u = unitLabel(lbs)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(initial.name.ifBlank { s.addGym }) },
                        navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, s.close) } },
                        actions = {
                            if (canDelete) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, s.deleteGym) }
                            TextButton(onClick = {
                                onSave(
                                    initial.copy(
                                        name = name.trim().ifBlank { s.myGym }, lbs = lbs,
                                        plates = plates.filter { it.weight > 0 }.sortedByDescending { it.weight },
                                        bars = bars.mapNotNull { (b, w) -> parseDecimal(w)?.let { b.copy(name = b.name.trim().ifBlank { "—" }, weight = it) } },
                                        dumbbells = dumbbells.sorted(),
                                    )
                                )
                            }) { Text(s.save) }
                        },
                    )
                },
            ) { pad ->
                LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(name, { name = it }, label = { Text(s.gymName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(!lbs, { lbs = false }, label = { Text("kg") })
                            FilterChip(lbs, { lbs = true }, label = { Text("lb") })
                        }
                    }
                    item { SectionTitle(s.plates, Modifier.padding(start = 0.dp)) }
                    items(plates.size) { i ->
                        val p = plates[i]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${fmtNum(p.weight)} $u", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (p.pairs > 1) plates[i] = p.copy(pairs = p.pairs - 1) }) { Icon(Icons.Default.Remove, s.less) }
                            Text(s.pairsN(p.pairs), modifier = Modifier.width(72.dp))
                            IconButton(onClick = { plates[i] = p.copy(pairs = p.pairs + 1) }) { Icon(Icons.Default.Add, s.more) }
                            IconButton(onClick = { plates.removeAt(i) }) { Icon(Icons.Default.Close, s.remove, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                newPlate, { newPlate = it }, label = { Text("${s.plateWeight} ($u)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                parseDecimal(newPlate)?.takeIf { it > 0 }?.let { w ->
                                    if (plates.none { kotlin.math.abs(it.weight - w) < 1e-6 }) {
                                        plates.add(PlateSpec(w, 2))
                                        plates.sortByDescending { it.weight }
                                    }
                                    newPlate = ""
                                }
                            }) { Text(s.addPlate) }
                        }
                    }
                    item { SectionTitle(s.bars, Modifier.padding(start = 0.dp)) }
                    items(bars.size) { i ->
                        val (b, w) = bars[i]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(b.name, { bars[i] = b.copy(name = it) to w }, label = { Text(s.barName) }, singleLine = true, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                w, { bars[i] = b to it }, label = { Text("${s.barWeight} ($u)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(110.dp),
                            )
                            IconButton(onClick = { bars.removeAt(i) }) { Icon(Icons.Default.Close, s.remove, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { bars.add(BarSpec(UUID.randomUUID().toString(), "", 0.0) to "") }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.addBar)
                        }
                    }
                    item { SectionTitle(s.dumbbells, Modifier.padding(start = 0.dp)) }
                    item {
                        Text(s.tapToRemove, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dumbbells.sorted().forEach { w ->
                                InputChip(
                                    selected = false, onClick = { dumbbells.remove(w) },
                                    label = { Text(fmtNum(w)) },
                                    trailingIcon = { Icon(Icons.Default.Close, s.remove, Modifier.size(16.dp)) },
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                newDumbbell, { newDumbbell = it }, label = { Text("${s.dumbbellWeight} ($u)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                parseDecimal(newDumbbell)?.takeIf { it > 0 }?.let { w ->
                                    if (dumbbells.none { kotlin.math.abs(it - w) < 1e-6 }) dumbbells.add(w)
                                    newDumbbell = ""
                                }
                            }) { Text(s.addDumbbell) }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = s.deleteGym, text = initial.name, confirmText = s.delete, destructive = true,
            onConfirm = onDelete, onDismiss = { confirmDelete = false },
        )
    }
}
