package com.gymlog.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ExerciseType { WEIGHT_REPS, REPS_ONLY, DURATION }

enum class SetType { NORMAL, WARMUP, DROP, FAILURE }

/**
 * [muscle] e [equipment] são chaves do Catalog (ex.: "chest", "barbell").
 * [nameKey] identifica os exercícios incluídos (nome traduzido); null = nome livre em [name].
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscle: String,
    val equipment: String,
    val type: ExerciseType = ExerciseType.WEIGHT_REPS,
    val isCustom: Boolean = false,
    val nameKey: String? = null,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(Routine::class, ["id"], ["routineId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("startTime"), Index("routineId")],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTime: Long,
    /** null = treino em curso */
    val endTime: Long? = null,
    val notes: String = "",
    /**
     * Rotina a que o treino pertence (definida ao iniciar a partir de uma rotina, ou escolhida à mão).
     * null = sem ligação; os gráficos por rotina detetam-na pelos exercícios.
     */
    val routineId: Long? = null,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(Workout::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val restSeconds: Int = 90,
    val notes: String = "",
)

/** Pesos guardados sempre em kg; durações em segundos. */
@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(WorkoutExercise::class, ["id"], ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val durationSec: Int = 0,
    val type: SetType = SetType.NORMAL,
    val completed: Boolean = false,
)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Para exercícios de duração, [reps] guarda os segundos alvo. */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(Routine::class, ["id"], ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Double = 0.0,
    val restSeconds: Int = 90,
)

@Entity(tableName = "body_weights", indices = [Index("date")])
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Double,
)

// ---------- Relações ----------

data class WorkoutExerciseFull(
    @Embedded val we: WorkoutExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId") val sets: List<WorkoutSet>,
)

data class WorkoutFull(
    @Embedded val workout: Workout,
    @Relation(entity = WorkoutExercise::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseFull>,
)

fun WorkoutFull.sorted() = copy(
    exercises = exercises.sortedBy { it.we.position }.map { e -> e.copy(sets = e.sets.sortedBy { it.position }) }
)

data class RoutineExerciseFull(
    @Embedded val re: RoutineExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise,
)

data class RoutineFull(
    @Embedded val routine: Routine,
    @Relation(entity = RoutineExercise::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseFull>,
)

fun RoutineFull.sorted() = copy(exercises = exercises.sortedBy { it.re.position })

/** Uma série concluída de um exercício, num treino terminado. */
data class SetHistoryRow(
    val workoutId: Long,
    val workoutName: String,
    val startTime: Long,
    val setId: Long,
    val weight: Double,
    val reps: Int,
    val durationSec: Int,
    val type: SetType,
)

data class UsageCount(val exerciseId: Long, val cnt: Int)
