package com.gymlog.app.data

import androidx.room.withTransaction
import com.gymlog.app.i18n.Catalog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Importação do CSV exportado pelo Hevy (Definições → Export & Import Data → Export Workouts).
 * Colunas: title, start_time, end_time, description, exercise_title, superset_id, exercise_notes,
 * set_index, set_type, weight_kg, reps, distance_km, duration_seconds, rpe
 */
object HevyImport {

    class HSet(val type: SetType, val weightKg: Double, val reps: Int, val durationSec: Int)
    class HExercise(val title: String, val notes: String, val sets: List<HSet>)
    class HWorkout(val title: String, val start: Long, val end: Long, val description: String, val exercises: List<HExercise>)
    class Parsed(val workouts: List<HWorkout>, val bodyWeights: List<Pair<Long, Double>>) {
        val exerciseTitles: Set<String> get() = workouts.flatMap { w -> w.exercises.map { it.title } }.toSet()
        val setCount: Int get() = workouts.sumOf { w -> w.exercises.sumOf { it.sets.size } }
    }
    class Result(val imported: Int, val skipped: Int, val sets: Int, val newExercises: Int)

    // ---------- CSV ----------

    /** Parser CSV (RFC 4180): aspas, "" escapadas e quebras de linha dentro de aspas. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("﻿")
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf() }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    private val datePatterns = listOf("d MMM yyyy, HH:mm", "d MMM yyyy HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm", "d MMM yyyy", "yyyy-MM-dd")

    fun parseDate(v: String): Long? {
        val t = v.trim()
        if (t.isEmpty()) return null
        for (p in datePatterns) {
            try {
                val f = SimpleDateFormat(p, Locale.ENGLISH).apply { isLenient = false }
                return f.parse(t)?.time
            } catch (_: ParseException) {
            }
        }
        return null
    }

    private fun num(v: String?) = v?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

    private fun setType(v: String) = when (v.trim().lowercase()) {
        "warmup", "warm_up", "warm-up" -> SetType.WARMUP
        "dropset", "drop_set", "drop" -> SetType.DROP
        "failure" -> SetType.FAILURE
        else -> SetType.NORMAL
    }

    /** Lê o CSV de treinos do Hevy (ou de medições, se tiver colunas de data e peso). */
    fun parse(text: String): Parsed {
        val rows = parseCsv(text)
        require(rows.isNotEmpty()) { "empty" }
        val h = rows.first().map { it.trim().lowercase() }
        fun col(name: String) = h.indexOf(name)
        fun List<String>.get(name: String): String = col(name).let { if (it >= 0 && it < size) this[it] else "" }

        if (col("exercise_title") < 0) {
            // Possível exportação de medições: data + peso
            val dateCol = h.indexOfFirst { it == "date" || it.contains("date") }
            val wCol = h.indexOfFirst { it == "weight_kg" || it.startsWith("weight") }
            require(dateCol >= 0 && wCol >= 0) { "not a Hevy export" }
            val bws = rows.drop(1).mapNotNull { r ->
                val d = parseDate(r.getOrElse(dateCol) { "" }) ?: return@mapNotNull null
                val w = num(r.getOrNull(wCol))
                if (w > 0) d to w else null
            }
            return Parsed(emptyList(), bws)
        }

        val workouts = rows.drop(1)
            .groupBy { Triple(it.get("title"), it.get("start_time"), it.get("end_time")) }
            .mapNotNull { (key, list) ->
                val start = parseDate(key.second) ?: return@mapNotNull null
                val end = parseDate(key.third) ?: start
                // Exercícios pela ordem em que aparecem (o mesmo exercício pode repetir-se separado)
                val exercises = mutableListOf<HExercise>()
                var current: String? = null
                var buf = mutableListOf<HSet>()
                var notes = ""
                fun flush() {
                    current?.let { if (buf.isNotEmpty()) exercises.add(HExercise(it, notes, buf)) }
                }
                list.forEach { r ->
                    val title = r.get("exercise_title").trim()
                    if (title != current) {
                        flush(); current = title; buf = mutableListOf(); notes = r.get("exercise_notes")
                    }
                    buf.add(HSet(setType(r.get("set_type")), num(r.get("weight_kg")), num(r.get("reps")).toInt(), num(r.get("duration_seconds")).toInt()))
                }
                flush()
                HWorkout(key.first.ifBlank { "Hevy" }, start, end, list.first().get("description"), exercises)
            }
            .sortedBy { it.start }
        return Parsed(workouts, emptyList())
    }

