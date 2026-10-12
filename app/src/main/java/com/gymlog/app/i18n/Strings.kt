package com.gymlog.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.SetType
import com.gymlog.app.domain.PrType
import java.util.Locale

/**
 * Todos os textos da app. Cada língua é um `object` que implementa esta classe;
 * se faltar uma tradução, o projeto não compila.
 */
abstract class Strings {
    abstract val code: String
    abstract val nativeName: String
    abstract val locale: Locale
    /** Coluna desta língua no [Catalog]. */
    abstract val col: Int

    // ---------- Geral ----------
    abstract val back: String
    abstract val options: String
    abstract val edit: String
    abstract val delete: String
    abstract val save: String
    abstract val cancel: String
    abstract val close: String
    abstract val remove: String
    abstract val open: String
    abstract val clear: String
    abstract val more: String
    abstract val less: String
    abstract val moveUp: String
    abstract val moveDown: String
    abstract val noData: String

    // ---------- Navegação ----------
    abstract val tabWorkout: String
    abstract val tabHistory: String
    abstract val tabExercises: String
    abstract val tabProfile: String

    // ---------- Nomes automáticos de treinos ----------
    abstract val morningWorkout: String
    abstract val afternoonWorkout: String
    abstract val eveningWorkout: String

    // ---------- Descanso ----------
    abstract val rest: String
    abstract val restTime: String
    abstract val restOff: String
    abstract val skip: String
    abstract val restChannelName: String
    abstract val restChannelDesc: String
    abstract val restOverTitle: String
    abstract val restOverText: String
    fun restValue(v: String) = "$rest: $v"

    // ---------- Início ----------
    abstract val workoutInProgress: String
    abstract val activeWorkoutText: String
    abstract val resume: String
    abstract val discardAndStart: String
    abstract val thisWeek: String
    abstract val workouts: String
    abstract val volume: String
    abstract val total: String
    abstract val quickStart: String
    abstract val resumeWorkout: String
    abstract val startEmptyWorkout: String
    abstract val routines: String
    abstract val newRoutine: String
    abstract val firstRoutineTitle: String
    abstract val firstRoutineText: String
    abstract val deleteRoutineTitle: String
    abstract fun deleteRoutineText(name: String): String
    abstract val noExercises: String
    abstract val startRoutine: String

    // ---------- Treino ----------
    abstract val editWorkout: String
    abstract val finish: String
    abstract val enterDuration: String
    abstract val enterReps: String
    abstract val startByAdding: String
    abstract val addExercise: String
    abstract val discardWorkout: String
    abstract val discardWorkoutTitle: String
    abstract val discardWorkoutText: String
    abstract val discard: String
    abstract val finishWorkoutTitle: String
    abstract val saveChangesTitle: String
    abstract val noCompletedSets: String
    abstract fun incompleteSets(n: Int): String
    abstract val goodJob: String
    abstract val changesWillBeSaved: String
    abstract val keepGoing: String
    abstract val newPr: String
    abstract val nice: String
    abstract val duration: String
    abstract val sets: String
    abstract val workoutNotesHint: String
    abstract val exerciseNoteHint: String
    abstract val removeExercise: String
    abstract val removeExerciseTitle: String
    abstract fun removeExerciseText(name: String): String
    abstract val colSet: String
    abstract val colPrevious: String
    abstract val colReps: String
    abstract val colTime: String
    abstract val colWeight: String
    abstract val colDate: String
    abstract val colWeightReps: String
    abstract val colSets: String
    abstract val addSet: String
    abstract val record: String
    abstract val deleteSet: String
    abstract val markDone: String
    abstract val done: String
    abstract fun reps(n: Int): String
    /** Abreviatura de série de aquecimento (PT "A", EN "W"…). */
    abstract val warmupShort: String

    // ---------- Exercícios ----------
    abstract val addExercises: String
    abstract val createExercise: String
    abstract fun addNExercises(n: Int): String
    abstract val searchExercise: String
    abstract val all: String
    abstract val custom: String
    abstract val noExerciseFound: String
    abstract fun createNamed(name: String): String
    abstract val newExercise: String
    abstract val editExercise: String
    abstract val name: String
    abstract val muscleGroup: String
    abstract val equipmentLabel: String
    abstract val trackingType: String
    abstract val typeWeightReps: String
    abstract val typeRepsOnly: String
    abstract val typeDuration: String
    abstract val setNormal: String
    abstract val setWarmup: String
    abstract val setDrop: String
    abstract val setFailure: String

