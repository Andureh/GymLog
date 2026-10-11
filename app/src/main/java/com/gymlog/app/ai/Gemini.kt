package com.gymlog.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GeminiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NETWORK, KEY, QUOTA, MODEL, OTHER }
}

/** Cliente mínimo da API REST do Gemini (sem bibliotecas extra). */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    const val FALLBACK_MODEL = "gemini-2.5-flash"

    class Turn(val fromUser: Boolean, val text: String)

    /**
     * Escolhe o modelo "Flash" estável mais recente disponível para esta chave
     * (ex.: gemini-2.5-flash), para a app não ficar presa a um modelo descontinuado.
     */
    suspend fun pickModel(key: String): String {
        val body = JSONObject(request("GET", "$BASE/models?pageSize=1000", key, null))
        val models = body.optJSONArray("models") ?: JSONArray()
        val re = Regex("""^gemini-(\d+(?:\.\d+)?)-flash$""")
        var best: Pair<Double, String>? = null
        for (i in 0 until models.length()) {
            val m = models.getJSONObject(i)
            val id = m.optString("name").removePrefix("models/")
            val methods = m.optJSONArray("supportedGenerationMethods")?.toString() ?: ""
            val v = re.find(id)?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
            if (!methods.contains("generateContent")) continue
            if (best == null || v > best.first) best = v to id
        }
        return best?.second ?: FALLBACK_MODEL
    }

    suspend fun generate(key: String, model: String, system: String, turns: List<Turn>): String {
        val contents = JSONArray()
        turns.forEach { t ->
            contents.put(
                JSONObject()
                    .put("role", if (t.fromUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", t.text)))
            )
        }
        val req = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.7))
        val res = JSONObject(request("POST", "$BASE/models/$model:generateContent", key, req.toString()))
        val parts = res.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw GeminiException(GeminiException.Kind.OTHER, res.optJSONObject("promptFeedback")?.optString("blockReason") ?: "empty response")
        return buildString {
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (!p.optBoolean("thought")) append(p.optString("text"))
            }
        }.trim()
    }

    private suspend fun request(method: String, url: String, key: String, body: String?): String = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 90_000
            c.setRequestProperty("x-goog-api-key", key)
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code >= 400) throw toError(code, text)
            text
        } catch (e: IOException) {
            throw GeminiException(GeminiException.Kind.NETWORK, e.message ?: "network")
        }
    }

    private fun toError(code: Int, text: String): GeminiException {
        val msg = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull() ?: "HTTP $code"
        val kind = when {
            code == 429 -> GeminiException.Kind.QUOTA
            code == 401 || code == 403 || msg.contains("API key", ignoreCase = true) -> GeminiException.Kind.KEY
            code == 404 -> GeminiException.Kind.MODEL
            else -> GeminiException.Kind.OTHER
        }
        return GeminiException(kind, msg)
    }
}
