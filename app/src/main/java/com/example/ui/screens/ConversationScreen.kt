package com.example.ui.screens

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@Composable
internal fun ConversationScreen(api: LayaliApi, peerId: Int, peerName: String, myId: Int, body: String, changeBody: (String) -> Unit, buy: () -> Unit, openCall: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var confirmCall by remember { mutableStateOf(false) }
    suspend fun refresh() {
        val a = api.request("/api/messages/$peerId").optJSONArray("items")
        rows = if(a == null) emptyList() else (0 until a.length()).map { a.getJSONObject(it) }.reversed()
    }
    fun perform(block: suspend () -> Unit) {
        if(busy) return
        busy = true; error = ""
        scope.launch { try { block(); refresh() } catch(e: CancellationException) { throw e } catch(e: Exception) { error = if(e.message == "calls_setup_required" || e.message == "server_unavailable") "خادم الاتصال غير جاهز بعد. حدّث الخادم لتفعيل الاتصال الداخلي." else if(e.message == "call_busy") "أحد الطرفين مشغول بمكالمة أخرى." else if(e.message == "paid_chat_requires_verified_payment") "هذه المحادثة مدفوعة؛ لم تُرسل الرسالة ولم يُخصم رصيد." else commerceError(e) } finally { busy = false } }
    }
    fun finishRecording(send: Boolean) {
        val current = recorder; recorder = null; recording = false
        val file = recordingFile; recordingFile = null
        val valid = runCatching { current?.stop(); true }.getOrDefault(false)
        current?.release()
        if(send && valid && file != null) perform { try {
            val media = api.upload(context, Uri.fromFile(file), "chat_audio")
            api.request("/api/messages/$peerId", "POST", JSONObject().put("attachment_id", media.getString("id")))
        } finally { file.delete() } } else file?.delete()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted && !busy && !recording) {
            runCatching {
                val file = File.createTempFile("relaxia-voice-", ".m4a", context.cacheDir)
                recordingFile = file
                @Suppress("DEPRECATION") val r = MediaRecorder()
                recorder = r
                r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setAudioEncodingBitRate(64000); r.setAudioSamplingRate(44100); r.setOutputFile(file.absolutePath); r.prepare(); r.start()
                seconds = 0; recording = true
            }.onFailure { recorder?.release(); recorder = null; recordingFile?.delete(); recordingFile = null; error = "تعذّر بدء تسجيل الصوت." }
        } else if(!granted) error = "اسمح باستخدام الميكروفون لتسجيل رسالة صوتية."
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected -> if(selected != null) perform {
        val media = api.upload(context, selected, "chat_image")
        api.request("/api/messages/$peerId", "POST", JSONObject().put("attachment_id", media.getString("id")))
    } }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if(event == Lifecycle.Event.ON_PAUSE) { if(recording) finishRecording(false); player?.release(); player = null }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(peerId, resumed) {
        if(!resumed) return@LaunchedEffect
        while(true) {
            try { refresh() } catch(e: CancellationException) { throw e } catch(e: Exception) { error = commerceError(e) }
            delay(3000)
        }
    }
    var historyLoaded by rememberSaveable(peerId) { mutableStateOf(false) }
    var previousNewest by rememberSaveable(peerId) { mutableIntStateOf(0) }
    val newest = rows.lastOrNull()?.optInt("id") ?: 0
    LaunchedEffect(newest) {
        if(rows.isEmpty()) return@LaunchedEffect
        val arrived = previousNewest > 0 && newest > previousNewest
        val atBottom = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size >= rows.size - 2
        if(!historyLoaded || (arrived && (atBottom || rows.last().optInt("sender_id") == myId))) listState.scrollToItem(rows.lastIndex)
        historyLoaded = true; previousNewest = newest
    }
    LaunchedEffect(recording) {
        if(recording) { while(recording && seconds < 60) { delay(1000); seconds++ }; if(recording) finishRecording(true) }
    }
    DisposableEffect(Unit) { onDispose { runCatching { recorder?.stop() }; recorder?.release(); recordingFile?.delete(); player?.release() } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatHeader(peerName, { if(recording) finishRecording(false); back() }, { confirmCall = true }, buy, !busy && !recording) },
        bottomBar = { Surface(shadowElevation = 8.dp) { Column(Modifier.navigationBarsPadding().imePadding().padding(10.dp)) {
            if(error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(recording) {
                Text("جارٍ التسجيل · $seconds ثانية · 60 ثانية كحد أقصى")
                Row { Button(onClick = { finishRecording(true) }) { Text("إرسال التسجيل") }; TextButton(onClick = { finishRecording(false) }) { Text("إلغاء") } }
            } else {
                ChatComposer(body, { changeBody(it.take(2000)) }, busy, { photo.launch("image/*") }, { permission.launch(Manifest.permission.RECORD_AUDIO) }) {
                    perform { api.request("/api/messages/$peerId", "POST", JSONObject().put("body", body)); changeBody("") }
                }
            }
        } } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if(rows.isEmpty() && error.isBlank()) Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("مساحة لحديثكما", style = MaterialTheme.typography.titleLarge); Text("اكتب رسالة أو أرسل صورة أو تسجيلًا صوتيًا.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows, key = { it.optInt("id") }) { message ->
                val mine = message.optInt("sender_id") == myId
                ChatBubble(message, mine, { media ->
                    player?.release()
                    runCatching { MediaPlayer().also { p -> player = p; p.setDataSource(media); p.setOnPreparedListener { it.start() }; p.setOnCompletionListener { it.release(); if(player == it) player = null }; p.setOnErrorListener { mp, _, _ -> mp.release(); if(player == mp) player = null; error = "تعذّر تشغيل التسجيل."; true }; p.prepareAsync() } }.onFailure { error = "تعذّر تشغيل التسجيل." }
                }, { openCall(it) })
            }
        }
        }
    }
    if(confirmCall) AlertDialog(onDismissRequest = { confirmCall = false }, title = { Text("اتصال داخل Relaxia") }, text = { Text("اختر نوع المكالمة. سيفتح الميكروفون أو الكاميرا بعد إذنك. الطرف الآخر يحتاج أن يكون التطبيق مفتوحًا لاستقبال الرنين.") }, confirmButton = {
        TextButton(onClick = { confirmCall = false; perform { val call = api.request("/api/calls", "POST", JSONObject().put("peer_id", peerId).put("mode", "audio")); openCall(call.getString("id")) } }) { Text("اتصال صوتي") }
    }, dismissButton = {
        TextButton(onClick = { confirmCall = false; perform { val call = api.request("/api/calls", "POST", JSONObject().put("peer_id", peerId).put("mode", "video")); openCall(call.getString("id")) } }) { Text("اتصال فيديو") }
    })
}
