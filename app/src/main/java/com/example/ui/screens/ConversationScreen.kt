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
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject
import java.io.File

@Composable
internal fun ConversationScreen(api: LayaliApi, peerId: Int, peerName: String, myId: Int, body: String, changeBody: (String) -> Unit, buy: () -> Unit, openCall: (String) -> Unit, isSeller: Boolean = false, back: () -> Unit) {
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
    var peerOnline by remember { mutableStateOf(false) }
    var presenceKnown by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var amplitudeHistory by remember { mutableStateOf<List<Float>>(emptyList()) }
    var chooseAttachment by remember { mutableStateOf(false) }
    var messagingVersion by remember { mutableIntStateOf(0) }
    var staged by remember { mutableStateOf(false) }
    var stagedUri by remember { mutableStateOf<Uri?>(null) }
    var stagedKind by remember { mutableStateOf("text") }
    var stagedText by remember { mutableStateOf("") }
    var stagedCost by remember { mutableLongStateOf(0L) }
    var mediaPriceInput by remember { mutableStateOf("0") }
    var unlockTarget by remember { mutableStateOf<JSONObject?>(null) }
    var replyTarget by remember{mutableStateOf<JSONObject?>(null)}
    var deleteTarget by remember{mutableStateOf<JSONObject?>(null)}
    var blockConfirm by remember{mutableStateOf(false)}
    var paidCallChoices by remember{mutableStateOf<List<JSONObject>>(emptyList())}
    var pendingCallMode by remember{mutableStateOf("audio")}
    var retryKey by remember { mutableStateOf("") }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var nextOlder by remember { mutableStateOf("") }
    var olderLoaded by remember { mutableStateOf(false) }
    var loadingOlder by remember { mutableStateOf(false) }
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
        result.optJSONObject("peer")?.let { actualPeer=it.optString("display_name",it.optString("username"));peerHandle=it.optString("username");peerAvatar=it.optString("avatar_url");peerVerified=it.optBoolean("creator");peerPrice=it.optLong("chat_price_cents");peerOnline=it.optBoolean("online");presenceKnown=it.has("online") }
        val a=result.optJSONArray("items")
        val loaded=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)}.reversed()
        val acknowledged=loaded.map{it.optString("client_key")}.filter{it.isNotBlank()}.toSet()
        withContext(Dispatchers.IO){outbox.read().filter{it.optString("client_key") in acknowledged}.forEach{outbox.remove(it.optString("client_key"))}}
        withContext(Dispatchers.IO){inbox.save(loaded)}
        if(!olderLoaded)nextOlder=result.optString("next_cursor").takeUnless{it=="null"}.orEmpty()
        rows=(rows+loaded).associateBy{it.optInt("id")}.values.sortedBy{it.optInt("id")};pending=withContext(Dispatchers.IO){outbox.read()}
        val delivered=org.json.JSONArray(loaded.filter{it.optInt("sender_id")!=myId&&it.isNull("delivered_at")}.map{it.optInt("id")})
        if(delivered.length()>0)api.request("/api/messages/$peerId/delivered","POST",JSONObject().put("ids",delivered))
    }
    fun clearTemporary(source:Uri?) { if(source?.scheme=="file")source.path?.let{File(it)}?.takeIf{it.parentFile==context.cacheDir && it.name.startsWith("relaxia-voice-")}?.delete() }
    fun queueActual(source:Uri?,kind:String,text:String,cost:Long,mediaPrice:Long) {
        val replyId=replyTarget?.optInt("id")?.takeIf{it>0};replyTarget=null
        if(source==null){try{outbox.text(text,cost,replyId);pending=outbox.read();if(body.trim()==text)changeBody("");error=""}catch(e:Exception){error="تعذّر حفظ الرسالة للإرسال."};return}
        scope.launch { try{withContext(Dispatchers.IO){outbox.file(source,kind,cost,mediaPrice,replyId)};pending=withContext(Dispatchers.IO){outbox.read()};error=""}catch(e:Exception){error="تعذّر حفظ الملف. الحد الأقصى 8MB."}finally{clearTemporary(source)} }
    }
    fun enqueue(source:Uri?,kind:String) {
        if(peerPrice>0 || (isSeller && kind in listOf("chat_image","chat_video"))) {
            stagedUri=source;stagedKind=kind;stagedText=body.trim();stagedCost=peerPrice;mediaPriceInput="0";staged=true
        } else queueActual(source,kind,body.trim(),0,0)
    }
    fun perform(block: suspend () -> Unit) {
        if(busy) return
        busy = true; error = ""
        scope.launch { try { block(); refresh() } catch(e: CancellationException) { throw e } catch(e: Exception) { error = if(e.message == "calls_setup_required" || e.message == "server_unavailable") "خادم الاتصال غير جاهز بعد. حدّث الخادم لتفعيل الاتصال الداخلي." else if(e.message == "call_busy") "أحد الطرفين مشغول بمكالمة أخرى." else if(e.message == "paid_chat_requires_verified_payment") "هذه المحادثة مدفوعة؛ لم تُرسل الرسالة ولم يُخصم رصيد." else commerceError(e) } finally { busy = false } }
    }
    fun beginCall(mode:String){
        if(peerPrice>0)perform{
            val result=api.request("/api/bookings").optJSONArray("items")
            val choices=if(result==null)emptyList()else(0 until result.length()).map{result.getJSONObject(it)}.filter{it.optInt("owner_id")==peerId&&it.optString("status")=="accepted"}
            if(choices.isEmpty())throw IllegalStateException("paid_booking_required")
            pendingCallMode=mode;paidCallChoices=choices
        }else perform{val call=api.request("/api/calls","POST",JSONObject().put("peer_id",peerId).put("mode",mode));openCall(call.getString("id"))}
    }
    fun finishRecording(send: Boolean) {
        val current = recorder; recorder = null; recording = false; recordingPaused = false
        val file = recordingFile; recordingFile = null
        val valid = runCatching { current?.stop(); true }.getOrDefault(false)
        current?.release()
        if(send&&valid&&file!=null) enqueue(Uri.fromFile(file),"chat_audio") else file?.delete()
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
                seconds = 0; amplitudeHistory=emptyList(); recordingPaused=false; recording = true
            }.onFailure { recorder?.release(); recorder = null; recordingFile?.delete(); recordingFile = null; error = "تعذّر بدء تسجيل الصوت." }
        } else if(!granted) error = "اسمح باستخدام الميكروفون لتسجيل رسالة صوتية."
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){selected->if(selected!=null)enqueue(selected,"chat_video")}
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
        var paidChatReady=false
        var checkedAt=0L
        while(true) {
            if(System.currentTimeMillis()-checkedAt>10000){checkedAt=System.currentTimeMillis();try{val capabilities=api.request("/health/");messagingVersion=capabilities.optInt("messaging_version");paidChatReady=capabilities.optInt("paid_chat_version")>=1;reliable=messagingVersion>=2;versionKnown=true}catch(e:CancellationException){throw e}catch(e:Exception){versionKnown=false}}
            try {
                val first=withContext(Dispatchers.IO){outbox.read().firstOrNull{!it.optBoolean("failed")}}
                if(first!=null&&reliable) {
                    if((first.optLong("price_consent")>0 || first.optLong("media_price_cents")>0)&&!paidChatReady){error="حدّث الخادم لتفعيل الرسائل والمرفقات المدفوعة. لم يُخصم رصيد.";delay(1500);continue}
                    if(first.optString("kind")!="text"&&!first.has("attachment_id")) {
                        val media=api.upload(context,Uri.fromFile(outbox.fileFor(first)),first.getString("kind"))
                        first.put("attachment_id",media.getString("id"));withContext(Dispatchers.IO){outbox.update(first)}
                    }
                    val payload=JSONObject().put("client_key",first.getString("client_key")).put("body",first.optString("body")).put("price_consent",first.optLong("price_consent")).put("media_price_cents",first.optLong("media_price_cents"))
                    if(first.has("reply_to"))payload.put("reply_to",first.getInt("reply_to"))
                    if(first.has("attachment_id"))payload.put("attachment_id",first.getString("attachment_id"))
                    try{val sent=api.request("/api/messages/$peerId","POST",payload);rows=rows.filterNot{it.optString("client_key")==first.optString("client_key")}+sent;withContext(Dispatchers.IO){outbox.remove(first.getString("client_key"))};error=""}
                    catch(e:Exception){if(e.message in listOf("paid_chat_requires_verified_payment","recipient_unavailable","invalid_body","invalid_attachment","message_request_conflict","paid_price_changed","insufficient_balance","creator_account_required","invalid_media_price")){first.put("failed",true).put("failure_reason",if(e.message=="insufficient_balance")"الرصيد غير كافٍ"else if(e.message=="paid_price_changed")"تغيّر سعر الرسالة. أعد تأكيد السعر."else"تعذّر الإرسال");withContext(Dispatchers.IO){outbox.update(first)}}else throw e}
                    pending=withContext(Dispatchers.IO){outbox.read()}
                } else if(first!=null&&!reliable) error=if(versionKnown)"حدّث الخادم لتفعيل الإرسال الموثوق وحالات تسليم الرسائل."else"بانتظار الاتصال لإرسال الرسائل…"
            }catch(e:CancellationException){throw e}catch(e:Exception){error="بانتظار الاتصال لإرسال الرسائل…"}
            try { refresh() }catch(e:CancellationException){throw e}catch(e:Exception){peerOnline=false;presenceKnown=false;if(pending.isEmpty())error=commerceError(e)}
            delay(if(pending.isEmpty())1500 else 900)
        }
    }
    LaunchedEffect(recording,recordingPaused) { while(recording&&!recordingPaused){amplitude=runCatching{(recorder?.maxAmplitude?:0)/32767f}.getOrDefault(0f);amplitudeHistory=(amplitudeHistory+amplitude).takeLast(40);delay(120)};amplitude=0f }
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
    LaunchedEffect(recording,recordingPaused) {
        if(recording&&!recordingPaused) { while(recording && !recordingPaused && seconds < 60) { delay(1000); seconds++ }; if(recording && seconds>=60) finishRecording(true) }
    }
    LaunchedEffect(resumed,rows,messagingVersion) {
        if(!resumed || messagingVersion<3)return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map{it.key.toString()} }.distinctUntilChanged().collect { keys ->
            val visible=rows.filter{it.optInt("sender_id")!=myId && it.isNull("read_at") && (it.optString("client_key").ifBlank{it.optInt("id").toString()}) in keys}.map{it.optInt("id")}.filter{it>0}
            if(visible.isNotEmpty())try{api.request("/api/messages/$peerId/read","POST",JSONObject().put("ids",org.json.JSONArray(visible)))}catch(e:CancellationException){throw e}catch(_:Exception){}
        }
    }
    DisposableEffect(Unit) { onDispose { clearTemporary(stagedUri);runCatching { recorder?.stop() }; recorder?.release(); recordingFile?.delete(); voice.close() } }
    Scaffold(
        containerColor = conversationColors().background,
        topBar = { ChatHeader(actualPeer.ifBlank { "المحادثة" }, { if(recording) finishRecording(false); back() }, { confirmCall = true }, buy, !busy && !recording, peerHandle, peerAvatar, video = { beginCall("video") }, verified = peerVerified, priceCoins = peerPrice, online = peerOnline, presenceKnown = presenceKnown, block={blockConfirm=true}) },
        bottomBar = { Surface(color = conversationColors().background) { Column(Modifier.navigationBarsPadding().imePadding()) {
            if(replyTarget!=null)Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Text("رد: "+replyTarget!!.optString("body").ifBlank{"مرفق"},Modifier.weight(1f),maxLines=1,style=MaterialTheme.typography.bodySmall);TextButton({replyTarget=null}){Text("إلغاء")}}
            if(error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if(recording) {
                RecordingIndicator(seconds, amplitude, amplitudeHistory, recordingPaused)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    TextButton(onClick={runCatching{if(recordingPaused)recorder?.resume()else recorder?.pause()}.onSuccess{recordingPaused=!recordingPaused}.onFailure{error="تعذّر تغيير حالة التسجيل."}}){Text(if(recordingPaused)"استكمال"else"إيقاف مؤقت")}
                    Button(onClick={finishRecording(true)}){Text("إرسال")}
                    TextButton(onClick={finishRecording(false)}){Text("إلغاء")}
                }
            } else {
                ChatComposer(body, { changeBody(it.take(2000)) }, busy, { chooseAttachment = true }, { permission.launch(Manifest.permission.RECORD_AUDIO) }) {
                    enqueue(null, "text")
                }
            }
        } } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if(displayed.isEmpty() && error.isBlank()) Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("مساحة لحديثكما", style = MaterialTheme.typography.titleLarge); Text("اكتب رسالة أو أرسل صورة أو تسجيلًا صوتيًا.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if(nextOlder.isNotBlank()) item(key="older") {
                TextButton(enabled=!loadingOlder,onClick={scope.launch{
                    loadingOlder=true
                    try{val result=api.request("/api/messages/$peerId?before=$nextOlder");val array=result.getJSONArray("items");val older=(0 until array.length()).map{array.getJSONObject(it)};rows=(older+rows).associateBy{it.optInt("id")}.values.sortedBy{it.optInt("id")};nextOlder=result.optString("next_cursor").takeUnless{it=="null"}.orEmpty();olderLoaded=true}
                    catch(e:Exception){error="تعذّر تحميل الرسائل الأقدم. أعد المحاولة."}finally{loadingOlder=false}
                }}){Text(if(loadingOlder)"تحميل…"else"عرض الرسائل الأقدم")}
            }
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
                    ChatBubble(message,mine,{}, {openCall(it)}, {key->scope.launch{val row=outbox.read().firstOrNull{it.optString("client_key")==key};if(row!=null){if(row.optString("failure_reason").contains("سعر")){retryKey=key;stagedCost=peerPrice;stagedText=row.optString("body");stagedUri=null;stagedKind="retry";mediaPriceInput="0";staged=true}else{row.remove("failed");withContext(Dispatchers.IO){outbox.update(row)};pending=outbox.read()}}}}, {key->scope.launch{withContext(Dispatchers.IO){outbox.remove(key)};pending=outbox.read()}}, peerName = actualPeer, peerAvatar = peerAvatar, unlock = { unlockTarget=message }, reply={replyTarget=message},delete={deleteTarget=message})
                }
            }
        }
        }
    }
    deleteTarget?.let{target->AlertDialog(onDismissRequest={deleteTarget=null},title={Text("حذف الرسالة للجميع")},text={Text("يُحذف محتوى الرسالة من المحادثة. تبقى سجلات العملية المالية الضرورية.")},confirmButton={TextButton({deleteTarget=null;perform{api.request("/api/messages/${target.optInt("id")}/action","POST",JSONObject().put("action","delete"))}}){Text("حذف")}},dismissButton={TextButton({deleteTarget=null}){Text("إلغاء")}})}
    if(blockConfirm)AlertDialog(onDismissRequest={blockConfirm=false},title={Text("حظر $actualPeer")},text={Text("لن تتاح الرسائل والمكالمات والبث بينكما. يمكنك إلغاء الحظر من إعدادات الخصوصية.")},confirmButton={TextButton({blockConfirm=false;perform{api.request("/api/blocks","POST",JSONObject().put("user_id",peerId).put("blocked",true));back()}}){Text("حظر")}},dismissButton={TextButton({blockConfirm=false}){Text("إلغاء")}})
    if(paidCallChoices.isNotEmpty())AlertDialog(onDismissRequest={paidCallChoices=emptyList()},title={Text("اختر الحجز المقبول")},text={Column{paidCallChoices.forEach{booking->TextButton({paidCallChoices=emptyList();perform{val call=api.request("/api/calls","POST",JSONObject().put("peer_id",peerId).put("mode",pendingCallMode).put("booking_id",booking.getString("id")));openCall(call.getString("id"))}}){Text(booking.optString("title")+" · "+booking.optInt("duration_minutes")+" دقيقة")}}}},confirmButton={TextButton({paidCallChoices=emptyList()}){Text("إغلاق")}})
    if(staged) AlertDialog(onDismissRequest={staged=false;clearTemporary(stagedUri);stagedUri=null},title={Text("تأكيد الإرسال")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text(if(stagedCost>0)"سيُخصم $stagedCost رصيد من رصيدك لإرسال هذه الرسالة. يصل لصاحب الحساب 80٪ من المبلغ."else"إرسال مرفق خاص إلى $actualPeer")
        if(isSeller && stagedKind in listOf("chat_image","chat_video")) {
            Text("سعر فتح المرفق للمستلم · 0 للإرسال مجانًا")
            OutlinedTextField(mediaPriceInput,{mediaPriceInput=it.filter{c->c.isDigit()}.take(7)},label={Text("رصيد")},singleLine=true)
            Text("تحصل على 80٪ عند الشراء. لا يُخصم من المستلم دون تأكيده.",style=MaterialTheme.typography.bodySmall)
        }
    }},confirmButton={TextButton(onClick={
        val price=mediaPriceInput.toLongOrNull()
        if(price==null || price !in 0..1000000){error="أدخل سعرًا صحيحًا حتى 1000000 رصيد.";return@TextButton}
        staged=false
        if(stagedKind=="retry")scope.launch{val row=outbox.read().firstOrNull{it.optString("client_key")==retryKey};if(row!=null){row.remove("failed");row.put("price_consent",stagedCost);withContext(Dispatchers.IO){outbox.update(row)};pending=outbox.read()}}
        else queueActual(stagedUri,stagedKind,stagedText,stagedCost,price)
        stagedUri=null
    }){Text(if(stagedCost>0)"تأكيد وخصم $stagedCost"else"إرسال")}},dismissButton={TextButton({staged=false;clearTemporary(stagedUri);stagedUri=null}){Text("إلغاء")}})
    unlockTarget?.let { target -> AlertDialog(onDismissRequest={unlockTarget=null},title={Text("فتح المرفق الخاص")},text={Text("سيُخصم ${target.optLong("media_price_cents")} رصيد مرة واحدة من رصيدك. تحصل الجهة الناشرة على 80٪. يبقى المرفق متاحًا لك بعد الشراء.")},confirmButton={TextButton({unlockTarget=null;perform{api.request("/api/messages/${target.optInt("id")}/unlock","POST",JSONObject().put("price_cents",target.optLong("media_price_cents")))}}){Text("تأكيد الشراء")}},dismissButton={TextButton({unlockTarget=null}){Text("إلغاء")}}) }
    if(chooseAttachment) AlertDialog(onDismissRequest={chooseAttachment=false},title={Text("إرسال مرفق")},text={Text("اختر صورة أو فيديو MP4. الحد الأقصى 8MB.")},confirmButton={TextButton({chooseAttachment=false;photo.launch("image/*")}){Text("صورة")}},dismissButton={TextButton({chooseAttachment=false;videoPicker.launch("video/mp4")}){Text("فيديو")}})
    if(confirmCall) AlertDialog(onDismissRequest = { confirmCall = false }, title = { Text("اتصال داخل Relaxia") }, text = { Text("اختر نوع المكالمة. سيفتح الميكروفون أو الكاميرا بعد إذنك. الطرف الآخر يحتاج أن يكون التطبيق مفتوحًا لاستقبال الرنين.") }, confirmButton = {
        TextButton(onClick = { confirmCall = false; beginCall("audio") }) { Text("اتصال صوتي") }
    }, dismissButton = {
        TextButton(onClick = { confirmCall = false; beginCall("video") }) { Text("اتصال فيديو") }
    })
}