    // ---------- Detalhe do exercício ----------
    abstract val noHistoryTitle: String
    abstract val noHistoryText: String
    abstract val personalRecords: String
    abstract val bestWeightByReps: String
    abstract fun historySessions(n: Int): String
    abstract val deleteExerciseTitle: String
    abstract fun deleteExerciseText(name: String): String
    abstract val maxWeight: String
    abstract val est1rm: String
    abstract val bestSet: String
    abstract val sessionVolume: String
    abstract val bestSessionVolume: String
    abstract val maxReps: String
    abstract val mostRepsSet: String
    abstract val mostRepsSession: String
    abstract val totalReps: String
    abstract val longestSet: String
    abstract val longestSession: String
    abstract val totalDuration: String
    abstract val sessionsLogged: String
    abstract val rangeAll: String
    /** Abreviaturas de mês e ano para os filtros (1M, 3M, 1A / 1Y…). */
    abstract val monthAbbr: String
    abstract val yearAbbr: String

    // ---------- Histórico ----------
    abstract val noWorkoutsTitle: String
    abstract val noWorkoutsText: String
    abstract fun monthCount(month: String, n: Int): String
    abstract fun moreExercises(n: Int): String
    abstract fun prCount(n: Int): String
    abstract val repeatWorkout: String
    abstract val saveAsRoutine: String
    abstract val routineCreated: String
    abstract val deleteWorkout: String
    abstract val deleteWorkoutTitle: String
    abstract val deleteWorkoutText: String
    abstract val workoutDone: String
    abstract fun workoutNumber(n: Int): String
    abstract fun prsBeaten(n: Int): String
    abstract val prsShort: String

    // ---------- Rotinas ----------
    abstract val editRoutine: String
    abstract val routineNameRequired: String
    abstract val routineName: String
    abstract val routineNameHint: String
    abstract val optionalShort: String

    // ---------- Perfil ----------
    abstract val totalTime: String
    abstract val totalVolume: String
    abstract val streak: String
    abstract fun weeks(n: Int): String
    abstract val workoutsPerWeek: String
    abstract fun workoutsN(n: Int): String
    abstract fun weekOf(label: String): String
    abstract val volumePerWeek: String
    abstract val setsPerMuscle: String
    abstract fun setsN(n: Int): String
    abstract val body: String
    abstract val bodyWeight: String
    abstract val logYourWeight: String
    abstract val settings: String
    abstract val weightUnit: String
    abstract val pounds: String
    abstract val kilograms: String
    abstract val defaultRest: String
    abstract val keepScreenOn: String
    abstract val keepScreenOnDesc: String
    abstract val language: String
    abstract val systemLanguage: String
    abstract val data: String
    abstract val exportBackup: String
    abstract val exportBackupDesc: String
    abstract val importBackup: String
    abstract val importBackupDesc: String
    abstract val dataNote: String
    abstract val exported: String
    abstract val exportError: String
    abstract val importTitle: String
    abstract val importText: String
    abstract val importAction: String
    abstract val imported: String
    abstract val importError: String

    // ---------- Peso corporal ----------
    abstract val logAction: String
    abstract val noEntriesTitle: String
    abstract val noEntriesText: String
    abstract fun changeSince(date: String, value: String): String
    abstract val entries: String
    abstract val logWeight: String
    abstract fun weightIn(unit: String): String

