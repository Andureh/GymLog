@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.components.Card
import com.gymlog.app.ui.components.ChartPoint
import com.gymlog.app.ui.components.EmptyState
import com.gymlog.app.ui.components.LineChart
import com.gymlog.app.ui.components.SectionTitle
import com.gymlog.app.util.fmtDate
import com.gymlog.app.util.fmtDateShort
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.fmtWeight
import com.gymlog.app.util.fromDisplayWeight
import com.gymlog.app.util.parseDecimal
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.launch

@Composable
fun BodyWeightScreen(nav: NavController) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val lbs = settings.useLbs
    val list by remember { app.repo.bodyWeights() }.collectAsStateWithLifecycle(emptyList())
    var range by rememberSaveable { mutableStateOf(Range.ALL) }
    var showAdd by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    val points = remember(list, range, lbs) {
        val cutoff = range.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: Long.MIN_VALUE
        list.filter { it.date >= cutoff }.sortedBy { it.date }.map { ChartPoint(it.date, toDisplayWeight(it.weightKg, lbs)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.bodyWeight) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(s.logAction) })
        },
    ) { pad ->
        if (list.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(Icons.Default.MonitorWeight, s.noEntriesTitle, s.noEntriesText)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Card {
                    Column {
                        LineChart(points, yFormat = { "${fmtNum(it, 1)} ${unitLabel(lbs)}" }, xFormat = ::fmtDateShort)
                        Spacer(Modifier.height(8.dp))
                        RangeSelector(range) { range = it }
                    }
                }
            }
            if (list.size >= 2) {
                item {
                    val diff = list.first().weightKg - list.last().weightKg
                    Text(
                        s.changeSince(fmtDate(list.last().date), (if (diff >= 0) "+" else "−") + fmtWeight(kotlin.math.abs(diff), lbs, 1)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            item { SectionTitle(s.entries) }
            items(list, key = { it.id }) { b ->
                ListItem(
                    headlineContent = { Text(fmtWeight(b.weightKg, lbs, 1), fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(fmtDate(b.date)) },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { app.repo.deleteBodyWeight(b.id) } }) {
                            Icon(Icons.Default.Delete, s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }

    if (showAdd) {
        var text by remember { mutableStateOf(list.firstOrNull()?.let { fmtNum(toDisplayWeight(it.weightKg, lbs), 1) } ?: "") }
        val v = parseDecimal(text)
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(s.logWeight) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text(s.weightIn(unitLabel(lbs))) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(enabled = v != null && v > 0, onClick = {
                    showAdd = false
                    scope.launch { app.repo.addBodyWeight(System.currentTimeMillis(), fromDisplayWeight(v!!, lbs)) }
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(s.cancel) } },
        )
    }
}
