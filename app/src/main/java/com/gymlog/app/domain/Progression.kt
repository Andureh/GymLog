package com.gymlog.app.domain

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.util.KG_TO_LB
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class SuggestKind { UP, REPS, DELOAD, RESUME }

/** Sugestão para a próxima vez: [weight] em kg, [duration] em segundos. */
data class Suggestion(val kind: SuggestKind, val weight: Double, val reps: Int, val duration: Int = 0)

private const val DAY = 86_400_000L

/** Mais do que isto sem fazer o exercício conta como pausa (retoma, não estagnação). */
const val BREAK_DAYS = 21

private const val EPS = 1e-6

/** Quando o salto de peso é grande, quantas repetições a mais (acima da gama) se pedem antes de subir. */
const val MAX_EXTRA_REPS = 6

/** Incremento por defeito em kg (2,5 kg barra/máquinas, 2 kg halteres; 5 lb em libras). */
fun weightIncrement(equipment: String, lbs: Boolean): Double = when {
    lbs -> 5 / KG_TO_LB
    equipment == "dumbbell" || equipment == "kettlebell" -> 2.0
    else -> 2.5
}

/** Resumo de uma sessão: o peso mais alto e as repetições de cada série feita com esse peso. */
private class Perf(val date: Long, val top: Double, val repsAtTop: List<Int>) {
    val minReps get() = repsAtTop.min()
    val avgReps get() = repsAtTop.average()
}

private fun Session.perf(): Perf? {
    val work = sets.filter { it.type != SetType.WARMUP && it.reps > 0 && it.weight > 0 }
    if (work.isEmpty()) return null
    val top = work.maxOf { it.weight }
    return Perf(date, top, work.filter { it.weight >= top - EPS }.map { it.reps })
}

/** Melhor do que [other]: mais peso, ou o mesmo peso com mais repetições em média. */
private fun Perf.betterThan(other: Perf) =
    top > other.top + EPS || (abs(top - other.top) <= EPS && avgReps > other.avgReps + 0.3)

/**
 * Degrau de peso aprendido com o histórico do exercício: a menor diferença entre os pesos que já usaste
 * (ex.: máquinas que sobem 2,25 kg). Sem histórico suficiente usa [fallback].
 */
fun learnIncrement(weights: Collection<Double>, fallback: Double): Double {
    val distinct = weights.map { (it * 100).roundToInt() / 100.0 }.distinct().sorted()
    return distinct.zipWithNext { a, b -> b - a }.filter { it >= 0.45 }.minOrNull() ?: fallback
}

/** Arredonda sugestões de peso: múltiplos de 0,25 kg ficam exatos, o resto a 0,1 kg. */
private fun roundWeight(w: Double): Double {
    val q = (w * 4).roundToInt() / 4.0
    return if (abs(q - w) < 0.02) q else (w * 10).roundToInt() / 10.0
}

/**
 * Gama de repetições: a do objetivo da rotina, se houver; senão a mediana das tuas sessões com os pesos
 * anteriores ao atual. As repetições extra pedidas antes de um salto grande não contam, para a gama
 * não ir subindo sozinha.
 */
private fun repRange(perfs: List<Perf>, targetReps: Int?): Pair<Int, Int> {
    if (targetReps != null && targetReps > 0) return max(1, (targetReps * 2 + 2) / 3) to targetReps
    val current = perfs.last().top
    val earlier = perfs.filter { abs(it.top - current) > EPS }.takeLast(6)
    val source = if (earlier.size >= 2) earlier else perfs.takeLast(3)
    val reps = source.flatMap { it.repsAtTop }.sorted()
    val median = reps[reps.size / 2]
    return when {
        median <= 5 -> 3 to 5
        median <= 12 -> 8 to 12
        median <= 15 -> 12 to 15
        else -> 15 to 20
    }
}

/**
 * Sugestão de progressão dupla a partir das sessões anteriores ([history] por ordem cronológica):
 * 1. Pausa de mais de [BREAK_DAYS] dias → retoma com o peso e as repetições da última vez.
 * 2. Todas as séries com o peso de topo chegaram ao limite da gama → sobe para o peso seguinte
 *    (o próximo halter que existe, se houver [ladder]); as repetições para o peso novo são estimadas
 *    pelo teu 1RM (fórmula de Epley). Se o salto for grande (ex.: halteres de 10 → 12 kg), o limite da
 *    gama sobe até [MAX_EXTRA_REPS] repetições: só se sobe quando, no peso novo, ainda dá para fazer o
 *    mínimo da gama.
 * 3. Estagnação real → alívio de ~10% com as mesmas repetições. Só quando, sem pausas, as 3 últimas sessões não melhoraram
 *    (nem em peso nem em repetições) em relação à anterior a elas, não baixaste o peso tu mesmo, ainda não
 *    chegas ao topo da gama e a última sessão não foi melhor do que a anterior.
 * 4. Caso contrário → mesmo peso, mais uma repetição.
 *
 * [targetReps]: repetições objetivo da rotina; [plateIncrement]: degrau dos discos do ginásio;
 * [ladder]: pesos possíveis (kg, como se registam), ex.: os halteres do ginásio.
 */
