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
internal fun ConversationScreen(api: LayaliApi, peerId: Int, peerName: String, myId: Int, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var confirmCall by remember { mutableStateOf(false) }
    var confirmJoin by remember { mutableStateOf("") }
    suspend fun refresh() {
        val a = api.request("/api/messages/$peerId").optJSONArray("items")
        rows = if(a == null) emptyList() else (0 until a.length()).map { a.getJSONObject(it) }.reversed()
    }
    fun perform(block: suspend () -> Unit) {
        if(busy) return
        busy = true; error = ""
        scope.launch { try { block(); refresh() } catch(e: CancellationException) { throw e } catch(e: Exception) { error = if(e.message == "paid_chat_requires_verified_payment") "هذه المحادثة مدفوعة؛ لم تُرسل الرسالة ولم يُخصم رصيد." else commerceError(e) } finally { busy = false } }
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
    val newest = rows.lastOrNull()?.optInt("id") ?: 0
    LaunchedEffect(newest) {
        if(rows.isNotEmpty() && (listState.layoutInfo.totalItemsCount == 0 || listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size >= rows.size - 2 || rows.last().optInt("sender_id") == myId)) listState.animateScrollToItem(rows.lastIndex)
    }
    LaunchedEffect(recording) {
        if(recording) { while(recording && seconds < 60) { delay(1000); seconds++ }; if(recording) finishRecording(true) }
    }
    DisposableEffect(Unit) { onDispose { runCatching { recorder?.stop() }; recorder?.release(); recordingFile?.delete(); player?.release() } }
    Scaffold(
        topBar = { Surface { Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { if(recording) finishRecording(false); back() }) { Text("رجوع") }
            Text(peerName, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { confirmCall = true }, enabled = !busy && !recording) { Text("اتصال") }
        } } },
        bottomBar = { Surface(shadowElevation = 8.dp) { Column(Modifier.navigationBarsPadding().imePadding().padding(10.dp)) {
            if(error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(recording) {
                Text("جارٍ التسجيل · $seconds ثانية · 60 ثانية كحد أقصى")
                Row { Button(onClick = { finishRecording(true) }) { Text("إرسال التسجيل") }; TextButton(onClick = { finishRecording(false) }) { Text("إلغاء") } }
            } else {
                OutlinedTextField(body, { body = it.take(2000) }, placeholder = { Text("اكتب رسالتك") }, maxLines = 4, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { photo.launch("image/*") }, enabled = !busy) { Text("صورة") }
                    TextButton(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !busy) { Text("تسجيل صوتي") }
                    Button(onClick = { perform { api.request("/api/messages/$peerId", "POST", JSONObject().put("body", body)); body = "" } }, enabled = !busy && body.isNotBlank()) { Text("إرسال") }
                }
            }
        } } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows, key = { it.optInt("id") }) { message ->
                val mine = message.optInt("sender_id") == myId
                // Explicit LTR positioning keeps outgoing messages on the physical right in Arabic.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if(mine) Arrangement.End else Arrangement.Start) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Surface(Modifier.widthIn(max = 300.dp), shape = RoundedCornerShape(18.dp), color = if(mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if(message.optString("body").isNotBlank()) Text(message.optString("body"))
                                    val media = message.optString("attachment_url")
                                    if(media.isNotBlank() && message.optString("attachment_mime").startsWith("image/")) ReferenceImage(media, Modifier.width(250.dp).height(220.dp))
                                    if(media.isNotBlank() && message.optString("attachment_mime").startsWith("audio/")) TextButton(onClick = {
                                        player?.release()
                                        runCatching { MediaPlayer().also { p -> player = p; p.setDataSource(media); p.setOnPreparedListener { it.start() }; p.setOnCompletionListener { it.release(); if(player == it) player = null }; p.setOnErrorListener { mp, _, _ -> mp.release(); if(player == mp) player = null; error = "تعذّر تشغيل التسجيل."; true }; p.prepareAsync() } }.onFailure { error = "تعذّر تشغيل التسجيل." }
                                    }) { Text("▶ تشغيل التسجيل الصوتي") }
                                    if(message.optString("call_url").startsWith("https://meet.jit.si/Relaxia-")) TextButton(onClick = { confirmJoin = message.optString("call_url") }) { Text("الانضمام إلى الاتصال") }
                                    Text(message.optString("created_at").substringAfter('T').take(5), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if(confirmCall) AlertDialog(onDismissRequest = { confirmCall = false }, title = { Text("اتصال عبر Jitsi") }, text = { Text("سيُرسل رابط غرفة خاصة للطرف الآخر. الاتصال يفتح عبر خدمة Jitsi الخارجية وقد تتطلب من المضيف تسجيل الدخول. الكاميرا والميكروفون بإذنك، ولا توجد رنّة أو مكالمة خلفية داخل التطبيق.") }, confirmButton = { TextButton(onClick = { confirmCall = false; perform { val call = api.request("/api/messages/$peerId", "POST", JSONObject().put("start_call", true)); confirmJoin = call.getString("call_url") } }) { Text("إرسال دعوة اتصال") } }, dismissButton = { TextButton({ confirmCall = false }) { Text("إلغاء") } })
    if(confirmJoin.isNotBlank()) AlertDialog(onDismissRequest = { confirmJoin = "" }, title = { Text("فتح غرفة الاتصال") }, text = { Text("ستنتقل إلى Jitsi للانضمام؛ لا تُفتح الكاميرا أو الميكروفون تلقائيًا.") }, confirmButton = { TextButton(onClick = { runCatching { uri.openUri(confirmJoin) }; confirmJoin = "" }) { Text("فتح الاتصال") } }, dismissButton = { TextButton({ confirmJoin = "" }) { Text("إلغاء") } })
}
