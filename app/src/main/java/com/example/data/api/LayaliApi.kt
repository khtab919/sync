package com.example.data.api

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class ApiException(val code: String) : Exception(code)

/** Passwords stay in memory; only revocable session tokens can be persisted. */
class LayaliApi internal constructor(private val baseUrl: String, private val sessionStore: SessionStore? = null) {
    val origin: String get() = baseUrl.trimEnd('/')
    @Volatile var token: String? = null
        private set
    var sessionSaved: Boolean = false
        private set

    init {
        val url = URL(baseUrl)
        require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null)
    }

    suspend fun upload(context: android.content.Context, uri: android.net.Uri, purpose: String): JSONObject = withContext(Dispatchers.IO) {
        val maximum = if (purpose == "reel") 20 * 1024 * 1024 else 8 * 1024 * 1024
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > maximum) throw ApiException("file_too_large")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw ApiException("invalid_file")
        request("/api/upload", "POST", JSONObject().put("purpose", purpose).put("data", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)))
    }

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        token = sessionStore?.read()
        sessionSaved = token != null
        token != null
    }
    suspend fun forgetSession() = withContext(Dispatchers.IO) { setSession(null) }
    suspend fun requireSimpleRegistration() {
        if (request("/health/").optInt("registration_version") < 4) throw ApiException("server_update_required")
    }
    @Synchronized private fun setSession(value: String?) {
        token = value
        sessionSaved = false
        try {
            if (value == null) sessionStore?.clear()
            else if (sessionStore != null) { sessionStore.write(value); sessionSaved = true }
        } catch (_: Exception) { /* A keystore failure must not turn a successful login into a failed one. */ }
    }

    @Synchronized private fun invalidateSession(expected: String?) { if (token == expected) setSession(null) }

    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        require(path.startsWith("/api/") || path == "/health/")
        val requestToken = token
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = if (path == "/api/upload") 90000 else 15000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            requestToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (status !in 200..299) {
                if (status == 401) invalidateSession(requestToken)
                throw ApiException(if (status >= 500) "server_unavailable" else json.optString("error", "server_error_$status"))
            }
            if (path == "/api/login" || path == "/api/register") setSession(json.getString("token"))
            if (path == "/api/logout") setSession(null)
            json
        } finally { connection.disconnect() }
    }
}
