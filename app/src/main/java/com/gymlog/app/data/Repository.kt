package com.gymlog.app.data

import androidx.room.withTransaction
import com.gymlog.app.domain.Bests
import com.gymlog.app.domain.Session
import com.gymlog.app.domain.buildSessions
import com.gymlog.app.i18n.Catalog
import com.gymlog.app.i18n.I18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class Repository(private val db: AppDatabase, private val settings: Settings) {
    private val exDao = db.exerciseDao()
    private val wDao = db.workoutDao()
    private val rDao = db.routineDao()
    private val bwDao = db.bodyWeightDao()

    // ---------- Exercícios ----------

    fun exercises(): Flow<List<Exercise>> = exDao.all()
    fun exercise(id: Long): Flow<Exercise?> = exDao.byId(id)
    fun usageCounts(): Flow<Map<Long, Int>> = exDao.usageCounts().map { l -> l.associate { it.exerciseId to it.cnt } }
    suspend fun addExercise(e: Exercise): Long = exDao.insert(e)
    suspend fun updateExercise(e: Exercise) = exDao.update(e)
    suspend fun deleteExercise(e: Exercise) = exDao.delete(e)

    /**
     * Acrescenta à base de dados os exercícios do catálogo que ainda não existem (instalação nova ou
     * catálogo atualizado). Se já houver um exercício personalizado com o mesmo nome (ex.: importado do
     * Hevy), converte-o no exercício do catálogo, mantendo o histórico.
     */
    suspend fun syncCatalog(lastVersion: Int): Int {
        if (lastVersion >= Catalog.VERSION) return lastVersion
        val all = exDao.allOnce()
        val keys = all.mapNotNull { it.nameKey }.toSet()
        for (item in Catalog.exercises.filter { it.key !in keys }) {
            val names = item.names.map { it.lowercase() }.toSet()
            val custom = all.find { it.nameKey == null && (it.name.lowercase() in names || HevyImport.matchKey(it.name) == item.key) }
            if (custom != null) {
                exDao.update(custom.copy(nameKey = item.key, isCustom = false, muscle = item.muscle, equipment = item.equipment, type = item.type))
            } else {
                exDao.insert(Exercise(name = item.names[1], muscle = item.muscle, equipment = item.equipment, type = item.type, nameKey = item.key))
            }
        }
        return Catalog.VERSION
    }

    fun exerciseHistory(exerciseId: Long): Flow<List<SetHistoryRow>> = wDao.history(exerciseId)

    suspend fun bestsBefore(exerciseId: Long, before: Long): Bests =
        wDao.historyOnce(exerciseId)
            .filter { it.startTime < before && it.type != SetType.WARMUP }
            .fold(Bests()) { b, s -> b.add(s.weight, s.reps, s.durationSec) }

    /** Sessões anteriores a [before], por ordem cronológica. */
    suspend fun sessionsBefore(exerciseId: Long, before: Long): List<Session> =
        buildSessions(wDao.historyOnce(exerciseId).filter { it.startTime < before })

    /** Preenche as séries por concluir (exceto aquecimento) com os valores sugeridos. */
    suspend fun applySuggestion(weId: Long, kg: Double, reps: Int, sec: Int) = wDao.applyToPending(weId, kg, reps, sec)

    suspend fun previousSets(exerciseId: Long, before: Long): List<WorkoutSet> =
        wDao.previousSets(exerciseId, before)

    // ---------- Treinos ----------

    fun activeWorkout(): Flow<WorkoutFull?> = wDao.activeFull().map { it?.sorted() }
    suspend fun activeWorkoutOnce(): Workout? = wDao.active()
    fun workout(id: Long): Flow<WorkoutFull?> = wDao.full(id).map { it?.sorted() }
    fun finishedWorkouts(): Flow<List<WorkoutFull>> = wDao.finishedFull().map { l -> l.map { it.sorted() } }

    private fun defaultName(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> I18n.current.morningWorkout
        in 12..18 -> I18n.current.afternoonWorkout
        else -> I18n.current.eveningWorkout
    }

    suspend fun startEmptyWorkout(): Long =
        wDao.insertWorkout(Workout(name = defaultName(), startTime = System.currentTimeMillis()))

    suspend fun startFromRoutine(routineId: Long): Long = db.withTransaction {
        val r = rDao.fullOnce(routineId)?.sorted() ?: return@withTransaction startEmptyWorkout()
        val wid = wDao.insertWorkout(Workout(name = r.routine.name, startTime = System.currentTimeMillis()))
        r.exercises.forEachIndexed { i, item ->
            val re = item.re
            val weId = wDao.insertWe(
                WorkoutExercise(workoutId = wid, exerciseId = re.exerciseId, position = i, restSeconds = re.restSeconds)
            )
            val isDuration = item.exercise.type == ExerciseType.DURATION
            repeat(re.sets.coerceAtLeast(1)) { k ->
                wDao.insertSet(
                    WorkoutSet(
                        workoutExerciseId = weId, position = k,
                        weight = re.weight,
                        reps = if (isDuration) 0 else re.reps,
                        durationSec = if (isDuration) re.reps else 0,
                    )
                )
            }
        }
        wid
    }

    suspend fun repeatWorkout(workoutId: Long): Long = db.withTransaction {
        val src = wDao.fullOnce(workoutId)?.sorted() ?: return@withTransaction startEmptyWorkout()
        val wid = wDao.insertWorkout(Workout(name = src.workout.name, startTime = System.currentTimeMillis()))
        src.exercises.forEachIndexed { i, e ->
            val weId = wDao.insertWe(
                WorkoutExercise(workoutId = wid, exerciseId = e.exercise.id, position = i, restSeconds = e.we.restSeconds)
            )
            e.sets.forEachIndexed { k, s ->
                wDao.insertSet(
                    WorkoutSet(
                        workoutExerciseId = weId, position = k, weight = s.weight, reps = s.reps,
                        durationSec = s.durationSec, type = s.type,
                    )
                )
            }
        }
        wid
    }

    suspend fun addExercises(workoutId: Long, exerciseIds: List<Long>) = db.withTransaction {
        val start = wDao.workoutOnce(workoutId)?.startTime ?: System.currentTimeMillis()
        var pos = (wDao.wesOf(workoutId).maxOfOrNull { it.position } ?: -1) + 1
        for (exId in exerciseIds) {
            val weId = wDao.insertWe(
                WorkoutExercise(
                    workoutId = workoutId, exerciseId = exId, position = pos++,
                    restSeconds = settings.current.defaultRest,
                )
            )
            // Mesmo número de séries que da última vez (valores aparecem como sugestão)
            val count = wDao.previousSets(exId, start).size.coerceIn(1, 10)
            repeat(count) { k -> wDao.insertSet(WorkoutSet(workoutExerciseId = weId, position = k)) }
        }
    }

    suspend fun addSet(weId: Long) {
        val last = wDao.setsOf(weId).lastOrNull()
        wDao.insertSet(
            WorkoutSet(
                workoutExerciseId = weId,
                position = (last?.position ?: -1) + 1,
                weight = last?.weight ?: 0.0,
                reps = last?.reps ?: 0,
                durationSec = last?.durationSec ?: 0,
            )
        )
    }

    suspend fun deleteSet(id: Long) = wDao.deleteSet(id)
    suspend fun setWeight(id: Long, kg: Double) = wDao.setWeight(id, kg)
    suspend fun setReps(id: Long, reps: Int) = wDao.setReps(id, reps)
    suspend fun setDuration(id: Long, sec: Int) = wDao.setDuration(id, sec)
    suspend fun setSetType(id: Long, type: SetType) = wDao.setType(id, type)
    suspend fun setCompleted(id: Long, done: Boolean) = wDao.setCompleted(id, done)
    suspend fun completeSet(id: Long, kg: Double, reps: Int, sec: Int) = wDao.complete(id, kg, reps, sec)
    suspend fun setRest(weId: Long, sec: Int) = wDao.setRest(weId, sec)
    suspend fun setExerciseNotes(weId: Long, notes: String) = wDao.setWeNotes(weId, notes)
    suspend fun removeExercise(weId: Long) = wDao.deleteWe(weId)
    suspend fun renameWorkout(id: Long, name: String) = wDao.setName(id, name)
    suspend fun setWorkoutNotes(id: Long, notes: String) = wDao.setNotes(id, notes)
    suspend fun deleteWorkout(id: Long) = wDao.deleteWorkout(id)

    suspend fun moveExercise(workoutId: Long, weId: Long, dir: Int) = db.withTransaction {
        val list = wDao.wesOf(workoutId).toMutableList()
        val i = list.indexOfFirst { it.id == weId }
        val j = i + dir
        if (i < 0 || j !in list.indices) return@withTransaction
        java.util.Collections.swap(list, i, j)
        list.forEachIndexed { k, we -> if (we.position != k) wDao.updateWe(we.copy(position = k)) }
    }

    /**
     * Remove séries por concluir e exercícios vazios. Se [setEnd], marca o treino como terminado.
     * Devolve false se o treino ficou vazio (e foi eliminado).
     */
    suspend fun finishWorkout(id: Long, setEnd: Boolean): Boolean = db.withTransaction {
        wDao.deleteIncompleteSets(id)
        wDao.deleteEmptyExercises(id)
        if (wDao.wesOf(id).isEmpty()) {
            wDao.deleteWorkout(id)
            false
        } else {
            if (setEnd) wDao.setEnd(id, System.currentTimeMillis())
            true
        }
    }

    // ---------- Rotinas ----------

    fun routines(): Flow<List<RoutineFull>> = rDao.allFull().map { l -> l.map { it.sorted() } }
    suspend fun routineOnce(id: Long): RoutineFull? = rDao.fullOnce(id)?.sorted()
    suspend fun deleteRoutine(id: Long) = rDao.delete(id)

    suspend fun saveRoutine(id: Long, name: String, notes: String, items: List<RoutineExercise>): Long =
        db.withTransaction {
            val rid = if (id == 0L) {
                rDao.insert(Routine(name = name, notes = notes))
            } else {
                val old = rDao.fullOnce(id)?.routine
                rDao.update(Routine(id = id, name = name, notes = notes, createdAt = old?.createdAt ?: System.currentTimeMillis()))
                id
            }
            rDao.clearExercises(rid)
            rDao.insertExercises(items.mapIndexed { i, it -> it.copy(id = 0, routineId = rid, position = i) })
            rid
        }

    suspend fun saveWorkoutAsRoutine(workoutId: Long): Long? {
        val w = wDao.fullOnce(workoutId)?.sorted() ?: return null
        val items = w.exercises.map { e ->
            val working = e.sets.filter { it.type != SetType.WARMUP }.ifEmpty { e.sets }
            val first = working.firstOrNull()
            RoutineExercise(
                routineId = 0, exerciseId = e.exercise.id, position = 0,
                sets = working.size.coerceAtLeast(1),
                reps = if (e.exercise.type == ExerciseType.DURATION) first?.durationSec ?: 0 else first?.reps ?: 0,
                weight = working.maxOfOrNull { it.weight } ?: 0.0,
                restSeconds = e.we.restSeconds,
            )
        }
        return saveRoutine(0, w.workout.name, "", items)
    }

    // ---------- Peso corporal ----------

    fun bodyWeights(): Flow<List<BodyWeight>> = bwDao.all()
    suspend fun addBodyWeight(date: Long, kg: Double) = bwDao.insert(BodyWeight(date = date, weightKg = kg))
    suspend fun deleteBodyWeight(id: Long) = bwDao.delete(id)
}
