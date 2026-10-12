package com.gymlog.app.domain

import com.gymlog.app.data.WorkoutFull

/** Uma sessão de uma rotina: volume total (kg) e séries concluídas. */
class RoutineSession(val workoutId: Long, val date: Long, val volumeKg: Double, val sets: Int)

/** Todos os treinos com o mesmo nome, por ordem cronológica. */
class RoutineGroup(val key: String, val name: String, val sessions: List<RoutineSession>)

/** Chave de agrupamento: o nome do treino sem diferenças de maiúsculas ou espaços. */
fun routineKey(name: String) = name.trim().lowercase().replace(Regex("\\s+"), " ")

/**
 * Agrupa os treinos terminados pelo nome (os treinos iniciados a partir de uma rotina têm o nome dela,
 * e os importados do Hevy mantêm o título). Ordena pelas rotinas feitas mais recentemente.
 */
fun groupByRoutine(workouts: List<WorkoutFull>): List<RoutineGroup> =
    workouts
        .filter { it.workout.endTime != null && it.completedSets() > 0 }
        .groupBy { routineKey(it.workout.name) }
        .map { (key, list) ->
            val asc = list.sortedBy { it.workout.startTime }
            RoutineGroup(
                key = key,
                name = asc.last().workout.name.trim(),
                sessions = asc.map { RoutineSession(it.workout.id, it.workout.startTime, it.volumeKg(), it.completedSets()) },
            )
        }
        .sortedByDescending { g -> g.sessions.last().date }
