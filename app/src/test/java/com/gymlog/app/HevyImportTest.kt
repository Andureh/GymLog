package com.gymlog.app

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.HevyImport
import com.gymlog.app.data.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Calendar

class HevyImportTest {
    private val q = "\""
    private val header = listOf(
        "title", "start_time", "end_time", "description", "exercise_title", "superset_id", "exercise_notes",
        "set_index", "set_type", "weight_kg", "reps", "distance_km", "duration_seconds", "rpe",
    ).joinToString(",") { "$q$it$q" }
    private val push = "${q}Push, ${q}${q}heavy${q}${q}${q},${q}10 Oct 2026, 16:51${q},${q}10 Oct 2026, 18:03${q},${q}linha 1\nlinha 2${q}"
    private val sample = "﻿" + listOf(
        header,
        "$push,${q}Bench Press (Barbell)${q},,${q}${q},0,${q}warmup${q},40,10,,,",
        "$push,${q}Bench Press (Barbell)${q},,${q}${q},1,${q}normal${q},82.5,8,,,8",
        "$push,${q}Iso-Lateral High Row (Machine)${q},,${q}boa${q},0,${q}dropset${q},75,12,,,",
        "${q}Legs${q},${q}12 Oct 2026, 09:00${q},${q}12 Oct 2026, 10:00${q},${q}${q},${q}Walking${q},,${q}${q},0,${q}normal${q},,,1.2,900,",
    ).joinToString("\r\n")

    @Test fun parsesHevyCsv() {
        val p = HevyImport.parse(sample)
        assertEquals(2, p.workouts.size)
        val w = p.workouts[0]
        assertEquals("Push, \"heavy\"", w.title)
        assertEquals("linha 1\nlinha 2", w.description)
        assertEquals(2, w.exercises.size)
        assertEquals(SetType.WARMUP, w.exercises[0].sets[0].type)
        assertEquals(82.5, w.exercises[0].sets[1].weightKg, 1e-9)
        assertEquals(SetType.DROP, w.exercises[1].sets[0].type)
        assertEquals("boa", w.exercises[1].notes)
        val cal = Calendar.getInstance().apply { timeInMillis = w.start }
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(51, cal.get(Calendar.MINUTE))
        assertEquals(72L * 60_000, w.end - w.start)
        assertEquals(900, p.workouts[1].exercises[0].sets[0].durationSec)
    }

    @Test fun matchesExercises() {
        assertEquals("bench_bb", HevyImport.matchKey("Bench Press (Barbell)"))
        assertEquals("incline_bench_smith", HevyImport.matchKey("Incline Bench Press (Smith Machine)"))
        assertEquals("curl_ez", HevyImport.matchKey("EZ Bar Biceps Curl"))
        assertNull(HevyImport.matchKey("Belt Squat (Machine)"))
        val added = listOf(
            "Bench Press (Smith Machine)", "Chest Supported Incline Row (Dumbbell)", "Chest Supported T Bar Row", "Cycling",
            "Decline Crunch (Weighted)", "Iso-Lateral Chest Press (Machine)", "Iso-Lateral High Row (Machine)",
            "Leg Press Horizontal (Machine)", "Seated Lateral Raise (Dumbbell)", "Standing Calf Raise (Smith)", "Walking",
        )
        added.forEach { assertNotNull(it, HevyImport.matchKey(it)) }
        val (m, e, t) = HevyImport.guess("Iso-Lateral High Row (Machine)", listOf(HevyImport.HSet(SetType.NORMAL, 70.0, 10, 0)))
        assertEquals("back", m)
        assertEquals("machine", e)
        assertEquals(ExerciseType.WEIGHT_REPS, t)
        val walk = HevyImport.guess("Walking", listOf(HevyImport.HSet(SetType.NORMAL, 0.0, 0, 900)))
        assertEquals("cardio", walk.first)
        assertEquals(ExerciseType.DURATION, walk.third)
    }

    /** Só corre localmente: HEVY_CSV=/caminho/workout_data.csv ./gradlew testDebugUnitTest */
    @Test fun parsesRealExport() {
        val path = System.getenv("HEVY_CSV")
        assumeTrue(!path.isNullOrBlank() && File(path).exists())
        val p = HevyImport.parse(File(path!!).readText())
        println("Hevy: ${p.workouts.size} treinos, ${p.setCount} séries, ${p.exerciseTitles.size} exercícios")
        assertNotNull(p.workouts.firstOrNull())
        val unmatched = p.exerciseTitles.filter { HevyImport.matchKey(it) == null }
        assertEquals("Sem correspondência: $unmatched", 0, unmatched.size)
    }
}
