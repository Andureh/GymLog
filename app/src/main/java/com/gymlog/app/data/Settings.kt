package com.gymlog.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val useLbs: Boolean = false,
    val defaultRest: Int = 90,
    val keepScreenOn: Boolean = true,
    /** Código da língua ("pt", "en"…) ou "system". */
    val language: String = "system",
    /** Sugestões automáticas de progressão durante o treino. */
    val suggestions: Boolean = true,
    /** Fotos dos exercícios (descarregadas a pedido). */
    val showImages: Boolean = true,
    /** Treinador IA (Gemini) — desligado por defeito. */
    val aiEnabled: Boolean = false,
    val aiKey: String = "",
    /** Modelo escolhido manualmente; vazio = automático. */
    val aiModel: String = "",
    /** Último modelo escolhido automaticamente (cache). */
    val aiAutoModel: String = "",
    /** Versão do catálogo de exercícios já sincronizada na base de dados. */
    val catalogVersion: Int = 0,
)

class Settings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        AppSettings(
            useLbs = prefs.getBoolean("lbs", false),
            defaultRest = prefs.getInt("rest", 90),
            keepScreenOn = prefs.getBoolean("screen", true),
            language = prefs.getString("lang", "system") ?: "system",
            suggestions = prefs.getBoolean("suggestions", true),
            showImages = prefs.getBoolean("images", true),
            aiEnabled = prefs.getBoolean("ai", false),
            aiKey = prefs.getString("aiKey", "") ?: "",
            aiModel = prefs.getString("aiModel", "") ?: "",
            aiAutoModel = prefs.getString("aiAutoModel", "") ?: "",
            catalogVersion = prefs.getInt("catalogVersion", 0),
        )
    )
    val flow: StateFlow<AppSettings> = state
    val current: AppSettings get() = state.value

    fun update(f: (AppSettings) -> AppSettings) {
        val n = f(state.value)
        state.value = n
        prefs.edit()
            .putBoolean("lbs", n.useLbs)
            .putInt("rest", n.defaultRest)
            .putBoolean("screen", n.keepScreenOn)
            .putString("lang", n.language)
            .putBoolean("suggestions", n.suggestions)
            .putBoolean("images", n.showImages)
            .putBoolean("ai", n.aiEnabled)
            .putString("aiKey", n.aiKey)
            .putString("aiModel", n.aiModel)
            .putString("aiAutoModel", n.aiAutoModel)
            .putInt("catalogVersion", n.catalogVersion)
            .apply()
    }
}
