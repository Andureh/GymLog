package com.gymlog.app.data

import androidx.room.withTransaction
import com.gymlog.app.i18n.Catalog
import org.json.JSONArray
import org.json.JSONObject

/** Cópia de segurança completa em JSON (exportar / importar). */
object Backup {
    private const val APP = "GymLog"
    private const val VERSION = 2

    suspend fun export(db: AppDatabase): String {
        val d = db.backupDao()
        val root = JSONObject()
        root.put("app", APP)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exercises", arr(d.exercises()) {
            JSONObject().put("id", it.id).put("name", it.name).put("muscle", it.muscle)
                .put("equipment", it.equipment).put("type", it.type.name).put("isCustom", it.isCustom)
                .put("nameKey", it.nameKey ?: JSONObject.NULL)
        })
        root.put("workouts", arr(d.workouts()) {
            JSONObject().put("id", it.id).put("name", it.name).put("startTime", it.startTime)
                .put("endTime", it.endTime ?: JSONObject.NULL).put("notes", it.notes)
        })
        root.put("workoutExercises", arr(d.workoutExercises()) {
            JSONObject().put("id", it.id).put("workoutId", it.workoutId).put("exerciseId", it.exerciseId)
                .put("position", it.position).put("restSeconds", it.restSeconds).put("notes", it.notes)
        })
        root.put("sets", arr(d.sets()) {
            JSONObject().put("id", it.id).put("workoutExerciseId", it.workoutExerciseId).put("position", it.position)
                .put("weight", it.weight).put("reps", it.reps).put("durationSec", it.durationSec)
                .put("type", it.type.name).put("completed", it.completed)
        })
        root.put("routines", arr(d.routines()) {
            JSONObject().put("id", it.id).put("name", it.name).put("notes", it.notes).put("createdAt", it.createdAt)
        })
        root.put("routineExercises", arr(d.routineExercises()) {
            JSONObject().put("id", it.id).put("routineId", it.routineId).put("exerciseId", it.exerciseId)
                .put("position", it.position).put("sets", it.sets).put("reps", it.reps)
                .put("weight", it.weight).put("restSeconds", it.restSeconds)
        })
        root.put("bodyWeights", arr(d.bodyWeights()) {
            JSONObject().put("id", it.id).put("date", it.date).put("weightKg", it.weightKg)
        })
        return root.toString()
    }

    /** Substitui todos os dados pelos da cópia. Lança exceção se o ficheiro for inválido. */
    suspend fun import(db: AppDatabase, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == APP) { "Not a GymLog backup" }

        val legacy = root.optInt("version", 1) < 2
        val exercises = objs(root, "exercises").map {
            val name = it.getString("name")
            val custom = it.optBoolean("isCustom")
            val muscle = it.getString("muscle")
            val equip = it.getString("equipment")
            Exercise(
                id = it.getLong("id"), name = name,
                muscle = if (legacy) Catalog.legacyMuscleKeys[muscle] ?: muscle else muscle,
                equipment = if (legacy) Catalog.legacyEquipmentKeys[equip] ?: equip else equip,
                type = ExerciseType.valueOf(it.getString("type")),
                isCustom = custom,
                nameKey = when {
                    !it.isNull("nameKey") && it.has("nameKey") -> it.getString("nameKey")
                    legacy && !custom -> Catalog.legacyExerciseKeys[name]
                    else -> null
                },
            )
        }
        val workouts = objs(root, "workouts").map {
            Workout(
                id = it.getLong("id"), name = it.getString("name"), startTime = it.getLong("startTime"),
                endTime = if (it.isNull("endTime")) null else it.getLong("endTime"), notes = it.optString("notes"),
            )
        }
        val wes = objs(root, "workoutExercises").map {
            WorkoutExercise(
                id = it.getLong("id"), workoutId = it.getLong("workoutId"), exerciseId = it.getLong("exerciseId"),
                position = it.getInt("position"), restSeconds = it.optInt("restSeconds", 90), notes = it.optString("notes"),
            )
        }
        val sets = objs(root, "sets").map {
            WorkoutSet(
                id = it.getLong("id"), workoutExerciseId = it.getLong("workoutExerciseId"), position = it.getInt("position"),
                weight = it.optDouble("weight", 0.0), reps = it.optInt("reps"), durationSec = it.optInt("durationSec"),
                type = SetType.valueOf(it.optString("type", "NORMAL")), completed = it.optBoolean("completed"),
            )
        }
        val routines = objs(root, "routines").map {
            Routine(id = it.getLong("id"), name = it.getString("name"), notes = it.optString("notes"), createdAt = it.optLong("createdAt"))
        }
        val res = objs(root, "routineExercises").map {
            RoutineExercise(
                id = it.getLong("id"), routineId = it.getLong("routineId"), exerciseId = it.getLong("exerciseId"),
                position = it.getInt("position"), sets = it.optInt("sets", 3), reps = it.optInt("reps", 10),
                weight = it.optDouble("weight", 0.0), restSeconds = it.optInt("restSeconds", 90),
            )
        }
        val bws = objs(root, "bodyWeights").map {
            BodyWeight(id = it.getLong("id"), date = it.getLong("date"), weightKg = it.getDouble("weightKg"))
        }

        val d = db.backupDao()
        db.withTransaction {
            d.clearSets(); d.clearWorkoutExercises(); d.clearWorkouts()
            d.clearRoutineExercises(); d.clearRoutines(); d.clearBodyWeights(); d.clearExercises()
            d.insertExercises(exercises)
            d.insertWorkouts(workouts)
            d.insertWorkoutExercises(wes)
            d.insertSets(sets)
            d.insertRoutines(routines)
            d.insertRoutineExercises(res)
            d.insertBodyWeights(bws)
        }
    }

    private fun <T> arr(items: List<T>, f: (T) -> JSONObject) = JSONArray().apply { items.forEach { put(f(it)) } }

    private fun objs(root: JSONObject, key: String): List<JSONObject> {
        val a = root.optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }
    }
}
