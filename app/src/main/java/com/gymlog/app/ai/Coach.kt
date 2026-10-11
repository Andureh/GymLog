package com.gymlog.app.ai

import com.gymlog.app.data.BodyWeight
import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.Repository
import com.gymlog.app.data.SetType
import com.gymlog.app.data.Settings
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.domain.completedSets
import com.gymlog.app.domain.durationMs
import com.gymlog.app.domain.e1rm
import com.gymlog.app.domain.volumeKg
import com.gymlog.app.i18n.I18n
import com.gymlog.app.i18n.Strings
import com.gymlog.app.util.fmtNum
import com.gymlog.app.util.toDisplayWeight
import com.gymlog.app.util.unitLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

/** Conversa com o treinador IA. Vive na Application para não se perder ao mudar de ecrã. */
class Coach(private val repo: Repository, private val settings: Settings, private val scope: CoroutineScope) {
    class Message(val fromUser: Boolean, val text: String, val isError: Boolean = false)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun clear() {
        if (!_loading.value) _messages.value = emptyList()
    }

    fun send(text: String, focusWorkoutId: Long? = null) {
        if (_loading.value || text.isBlank()) return
        val s = I18n.current
        _messages.value = _messages.value + Message(true, text.trim())
        _loading.value = true
        scope.launch {
            val reply = try {
                val cfg = settings.current
                if (cfg.aiKey.isBlank()) throw GeminiException(GeminiException.Kind.KEY, "")
                val workouts = repo.finishedWorkouts().first()
                val weights = repo.bodyWeights().first()
                val system = systemPrompt(s, cfg.useLbs, buildTrainingContext(workouts, weights, s, cfg.useLbs, focusWorkoutId))
                val turns = _messages.value.filter { !it.isError }.takeLast(20).map { Gemini.Turn(it.fromUser, it.text) }
                Message(false, generateWithModel(cfg.aiKey, system, turns))
            } catch (e: GeminiException) {
                Message(false, errorText(s, e), isError = true)
            } catch (e: Exception) {
                Message(false, s.aiError(e.message ?: e.javaClass.simpleName), isError = true)
            }
            _messages.value = _messages.value + reply
            _loading.value = false
        }
    }

    /** Usa o modelo manual, ou escolhe/recorda o automático; se este deixar de existir, volta a escolher. */
    private suspend fun generateWithModel(key: String, system: String, turns: List<Gemini.Turn>): String {
        val manual = settings.current.aiModel.trim()
        if (manual.isNotEmpty()) return Gemini.generate(key, manual, system, turns)
        var model = settings.current.aiAutoModel.ifBlank { resolveModel(key) }
        return try {
            Gemini.generate(key, model, system, turns)
        } catch (e: GeminiException) {
            if (e.kind != GeminiException.Kind.MODEL) throw e
            model = resolveModel(key)
            Gemini.generate(key, model, system, turns)
        }
    }

    /** Testa a chave e devolve o modelo que vai ser usado. */
    suspend fun testConnection(): String {
        val cfg = settings.current
        if (cfg.aiKey.isBlank()) throw GeminiException(GeminiException.Kind.KEY, "")
        val auto = resolveModel(cfg.aiKey)
        return cfg.aiModel.trim().ifEmpty { auto }
    }

    private suspend fun resolveModel(key: String): String {
        val m = Gemini.pickModel(key)
        settings.update { it.copy(aiAutoModel = m) }
        return m
    }

    companion object {
        fun errorText(s: Strings, e: GeminiException) = when (e.kind) {
            GeminiException.Kind.NETWORK -> s.aiErrorNetwork
            GeminiException.Kind.KEY -> s.aiErrorKey
            GeminiException.Kind.QUOTA -> s.aiErrorQuota
            else -> s.aiError(e.message ?: "")
        }
    }
}

private fun systemPrompt(s: Strings, lbs: Boolean, data: String) = """
You are an experienced, encouraging strength & conditioning coach inside the workout-tracking app "GymLog".
You can see the user's real training log below. Base every answer on that data and be specific (exercises, weights, reps, dates).
Guidelines:
- Always answer in ${s.nativeName} (language code "${s.code}").
- Be concise and practical: short paragraphs and bullet points, light markdown (**bold**, "- " bullets). No tables.
- Use ${unitLabel(lbs)} for weights.
- Suggest concrete next steps (progressive overload, volume per muscle, frequency, recovery, deloads) when relevant.
- If the data is insufficient, say so and give general advice.
- You are not a doctor: for pain or injuries, recommend seeing a professional.

=== USER TRAINING DATA ===
$data
""".trimIndent()

private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private fun d(ms: Long) = iso.format(Date(ms))

