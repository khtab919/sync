package com.example.data.api

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Native HTTPS client. Session credentials are kept only in memory. */
class LayaliApi(private val baseUrl: String) {
    var token: String? = null
        private set

    init {
        val url = URL(baseUrl)
        require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null)
    }

    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            require(path.startsWith("/api/"))
            val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
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
                    if (status == 401) token = null
                    error(json.optString("error", "server_error_$status"))
                }
                if (path == "/api/login" || path == "/api/register") token = json.getString("token")
                if (path == "/api/logout") token = null
                json
            } finally {
                connection.disconnect()
            }
        }
}
