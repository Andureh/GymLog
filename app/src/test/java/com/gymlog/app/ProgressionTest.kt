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
import com.gymlog.app.domain.learnIncrement
import com.gymlog.app.domain.suggestNext
import com.gymlog.app.i18n.En
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {
    private val day = 86_400_000L
    private var id = 1L

    /** Uma sessão no dia [d] com as séries (peso, reps). */
    private fun session(d: Long, vararg sets: Pair<Double, Int>) = sets.map { (w, r) ->
        SetHistoryRow(d, "W$d", d * day, id++, w, r, 0, SetType.NORMAL)
    }

    private fun suggest(rows: List<SetHistoryRow>, equipment: String = "barbell", now: Long? = null, target: Int? = null) =
        suggestNext(
            ExerciseType.WEIGHT_REPS, equipment, buildSessions(rows), false,
            targetReps = target, now = now ?: (rows.maxOf { it.startTime } + 3 * day),
        )!!

    @Test fun moreRepsWhenBelowTopOfRange() {
        val s = suggest(session(1, 80.0 to 10, 80.0 to 9, 80.0 to 8))
        assertEquals(SuggestKind.REPS, s.kind)
        assertEquals(80.0, s.weight, 1e-9)
        assertEquals(9, s.reps)
    }

    @Test fun increaseWeightWhenAllSetsHitTop() {
        val s = suggest(session(1, 80.0 to 12, 80.0 to 12, 80.0 to 12))
        assertEquals(SuggestKind.UP, s.kind)
        assertEquals(82.5, s.weight, 1e-9)
        // Reps estimadas pelo 1RM: 80×12 → 112 kg; a 82,5 kg ≈ 10 reps
        assertEquals(10, s.reps)
        val db = suggest(session(1, 20.0 to 12, 20.0 to 12), equipment = "dumbbell")
        assertEquals(22.0, db.weight, 1e-9)
    }

    @Test fun strengthRangeKeepsReps() {
        val s = suggest(session(1, 100.0 to 5, 100.0 to 5, 100.0 to 5))
        assertEquals(SuggestKind.UP, s.kind)
        assertEquals(102.5, s.weight, 1e-9)
        assertEquals(5, s.reps)
    }

    @Test fun routineTargetDefinesRange() {
        // Objetivo de 10 reps na rotina: 10,10,10 já chega para subir
        val s = suggest(session(1, 50.0 to 10, 50.0 to 10, 50.0 to 10), target = 10)
        assertEquals(SuggestKind.UP, s.kind)
    }

    @Test fun deloadOnlyWhenReallyStalled() {
        val rows = session(1, 100.0 to 6) + session(4, 100.0 to 6) + session(8, 100.0 to 5) + session(11, 100.0 to 6)
        val s = suggest(rows)
        assertEquals(SuggestKind.DELOAD, s.kind)
        assertEquals(90.0, s.weight, 1e-9)
        assertEquals(6, s.reps)
    }

    /** Caso real: mais repetições com o mesmo peso é progresso, não estagnação. */
    @Test fun moreRepsAtSameWeightIsProgress() {
        val rows = session(1, 60.0 to 11, 60.0 to 11, 60.0 to 12) +
            session(7, 60.0 to 11, 60.0 to 12, 60.0 to 10) +
            session(14, 60.0 to 11, 60.0 to 11, 60.0 to 11) +
            session(21, 60.0 to 12, 60.0 to 12, 60.0 to 12)
        val s = suggest(rows, equipment = "cable")
        assertEquals(SuggestKind.UP, s.kind)
        assertTrue(s.weight > 60.0)
    }

    /** Caso real: depois de baixares o peso tu mesmo, não volta a pedir para baixar. */
    @Test fun noDeloadAfterVoluntaryDrop() {
        val rows = session(1, 18.0 to 12, 18.0 to 13, 18.0 to 12) +
            session(5, 18.0 to 12, 18.0 to 12, 18.0 to 8) +
            session(8, 18.0 to 12, 18.0 to 13, 18.0 to 12) +
            session(14, 16.0 to 10, 16.0 to 10, 16.0 to 10) +
            session(20, 16.0 to 10, 16.0 to 11, 16.0 to 10)
        val s = suggest(rows, equipment = "dumbbell")
        assertNotEquals(SuggestKind.DELOAD, s.kind)
    }

    /** Caso real: uma pausa de meses não é estagnação; sugere retomar onde ficaste. */
    @Test fun resumeAfterBreak() {
        val rows = session(1, 32.0 to 12) + session(8, 32.0 to 12) + session(15, 32.0 to 12)
        val s = suggest(rows, equipment = "dumbbell", now = 90 * day)
        assertEquals(SuggestKind.RESUME, s.kind)
        assertEquals(32.0, s.weight, 1e-9)
    }

    /** Caso real: máquinas que sobem 2,25 kg (5 lb) — a sugestão segue esse degrau. */
    @Test fun learnsMachineIncrement() {
        assertEquals(2.25, learnIncrement(listOf(27.0, 29.25, 31.5, 33.75), 2.5), 1e-9)
        assertEquals(2.5, learnIncrement(listOf(40.0), 2.5), 1e-9)
        val rows = session(1, 29.25 to 12) + session(5, 31.5 to 12) + session(9, 33.75 to 12, 33.75 to 12)
        val s = suggest(rows, equipment = "machine")
        assertEquals(36.0, s.weight, 1e-9)
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
