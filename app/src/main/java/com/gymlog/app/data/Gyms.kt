package com.gymlog.app.data

import android.content.Context
import com.gymlog.app.i18n.I18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Um tamanho de disco e quantos pares desse tamanho o ginásio tem. Pesos na unidade do ginásio. */
data class PlateSpec(val weight: Double, val pairs: Int)

/** Uma barra (ou base de máquina) com o seu peso, na unidade do ginásio. */
data class BarSpec(val id: String, val name: String, val weight: Double)

data class Gym(
    val id: String,
    val name: String,
    /** true = discos e barras em libras. */
    val lbs: Boolean,
    val plates: List<PlateSpec>,
    val bars: List<BarSpec>,
)

/** Como se carrega um exercício: que barra ([NO_BAR] = sem barra) e de quantos lados (1 ou 2). */
data class LoadSetup(val barId: String, val sides: Int = 2)

data class GymState(val gyms: List<Gym>, val activeId: String, val setups: Map<Long, LoadSetup>) {
    val active: Gym get() = gyms.find { it.id == activeId } ?: gyms.first()
}

const val NO_BAR = "none"

/** Ginásios guardados nas preferências (JSON). Também entram na cópia de segurança. */
class GymStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("gyms", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val flow: StateFlow<GymState> = state
    val current: GymState get() = state.value

    private fun load(): GymState =
        prefs.getString("state", null)?.let { runCatching { fromJson(JSONObject(it)) }.getOrNull() }
            ?: GymState(listOf(defaultGym(lbs = false)), "", emptyMap()).let { it.copy(activeId = it.gyms.first().id) }

    fun update(f: (GymState) -> GymState) {
        val n = f(state.value).let { s ->
            // Há sempre pelo menos um ginásio e um ativo válido
            val gyms = s.gyms.ifEmpty { listOf(defaultGym(lbs = false)) }
            s.copy(gyms = gyms, activeId = if (gyms.any { it.id == s.activeId }) s.activeId else gyms.first().id)
        }
        state.value = n
        prefs.edit().putString("state", toJson(n).toString()).apply()
    }

    fun upsertGym(g: Gym) = update { s ->
        s.copy(gyms = if (s.gyms.any { it.id == g.id }) s.gyms.map { if (it.id == g.id) g else it } else s.gyms + g)
    }

    fun deleteGym(id: String) = update { s -> s.copy(gyms = s.gyms.filter { it.id != id }) }
    fun setActive(id: String) = update { it.copy(activeId = id) }
    fun setSetup(exerciseId: Long, setup: LoadSetup) = update { it.copy(setups = it.setups + (exerciseId to setup)) }

    /** Barra por defeito de um exercício, pelo equipamento (barra olímpica, EZ, Smith; máquinas sem barra). */
    fun setupFor(exerciseId: Long?, equipment: String?): LoadSetup {
        exerciseId?.let { current.setups[it] }?.let { return it }
        val bars = current.active.bars
        val barId = when (equipment) {
            "barbell" -> bars.getOrNull(0)?.id
            "ez_bar" -> bars.find { it.id.startsWith("ez") }?.id ?: bars.getOrNull(1)?.id
            "smith" -> bars.find { it.id.startsWith("smith") }?.id
            else -> null
        } ?: NO_BAR
        return LoadSetup(barId)
    }

    fun replaceAll(s: GymState) = update { s }

    companion object {
        fun defaultGym(lbs: Boolean): Gym {
            val s = I18n.current
            return if (lbs) Gym(
                UUID.randomUUID().toString(), s.myGym, true,
                listOf(45.0 to 6, 35.0 to 2, 25.0 to 4, 10.0 to 4, 5.0 to 4, 2.5 to 2).map { PlateSpec(it.first, it.second) },
                listOf(BarSpec("olympic", s.barOlympic, 45.0), BarSpec("ez", s.barEz, 25.0), BarSpec("smith", s.barSmith, 35.0)),
            ) else Gym(
                UUID.randomUUID().toString(), s.myGym, false,
                listOf(25.0 to 4, 20.0 to 4, 15.0 to 2, 10.0 to 4, 5.0 to 4, 2.5 to 4, 1.25 to 2).map { PlateSpec(it.first, it.second) },
                listOf(BarSpec("olympic", s.barOlympic, 20.0), BarSpec("ez", s.barEz, 10.0), BarSpec("smith", s.barSmith, 15.0)),
            )
        }

        fun toJson(s: GymState): JSONObject = JSONObject()
            .put("activeId", s.activeId)
            .put("gyms", JSONArray().apply {
                s.gyms.forEach { g ->
                    put(JSONObject().put("id", g.id).put("name", g.name).put("lbs", g.lbs)
                        .put("plates", JSONArray().apply { g.plates.forEach { put(JSONObject().put("w", it.weight).put("pairs", it.pairs)) } })
                        .put("bars", JSONArray().apply { g.bars.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("w", it.weight)) } }))
                }
            })
            .put("setups", JSONObject().apply { s.setups.forEach { (k, v) -> put(k.toString(), JSONObject().put("bar", v.barId).put("sides", v.sides)) } })

        fun fromJson(o: JSONObject): GymState {
            val gyms = o.getJSONArray("gyms").let { a ->
                (0 until a.length()).map { i ->
                    val g = a.getJSONObject(i)
                    val plates = g.getJSONArray("plates").let { p -> (0 until p.length()).map { PlateSpec(p.getJSONObject(it).getDouble("w"), p.getJSONObject(it).getInt("pairs")) } }
                    val bars = g.getJSONArray("bars").let { b ->
                        (0 until b.length()).map { val x = b.getJSONObject(it); BarSpec(x.getString("id"), x.getString("name"), x.getDouble("w")) }
                    }
                    Gym(g.getString("id"), g.getString("name"), g.optBoolean("lbs"), plates, bars)
                }
            }
            val setups = o.optJSONObject("setups")?.let { m ->
                m.keys().asSequence().associate { k -> k.toLong() to m.getJSONObject(k).let { LoadSetup(it.getString("bar"), it.optInt("sides", 2)) } }
            } ?: emptyMap()
            return GymState(gyms, o.optString("activeId"), setups)
        }
    }
}
