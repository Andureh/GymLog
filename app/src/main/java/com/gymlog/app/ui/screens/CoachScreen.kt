@file:OptIn(ExperimentalMaterial3Api::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gymlog.app.app
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.navigateTab
import com.gymlog.app.util.fmtDate
import kotlinx.coroutines.flow.first

/** Converte markdown simples (títulos, listas, **negrito**) em texto formatado. */
fun markdownToAnnotated(md: String): AnnotatedString = buildAnnotatedString {
    val lines = md.replace("\r", "").lines()
    lines.forEachIndexed { i, raw ->
        var line = raw.trimEnd()
        var heading = false
        when {
            line.startsWith("#") -> { line = line.trimStart('#').trim(); heading = true }
            Regex("^\\s*[-*•] ").containsMatchIn(line) -> line = "• " + line.trimStart().drop(2)
        }
        fun inline(text: String) {
            val parts = text.split("**")
            parts.forEachIndexed { k, p ->
                if (k % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p)
            }
        }
        if (heading) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { inline(line) } else inline(line)
        if (i < lines.lastIndex) append("\n")
    }
}

@Composable
fun CoachScreen(nav: NavController, focusWorkoutId: Long) {
    val app = LocalContext.current.app
    val s = LocalStrings.current
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val messages by app.coach.messages.collectAsStateWithLifecycle()
    val loading by app.coach.loading.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var autoSent by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val hasKey = settings.aiKey.isNotBlank()

    // Vindo de "Analisar com IA" num treino: envia logo a pergunta
    LaunchedEffect(focusWorkoutId, hasKey) {
        if (focusWorkoutId > 0 && hasKey && !autoSent) {
            autoSent = true
            val w = app.repo.workout(focusWorkoutId).first() ?: return@LaunchedEffect
            app.coach.send(s.aiAnalyzePrompt(w.workout.name, fmtDate(w.workout.startTime)), focusWorkoutId)
        }
    }
    LaunchedEffect(messages.size, loading) {
        val count = messages.size + (if (loading) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.aiCoach) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) } },
                actions = {
                    if (messages.isNotEmpty()) IconButton(onClick = { app.coach.clear() }) { Icon(Icons.Default.DeleteSweep, s.aiClear) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            if (!hasKey) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Psychology, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Text(s.aiNoKey, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(16.dp))
                    Button(onClick = { nav.navigateTab("profile") }) { Text(s.aiSetup) }
                }
                return@Column
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Bubble(s.aiIntro, fromUser = false) }
                if (messages.isEmpty()) {
                    items(s.aiPrompts) { p ->
                        SuggestionChip(onClick = { app.coach.send(p) }, label = { Text(p) })
                    }
                }
                items(messages) { m -> Bubble(m.text, m.fromUser, m.isError) }
                if (loading) item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text(s.aiThinking, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text(s.aiHint) },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        enabled = input.isNotBlank() && !loading,
                        onClick = { app.coach.send(input); input = "" },
                    ) { Icon(Icons.AutoMirrored.Filled.Send, s.aiSend) }
                }
            }
        }
    }
}

@Composable
private fun Bubble(text: String, fromUser: Boolean, isError: Boolean = false) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        val bg = when {
            isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            fromUser -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        SelectionContainer {
            Text(
                if (fromUser) AnnotatedString(text) else markdownToAnnotated(text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