fun suggestNext(
    type: ExerciseType,
    equipment: String,
    history: List<Session>,
    lbs: Boolean,
    targetReps: Int? = null,
    now: Long = System.currentTimeMillis(),
    plateIncrement: Double? = null,
    ladder: List<Double>? = null,
): Suggestion? {
    val lastSession = history.lastOrNull() ?: return null
    val afterBreak = now - lastSession.date > BREAK_DAYS * DAY
    val work = lastSession.sets.filter { it.type != SetType.WARMUP }
    when (type) {
        ExerciseType.REPS_ONLY -> {
            val r = work.maxOfOrNull { it.reps } ?: return null
            if (r <= 0) return null
            return if (afterBreak) Suggestion(SuggestKind.RESUME, 0.0, r) else Suggestion(SuggestKind.REPS, 0.0, r + 1)
        }
        ExerciseType.DURATION -> {
            val d = work.maxOfOrNull { it.durationSec } ?: return null
            if (d <= 0) return null
            if (afterBreak) return Suggestion(SuggestKind.RESUME, 0.0, 0, d)
            val next = ((d + max(5, d / 10)) / 5.0).roundToInt() * 5
            return Suggestion(SuggestKind.REPS, 0.0, 0, next)
        }
        ExerciseType.WEIGHT_REPS -> {
            val perfs = history.mapNotNull { it.perf() }
            val last = perfs.lastOrNull() ?: return null
            val top = last.top
            val minReps = last.minReps
            val inc = learnIncrement(perfs.map { it.top }, plateIncrement ?: weightIncrement(equipment, lbs))
            val (lower, upper) = repRange(perfs, targetReps)

            if (afterBreak) return Suggestion(SuggestKind.RESUME, top, minReps)

            // Próximo peso: o seguinte que existe (halteres do ginásio) ou + o degrau
            val next = ladder?.firstOrNull { it > top + 0.05 } ?: roundWeight(top + inc)
            // Repetições precisas no peso atual para, no peso novo, ainda fazer o mínimo da gama
            val needed = ceil(30 * (e1rm(next, lower) / top - 1) - 1e-9).toInt()
            val effUpper = if (upper <= 5) upper else needed.coerceIn(upper, upper + MAX_EXTRA_REPS)

            if (minReps >= effUpper) {
                // Mantém o 1RM estimado no peso novo: reps = 30 × (1RM / peso − 1)
                val est = e1rm(top, minReps)
                val reps = if (upper <= 5) minReps
                else (if (est > next) (30 * (est / next - 1)).toInt() else 1).coerceIn(lower, upper - 1)
                return Suggestion(SuggestKind.UP, next, reps)
            }

            // Sessões recentes sem pausas pelo meio
            val recent = mutableListOf(last)
            for (p in perfs.dropLast(1).asReversed()) {
                if (recent.last().date - p.date > BREAK_DAYS * DAY) break
                recent.add(p)
            }
            recent.reverse()
            val window = recent.takeLast(4)
            val loweredOnPurpose = window.zipWithNext().any { (a, b) -> b.top < a.top - EPS }
            // Não é estagnação se: já chegas ao topo normal da gama (o salto é que é grande),
            // ou a última sessão foi melhor do que a anterior (estás a recuperar)
            val improvedLast = window.size >= 2 && window.last().betterThan(window[window.size - 2])
            if (window.size == 4 && !loweredOnPurpose && minReps < upper && !improvedLast &&
                window.drop(1).none { it.betterThan(window[0]) }
            ) {
                val deload = ladder?.lastOrNull { it <= top * 0.9 + 0.05 }
                    ?: max(inc, roundWeight(((top * 0.9) / inc).roundToInt() * inc))
                return Suggestion(SuggestKind.DELOAD, deload, minReps)
            }
            return Suggestion(SuggestKind.REPS, top, (minReps + 1).coerceAtMost(effUpper))
        }
    }
}
