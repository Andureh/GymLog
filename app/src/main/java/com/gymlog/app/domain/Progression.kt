package com.gymlog.app.domain

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.util.KG_TO_LB
import kotlin.math.max
import kotlin.math.roundToInt

enum class SuggestKind { UP, REPS, DELOAD }

/** Sugestão para a próxima vez: [weight] em kg, [duration] em segundos. */
data class Suggestion(val kind: SuggestKind, val weight: Double, val reps: Int, val duration: Int = 0)

/** Incremento de carga em kg (2,5 kg barra/máquinas, 2 kg halteres; 5 lb em libras). */
fun weightIncrement(equipment: String, lbs: Boolean): Double = when {
    lbs -> 5 / KG_TO_LB
    equipment == "dumbbell" || equipment == "kettlebell" -> 2.0
    else -> 2.5
}

/**
 * Progressão dupla com base nas sessões anteriores ([history] por ordem cronológica):
 * - Se em todas as séries de trabalho com o peso de topo chegaste ao limite da gama de repetições → sobe o peso.
 * - Caso contrário → mesmo peso, mais uma repetição.
 * - Se o 1RM estimado não sobe há 3 sessões → alívio (deload) de ~10%.
 */
fun suggestNext(type: ExerciseType, equipment: String, history: List<Session>, lbs: Boolean): Suggestion? {
    val last = history.lastOrNull() ?: return null
    val work = last.sets.filter { it.type != SetType.WARMUP }
    when (type) {
        ExerciseType.REPS_ONLY -> {
            val r = work.maxOfOrNull { it.reps } ?: return null
            if (r <= 0) return null
            return Suggestion(SuggestKind.REPS, 0.0, r + 1)
        }
        ExerciseType.DURATION -> {
            val d = work.maxOfOrNull { it.durationSec } ?: return null
            if (d <= 0) return null
            val next = ((d + max(5, d / 10)) / 5.0).roundToInt() * 5
            return Suggestion(SuggestKind.REPS, 0.0, 0, next)
        }
        ExerciseType.WEIGHT_REPS -> {
            val valid = work.filter { it.reps > 0 && it.weight > 0 }
            if (valid.isEmpty()) return null
            val top = valid.maxOf { it.weight }
            val minReps = valid.filter { it.weight >= top - 1e-6 }.minOf { it.reps }
            val inc = weightIncrement(equipment, lbs)

            // Estagnação: as 3 últimas sessões não superaram a anterior a elas
            if (history.size >= 4) {
                val ref = history[history.size - 4].bestE1rm
                if (history.takeLast(3).all { it.bestE1rm <= ref + 1e-6 }) {
                    val deload = max(inc, ((top * 0.9) / inc).roundToInt() * inc)
                    return Suggestion(SuggestKind.DELOAD, deload, minReps)
                }
            }
            val (lower, upper) = when {
                minReps <= 5 -> 3 to 5
                minReps <= 12 -> 8 to 12
                else -> 12 to 20
            }
            return if (minReps >= upper) {
                // Em força (≤5 reps) mantém as repetições; nas outras gamas volta ao início da gama
                Suggestion(SuggestKind.UP, top + inc, if (upper == 5) minReps else lower)
            } else {
                Suggestion(SuggestKind.REPS, top, minReps + 1)
            }
        }
    }
}