    // ---------- Ajudas: sugestões, imagens e treinador IA ----------
    abstract val assistants: String
    abstract val suggestionsSetting: String
    abstract val suggestionsDesc: String
    abstract val showImages: String
    abstract val showImagesDesc: String
    abstract val imagesCredit: String
    abstract val watchVideo: String
    /** Palavra acrescentada à pesquisa no YouTube (ex.: "técnica"). */
    abstract val videoQuery: String
    abstract val suggestion: String
    abstract val suggestUp: String
    abstract val suggestMore: String
    abstract val suggestDeload: String
    abstract val apply: String
    abstract val aiCoach: String
    abstract val aiCoachDesc: String
    abstract val aiEnable: String
    abstract val aiKey: String
    abstract val aiKeyNotSet: String
    abstract val aiKeySet: String
    abstract val aiGetKey: String
    abstract val aiKeyHelp: String
    abstract val aiModel: String
    abstract val aiModelAuto: String
    abstract val aiModelHelp: String
    abstract val aiTest: String
    abstract val aiTesting: String
    abstract fun aiTestOk(model: String): String
    abstract val aiPrivacy: String
    abstract val aiHint: String
    abstract val aiSend: String
    abstract val aiThinking: String
    abstract val aiClear: String
    abstract val aiIntro: String
    abstract val aiPrompts: List<String>
    abstract val aiAnalyzeWorkout: String
    abstract fun aiAnalyzePrompt(name: String, date: String): String
    abstract val aiNoKey: String
    abstract val aiSetup: String
    abstract val aiErrorNetwork: String
    abstract val aiErrorKey: String
    abstract val aiErrorQuota: String
    abstract fun aiError(msg: String): String

    // ---------- Importação do Hevy ----------
    abstract val hevyImport: String
    abstract val hevyImportDesc: String
    abstract val hevyConfirmTitle: String
    abstract fun hevySummary(workouts: Int, sets: Int, matched: Int, created: Int): String
    abstract fun hevyWeights(n: Int): String
    abstract fun hevyDone(imported: Int, skipped: Int): String
    abstract val hevyError: String

    // ---------- Progresso por rotina ----------
    abstract val routineProgress: String
    abstract val routineProgressDesc: String
    abstract val totalSets: String
    abstract val average: String
    abstract val best: String
    abstract val change: String
    abstract val viewProgress: String
    abstract val routineGroupingNote: String
    abstract val noRoutineData: String

    abstract val autoDetected: String
    abstract val assignRoutine: String
    abstract val routineAuto: String
    abstract val exercisesWord: String
    abstract fun routineOf(name: String): String

    // ---------- Derivados ----------
    fun exerciseType(t: ExerciseType) = when (t) {
        ExerciseType.WEIGHT_REPS -> typeWeightReps
        ExerciseType.REPS_ONLY -> typeRepsOnly
        ExerciseType.DURATION -> typeDuration
    }

    fun setType(t: SetType) = when (t) {
        SetType.NORMAL -> setNormal
        SetType.WARMUP -> setWarmup
        SetType.DROP -> setDrop
        SetType.FAILURE -> setFailure
    }

    fun prType(t: PrType) = when (t) {
        PrType.WEIGHT -> maxWeight
        PrType.E1RM -> est1rm
        PrType.VOLUME -> bestSet
        PrType.REPS -> mostRepsSet
        PrType.DURATION -> longestSet
    }

    /** Ex.: "+1 / −2 exercícios" (diferença de exercícios em relação à sessão anterior). */
    fun exercisesDiff(added: Int, removed: Int): String =
        listOfNotNull(if (added > 0) "+$added" else null, if (removed > 0) "−$removed" else null).joinToString(" / ") + " " + exercisesWord

    fun muscle(key: String) = Catalog.muscle(key, col)
    fun equipment(key: String) = Catalog.equipment(key, col)

    /** Nome a mostrar: os exercícios incluídos são traduzidos; os personalizados mantêm o nome. */
    fun exName(e: Exercise): String = e.nameKey?.let { Catalog.exerciseName(it, col) } ?: e.name

    protected fun pl(n: Int, one: String, other: String) = if (n == 1) one else other
}

object I18n {
    val all: List<Strings> = listOf(Pt, En, Es, Fr, De, It)

    @Volatile
    var current: Strings = En
        private set

    fun resolve(code: String): Strings {
        if (code != SYSTEM) all.find { it.code == code }?.let { return it }
        val sys = Locale.getDefault().language
        return all.find { it.code == sys } ?: En
    }

    fun apply(code: String) {
        current = resolve(code)
    }

    const val SYSTEM = "system"
}

val LocalStrings = staticCompositionLocalOf<Strings> { En }
