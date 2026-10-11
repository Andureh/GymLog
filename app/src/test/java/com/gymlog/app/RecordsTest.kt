package com.gymlog.app

import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.data.Workout
import com.gymlog.app.data.WorkoutExercise
import com.gymlog.app.data.WorkoutExerciseFull
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.data.WorkoutSet
import com.gymlog.app.domain.Bests
import com.gymlog.app.domain.PrType
import com.gymlog.app.domain.analyzePrs
import com.gymlog.app.domain.e1rm
import com.gymlog.app.domain.prsFor
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.util.fmtClock
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.parseDecimal
import com.gymlog.app.util.parseDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsTest {
    private val bench = Exercise(id = 1, name = "Supino", muscle = "Peito", equipment = "Barra")
    private var nextSet = 1L

    private fun workout(id: Long, day: Long, vararg sets: Triple<Double, Int, SetType>) = WorkoutFull(
        Workout(id = id, name = "T$id", startTime = day * 86_400_000L, endTime = day * 86_400_000L + 3_600_000L),
        listOf(
            WorkoutExerciseFull(
                WorkoutExercise(id = id, workoutId = id, exerciseId = 1, position = 0),
                bench,
                sets.mapIndexed { i, (w, r, t) ->
                    WorkoutSet(id = nextSet++, workoutExerciseId = id, position = i, weight = w, reps = r, type = t, completed = true)
                },
            )
        ),
    )

    @Test fun epley() {
        assertEquals(100.0, e1rm(100.0, 1), 1e-9)
        assertEquals(116.6667, e1rm(100.0, 5), 1e-3)
        assertEquals(0.0, e1rm(100.0, 0), 1e-9)
    }

    @Test fun noPrWithoutHistory() {
        assertTrue(prsFor(ExerciseType.WEIGHT_REPS, Bests(), 100.0, 5, 0).isEmpty())
    }

    @Test fun prDetection() {
        val b = Bests().add(100.0, 5, 0)
        assertEquals(listOf(PrType.WEIGHT, PrType.E1RM, PrType.VOLUME), prsFor(ExerciseType.WEIGHT_REPS, b, 102.5, 5, 0))
        assertEquals(listOf(PrType.E1RM, PrType.VOLUME), prsFor(ExerciseType.WEIGHT_REPS, b, 100.0, 6, 0))
        assertTrue(prsFor(ExerciseType.WEIGHT_REPS, b, 90.0, 5, 0).isEmpty())
        assertEquals(listOf(PrType.REPS), prsFor(ExerciseType.REPS_ONLY, Bests().add(0.0, 10, 0), 0.0, 11, 0))
    }

    @Test fun analyzeMarksOnlyTopSetAndIgnoresWarmups() {
        val w1 = workout(1, 1, Triple(100.0, 5, SetType.NORMAL))
        val w2 = workout(
            2, 2,
            Triple(120.0, 1, SetType.WARMUP),   // aquecimento: nunca conta
            Triple(102.5, 5, SetType.NORMAL),   // melhor peso, 1RM e volume
            Triple(101.0, 5, SetType.NORMAL),
        )
        val prs = analyzePrs(listOf(w1, w2))
        assertTrue(w1.exercises[0].sets.none { prs.containsKey(it.id) })
        val sets = w2.exercises[0].sets
        assertTrue(!prs.containsKey(sets[0].id))
        assertEquals(setOf(PrType.WEIGHT, PrType.E1RM, PrType.VOLUME), prs[sets[1].id]!!.toSet())
        assertTrue(!prs.containsKey(sets[2].id))
        assertEquals(120.0 + 512.5 + 505.0, w2.volumeKg(), 1e-9)
    }

    @Test fun formatting() {
        assertEquals("1:30", fmtClock(90))
        assertEquals("1:02:05", fmtClock(3725))
        assertEquals(90.0, parseDuration("130")!!, 1e-9)
        assertEquals(90.0, parseDuration("1:30")!!, 1e-9)
        assertEquals(45.0, parseDuration("45")!!, 1e-9)
        assertEquals(82.5, parseDecimal("82,5")!!, 1e-9)
        assertEquals("82.5", fmtNum(82.5))
        assertEquals("100", fmtNum(100.0))
        assertEquals("0", fmtNum(0.0))
    }
}
