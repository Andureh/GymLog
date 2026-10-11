package com.gymlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlog.app.data.Exercise
import com.gymlog.app.i18n.I18n
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.util.fmtClock
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.parseDecimal
import kotlinx.coroutines.delay
import kotlin.math.abs

/** Hora atual que se atualiza a cada [periodMs]. */
@Composable
fun rememberNow(periodMs: Long = 1000): Long {
    val s = produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(periodMs)
        }
    }
    return s.value
}

/** Envolve um valor para distinguir "a carregar" (null) de "não existe" (Loaded(null)). */
data class Loaded<T>(val value: T)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

private val muscleColors = listOf(
    Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFFA855F7),
    Color(0xFF06B6D4), Color(0xFFEC4899), Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF14B8A6),
)

fun muscleColor(muscle: String): Color = muscleColors[abs(muscle.hashCode()) % muscleColors.size]

@Composable
fun ExerciseAvatar(e: Exercise, size: Dp = 40.dp) {
    val c = muscleColor(e.muscle)
    Box(
        Modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            LocalStrings.current.exName(e).take(1).uppercase(), color = c, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/**
 * Campo numérico compacto para séries. Mantém o texto local enquanto tem foco
 * (para não perder teclas) e sincroniza com o valor externo quando perde o foco.
 */
@Composable
fun CompactNumberField(
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    decimal: Boolean = true,
    format: (Double) -> String = { if (it == 0.0) "" else fmtNum(it) },
    parse: (String) -> Double? = { parseDecimal(it) },
    highlight: Boolean = false,
) {
    var text by remember { mutableStateOf(format(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused) {
        if (!focused) {
            val p = parse(text) ?: 0.0
            if (abs(p - value) > 1e-6) text = format(value)
        }
    }
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
    )
    BasicTextField(
        value = text,
        onValueChange = { t ->
            val clean = t.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) || (!decimal && it == ':') }.take(7)
            text = clean
            onValue(parse(clean) ?: 0.0)
        },
        singleLine = true,
        textStyle = style,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.height(36.dp).onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (highlight) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) {
                    Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)))
                }
                inner()
            }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmText, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(LocalStrings.current.cancel) } },
    )
}

val REST_OPTIONS = listOf(0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)

fun restLabel(sec: Int) = if (sec == 0) I18n.current.restOff else fmtClock(sec)

@Composable
fun RestPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit, title: String = LocalStrings.current.restTime) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(REST_OPTIONS) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(s); onDismiss() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = s == current, onClick = { onPick(s); onDismiss() })
                        Spacer(Modifier.size(8.dp))
                        Text(restLabel(s))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(LocalStrings.current.close) } },
    )
}
