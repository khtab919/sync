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
import com.example.data.api.MessageOutbox
import com.example.data.api.MessageInbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val outbox = remember(api.origin, myId, peerId) { MessageOutbox(context, api.origin, myId, peerId) }
    val inbox=remember(api.origin,myId){MessageInbox(context,api.origin,myId)}
    val cached=remember(peerId){runCatching{inbox.read(peerId)}.getOrDefault(emptyList())}
    var pending by remember { mutableStateOf(runCatching { outbox.read() }.getOrDefault(emptyList())) }
    var actualPeer by remember { mutableStateOf(peerName.takeUnless { it.startsWith("حساب #") }.orEmpty().ifBlank{cached.lastOrNull()?.let{if(it.optInt("sender_id")==myId)it.optString("recipient_name")else it.optString("sender_name")}.orEmpty()}) }
    var peerHandle by remember { mutableStateOf("") }
    var peerAvatar by remember { mutableStateOf("") }
    var peerVerified by remember { mutableStateOf(false) }
    var peerPrice by remember { mutableLongStateOf(0L) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var rows by remember { mutableStateOf<List<JSONObject>>(cached) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    val voice = remember { VoicePlayback(context) }
    var confirmCall by remember { mutableStateOf(false) }
    suspend fun refresh() {
        val result=api.request("/api/messages/$peerId")
        result.optJSONObject("peer")?.let { actualPeer=it.optString("display_name",it.optString("username"));peerHandle=it.optString("username");peerAvatar=it.optString("avatar_url");peerVerified=it.optBoolean("creator");peerPrice=it.optLong("chat_price_cents") }
        val a=result.optJSONArray("items")
        val loaded=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)}.reversed()
        val acknowledged=loaded.map{it.optString("client_key")}.filter{it.isNotBlank()}.toSet()
        withContext(Dispatchers.IO){outbox.read().filter{it.optString("client_key") in acknowledged}.forEach{outbox.remove(it.optString("client_key"))}}
        withContext(Dispatchers.IO){inbox.save(loaded)}
        rows=loaded;pending=withContext(Dispatchers.IO){outbox.read()}
        val delivered=org.json.JSONArray(loaded.filter{it.optInt("sender_id")!=myId&&it.isNull("delivered_at")}.map{it.optInt("id")})
        if(delivered.length()>0)api.request("/api/messages/$peerId/delivered","POST",JSONObject().put("ids",delivered))
    }
    fun enqueue(uri:Uri?,kind:String) {
        if(uri==null){try{outbox.text(body.trim());pending=outbox.read();changeBody("");error=""}catch(e:Exception){error="تعذّر حفظ الرسالة للإرسال."};return}
        scope.launch { try { withContext(Dispatchers.IO){outbox.file(uri,kind)};pending=withContext(Dispatchers.IO){outbox.read()};error="" }catch(e:Exception){error="تعذّر حفظ الملف. الحد الأقصى 8MB."} }
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
        if(send&&valid&&file!=null) {
            scope.launch { try { withContext(Dispatchers.IO){outbox.file(Uri.fromFile(file),"chat_audio")};pending=withContext(Dispatchers.IO){outbox.read()} } catch(e:Exception){error="تعذّر حفظ التسجيل للإرسال."} finally{file.delete()} }
        } else file?.delete()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted && !busy && !recording) {
            runCatching {
                voice.pause()
                val file = File.createTempFile("relaxia-voice-", ".m4a", context.cacheDir)
                recordingFile = file
                @Suppress("DEPRECATION") val r = MediaRecorder()
                recorder = r
                r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setAudioEncodingBitRate(64000); r.setAudioSamplingRate(44100); r.setOutputFile(file.absolutePath); r.prepare(); r.start()
                seconds = 0; recording = true
            }.onFailure { recorder?.release(); recorder = null; recordingFile?.delete(); recordingFile = null; error = "تعذّر بدء تسجيل الصوت." }
        } else if(!granted) error = "اسمح باستخدام الميكروفون لتسجيل رسالة صوتية."
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){selected->if(selected!=null)enqueue(selected,"chat_image")}
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if(event == Lifecycle.Event.ON_PAUSE) { if(recording) finishRecording(false); voice.pause() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(peerId,resumed) {
        if(!resumed)return@LaunchedEffect
        var reliable=false
        var versionKnown=false
        var checkedAt=0L
        while(true) {
            if(!reliable&&System.currentTimeMillis()-checkedAt>10000){checkedAt=System.currentTimeMillis();try{reliable=api.request("/health/").optInt("messaging_version")>=2;versionKnown=true}catch(e:CancellationException){throw e}catch(e:Exception){versionKnown=false}}
            try {
                val first=withContext(Dispatchers.IO){outbox.read().firstOrNull{!it.optBoolean("failed")}}
                if(first!=null&&reliable) {
                    if(first.optString("kind")!="text"&&!first.has("attachment_id")) {
                        val media=api.upload(context,Uri.fromFile(outbox.fileFor(first)),first.getString("kind"))
                        first.put("attachment_id",media.getString("id"));withContext(Dispatchers.IO){outbox.update(first)}
                    }
                    val payload=JSONObject().put("client_key",first.getString("client_key")).put("body",first.optString("body"))
                    if(first.has("attachment_id"))payload.put("attachment_id",first.getString("attachment_id"))
                    try{val sent=api.request("/api/messages/$peerId","POST",payload);rows=rows.filterNot{it.optString("client_key")==first.optString("client_key")}+sent;withContext(Dispatchers.IO){outbox.remove(first.getString("client_key"))};error=""}
                    catch(e:Exception){if(e.message in listOf("paid_chat_requires_verified_payment","recipient_unavailable","invalid_body","invalid_attachment","message_request_conflict")){first.put("failed",true);withContext(Dispatchers.IO){outbox.update(first)}}else throw e}
                    pending=withContext(Dispatchers.IO){outbox.read()}
                } else if(first!=null&&!reliable) error=if(versionKnown)"حدّث الخادم لتفعيل الإرسال الموثوق وحالات تسليم الرسائل."else"بانتظار الاتصال لإرسال الرسائل…"
            }catch(e:CancellationException){throw e}catch(e:Exception){error="بانتظار الاتصال لإرسال الرسائل…"}
            try { refresh() }catch(e:CancellationException){throw e}catch(e:Exception){if(pending.isEmpty())error=commerceError(e)}
            delay(if(pending.isEmpty())2500 else 900)
        }
    }
    LaunchedEffect(recording) { while(recording){amplitude=runCatching{(recorder?.maxAmplitude?:0)/32767f}.getOrDefault(0f);delay(120)};amplitude=0f }
    val confirmedKeys=rows.map{it.optString("client_key")}.filter{it.isNotBlank()}.toSet()
    val displayed=rows+pending.filterNot{it.optString("client_key") in confirmedKeys}.map{outbox.pendingJson(it,myId)}
    LaunchedEffect(voice.playing){while(voice.playing){voice.tick();delay(200)}}
    LaunchedEffect(pending.lastOrNull()?.optString("client_key")){if(pending.isNotEmpty())listState.scrollToItem(displayed.lastIndex)}
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
    DisposableEffect(Unit) { onDispose { runCatching { recorder?.stop() }; recorder?.release(); recordingFile?.delete(); voice.close() } }
    Scaffold(
        containerColor = conversationColors().background,
        topBar = { ChatHeader(actualPeer.ifBlank { "المحادثة" }, { if(recording) finishRecording(false); back() }, { confirmCall = true }, buy, !busy && !recording, peerHandle, peerAvatar, video = { perform { val call = api.request("/api/calls", "POST", JSONObject().put("peer_id", peerId).put("mode", "video")); openCall(call.getString("id")) } }, verified = peerVerified, priceCoins = peerPrice) },
        bottomBar = { Surface(color = conversationColors().background) { Column(Modifier.navigationBarsPadding().imePadding()) {
            if(error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if(recording) {
                RecordingIndicator(seconds, amplitude)
                Row { Button(onClick = { finishRecording(true) }) { Text("إرسال التسجيل") }; TextButton(onClick = { finishRecording(false) }) { Text("إلغاء") } }
            } else {
                ChatComposer(body, { changeBody(it.take(2000)) }, busy, { photo.launch("image/*") }, { permission.launch(Manifest.permission.RECORD_AUDIO) }) {
                    enqueue(null, "text")
                }
            }
        } } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if(displayed.isEmpty() && error.isBlank()) Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("مساحة لحديثكما", style = MaterialTheme.typography.titleLarge); Text("اكتب رسالة أو أرسل صورة أو تسجيلًا صوتيًا.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayed, key = { it.optString("client_key").ifBlank { it.optInt("id").toString() } }) { message ->
                val mine = message.optInt("sender_id") == myId
                val index = displayed.indexOf(message)
                val day = message.optString("created_at").take(10)
                val previousDay = displayed.getOrNull(index - 1)?.optString("created_at")?.take(10)
                if(day.isNotBlank() && day != previousDay) ChatDayLabel(runCatching {
                    val date = java.time.LocalDate.parse(day)
                    when(date) { java.time.LocalDate.now() -> "اليوم"; java.time.LocalDate.now().minusDays(1) -> "أمس"; else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("ar"))) }
                }.getOrDefault(day))
                CompositionLocalProvider(LocalVoicePlayback provides voice) {
                    ChatBubble(message,mine,{}, {openCall(it)}, {key->scope.launch{val row=outbox.read().firstOrNull{it.optString("client_key")==key};if(row!=null){row.remove("failed");withContext(Dispatchers.IO){outbox.update(row)};pending=outbox.read()}}}, {key->scope.launch{withContext(Dispatchers.IO){outbox.remove(key)};pending=outbox.read()}}, peerName = actualPeer, peerAvatar = peerAvatar)
                }
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
