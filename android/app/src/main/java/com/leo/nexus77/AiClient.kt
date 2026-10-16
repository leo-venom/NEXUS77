package com.leo.nexus77

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Erro de comunicação/parse com a API compatível OpenAI. */
class AiException(message: String) : Exception(message)

/** Resultado do chat: conteúdo gerado + consumo de tokens informado pela API. */
data class ChatResult(
    val content: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Cliente HTTP mínimo (HttpURLConnection) para APIs compatíveis com OpenAI.
 * Sem dependências externas e sem limite de tamanho de resposta.
 */
object AiClient {

    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val CHAT_READ_TIMEOUT_MS = 600_000
    private const val MODELS_READ_TIMEOUT_MS = 60_000

    fun chatUrl(endpoint: String): String = buildUrl(endpoint, "chat/completions")

    fun modelsUrl(endpoint: String): String = buildUrl(endpoint, "models")

    /**
     * Envia a conversa e retorna conteúdo + consumo de tokens.
     *
     * Usa **streaming (SSE)** para o [onProgress] reportar os bytes que vão
     * chegando (é o que faz o contador de KB andar em tempo real) e para capturar
     * o `usage` no último pedaço. Se o provedor recusar o streaming (HTTP 4xx),
     * repete sem streaming.
     */
    fun chat(
        endpoint: String,
        apiKey: String,
        model: String,
        temperature: Float,
        maxTokens: Int,
        system: String,
        user: String,
        onProgress: ((Int) -> Unit)? = null
    ): ChatResult {
        val messages = JSONArray().apply {
            put(JSONObject().apply { put("role", "system"); put("content", system) })
            put(JSONObject().apply { put("role", "user"); put("content", user) })
        }
        fun body(stream: Boolean): String = JSONObject().apply {
            put("model", model)
            put("temperature", temperature.toDouble())
            if (maxTokens > 0) put("max_tokens", maxTokens)
            put("messages", messages)
            if (stream) {
                put("stream", true)
                put("stream_options", JSONObject().apply { put("include_usage", true) })
            }
        }.toString()

        val url = chatUrl(endpoint)
        return try {
            parseChat(post(url, apiKey, body(true), CHAT_READ_TIMEOUT_MS, onProgress))
        } catch (e: AiException) {
            if (e.message?.startsWith("HTTP 4") == true) {
                parseChat(post(url, apiKey, body(false), CHAT_READ_TIMEOUT_MS, onProgress))
            } else {
                throw e
            }
        }
    }

    /** Interpreta a resposta: streaming (SSE) com fallback para JSON simples. */
    private fun parseChat(raw: String): ChatResult {
        val content = StringBuilder()
        var prompt = 0
        var completion = 0
        var total = 0
        var sawSse = false
        for (line in raw.lineSequence()) {
            val t = line.trim()
            if (!t.startsWith("data:")) continue
            sawSse = true
            val payload = t.substring(5).trim()
            if (payload.isEmpty() || payload == "[DONE]") continue
            val obj = try {
                JSONObject(payload)
            } catch (_: Exception) {
                continue
            }
            obj.optJSONObject("usage")?.let { u ->
                prompt = u.optInt("prompt_tokens", prompt)
                completion = u.optInt("completion_tokens", completion)
                total = u.optInt("total_tokens", total)
            }
            val choices = obj.optJSONArray("choices") ?: continue
            if (choices.length() == 0) continue
            val c0 = choices.getJSONObject(0)
            val piece = c0.optJSONObject("delta")?.optString("content", "")
                ?: c0.optJSONObject("message")?.optString("content", "")
                ?: ""
            if (piece.isNotEmpty()) content.append(piece)
        }
        if (!sawSse) return parseNonStream(raw)
        if (total <= 0) total = prompt + completion
        if (content.isBlank()) throw AiException("resposta vazia do modelo")
        return ChatResult(content.toString(), prompt, completion, total)
    }

    /** Resposta não-streaming (um único JSON). */
    private fun parseNonStream(raw: String): ChatResult {
        val response = JSONObject(raw)
        val choices = response.optJSONArray("choices")
            ?: throw AiException("resposta sem 'choices'")
        if (choices.length() == 0) throw AiException("resposta sem escolhas")
        val content = choices.getJSONObject(0)
            .optJSONObject("message")?.optString("content", "")
            ?: throw AiException("resposta sem 'message'")
        if (content.isBlank()) throw AiException("resposta vazia do modelo")
        val usage = response.optJSONObject("usage")
        val prompt = usage?.optInt("prompt_tokens", 0) ?: 0
        val completion = usage?.optInt("completion_tokens", 0) ?: 0
        var total = usage?.optInt("total_tokens", 0) ?: 0
        if (total <= 0) total = prompt + completion
        return ChatResult(content, prompt, completion, total)
    }

    /** Lista os ids de modelos disponíveis, ordenados. */
    fun listModels(endpoint: String, apiKey: String): List<String> {
        val response = get(modelsUrl(endpoint), apiKey, MODELS_READ_TIMEOUT_MS)
        val data = response.optJSONArray("data") ?: throw AiException("resposta sem 'data'")
        val ids = ArrayList<String>(data.length())
        for (i in 0 until data.length()) {
            val id = data.optJSONObject(i)?.optString("id", "") ?: ""
            if (id.isNotEmpty()) ids.add(id)
        }
        return ids.sorted()
    }

    /** Extrai a mensagem de erro mais útil possível de um erro/resposta falha. */
    fun errorMessage(e: Throwable): String {
        val raw = e.message?.trim().orEmpty()
        if (raw.isEmpty()) return "falha na requisição"
        val jsonPart = if (raw.startsWith("HTTP ")) raw.substringAfter(": ", raw) else raw
        try {
            val json = JSONObject(jsonPart)
            val nested = json.optJSONObject("error")?.optString("message", "").orEmpty()
            if (nested.isNotEmpty()) return nested
            val direct = json.optString("message", "")
            if (direct.isNotEmpty()) return direct
        } catch (_: Exception) {
            // não era JSON; usa o texto cru abaixo
        }
        return raw.take(200)
    }

    private fun buildUrl(endpoint: String, suffix: String): String {
        var base = endpoint.trim().ifEmpty { Prefs.DEFAULT_ENDPOINT }
        if (base.endsWith("/chat/completions")) base = base.removeSuffix("/chat/completions")
        else if (base.endsWith("/models")) base = base.removeSuffix("/models")
        else if (!base.endsWith("/v1")) base = base.trimEnd('/') + "/v1"
        return base.trimEnd('/') + "/" + suffix
    }

    private fun post(
        url: String,
        apiKey: String,
        body: String,
        readTimeoutMs: Int,
        onProgress: ((Int) -> Unit)? = null
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            read(conn, onProgress)
        } finally {
            conn.disconnect()
        }
    }

    private fun get(url: String, apiKey: String, readTimeoutMs: Int): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            JSONObject(read(conn))
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Lê a resposta. Em streaming (SSE), [onProgress] recebe o tamanho **decodificado**
     * do documento (bytes de `delta.content`) — ou seja, o tamanho real da página.
     * Em resposta única (não-streaming), reporta o tamanho do corpo ao final.
     */
    private fun read(conn: HttpURLConnection, onProgress: ((Int) -> Unit)? = null): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = StringBuilder()
        var contentBytes = 0
        var sawSse = false
        stream?.bufferedReader(Charsets.UTF_8)?.use { br ->
            var line = br.readLine()
            while (line != null) {
                raw.append(line).append('\n')
                val t = line.trim()
                if (t.startsWith("data:")) {
                    sawSse = true
                    val payload = t.substring(5).trim()
                    if (payload.isNotEmpty() && payload != "[DONE]") {
                        val piece = deltaPiece(payload)
                        if (piece.isNotEmpty()) {
                            contentBytes += piece.toByteArray(Charsets.UTF_8).size
                            onProgress?.invoke(contentBytes)
                        }
                    }
                }
                line = br.readLine()
            }
        }
        val text = raw.toString()
        if (code !in 200..299) {
            throw AiException("HTTP $code: " + text.ifBlank { "sem detalhes" })
        }
        if (text.isBlank()) throw AiException("resposta vazia do servidor")
        if (!sawSse && onProgress != null) onProgress(text.toByteArray(Charsets.UTF_8).size)
        return text
    }

    /** Extrai `delta.content` (ou `message.content`) de um payload SSE. */
    private fun deltaPiece(payload: String): String = try {
        val choices = JSONObject(payload).optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            ""
        } else {
            val c0 = choices.getJSONObject(0)
            c0.optJSONObject("delta")?.optString("content", "")
                ?: c0.optJSONObject("message")?.optString("content", "")
                ?: ""
        }
    } catch (_: Exception) {
        ""
    }
}
