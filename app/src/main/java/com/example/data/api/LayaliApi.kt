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

    suspend fun upload(context: android.content.Context, uri: android.net.Uri, purpose: String, progress:(Float)->Unit={}): JSONObject = withContext(Dispatchers.IO) {
        val maximum = if(purpose=="reel")20*1024*1024 else 8*1024*1024
        val temporary=java.io.File.createTempFile("relaxia-upload-",".bin",context.cacheDir)
        try {
            val digest=java.security.MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input -> temporary.outputStream().use { output ->
                val buffer=ByteArray(65536);var size=0
                while(true){val n=input.read(buffer);if(n<0)break;size+=n;if(size>maximum)throw ApiException("file_too_large");output.write(buffer,0,n);digest.update(buffer,0,n)}
            }} ?: throw ApiException("invalid_file")
            // During a staged deployment older servers still accept the original upload API.
            if(request("/health/").optInt("upload_version")<2){
                val legacy=request("/api/upload","POST",JSONObject().put("purpose",purpose).put("data",android.util.Base64.encodeToString(temporary.readBytes(),android.util.Base64.NO_WRAP)))
                withContext(Dispatchers.Main){progress(1f)}
                return@withContext legacy
            }
            val checksum=digest.digest().joinToString(""){"%02x".format(it)}
            val preferences=context.getSharedPreferences("relaxia_uploads",android.content.Context.MODE_PRIVATE)
            val pendingKey=java.security.MessageDigest.getInstance("SHA-256").digest((origin+":"+token+":"+purpose+":"+checksum).toByteArray()).joinToString(""){"%02x".format(it)}
            val requestKey=preferences.getString(pendingKey,null) ?: java.util.UUID.randomUUID().toString().also{preferences.edit().putString(pendingKey,it).commit()}
            val session=request("/api/uploads","POST",JSONObject().put("purpose",purpose).put("size",temporary.length()).put("sha256",checksum).put("request_key",requestKey))
            val id=session.getString("id");var offset=session.getLong("offset")
            java.io.RandomAccessFile(temporary,"r").use { input ->
                val buffer=ByteArray(262144)
                while(offset<temporary.length()){
                    input.seek(offset);val n=input.read(buffer);if(n<=0)throw ApiException("invalid_file")
                    val payload=JSONObject().put("offset",offset).put("data",android.util.Base64.encodeToString(buffer,0,n,android.util.Base64.NO_WRAP))
                    var result:JSONObject?=null
                    for(attempt in 0..2){
                        try{result=request("/api/uploads/$id/chunk","POST",payload);break}
                        catch(e:kotlinx.coroutines.CancellationException){throw e}
                        catch(e:Exception){if(attempt==2)throw e;kotlinx.coroutines.delay((attempt+1)*1000L)}
                    }
                    offset=result!!.getLong("offset");withContext(Dispatchers.Main){progress(offset.toFloat()/temporary.length())}
                }
            }
            val result=request("/api/uploads/$id/finish","POST",JSONObject())
            preferences.edit().remove(pendingKey).apply()
            result
        }finally{temporary.delete()}
    }

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        token = sessionStore?.read()
        sessionSaved = token != null
        token != null
    }
    suspend fun forgetSession() = withContext(Dispatchers.IO) { setSession(null) }
    suspend fun requireSimpleRegistration() {
        if (request("/health/").optInt("registration_version") < 6) throw ApiException("server_update_required")
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
            connection.readTimeout = if (path == "/api/upload" || path.startsWith("/api/uploads")) 90000 else 15000
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
