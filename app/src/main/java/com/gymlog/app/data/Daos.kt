package com.gymlog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun byId(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises")
    suspend fun allOnce(): List<Exercise>

    @Insert
    suspend fun insert(e: Exercise): Long

    @Insert
    suspend fun insertAll(list: List<Exercise>)

    @Update
    suspend fun update(e: Exercise)

    @Delete
    suspend fun delete(e: Exercise)

    @Query(
        """SELECT we.exerciseId AS exerciseId, COUNT(*) AS cnt FROM workout_exercises we
           JOIN workouts w ON we.workoutId = w.id WHERE w.endTime IS NOT NULL GROUP BY we.exerciseId"""
    )
    fun usageCounts(): Flow<List<UsageCount>>
}

private const val HISTORY_QUERY = """
    SELECT w.id AS workoutId, w.name AS workoutName, w.startTime AS startTime, s.id AS setId,
           s.weight AS weight, s.reps AS reps, s.durationSec AS durationSec, s.type AS type
    FROM sets s
    JOIN workout_exercises we ON s.workoutExerciseId = we.id
    JOIN workouts w ON we.workoutId = w.id
    WHERE we.exerciseId = :exerciseId AND w.endTime IS NOT NULL AND s.completed = 1
    ORDER BY w.startTime ASC, we.position ASC, s.position ASC"""

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun active(): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun activeFull(): Flow<WorkoutFull?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun full(id: Long): Flow<WorkoutFull?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun fullOnce(id: Long): WorkoutFull?

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workoutOnce(id: Long): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun finishedFull(): Flow<List<WorkoutFull>>

    @Insert
    suspend fun insertWorkout(w: Workout): Long

    @Query("UPDATE workouts SET name = :name WHERE id = :id")
    suspend fun setName(id: Long, name: String)

    @Query("UPDATE workouts SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String)

    @Query("UPDATE workouts SET routineId = :routineId WHERE id IN (:ids)")
    suspend fun setRoutine(ids: List<Long>, routineId: Long?)

    @Query("UPDATE workouts SET endTime = :end WHERE id = :id")
    suspend fun setEnd(id: Long, end: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Insert
    suspend fun insertWe(we: WorkoutExercise): Long

    @Update
    suspend fun updateWe(we: WorkoutExercise)

    @Query("UPDATE workout_exercises SET restSeconds = :sec WHERE id = :id")
    suspend fun setRest(id: Long, sec: Int)

    @Query("UPDATE workout_exercises SET notes = :notes WHERE id = :id")
    suspend fun setWeNotes(id: Long, notes: String)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWe(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :wid ORDER BY position")
    suspend fun wesOf(wid: Long): List<WorkoutExercise>

    @Insert
    suspend fun insertSet(s: WorkoutSet): Long

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :weId ORDER BY position")
    suspend fun setsOf(weId: Long): List<WorkoutSet>

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("UPDATE sets SET weight = :kg WHERE id = :id")
    suspend fun setWeight(id: Long, kg: Double)

    @Query("UPDATE sets SET reps = :reps WHERE id = :id")
    suspend fun setReps(id: Long, reps: Int)

    @Query("UPDATE sets SET durationSec = :sec WHERE id = :id")
    suspend fun setDuration(id: Long, sec: Int)

    @Query("UPDATE sets SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: SetType)

    @Query("UPDATE sets SET completed = :done WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean)

    @Query("UPDATE sets SET weight = :kg, reps = :reps, durationSec = :sec, completed = 1 WHERE id = :id")
    suspend fun complete(id: Long, kg: Double, reps: Int, sec: Int)

    @Query("UPDATE sets SET weight = :kg, reps = :reps, durationSec = :sec WHERE workoutExerciseId = :weId AND completed = 0 AND type != 'WARMUP'")
    suspend fun applyToPending(weId: Long, kg: Double, reps: Int, sec: Int)

    @Query("DELETE FROM sets WHERE completed = 0 AND workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :wid)")
    suspend fun deleteIncompleteSets(wid: Long)

    @Query(
        """DELETE FROM workout_exercises WHERE workoutId = :wid
           AND NOT EXISTS (SELECT 1 FROM sets WHERE sets.workoutExerciseId = workout_exercises.id)"""
    )
    suspend fun deleteEmptyExercises(wid: Long)

    /** Séries da última vez (antes de [before]) que este exercício foi feito. */
    @Query(
        """SELECT * FROM sets WHERE workoutExerciseId = (
              SELECT we.id FROM workout_exercises we JOIN workouts w ON we.workoutId = w.id
              WHERE we.exerciseId = :exerciseId AND w.endTime IS NOT NULL AND w.startTime < :before
              ORDER BY w.startTime DESC, we.position ASC LIMIT 1)
           ORDER BY position"""
    )
    suspend fun previousSets(exerciseId: Long, before: Long): List<WorkoutSet>

    @Query(HISTORY_QUERY)
    fun history(exerciseId: Long): Flow<List<SetHistoryRow>>

    @Query(HISTORY_QUERY)
    suspend fun historyOnce(exerciseId: Long): List<SetHistoryRow>
}

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun allFull(): Flow<List<RoutineFull>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun fullOnce(id: Long): RoutineFull?

    @Insert
    suspend fun insert(r: Routine): Long

    @Update
    suspend fun update(r: Routine)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM routine_exercises WHERE routineId = :rid")
    suspend fun clearExercises(rid: Long)

    @Insert
    suspend fun insertExercises(list: List<RoutineExercise>)
}

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weights ORDER BY date DESC")
    fun all(): Flow<List<BodyWeight>>

    @Insert
    suspend fun insert(b: BodyWeight): Long

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM exercises") suspend fun exercises(): List<Exercise>
    @Query("SELECT * FROM workouts") suspend fun workouts(): List<Workout>
    @Query("SELECT * FROM workout_exercises") suspend fun workoutExercises(): List<WorkoutExercise>
    @Query("SELECT * FROM sets") suspend fun sets(): List<WorkoutSet>
    @Query("SELECT * FROM routines") suspend fun routines(): List<Routine>
    @Query("SELECT * FROM routine_exercises") suspend fun routineExercises(): List<RoutineExercise>
    @Query("SELECT * FROM body_weights") suspend fun bodyWeights(): List<BodyWeight>

    @Insert suspend fun insertExercises(l: List<Exercise>)
    @Insert suspend fun insertWorkouts(l: List<Workout>)
    @Insert suspend fun insertWorkoutExercises(l: List<WorkoutExercise>)
    @Insert suspend fun insertSets(l: List<WorkoutSet>)
    @Insert suspend fun insertRoutines(l: List<Routine>)
    @Insert suspend fun insertRoutineExercises(l: List<RoutineExercise>)
    @Insert suspend fun insertBodyWeights(l: List<BodyWeight>)

    @Query("DELETE FROM sets") suspend fun clearSets()
    @Query("DELETE FROM workout_exercises") suspend fun clearWorkoutExercises()
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM routine_exercises") suspend fun clearRoutineExercises()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM body_weights") suspend fun clearBodyWeights()
}