    // ---------- Associação de exercícios ----------

    private fun norm(s: String) = s.lowercase()
        .replace("smith machine", "smith")
        .replace("biceps", "bicep")
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Nomes do Hevy que correspondem a exercícios do catálogo com nome diferente. */
    private val aliases = mapOf(
        "Back Extension (Weighted Hyperextension)" to "hyperextension",
        "Back Extension" to "hyperextension",
        "Calf Press (Machine)" to "calf_press",
        "Chest Fly (Machine)" to "pec_deck",
        "Butterfly (Pec Deck)" to "pec_deck",
        "Chest Fly (Dumbbell)" to "fly_db",
        "Cable Fly Crossovers" to "crossover_cable",
        "EZ Bar Biceps Curl" to "curl_ez",
        "Hack Squat (Machine)" to "hack_squat",
        "Leg Extension (Machine)" to "leg_extension",
        "Leg Press (Machine)" to "leg_press",
        "Lying Leg Curl (Machine)" to "lying_leg_curl",
        "Seated Leg Curl (Machine)" to "seated_leg_curl",
        "Rear Delt Reverse Fly (Machine)" to "reverse_pec_deck",
        "Rear Delt Reverse Fly (Dumbbell)" to "rear_delt_fly_db",
        "Seated Cable Row - V Grip (Cable)" to "seated_row_cable",
        "Seated Cable Row - Bar Grip" to "seated_row_cable",
        "Seated Calf Raise" to "seated_calf_raise",
        "Standing Calf Raise (Machine)" to "standing_calf_raise",
        "Seated Shoulder Press (Machine)" to "shoulder_press_machine",
        "Shoulder Press (Machine Plates)" to "shoulder_press_machine",
        "Straight Arm Lat Pulldown (Cable)" to "pullover_cable",
        "Triceps Pushdown" to "bar_pushdown",
        "Triceps Dip" to "dips",
        "Pull Up" to "pull_up",
        "Pull Up (Weighted)" to "weighted_pull_up",
        "Pull Up (Assisted)" to "assisted_pull_up",
        "Chin Up" to "chin_up",
        "Push Up" to "push_up",
        "Dumbbell Row" to "one_arm_row_db",
        "T Bar Row" to "t_bar_row",
        "Face Pull" to "face_pull",
        "Preacher Curl (Barbell)" to "preacher_curl",
        "Preacher Curl (Machine)" to "curl_machine",
        "Skullcrusher (Barbell)" to "skull_crusher",
        "Bulgarian Split Squat" to "bulgarian_split_squat",
        "Romanian Deadlift (Dumbbell)" to "rdl_db",
        "Straight Leg Deadlift" to "stiff_leg_deadlift",
        "Sumo Deadlift" to "sumo_deadlift",
        "Front Squat" to "front_squat",
        "Hip Abduction (Machine)" to "hip_abduction",
        "Hip Adduction (Machine)" to "hip_adduction",
        "Cable Crunch" to "cable_crunch",
        "Elliptical Trainer" to "elliptical",
        "Stair Machine" to "stair_climber",
        "Spinning" to "stationary_bike",
        "Upright Row (Barbell)" to "upright_row_bb",
        "Shrug (Dumbbell)" to "shrug_db",
        "Shrug (Barbell)" to "shrug_bb",
        "Wrist Curl (Barbell)" to "wrist_curl",
        "Kettlebell Swing" to "kb_swing",
    ).mapKeys { norm(it.key) }

    private val englishNames: Map<String, String> by lazy {
        Catalog.exercises.associate { norm(it.names[1]) to it.key }
    }

    /** Chave do catálogo correspondente a um nome do Hevy, ou null. */
    fun matchKey(hevyTitle: String): String? = norm(hevyTitle).let { englishNames[it] ?: aliases[it] }

