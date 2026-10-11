package com.gymlog.app.domain

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetHistoryRow
import com.gymlog.app.data.SetType
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.data.WorkoutSet
import kotlin.math.max

enum class PrType { WEIGHT, E1RM, VOLUME, REPS, DURATION }

/** 1RM estimado pela fórmula de Epley. */
fun e1rm(weight: Double, reps: Int): Double = when {
    reps <= 0 || weight <= 0 -> 0.0
    reps == 1 -> weight
    else -> weight * (1 + reps / 30.0)
}

/** Melhores marcas de um exercício (séries de aquecimento excluídas). */
data class Bests(
    val weight: Double = 0.0,
    val e1rm: Double = 0.0,
    val volume: Double = 0.0,
    val reps: Int = 0,
    val duration: Int = 0,
    val count: Int = 0,
) {
    fun add(w: Double, r: Int, d: Int) = Bests(
        weight = if (r > 0) max(weight, w) else weight,
        e1rm = max(e1rm, e1rm(w, r)),
        volume = max(volume, w * r),
        reps = max(reps, r),
        duration = max(duration, d),
        count = count + 1,
    )
}

private const val EPS = 1e-6

/** Recordes batidos por uma série em relação a [b]. Sem histórico anterior não há recordes. */
fun prsFor(type: ExerciseType, b: Bests, w: Double, r: Int, d: Int): List<PrType> {
    if (b.count == 0) return emptyList()
    return when (type) {
        ExerciseType.WEIGHT_REPS -> buildList {
            if (r > 0 && w > b.weight + EPS) add(PrType.WEIGHT)
            if (e1rm(w, r) > b.e1rm + EPS) add(PrType.E1RM)
            if (w * r > b.volume + EPS) add(PrType.VOLUME)
        }
        ExerciseType.REPS_ONLY -> if (r > b.reps) listOf(PrType.REPS) else emptyList()
        ExerciseType.DURATION -> if (d > b.duration) listOf(PrType.DURATION) else emptyList()
    }
}

/** Marca, para cada tipo de recorde, a melhor série do treino se esta bater o histórico anterior. */
fun markPrs(type: ExerciseType, before: Bests, sets: List<WorkoutSet>): Map<Long, List<PrType>> {
    val valid = sets.filter { it.completed && it.type != SetType.WARMUP }
    if (before.count == 0 || valid.isEmpty()) return emptyMap()
    val res = mutableMapOf<Long, MutableList<PrType>>()
    fun mark(t: PrType, best: Double, sel: (WorkoutSet) -> Double) {
        val top = valid.maxBy(sel)
        if (sel(top) > best + EPS) res.getOrPut(top.id) { mutableListOf() }.add(t)
    }
    when (type) {
        ExerciseType.WEIGHT_REPS -> {
            mark(PrType.WEIGHT, before.weight) { if (it.reps > 0) it.weight else 0.0 }
            mark(PrType.E1RM, before.e1rm) { e1rm(it.weight, it.reps) }
            mark(PrType.VOLUME, before.volume) { it.weight * it.reps }
        }
        ExerciseType.REPS_ONLY -> mark(PrType.REPS, before.reps.toDouble()) { it.reps.toDouble() }
        ExerciseType.DURATION -> mark(PrType.DURATION, before.duration.toDouble()) { it.durationSec.toDouble() }
    }
    return res
}

/** Percorre todos os treinos (ordem cronológica) e devolve os recordes de cada série (setId -> recordes). */
fun analyzePrs(workoutsAsc: List<WorkoutFull>): Map<Long, List<PrType>> {
    val bests = HashMap<Long, Bests>()
    val out = HashMap<Long, List<PrType>>()
    for (w in workoutsAsc) {
        for ((exId, list) in w.exercises.groupBy { it.exercise.id }) {
            val type = list.first().exercise.type
            val sets = list.flatMap { it.sets }
            val before = bests[exId] ?: Bests()
            out.putAll(markPrs(type, before, sets))
            bests[exId] = sets.filter { it.completed && it.type != SetType.WARMUP }
                .fold(before) { b, s -> b.add(s.weight, s.reps, s.durationSec) }
        }
    }
    return out
}

fun WorkoutFull.volumeKg(): Double = exercises.sumOf { e ->
    if (e.exercise.type == ExerciseType.DURATION) 0.0
    else e.sets.filter { it.completed }.sumOf { it.weight * it.reps }
}

fun WorkoutFull.completedSets(): Int = exercises.sumOf { e -> e.sets.count { it.completed } }

fun WorkoutFull.durationMs(now: Long = System.currentTimeMillis()): Long =
    (workout.endTime ?: now) - workout.startTime

/** Uma sessão (treino) de um exercício, com métricas agregadas. */
class Session(val workoutId: Long, val workoutName: String, val date: Long, val sets: List<SetHistoryRow>) {
    private val work = sets.filter { it.type != SetType.WARMUP }
    val maxWeight = work.filter { it.reps > 0 }.maxOfOrNull { it.weight } ?: 0.0
    val bestE1rm = work.maxOfOrNull { e1rm(it.weight, it.reps) } ?: 0.0
    val bestSetVolume = work.maxOfOrNull { it.weight * it.reps } ?: 0.0
    val volume = sets.sumOf { it.weight * it.reps }
    val maxReps = work.maxOfOrNull { it.reps } ?: 0
    val totalReps = sets.sumOf { it.reps }
    val maxDuration = work.maxOfOrNull { it.durationSec } ?: 0
    val totalDuration = sets.sumOf { it.durationSec }
}

fun buildSessions(rows: List<SetHistoryRow>): List<Session> =
    rows.groupBy { it.workoutId }.map { (wid, sets) ->
        Session(wid, sets.first().workoutName, sets.first().startTime, sets)
    }
