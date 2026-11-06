package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.URLEncoder

internal fun trustedCallOrigin(origin: String, baseUrl: String): Boolean = runCatching {
    val actual = Uri.parse(origin); val base = Uri.parse(baseUrl)
    actual.scheme == "https" && actual.host == base.host && (if(actual.port == -1) 443 else actual.port) == (if(base.port == -1) 443 else base.port) && actual.userInfo == null
}.getOrDefault(false)

@Composable
internal fun InternalCallScreen(api: LayaliApi, baseUrl: String, callId: String, finish: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var retryAttempt by remember(callId) { mutableIntStateOf(0) }
    var call by remember(callId) { mutableStateOf(JSONObject()) }
    var bootstrap by remember(callId) { mutableStateOf<JSONObject?>(null) }
    var permissionGranted by remember(callId) { mutableStateOf(false) }
    var permissionRequested by remember(callId) { mutableStateOf(false) }
    var error by remember(callId) { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var speaker by remember { mutableStateOf(false) }
    val audio = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionGranted = result[Manifest.permission.RECORD_AUDIO] == true && (call.optString("mode") != "video" || result[Manifest.permission.CAMERA] == true)
        if(!permissionGranted) error = "يلزم السماح بالميكروفون، وبالكاميرا لمكالمة الفيديو."
    }
    BackHandler { finish() }
    LaunchedEffect(callId,retryAttempt) {
        try { call = api.request("/api/calls/$callId"); speaker = call.optString("mode") == "video" }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { error = "تعذّر فتح المكالمة. تحقق من تحديث الخادم والاتصال." }
    }
    LaunchedEffect(call.optString("mode"),retryAttempt) {
        if(!call.has("mode") || permissionRequested) return@LaunchedEffect
        permissionRequested = true
        val needed = if(call.optString("mode") == "video") arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) else arrayOf(Manifest.permission.RECORD_AUDIO)
        if(needed.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) permissionGranted = true else permissions.launch(needed)
    }
    LaunchedEffect(permissionGranted, callId,retryAttempt) {
        if(!permissionGranted) return@LaunchedEffect
        try { bootstrap = api.request("/api/calls/$callId/ticket", "POST", JSONObject()) }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { error = "تعذّر تجهيز الاتصال. ربما انتهت المكالمة أو لم يكتمل إعداد خادم الاتصال." }
    }
    LaunchedEffect(callId) {
        while(true) {
            delay(2000)
            try {
                val state = api.request("/api/calls/$callId").optString("status")
                if(state !in listOf("ringing", "accepted")) { finish(); break }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(api.token == null) { finish(); break } }
        }
    }
    @Suppress("DEPRECATION")
    DisposableEffect(audio, speaker) {
        val oldMode = audio.mode
        val oldSpeaker = audio.isSpeakerphoneOn
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        audio.isSpeakerphoneOn = speaker
        onDispose { audio.isSpeakerphoneOn = oldSpeaker; audio.mode = oldMode }
    }
    DisposableEffect(lifecycle, webView) {
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_STOP && webView != null && (context as? android.app.Activity)?.isChangingConfigurations != true) finish() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) { onDispose { webView?.stopLoading(); webView?.loadUrl("about:blank"); webView?.destroy(); webView = null } }
    Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if(call.optString("mode") == "video") "مكالمة فيديو" else "مكالمة صوتية", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(10.dp))
            TextButton(onClick = { speaker = !speaker }) { Text(if(speaker) "السماعة الخارجية" else "سماعة الهاتف") }
            Button(onClick = finish, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("إنهاء") }
        }
        if(error.isNotBlank()) {
            Text(error, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            if(webView == null) OutlinedButton(onClick = {
                webView?.stopLoading(); webView?.destroy(); webView = null
                bootstrap = null; error = ""; permissionRequested = false
                retryAttempt++
            }, modifier = Modifier.padding(horizontal = 20.dp)) { Text("إعادة محاولة الاتصال") }
        }
        val room = bootstrap
        if(permissionGranted && room != null && error.isBlank()) {
            AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { viewContext ->
                WebView(viewContext).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return true
                            return !trustedCallOrigin(uri.toString(), baseUrl) || uri.path != "/api/calls/$callId/room"
                        }
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, sslError: android.net.http.SslError?) { handler?.cancel(); error = "شهادة أمان الخادم غير صالحة. لم يُفتح الاتصال." }
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, failure: WebResourceError?) { if(request?.isForMainFrame == true) error = "تعذّر تحميل شاشة الاتصال. تحقق من الإنترنت." }
                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) { if(request?.isForMainFrame == true) error = "انتهت صلاحية فتح المكالمة. أغلقها وأعد الاتصال." }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            if(request == null) return
                            if(!trustedCallOrigin(request.origin.toString(), baseUrl)) { request.deny(); return }
                            val allowed = request.resources.filter { resource ->
                                (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) ||
                                    (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE && call.optString("mode") == "video" && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                            }
                            if(allowed.size == request.resources.size && allowed.isNotEmpty()) request.grant(allowed.toTypedArray()) else request.deny()
                        }
                    }
                    val roomUrl = room.getString("url")
                    if(trustedCallOrigin(roomUrl, baseUrl) && Uri.parse(roomUrl).path == "/api/calls/$callId/room") postUrl(roomUrl, ("ticket="+URLEncoder.encode(room.getString("ticket"), "UTF-8")).toByteArray())
                    else error = "رفض فتح عنوان اتصال غير تابع للخادم."
                }
            })
        } else if(error.isBlank()) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("جارٍ تجهيز الاتصال داخل التطبيق…", Modifier.padding(24.dp)) }
    } }
}
