package com.gymlog.app

import com.gymlog.app.data.Exercise
import com.gymlog.app.data.Workout
import com.gymlog.app.data.WorkoutExercise
import com.gymlog.app.data.WorkoutExerciseFull
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.data.WorkoutSet
import com.gymlog.app.domain.groupByRoutine
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineStatsTest {
    private val bench = Exercise(id = 1, name = "Bench", muscle = "chest", equipment = "barbell")

    private fun workout(id: Long, name: String, day: Long, vararg sets: Pair<Double, Int>, done: Boolean = true) = WorkoutFull(
        Workout(id = id, name = name, startTime = day * 86_400_000L, endTime = day * 86_400_000L + 3_600_000L),
        listOf(
            WorkoutExerciseFull(
                WorkoutExercise(id = id, workoutId = id, exerciseId = 1, position = 0), bench,
                sets.mapIndexed { i, (w, r) -> WorkoutSet(id = id * 100 + i, workoutExerciseId = id, position = i, weight = w, reps = r, completed = done) },
            )
        ),
    )

    @Test fun groupsByNameIgnoringCaseAndSpaces() {
        val groups = groupByRoutine(
            listOf(
                workout(1, "Upper B", 1, 80.0 to 10, 80.0 to 8),
                workout(2, "upper  b ", 5, 85.0 to 10, 85.0 to 10, 85.0 to 9),
                workout(3, "Lower A", 3, 100.0 to 5),
                workout(4, "Empty", 6, 50.0 to 10, done = false),
            )
        )
        assertEquals(listOf("upper b", "lower a"), groups.map { it.key })
        val upper = groups.first()
        assertEquals("upper  b", upper.name)
        assertEquals(listOf(1L, 2L), upper.sessions.map { it.workoutId })
        assertEquals(1440.0, upper.sessions[0].volumeKg, 1e-9)
        assertEquals(2465.0, upper.sessions[1].volumeKg, 1e-9)
        assertEquals(listOf(2, 3), upper.sessions.map { it.sets })
    }
}
