package com.gymlog.app

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetHistoryRow
import com.gymlog.app.data.SetType
import com.gymlog.app.domain.SuggestKind
import com.gymlog.app.domain.buildSessions
import com.gymlog.app.domain.suggestNext
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Repete as sugestões sobre um histórico real (cópia de segurança do GymLog).
 * Só corre localmente: GYMLOG_BACKUP=/caminho/gymlog-AAAA-MM-DD.json ./gradlew testDebugUnitTest
 */
class RealDataTest {
    @Test fun suggestionsOnRealHistory() {
        val path = System.getenv("GYMLOG_BACKUP")
        assumeTrue(!path.isNullOrBlank() && File(path).exists())
        val root = JSONObject(File(path!!).readText())
        fun arr(k: String) = root.getJSONArray(k).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

        val exercises = arr("exercises").associateBy { it.getLong("id") }
        val workouts = arr("workouts").filter { !it.isNull("endTime") }.associateBy { it.getLong("id") }
        val wes = arr("workoutExercises").associateBy { it.getLong("id") }
        val rowsByExercise = arr("sets").filter { it.getBoolean("completed") }.mapNotNull { s ->
            val we = wes[s.getLong("workoutExerciseId")] ?: return@mapNotNull null
            val w = workouts[we.getLong("workoutId")] ?: return@mapNotNull null
            we.getLong("exerciseId") to SetHistoryRow(
                w.getLong("id"), w.getString("name"), w.getLong("startTime"), s.getLong("id"),
                s.getDouble("weight"), s.getInt("reps"), s.getInt("durationSec"), SetType.valueOf(s.getString("type")),
            )
        }.groupBy({ it.first }, { it.second })

        val kinds = mutableMapOf<SuggestKind, Int>()
        var wrongDeloads = 0
        for ((exId, rows) in rowsByExercise) {
            val ex = exercises.getValue(exId)
            if (ex.getString("type") != ExerciseType.WEIGHT_REPS.name) continue
            val sessions = buildSessions(rows.sortedBy { it.startTime })
            for (i in 1 until sessions.size) {
                val history = sessions.subList(0, i)
                val s = suggestNext(ExerciseType.WEIGHT_REPS, ex.getString("equipment"), history, false, now = sessions[i].date) ?: continue
                kinds[s.kind] = (kinds[s.kind] ?: 0) + 1
                // Nunca pedir para aliviar logo a seguir a uma sessão em que melhoraste
                if (s.kind == SuggestKind.DELOAD && history.size >= 2) {
                    val a = history[history.size - 2]
                    val b = history.last()
                    val topA = a.sets.maxOf { it.weight }
                    val topB = b.sets.maxOf { it.weight }
                    val avg = { ss: List<SetHistoryRow>, t: Double -> ss.filter { it.weight >= t - 1e-6 }.map { it.reps }.average() }
                    if (topB > topA + 1e-6 || (topB == topA && avg(b.sets, topB) > avg(a.sets, topA))) {
                        wrongDeloads++
                        println("Alívio errado em ${ex.getString("name")}: " + history.takeLast(5).joinToString(" ; ") { h ->
                            h.sets.joinToString(",") { "${it.weight}x${it.reps}" }
                        } + " → $s")
                    }
                }
            }
        }
        println("Sugestões no histórico real: $kinds")
        assertEquals("Alívios sugeridos depois de uma sessão melhor", 0, wrongDeloads)
    }
}
