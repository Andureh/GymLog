package com.gymlog.app

import com.gymlog.app.data.Exercise
import com.gymlog.app.data.Workout
import com.gymlog.app.data.WorkoutExercise
import com.gymlog.app.data.WorkoutExerciseFull
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.data.WorkoutSet
import com.gymlog.app.domain.RoutineTemplate
import com.gymlog.app.domain.groupByRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineStatsTest {
    private fun ex(id: Long) = Exercise(id = id, name = "E$id", muscle = "chest", equipment = "barbell")

    /** Treino com uma série concluída de 100 kg × 10 em cada exercício indicado. */
    private fun workout(id: Long, name: String, day: Long, exercises: List<Long>, routineId: Long? = null) = WorkoutFull(
        Workout(id = id, name = name, startTime = day * 86_400_000L, endTime = day * 86_400_000L + 3_600_000L, routineId = routineId),
        exercises.mapIndexed { i, e ->
            val weId = id * 100 + i
            WorkoutExerciseFull(
                WorkoutExercise(id = weId, workoutId = id, exerciseId = e, position = i), ex(e),
                listOf(WorkoutSet(id = weId, workoutExerciseId = weId, position = 0, weight = 100.0, reps = 10, completed = true)),
            )
        },
    )

    private val upper = listOf(1L, 2L, 3L, 4L, 5L, 6L)
    private val lower = listOf(10L, 11L, 12L, 13L, 14L)

    @Test fun differentTitlesSameExercisesAreOneRoutine() {
        val groups = groupByRoutine(
            listOf(
                workout(1, "Upper B", 1, upper),
                workout(2, "after reggaeton", 3, upper),
                workout(3, "Lower A", 4, lower),
                workout(4, "upper com pressa", 6, upper - 6L + 7L), // trocou um exercício
            )
        )
        assertEquals(2, groups.size)
        val up = groups.first { 1L in it.sessions.map { s -> s.workoutId } }
        assertEquals(listOf(1L, 2L, 4L), up.sessions.map { it.workoutId })
        assertTrue(up.isAuto)
        assertEquals("upper com pressa", up.name) // título mais recente
        assertEquals(1, up.sessions.last().added)
        assertEquals(1, up.sessions.last().removed)
    }

    @Test fun linkedWorkoutsStayInRoutineEvenIfChanged() {
        val routine = RoutineTemplate(7, "Upper B", upper.toSet())
        val groups = groupByRoutine(
            listOf(
                workout(1, "Upper B", 1, upper, routineId = 7),
                // Título diferente e quase tudo mudado, mas foi iniciado a partir da rotina
                workout(2, "treino estranho", 3, listOf(1L, 20L, 21L, 22L), routineId = 7),
                // Histórico antigo sem ligação: reconhecido pelos exercícios da rotina
                workout(3, "Hevy upper", 0, upper),
            ),
            listOf(routine),
        )
        assertEquals(1, groups.size)
        val g = groups.single()
        assertEquals("r:7", g.key)
        assertEquals("Upper B", g.name)
        assertEquals(listOf(3L, 1L, 2L), g.sessions.map { it.workoutId })
    }

    @Test fun differentRoutinesStaySeparate() {
        val groups = groupByRoutine(
            listOf(
                workout(1, "Treino", 1, upper),
                workout(2, "Treino", 2, lower), // mesmo título, exercícios diferentes
            )
        )
        assertEquals(2, groups.size)
    }

    @Test fun volumeAndSets() {
        val g = groupByRoutine(listOf(workout(1, "A", 1, upper))).single()
        assertEquals(6000.0, g.sessions.single().volumeKg, 1e-9)
        assertEquals(6, g.sessions.single().sets)
    }
}
