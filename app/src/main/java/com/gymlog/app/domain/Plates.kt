package com.gymlog.app.domain

import com.gymlog.app.data.PlateSpec
import kotlin.math.roundToLong

/** Uma forma de carregar: discos num lado (do maior para o menor) e o peso total que dá. */
data class Loading(val perSide: List<Double>, val total: Double)

/**
 * Resultado da calculadora: [exact] se os discos chegam exatamente ao alvo; senão as alternativas
 * mais próximas abaixo ([below]) e acima ([above]).
 */
data class PlateResult(val exact: Loading?, val below: Loading?, val above: Loading?)

/**
 * Que discos pôr para chegar a [target] (tudo na mesma unidade).
 * [bar] = peso da barra/base (0 sem barra); [sides] = 2 (carregado dos dois lados) ou 1.
 * Respeita quantos pares de cada disco existem e, entre as soluções, usa o menor número de discos.
 */
fun solvePlates(target: Double, bar: Double, sides: Int, plates: List<PlateSpec>): PlateResult {
    val unit = { x: Double -> (x * 100).roundToLong() }
    val perSide = (target - bar) / sides
    val sizes = plates.filter { it.weight > 0 && it.pairs > 0 }.sortedByDescending { it.weight }
    val total = { counts: IntArray -> bar + sides * sizes.indices.sumOf { sizes[it].weight * counts[it] } }
    val toLoading = { counts: IntArray ->
        Loading(sizes.indices.flatMap { i -> List(counts[i]) { sizes[i].weight } }, total(counts))
    }
    if (perSide < -1e-9) return PlateResult(null, null, Loading(emptyList(), bar))
    if (sizes.isEmpty()) {
        val only = Loading(emptyList(), bar)
        return if (perSide < 1e-9) PlateResult(only, null, null) else PlateResult(null, only, null)
    }

    // Mochila limitada em unidades de 0,01: menor número de discos para cada soma possível de um lado
    val g = sizes.map { unit(it.weight) }.reduce { a, b -> gcd(a, b) }
    val goal = unit(perSide)
    val limit = ((goal + unit(sizes.first().weight)) / g).toInt()
    val best = IntArray(limit + 1) { Int.MAX_VALUE }
    val counts = arrayOfNulls<IntArray>(limit + 1)
    best[0] = 0
    counts[0] = IntArray(sizes.size)
    sizes.forEachIndexed { i, p ->
        val w = (unit(p.weight) / g).toInt()
        // Um par = um disco de cada lado; com um só lado podem ir os dois discos do par
        repeat(p.pairs * 2 / sides) {
            for (s in limit downTo w) {
                val prev = best[s - w]
                if (prev != Int.MAX_VALUE && prev + 1 < best[s]) {
                    best[s] = prev + 1
                    counts[s] = counts[s - w]!!.copyOf().also { c -> c[i]++ }
                }
            }
        }
    }
    val goalUnits = goal.toDouble() / g
    val exactIdx = goalUnits.toInt().takeIf { goal % g == 0L && it <= limit && best[it] != Int.MAX_VALUE }
    val belowIdx = (minOf(limit, kotlin.math.ceil(goalUnits).toInt() - 1) downTo 0).firstOrNull { best[it] != Int.MAX_VALUE && it < goalUnits }
    val aboveIdx = (kotlin.math.floor(goalUnits).toInt() + 1..limit).firstOrNull { best[it] != Int.MAX_VALUE && it > goalUnits }
    return PlateResult(
        exact = exactIdx?.let { toLoading(counts[it]!!) },
        below = if (exactIdx != null) null else belowIdx?.let { toLoading(counts[it]!!) },
        above = if (exactIdx != null) null else aboveIdx?.let { toLoading(counts[it]!!) },
    )
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

/** Menor salto de peso possível com estes discos (2 × o disco mais pequeno se carregado dos dois lados). */
fun smallestJump(plates: List<PlateSpec>, sides: Int): Double? =
    plates.filter { it.pairs > 0 && it.weight > 0 }.minOfOrNull { it.weight }?.let { it * sides }
