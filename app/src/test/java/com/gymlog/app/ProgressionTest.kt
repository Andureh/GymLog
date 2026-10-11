package com.gymlog.app

import com.gymlog.app.ai.buildTrainingContext
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetHistoryRow
import com.gymlog.app.data.SetType
import com.gymlog.app.data.Workout
import com.gymlog.app.data.WorkoutExercise
import com.gymlog.app.data.WorkoutExerciseFull
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.data.WorkoutSet
import com.gymlog.app.domain.SuggestKind
import com.gymlog.app.domain.buildSessions
import com.gymlog.app.domain.suggestNext
import com.gymlog.app.i18n.En
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {
    private var id = 1L
    private fun session(day: Long, vararg sets: Pair<Double, Int>) = sets.map { (w, r) ->
        SetHistoryRow(day, "W$day", day * 86_400_000L, id++, w, r, 0, SetType.NORMAL)
    }

    @Test fun moreRepsWhenBelowTopOfRange() {
        val s = suggestNext(ExerciseType.WEIGHT_REPS, "barbell", buildSessions(session(1, 80.0 to 10, 80.0 to 9, 80.0 to 8)), false)!!
        assertEquals(SuggestKind.REPS, s.kind)
        assertEquals(80.0, s.weight, 1e-9)
        assertEquals(9, s.reps)
    }

    @Test fun increaseWeightWhenAllSetsHitTop() {
        val s = suggestNext(ExerciseType.WEIGHT_REPS, "barbell", buildSessions(session(1, 80.0 to 12, 80.0 to 12, 80.0 to 12)), false)!!
        assertEquals(SuggestKind.UP, s.kind)
        assertEquals(82.5, s.weight, 1e-9)
        assertEquals(8, s.reps)
        val db = suggestNext(ExerciseType.WEIGHT_REPS, "dumbbell", buildSessions(session(1, 20.0 to 12, 20.0 to 12)), false)!!
        assertEquals(22.0, db.weight, 1e-9)
    }

    @Test fun strengthRangeKeepsReps() {
        val s = suggestNext(ExerciseType.WEIGHT_REPS, "barbell", buildSessions(session(1, 100.0 to 5, 100.0 to 5, 100.0 to 5)), false)!!
        assertEquals(SuggestKind.UP, s.kind)
        assertEquals(102.5, s.weight, 1e-9)
        assertEquals(5, s.reps)
    }

    @Test fun deloadWhenStalled() {
        val rows = session(1, 100.0 to 6) + session(2, 100.0 to 6) + session(3, 100.0 to 5) + session(4, 100.0 to 6)
        val s = suggestNext(ExerciseType.WEIGHT_REPS, "barbell", buildSessions(rows), false)!!
        assertEquals(SuggestKind.DELOAD, s.kind)
        assertEquals(90.0, s.weight, 1e-9)
    }

    @Test fun noHistoryNoSuggestion() {
        assertNull(suggestNext(ExerciseType.WEIGHT_REPS, "barbell", emptyList(), false))
    }

    @Test fun trainingContextContainsSets() {
        val ex = Exercise(id = 1, name = "x", muscle = "chest", equipment = "barbell", nameKey = "bench_bb")
        val w = WorkoutFull(
            Workout(id = 7, name = "Push A", startTime = 1_700_000_000_000, endTime = 1_700_003_600_000),
            listOf(
                WorkoutExerciseFull(
                    WorkoutExercise(id = 1, workoutId = 7, exerciseId = 1, position = 0), ex,
                    listOf(WorkoutSet(id = 1, workoutExerciseId = 1, position = 0, weight = 80.0, reps = 8, completed = true)),
                )
            ),
        )
        val txt = buildTrainingContext(listOf(w), emptyList(), En, false, 7)
        assertTrue(txt, txt.contains("Bench Press (Barbell): 80x8"))
        assertTrue(txt.contains("WORKOUT THE USER IS ASKING ABOUT"))
        assertTrue(txt.contains("Push A"))
    }
}