/** Resumo compacto dos treinos para enviar ao modelo. [workouts] em ordem decrescente de data. */
fun buildTrainingContext(
    workouts: List<WorkoutFull>,
    weights: List<BodyWeight>,
    s: Strings,
    lbs: Boolean,
    focusWorkoutId: Long?,
): String = buildString {
    val u = unitLabel(lbs)
    fun w(kg: Double) = fmtNum(toDisplayWeight(kg, lbs), 1)
    fun setsText(e: com.gymlog.app.data.WorkoutExerciseFull) = e.sets.filter { it.completed }.joinToString(", ") { st ->
        val v = when (e.exercise.type) {
            ExerciseType.WEIGHT_REPS -> "${w(st.weight)}x${st.reps}"
            ExerciseType.REPS_ONLY -> if (st.weight > 0) "+${w(st.weight)}x${st.reps}" else "${st.reps} reps"
            ExerciseType.DURATION -> "${st.durationSec}s"
        }
        when (st.type) {
            SetType.WARMUP -> "$v(warm-up)"
            SetType.DROP -> "$v(drop)"
            SetType.FAILURE -> "$v(failure)"
            SetType.NORMAL -> v
        }
    }
    fun workoutLine(wf: WorkoutFull) = buildString {
        append("${d(wf.workout.startTime)} \"${wf.workout.name}\" (${wf.durationMs() / 60000} min, volume ${fmtNum(toDisplayWeight(wf.volumeKg(), lbs), 0)} $u, ${wf.completedSets()} sets)")
        if (wf.workout.notes.isNotBlank()) append(" notes: ${wf.workout.notes}")
        append("\n")
        wf.exercises.forEach { e ->
            append("  - ${s.exName(e.exercise)}: ${setsText(e)}")
            if (e.we.notes.isNotBlank()) append(" [note: ${e.we.notes}]")
            append("\n")
        }
    }

    appendLine("Today: ${d(System.currentTimeMillis())}. Weight unit: $u.")
    if (workouts.isEmpty()) {
        appendLine("No workouts logged yet.")
        return@buildString
    }
    appendLine("Total workouts logged: ${workouts.size} (first on ${d(workouts.last().workout.startTime)}).")

    // Frequência semanal (últimas 8 semanas)
    val zone = ZoneId.systemDefault()
    fun weekOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val thisWeek = weekOf(System.currentTimeMillis())
    val byWeek = workouts.groupBy { weekOf(it.workout.startTime) }
    appendLine("Workouts per week (oldest→newest, last 8 weeks): " +
        (7 downTo 0).joinToString(", ") { byWeek[thisWeek.minusWeeks(it.toLong())]?.size?.toString() ?: "0" })

    if (weights.isNotEmpty()) {
        val last = weights.first()
        val monthAgo = weights.firstOrNull { it.date <= last.date - 30L * 86_400_000 }
        append("Body weight: ${w(last.weightKg)} $u on ${d(last.date)}")
        monthAgo?.let { append(" (was ${w(it.weightKg)} $u on ${d(it.date)})") }
        appendLine()
    }

    focusWorkoutId?.let { id ->
        workouts.find { it.workout.id == id }?.let {
            appendLine()
            appendLine("=== WORKOUT THE USER IS ASKING ABOUT ===")
            append(workoutLine(it))
        }
    }

    // Progressão por exercício (últimos 120 dias)
    val since = System.currentTimeMillis() - 120L * 86_400_000
    val recent = workouts.filter { it.workout.startTime >= since }
    val byExercise = recent.reversed().flatMap { wf -> wf.exercises.map { wf.workout.startTime to it } }
        .groupBy { it.second.exercise.id }
    if (byExercise.isNotEmpty()) {
        appendLine()
        appendLine("=== EXERCISE PROGRESS (last 120 days; per session: date top-set / best est. 1RM) ===")
        byExercise.values.sortedByDescending { it.size }.forEach { list ->
            val ex = list.first().second.exercise
            val sessions = list.takeLast(8).joinToString("; ") { (date, e) ->
                val work = e.sets.filter { it.completed && it.type != SetType.WARMUP }
                when (ex.type) {
                    ExerciseType.WEIGHT_REPS -> {
                        val top = work.maxByOrNull { e1rm(it.weight, it.reps) }
                        if (top == null) d(date) else "${d(date)} ${w(top.weight)}x${top.reps} / ${w(e1rm(top.weight, top.reps))}"
                    }
                    ExerciseType.REPS_ONLY -> "${d(date)} max ${work.maxOfOrNull { it.reps } ?: 0} reps"
                    ExerciseType.DURATION -> "${d(date)} max ${work.maxOfOrNull { it.durationSec } ?: 0}s"
                }
            }
            appendLine("${s.exName(ex)} [${s.muscle(ex.muscle)}] ${list.size} sessions: $sessions")
        }
    }

    // Treinos recentes em detalhe
    appendLine()
    appendLine("=== MOST RECENT WORKOUTS (detail) ===")
    workouts.take(12).forEach { append(workoutLine(it)) }
}