    /** Grupo muscular, equipamento e tipo prováveis para exercícios sem correspondência. */
    fun guess(title: String, sets: List<HSet>): Triple<String, String, ExerciseType> {
        val t = title.lowercase()
        fun has(vararg w: String) = w.any { t.contains(it) }
        val muscle = when {
            has("walking", "cycling", "running", "treadmill", "bike", "rowing machine", "elliptical", "stair", "jump rope", "swim", "hiking") -> "cardio"
            has("calf") -> "calves"
            has("back extension", "hyperextension", "good morning") -> "lower_back"
            has("leg curl", "romanian", "stiff leg", "nordic") -> "hamstrings"
            has("hip thrust", "glute", "abduct", "kickback (cable)") -> "glutes"
            has("adduct") -> "adductors"
            has("squat", "leg press", "leg extension", "lunge", "step up", "split squat") -> "quads"
            has("tricep", "pushdown", "skull", "dip") -> "triceps"
            has("wrist") -> "forearms"
            has("shrug") -> "traps"
            has("curl") -> "biceps"
            has("lateral raise", "shoulder", "rear delt", "front raise", "face pull", "overhead press", "arnold", "military") -> "shoulders"
            has("crunch", "plank", "leg raise", "sit up", "twist", "ab wheel", "knee raise") -> "abs"
            has("row", "pulldown", "pull up", "chin up", "lat ", "pullover", "deadlift") -> "back"
            has("bench", "chest", "fly", "pec", "push up", "press") -> "chest"
            else -> "full_body"
        }
        val noWeight = sets.all { it.weightKg <= 0 }
        val type = when {
            sets.all { it.durationSec > 0 && it.reps == 0 } -> ExerciseType.DURATION
            noWeight -> ExerciseType.REPS_ONLY
            else -> ExerciseType.WEIGHT_REPS
        }
        val equipment = when {
            has("smith") -> "smith"
            has("barbell", "t bar") -> "barbell"
            has("dumbbell") -> "dumbbell"
            has("cable") -> "cable"
            has("kettlebell") -> "kettlebell"
            has("ez bar") -> "ez_bar"
            has("band") -> "band"
            has("machine") -> "machine"
            muscle == "cardio" -> "none"
            noWeight -> "bodyweight"
            else -> "machine"
        }
        return Triple(muscle, equipment, type)
    }

    // ---------- Importação ----------

    suspend fun import(db: AppDatabase, data: Parsed, defaultRest: Int): Result {
        val d = db.backupDao()
        var imported = 0
        var skipped = 0
        var sets = 0
        var created = 0
        db.withTransaction {
            val exercises = d.exercises().toMutableList()
            val existing = d.workouts().map { it.startTime to it.name }.toSet()
            val cache = HashMap<String, Long>()

            suspend fun exerciseId(title: String, hsets: List<HSet>): Long = cache.getOrPut(title) {
                val key = matchKey(title)
                key?.let { k -> exercises.find { it.nameKey == k } }?.let { return@getOrPut it.id }
                exercises.find { it.nameKey == null && it.name.equals(title, ignoreCase = true) }?.let { return@getOrPut it.id }
                val (muscle, equip, type) = guess(title, hsets)
                val e = Exercise(name = title, muscle = muscle, equipment = equip, type = type, isCustom = true)
                val id = db.exerciseDao().insert(e)
                exercises.add(e.copy(id = id))
                created++
                id
            }

            val wDao = db.workoutDao()
            for (w in data.workouts) {
                if ((w.start to w.title) in existing) { skipped++; continue }
                val wid = wDao.insertWorkout(Workout(name = w.title, startTime = w.start, endTime = maxOf(w.end, w.start), notes = w.description))
                w.exercises.forEachIndexed { pos, e ->
                    val weId = wDao.insertWe(
                        WorkoutExercise(workoutId = wid, exerciseId = exerciseId(e.title, e.sets), position = pos, restSeconds = defaultRest, notes = e.notes)
                    )
                    e.sets.forEachIndexed { i, s ->
                        wDao.insertSet(
                            WorkoutSet(
                                workoutExerciseId = weId, position = i, weight = s.weightKg, reps = s.reps,
                                durationSec = s.durationSec, type = s.type, completed = true,
                            )
                        )
                        sets++
                    }
                }
                imported++
            }
            val bwDao = db.bodyWeightDao()
            val existingBw = d.bodyWeights().map { it.date }.toSet()
            data.bodyWeights.filter { it.first !in existingBw }.forEach { (date, kg) -> bwDao.insert(BodyWeight(date = date, weightKg = kg)) }
        }
        return Result(imported, skipped, sets, created)
    }
}
