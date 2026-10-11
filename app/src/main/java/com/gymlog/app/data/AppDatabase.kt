package com.gymlog.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymlog.app.i18n.Catalog

@Database(
    entities = [
        Exercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class,
        Routine::class, RoutineExercise::class, BodyWeight::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** v1 guardava nomes, músculos e equipamento em português; v2 usa chaves traduzíveis. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN nameKey TEXT")
                Catalog.legacyExerciseKeys.forEach { (pt, key) ->
                    db.execSQL("UPDATE exercises SET nameKey = ? WHERE name = ? AND isCustom = 0", arrayOf(key, pt))
                }
                Catalog.legacyMuscleKeys.forEach { (pt, key) ->
                    db.execSQL("UPDATE exercises SET muscle = ? WHERE muscle = ?", arrayOf(key, pt))
                }
                Catalog.legacyEquipmentKeys.forEach { (pt, key) ->
                    db.execSQL("UPDATE exercises SET equipment = ? WHERE equipment = ?", arrayOf(key, pt))
                }
            }
        }
    }
}
