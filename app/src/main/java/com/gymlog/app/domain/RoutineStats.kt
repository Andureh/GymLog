package com.gymlog.app.domain

import com.gymlog.app.data.WorkoutFull

/**
 * Uma sessão de uma rotina: volume total (kg), séries concluídas e quantos exercícios foram
 * acrescentados/retirados em relação à sessão anterior da mesma rotina.
 */
class RoutineSession(
    val workoutId: Long,
    val date: Long,
    val title: String,
    val volumeKg: Double,
    val sets: Int,
    val exerciseIds: Set<Long>,
    val added: Int,
    val removed: Int,
)

/**
 * Treinos da mesma rotina, por ordem cronológica.
 * [routineId] != null → rotina guardada na app; null → grupo detetado automaticamente pelos exercícios.
 */
class RoutineGroup(val key: String, val name: String, val routineId: Long?, val sessions: List<RoutineSession>) {
    val isAuto: Boolean get() = routineId == null
}

/** Rotina guardada na app: id, nome e exercícios atuais. */
class RoutineTemplate(val id: Long, val name: String, val exerciseIds: Set<Long>)

/** Chave de comparação de títulos: sem diferenças de maiúsculas ou espaços. */
fun routineKey(name: String) = name.trim().lowercase().replace(Regex("\\s+"), " ")

/** Semelhança entre dois conjuntos de exercícios (0 = nada em comum, 1 = iguais). */
fun jaccard(a: Set<Long>, b: Set<Long>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    return a.intersect(b).size.toDouble() / a.union(b).size
}

/** Semelhança mínima para juntar um treino a uma rotina. */
const val MATCH_THRESHOLD = 0.5

/** Se o título também coincide, basta uma semelhança menor. */
const val MATCH_THRESHOLD_SAME_TITLE = 0.3

/** Quantas sessões recentes de cada rotina servem de referência (a rotina pode ir mudando aos poucos). */
private const val RECENT_SESSIONS = 3

/**
 * Agrupa os treinos terminados por rotina:
 * 1. Treinos ligados a uma rotina (iniciados a partir dela, ou escolhida à mão) ficam sempre nessa rotina,
 *    mesmo que o título ou os exercícios mudem.
 * 2. Os restantes (histórico antigo, importações do Hevy) são comparados pelos **exercícios** com a rotina
 *    guardada e com as sessões recentes de cada grupo; juntam-se ao mais parecido se a semelhança chegar ao
 *    limite. Caso contrário, começam um grupo novo, detetado automaticamente.
 *
 * Resultado ordenado pelas rotinas feitas mais recentemente.
 */
fun groupByRoutine(workouts: List<WorkoutFull>, routines: List<RoutineTemplate> = emptyList()): List<RoutineGroup> {
    class Builder(val key: String, var name: String, val routineId: Long?, val template: Set<Long>?) {
        val items = mutableListOf<Pair<WorkoutFull, Set<Long>>>()
        val titles = mutableSetOf<String>()

        fun score(ex: Set<Long>): Double =
            (listOfNotNull(template) + items.takeLast(RECENT_SESSIONS).map { it.second }).maxOfOrNull { jaccard(ex, it) } ?: 0.0

        fun accepts(title: String) = title in titles || title == routineKey(name)

        fun add(w: WorkoutFull, ex: Set<Long>) {
            items.add(w to ex)
            titles.add(routineKey(w.workout.name))
        }
    }

    val byRoutine = routines.associate { it.id to Builder("r:${it.id}", it.name, it.id, it.exerciseIds) }
    val autos = mutableListOf<Builder>()

    val done = workouts.filter { it.workout.endTime != null && it.completedSets() > 0 }.sortedBy { it.workout.startTime }
    for (w in done) {
        val ex = w.exercises.filter { e -> e.sets.any { it.completed } }.map { it.exercise.id }.toSet()
        val linked = w.workout.routineId?.let { byRoutine[it] }
        val target = linked ?: run {
            val title = routineKey(w.workout.name)
            (byRoutine.values + autos)
                .map { b -> b to b.score(ex) }
                .filter { (b, sc) -> sc >= if (b.accepts(title)) MATCH_THRESHOLD_SAME_TITLE else MATCH_THRESHOLD }
                .maxByOrNull { it.second }?.first
                ?: Builder("a:${w.workout.id}", w.workout.name.trim(), null, null).also { autos.add(it) }
        }
        target.add(w, ex)
    }

    return (byRoutine.values + autos)
        .filter { it.items.isNotEmpty() }
        .map { b ->
            var prev: Set<Long>? = null
            val sessions = b.items.map { (w, ex) ->
                val p = prev
                prev = ex
                RoutineSession(
                    workoutId = w.workout.id,
                    date = w.workout.startTime,
                    title = w.workout.name.trim(),
                    volumeKg = w.volumeKg(),
                    sets = w.completedSets(),
                    exerciseIds = ex,
                    added = if (p == null) 0 else (ex - p).size,
                    removed = if (p == null) 0 else (p - ex).size,
                )
            }
            // Grupos automáticos usam o título mais recente; rotinas guardadas usam o nome da rotina
            RoutineGroup(b.key, if (b.routineId == null) sessions.last().title else b.name, b.routineId, sessions)
        }
        .sortedByDescending { it.sessions.last().date }
}
