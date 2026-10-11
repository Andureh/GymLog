package com.gymlog.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymlog.app.i18n.LocalStrings
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class ChartPoint(val x: Long, val y: Double)

/** Escala "bonita" para o eixo Y: devolve (min, max, passo). */
private fun niceScale(lo: Double, hi: Double, ticks: Int = 4): Triple<Double, Double, Double> {
    var a = lo
    var b = hi
    if (abs(b - a) < 1e-9) { a -= 1; b += 1 }
    val raw = (b - a) / ticks
    val mag = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= raw }
    return Triple(floor(a / step) * step, ceil(b / step) * step, step)
}

/**
 * Gráfico de linha de uma série, com eixo temporal proporcional.
 * Tocar/arrastar mostra a mira e o valor do ponto mais próximo.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    yFormat: (Double) -> String,
    xFormat: (Long) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(LocalStrings.current.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val tm = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf(points.lastIndex) }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val ink = MaterialTheme.colorScheme.onSurface

    val minX = points.first().x
    val maxX = points.last().x
    val (yMin, yMax, yStep) = remember(points) {
        niceScale(points.minOf { it.y } * 0.98, points.maxOf { it.y } * 1.02)
    }
    val sel = points[selected.coerceIn(0, points.lastIndex)]

    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(yFormat(sel.y), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(xFormat(sel.x), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(8.dp))

        fun xs(width: Float, left: Float, right: Float): List<Float> {
            val w = width - left - right
            return if (maxX == minX) points.indices.map { left + w * (if (points.size == 1) 0.5f else it / (points.size - 1f)) }
            else points.map { left + w * ((it.x - minX).toFloat() / (maxX - minX)) }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures { off ->
                        val p = xs(size.width.toFloat(), 48.dp.toPx(), 12.dp.toPx())
                        selected = p.indices.minBy { abs(p[it] - off.x) }
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        val p = xs(size.width.toFloat(), 48.dp.toPx(), 12.dp.toPx())
                        selected = p.indices.minBy { abs(p[it] - change.position.x) }
                    }
                }
        ) {
            val left = 48.dp.toPx()
            val right = 12.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 22.dp.toPx()
            val h = size.height - top - bottom
            fun yPos(v: Double) = top + h * (1 - ((v - yMin) / (yMax - yMin)).toFloat())

            // Grelha discreta + rótulos do eixo Y
            var t = yMin
            while (t <= yMax + yStep / 2) {
                val y = yPos(t)
                drawLine(grid, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1f)
                val lr = tm.measure(yFormat(t), labelStyle)
                drawText(lr, topLeft = Offset(left - lr.size.width - 6.dp.toPx(), y - lr.size.height / 2f))
                t += yStep
            }

            val px = xs(size.width, left, right)
            val py = points.map { yPos(it.y) }

            // Área + linha (2px)
            if (points.size > 1) {
                val line = Path().apply {
                    moveTo(px[0], py[0])
                    for (i in 1 until px.size) lineTo(px[i], py[i])
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(px.last(), top + h)
                    lineTo(px.first(), top + h)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), top, top + h))
                drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // Marcadores com anel da cor da superfície (só se não houver demasiados pontos)
            val showDots = points.size <= 40
            val si = selected.coerceIn(0, points.lastIndex)
            if (showDots) {
                for (i in px.indices) {
                    drawCircle(surface, radius = 5.dp.toPx(), center = Offset(px[i], py[i]))
                    drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(px[i], py[i]))
                }
            }
            // Mira no ponto selecionado
            drawLine(
                ink.copy(alpha = 0.35f), Offset(px[si], top), Offset(px[si], top + h), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            drawCircle(surface, radius = 7.dp.toPx(), center = Offset(px[si], py[si]))
            drawCircle(color, radius = 5.dp.toPx(), center = Offset(px[si], py[si]))

            // Rótulos do eixo X: primeiro e último
            val first = tm.measure(xFormat(minX), labelStyle)
            drawText(first, topLeft = Offset(left, size.height - first.size.height))
            if (maxX != minX) {
                val last = tm.measure(xFormat(maxX), labelStyle)
                drawText(last, topLeft = Offset(size.width - right - last.size.width, size.height - last.size.height))
            }
        }
    }
}

data class Bar(val label: String, val value: Double)

/** Gráfico de barras verticais de uma série. Tocar numa barra mostra o seu valor. */
@Composable
fun BarChart(
    bars: List<Bar>,
    yFormat: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    caption: (Bar) -> String = { it.label },
) {
    if (bars.isEmpty()) return
    val tm = rememberTextMeasurer()
    var selected by remember(bars) { mutableStateOf(bars.lastIndex) }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxV = max(bars.maxOf { it.value }, 1e-9)
    val sel = bars[selected.coerceIn(0, bars.lastIndex)]

    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(yFormat(sel.value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(caption(sel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(bars) {
                    detectTapGestures { off ->
                        val slot = size.width.toFloat() / bars.size
                        selected = (off.x / slot).toInt().coerceIn(0, bars.lastIndex)
                    }
                }
        ) {
            val bottom = 18.dp.toPx()
            val h = size.height - bottom
            val slot = size.width / bars.size
            val gap = 2.dp.toPx()
            val barW = (slot * 0.62f).coerceAtLeast(2f)
            val r = 4.dp.toPx()
            drawLine(grid, Offset(0f, h), Offset(size.width, h), strokeWidth = 1f)
            val labelEvery = ceil(bars.size / 6.0).toInt().coerceAtLeast(1)
            bars.forEachIndexed { i, b ->
                val bh = (h - gap) * (b.value / maxV).toFloat()
                val x = i * slot + (slot - barW) / 2
                val isSel = i == selected
                val c = if (isSel) color else color.copy(alpha = 0.55f)
                if (bh > 0.5f) {
                    // Topo arredondado (4px), base reta assente na linha de base
                    val rr = minOf(r, bh / 2, barW / 2)
                    drawRoundRect(c, topLeft = Offset(x, h - bh), size = Size(barW, bh), cornerRadius = CornerRadius(rr, rr))
                    if (bh > rr) drawRect(c, topLeft = Offset(x, h - rr), size = Size(barW, rr))
                }
                if (i % labelEvery == 0 || i == bars.lastIndex) {
                    val lr = tm.measure(b.label, labelStyle)
                    val lx = (i * slot + slot / 2 - lr.size.width / 2f).coerceIn(0f, size.width - lr.size.width)
                    drawText(lr, topLeft = Offset(lx, size.height - lr.size.height))
                }
            }
        }
    }
}

/** Barra horizontal simples para distribuições (ex.: séries por grupo muscular). */
@Composable
fun HBar(label: String, value: Double, max: Double, valueText: String, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            val w = size.width * (value / max.coerceAtLeast(1e-9)).toFloat().coerceIn(0f, 1f)
            if (w > 0) drawRoundRect(color, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
        }
    }
}
