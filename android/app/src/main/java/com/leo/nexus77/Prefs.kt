package com.leo.nexus77

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferências do app: endpoint, API key, modelo, temperatura,
 * max tokens, tema ativo e consumo de tokens da sessão.
 * Persistidas em SharedPreferences ("nexus77").
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var endpoint: String
        get() = sp.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT)!!.trim().ifEmpty { DEFAULT_ENDPOINT }
        set(value) = sp.edit().putString(KEY_ENDPOINT, value.trim().ifEmpty { DEFAULT_ENDPOINT }).apply()

    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "")!!.trim()
        set(value) = sp.edit().putString(KEY_API_KEY, value.trim()).apply()

    var model: String
        get() = sp.getString(KEY_MODEL, DEFAULT_MODEL)!!.trim().ifEmpty { DEFAULT_MODEL }
        set(value) = sp.edit().putString(KEY_MODEL, value.trim().ifEmpty { DEFAULT_MODEL }).apply()

    var temperature: Float
        get() = sp.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE).coerceIn(0f, 2f)
        set(value) = sp.edit().putFloat(KEY_TEMPERATURE, value.coerceIn(0f, 2f)).apply()

    var maxTokens: Int
        get() = sp.getInt(KEY_MAX_TOKENS, 0).coerceAtLeast(0)
        set(value) = sp.edit().putInt(KEY_MAX_TOKENS, value.coerceAtLeast(0)).apply()

    var themeIndex: Int
        get() = sp.getInt(KEY_THEME, 0).coerceIn(0, NexusTheme.ALL.size - 1)
        set(value) = sp.edit().putInt(KEY_THEME, value.coerceIn(0, NexusTheme.ALL.size - 1)).apply()

    /** Nível de detalhe das páginas: 0 = simples, 1 = médio, 2 = rico. */
    var detailLevel: Int
        get() = sp.getInt(KEY_DETAIL, 1).coerceIn(0, 2)
        set(value) = sp.edit().putInt(KEY_DETAIL, value.coerceIn(0, 2)).apply()

    /** Total de tokens consumidos desde o último reset de sessão. */
    var sessionTokens: Int
        get() = sp.getInt(KEY_SESSION_TOKENS, 0).coerceAtLeast(0)
        set(value) = sp.edit().putInt(KEY_SESSION_TOKENS, value.coerceAtLeast(0)).apply()

    /** Tokens de entrada (prompt) consumidos na sessão. */
    var sessionPromptTokens: Int
        get() = sp.getInt(KEY_SESSION_PROMPT_TOKENS, 0).coerceAtLeast(0)
        set(value) = sp.edit().putInt(KEY_SESSION_PROMPT_TOKENS, value.coerceAtLeast(0)).apply()

    /** Tokens de saída (completion) consumidos na sessão. */
    var sessionCompletionTokens: Int
        get() = sp.getInt(KEY_SESSION_COMPLETION_TOKENS, 0).coerceAtLeast(0)
        set(value) = sp.edit().putInt(KEY_SESSION_COMPLETION_TOKENS, value.coerceAtLeast(0)).apply()

    /** Acumula entrada/saída de uma geração (e o total). */
    fun addSessionTokens(promptTokens: Int, completionTokens: Int) {
        if (promptTokens > 0) sessionPromptTokens += promptTokens
        if (completionTokens > 0) sessionCompletionTokens += completionTokens
        val add = promptTokens + completionTokens
        if (add > 0) sessionTokens += add
    }

    val hasKey: Boolean get() = apiKey.isNotEmpty()

    companion object {
        private const val NAME = "nexus77"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_THEME = "theme"
        private const val KEY_DETAIL = "detail_level"
        private const val KEY_SESSION_TOKENS = "session_tokens"
        private const val KEY_SESSION_PROMPT_TOKENS = "session_prompt_tokens"
        private const val KEY_SESSION_COMPLETION_TOKENS = "session_completion_tokens"

        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_TEMPERATURE = 0.7f

        /** Orçamento visual da barra de tokens da sessão. */
        const val SESSION_TOKEN_BUDGET = 1_000_000
    }
}
