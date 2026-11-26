@file:OptIn(ExperimentalLayoutApi::class)

package com.gymlog.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlog.app.app
import com.gymlog.app.data.LoadSetup
import com.gymlog.app.data.NO_BAR
import com.gymlog.app.domain.Loading
import com.gymlog.app.domain.solvePlates
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.util.KG_TO_LB
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.parseDecimal
import com.gymlog.app.util.unitLabel

/** Cores habituais dos discos (pela posição de tamanho), para os reconhecer de relance. */
private fun plateColor(weight: Double, lbs: Boolean): Color {
    val kg = if (lbs) weight / KG_TO_LB else weight
    return when {
        kg >= 24 -> Color(0xFFE53935) // 25 kg vermelho
        kg >= 19 -> Color(0xFF1E88E5) // 20 kg azul
        kg >= 14 -> Color(0xFFFDD835) // 15 kg amarelo
        kg >= 9 -> Color(0xFF43A047)  // 10 kg verde
        kg >= 4.5 -> Color(0xFFECEFF1) // 5 kg branco
        kg >= 2 -> Color(0xFFB71C1C)  // 2,5 kg vermelho escuro
        else -> Color(0xFF90A4AE)     // pequenos: prateado
    }
}

/** Desenho da barra com os discos de um lado (ou dos dois, espelhados). */
@Composable
private fun PlateDiagram(perSide: List<Double>, sides: Int, lbs: Boolean) {
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.surface
    val maxW = perSide.maxOrNull() ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val cy = size.height / 2
        val plateW = 12.dp.toPx()
        val gap = 2.dp.toPx()
        val sleeve = 18.dp.toPx()
        val maxH = size.height
        drawRoundRect(barColor, Offset(0f, cy - 3.dp.toPx()), Size(size.width, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
        fun side(fromRight: Boolean) {
            var x = if (fromRight) size.width / 2 + sleeve else size.width / 2 - sleeve - plateW
            perSide.forEach { w ->
                val h = maxH * (0.35f + 0.65f * (w / maxW).toFloat())
                val c = plateColor(w, lbs)
                drawRoundRect(c, Offset(x, cy - h / 2), Size(plateW, h), CornerRadius(3.dp.toPx()))
                drawRoundRect(outline, Offset(x, cy - h / 2), Size(plateW, h), CornerRadius(3.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                x += if (fromRight) plateW + gap else -(plateW + gap)
            }
        }
        // Batente central da barra
        drawRoundRect(barColor, Offset(size.width / 2 - sleeve + 4.dp.toPx(), cy - 9.dp.toPx()), Size(2 * sleeve - 8.dp.toPx(), 18.dp.toPx()), CornerRadius(2.dp.toPx()))
        side(fromRight = true)
        if (sides == 2) side(fromRight = false)
    }
}

/**
 * Calculadora de discos. [initialKg]: peso a calcular (kg); [exerciseId]/[equipment]: para escolher a barra
 * por defeito e lembrar a escolha; [onApply]: se não for null, mostra "Usar" para pôr o peso nas séries.
 */
@Composable
fun PlateCalculatorDialog(
    initialKg: Double?,
    exerciseId: Long?,
    equipment: String?,
    onDismiss: () -> Unit,
    onManageGyms: () -> Unit,
    onApply: ((Double) -> Unit)? = null,
) {
    val app = LocalContext.current.app
    val s = LocalStrings.current
    val store = app.gyms
    val state by store.flow.collectAsStateWithLifecycle()
    val gym = state.active
    val u = unitLabel(gym.lbs)
    var setup by remember(gym.id) { mutableStateOf(store.setupFor(exerciseId, equipment).let { st -> if (st.barId == NO_BAR || gym.bars.any { it.id == st.barId }) st else LoadSetup(NO_BAR, st.sides) }) }
    var text by remember(gym.id) {
        mutableStateOf(initialKg?.takeIf { it > 0 }?.let { fmtNum(if (gym.lbs) it * KG_TO_LB else it) } ?: "")
    }
    val target = parseDecimal(text)
    val bar = gym.bars.find { it.id == setup.barId }?.weight ?: 0.0
    val result = target?.let { solvePlates(it, bar, setup.sides, gym.plates) }
    val toKg = { w: Double -> if (gym.lbs) w / KG_TO_LB else w }

    fun changeSetup(n: LoadSetup) {
        setup = n
        exerciseId?.let { store.setSetup(it, n) }
    }

    @Composable
    fun LoadingView(l: Loading) {
        PlateDiagram(l.perSide, setup.sides, gym.lbs)
        Spacer(Modifier.height(4.dp))
        Text(
            if (l.perSide.isEmpty()) s.onlyBar
            else "${s.perSide}: " + l.perSide.joinToString(" + ") { fmtNum(it) } + " $u",
            fontWeight = FontWeight.SemiBold,
        )
        Text("${s.total}: ${fmtNum(l.total)} $u", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onApply != null) {
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = { onApply(toKg(l.total)); onDismiss() }) { Text(s.useWeight("${fmtNum(l.total)} $u")) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.plateCalculator) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.gyms.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.gyms.forEach { g -> FilterChip(g.id == gym.id, { store.setActive(g.id) }, label = { Text(g.name) }) }
                    }
                }
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("${s.targetWeight} ($u)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    gym.bars.forEach { b ->
                        FilterChip(setup.barId == b.id, { changeSetup(setup.copy(barId = b.id)) }, label = { Text("${b.name} · ${fmtNum(b.weight)} $u") })
                    }
                    FilterChip(setup.barId == NO_BAR, { changeSetup(setup.copy(barId = NO_BAR)) }, label = { Text(s.noBar) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(setup.sides == 2, { changeSetup(setup.copy(sides = 2)) }, label = { Text(s.sidesBoth) })
                    FilterChip(setup.sides == 1, { changeSetup(setup.copy(sides = 1)) }, label = { Text(s.sidesOne) })
                }
                HorizontalDivider()
                when {
                    result == null -> Unit
                    result.exact != null -> LoadingView(result.exact)
                    target != null && target < bar -> Text(s.lighterThanBar, color = MaterialTheme.colorScheme.error)
                    else -> {
                        Text(s.notExact, style = MaterialTheme.typography.bodyMedium)
                        result.below?.let { LoadingView(it) }
                        if (result.below != null && result.above != null) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        result.above?.let { LoadingView(it) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close) } },
        dismissButton = { TextButton(onClick = { onDismiss(); onManageGyms() }) { Text(s.gymsAndPlates) } },
    )
}
