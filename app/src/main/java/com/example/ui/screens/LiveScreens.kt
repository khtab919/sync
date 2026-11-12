package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.*
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.*

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.api.LayaliApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder

private fun liveError(e:Exception)=when(e.message){
    "live_update_required"->"حدّث الخادم لتفعيل الغرف الصوتية ذات 15 مقعدًا والهدايا الجديدة."
    "live_not_configured","live_server_unavailable"->"خادم البث غير متاح. حدّث الخادم وتحقق من إعداداته."
    "invalid_stage_mode","invalid_live_limits"->"تحقق من عدد المتحدثين والمشاهدين ومدة الخدمة."
    "already_broadcasting"->"لديك بث جارٍ بالفعل. أنهِ الجلسة السابقة أولًا."
    "live_capacity"->"اكتمل العدد المسموح لهذه الجلسة."
    "host_not_transmitting"->"صاحب البث يعيد الاتصال. حاول بعد قليل."
    "invitation_required"->"هذا البث مخصص للمدعوين."
    "broadcast_forbidden"->"لا يملك حسابك صلاحية دخول هذا البث."
    "broadcast_ended"->"انتهى البث."
    "unknown_invited_user"->"تحقق من أسماء المستخدمين المدعوين."
    "admission_refunded"->"أُعيد مبلغ هذه الجلسة؛ لا يتاح دخولها بهذا الطلب."
    else->commerceError(e)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LiveLobby(api:LayaliApi,baseUrl:String,openProfile:(Int)->Unit,buy:()->Unit,initialFilter:String="all",back:()->Unit){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")}
    var allowed by remember{mutableStateOf(false)};var available by remember{mutableStateOf(false)};var maximumLimit by remember{mutableIntStateOf(25)};var busy by remember{mutableStateOf(false)};var create by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")};var privateLive by remember{mutableStateOf(false)};var inviteOnly by remember{mutableStateOf(false)};var invites by remember{mutableStateOf("")}
    var price by remember{mutableStateOf("100")};var limit by remember{mutableStateOf("25")};var minimum by remember{mutableStateOf("30")};var gifts by remember{mutableStateOf(true)}
    var roomFilter by remember(initialFilter){mutableStateOf(if(initialFilter in listOf("audio","video","free","paid"))initialFilter else "all")};var audioOnly by remember(initialFilter){mutableStateOf(initialFilter=="audio")};var speakers by remember(initialFilter){mutableStateOf(if(initialFilter=="audio")"15" else "1")};var giftGoal by remember{mutableStateOf("0")}
    var selected by remember{mutableStateOf<JSONObject?>(null)};var active by remember{mutableStateOf<JSONObject?>(null)};var activeId by remember{mutableStateOf("")}
    fun act(block:suspend()->Unit){if(busy)return;busy=true;error="";scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=liveError(e)}finally{busy=false}}}
    fun enter(row:JSONObject){act{active=api.request("/api/live/${row.getString("id")}/admission","POST",JSONObject().put("confirmed",true).put("price_cents",row.optLong("price_cents")));activeId=row.getString("id");selected=null}}
    LaunchedEffect(query){while(true){try{val result=api.request("/api/live?q="+URLEncoder.encode(query,"UTF-8"));if(result.optInt("stage_version")<3)throw IllegalStateException("live_update_required");val a=result.optJSONArray("items");rows=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)};allowed=result.optBoolean("can_broadcast");available=result.optBoolean("available");maximumLimit=result.optInt("max_viewers",25);error=""}catch(e:CancellationException){throw e}catch(e:Exception){allowed=false;available=false;rows=emptyList();error=liveError(e)};delay(5000)}}
    active?.let{room->
        val broadcastId=activeId
        // A WebView failure must not silently end an owner's live room or charge a paid guest again.
        key(room.optString("ticket")){
            LiveRoomView(
                baseUrl=baseUrl,
                bootstrap=room,
                profile={id->active=null;openProfile(id)},
                retry={
                    if(broadcastId.isNotBlank() && !busy){
                        // Unmount the existing WebView before opening a new one so its
                        // media captures cannot race the next live-room instance.
                        active=null
                        act{
                            val fresh=api.request("/api/live/$broadcastId/admission","POST",JSONObject())
                            active=fresh
                        }
                    }
                },
                finish={
                    val ending=room.optBoolean("owner")
                    active=null
                    if(ending)scope.launch{
                        runCatching{api.request("/api/live/$broadcastId","POST",JSONObject().put("action","end"))}
                    }
                }
            )
        }
        return
    }
    val filteredRooms=rows.filter{when(roomFilter){"audio","video"->it.optString("mode")==roomFilter;"free"->!it.optBoolean("private");"paid"->it.optBoolean("private");else->true}}
    Scaffold(topBar={TopAppBar(title={Text("الغرف والبث")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowForward,"رجوع")}},actions={TextButton(buy){Text("الرصيد")};if(allowed)IconButton({create=true},enabled=available&&!busy){Icon(Icons.Outlined.Videocam,"بدء بث")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{OutlinedTextField(query,{query=it.take(100)},label={Text("ابحث عن بث أو صاحب بث")},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item{Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("all" to "الكل","audio" to "غرف صوتية","video" to "فيديو","free" to "مجانية","paid" to "مدفوعة").forEach{(key,label)->
                    FilterChip(roomFilter==key,{roomFilter=key},label={Text(label)})
                }
            }}
            if(allowed)item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button({audioOnly=true;speakers="15";create=true},enabled=available&&!busy){Text("إنشاء غرفة صوتية")}
                OutlinedButton({audioOnly=false;speakers="1";create=true},enabled=available&&!busy){Text("بث فيديو")}
            }}
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
            if(!available)item{Text("البث ينتظر جاهزية خادم الوسائط. لا توجد جلسات تجريبية.",style=MaterialTheme.typography.bodyMedium)}
            if(rows.isEmpty()&&error.isBlank()&&available)item{Text("لا توجد بثوث نشطة الآن.",style=MaterialTheme.typography.bodyLarge)}
            if(filteredRooms.isEmpty()&&rows.isNotEmpty())item{Text("لا توجد غرف نشطة ضمن هذا القسم الآن.",style=MaterialTheme.typography.bodyMedium)}
            items(filteredRooms,key={it.getString("id")}){row->LiveBroadcastCard(row){selected=row}}
            item{Text("لا يُسجَّل البث ولا تتوفر إعادة مشاهدة داخل المنصة. لا يمكن ضمان منع التسجيل بأدوات خارجية.",style=MaterialTheme.typography.bodySmall)}
        }
    }
    selected?.let{row->AlertDialog(onDismissRequest={selected=null},title={Text(row.optString("title"))},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(row.optString("owner_name"));Text(if(row.optBoolean("private"))"سعر الدخول: ${row.optLong("price_cents")} من الرصيد"else if(row.optBoolean("invite_only"))"بث بالدعوة · مجاني"else"بث عام مجاني");Text(row.optString("refund_policy"),style=MaterialTheme.typography.bodySmall);TextButton({selected=null;openProfile(row.optInt("owner_id"))}){Text("عرض الملف الشخصي")}}},confirmButton={TextButton({enter(row)},enabled=!busy){Text(if(row.optBoolean("private"))"تأكيد الدخول وحجز المبلغ"else if(row.optString("mode")=="audio")"دخول الغرفة"else"مشاهدة")}},dismissButton={TextButton({selected=null}){Text("إلغاء")}})}
    if(create){LiveHostSetupScreen(onClose={create=false},audioOnly=audioOnly,onAudioOnlyChange={audioOnly=it;speakers=if(it)"15"else"1"},title=title,onTitleChange={title=it},privateLive=privateLive,onPrivateLiveChange={privateLive=it},price=price,onPriceChange={price=it},inviteOnly=inviteOnly,onInviteOnlyChange={inviteOnly=it},invites=invites,onInvitesChange={invites=it},speakers=speakers,onSpeakersChange={speakers=it},limit=limit,onLimitChange={limit=it},minimum=minimum,onMinimumChange={minimum=it},gifts=gifts,onGiftsChange={gifts=it},giftGoal=giftGoal,onGiftGoalChange={giftGoal=it},maximumLimit=maximumLimit,busy=busy,error=error,onStartStream={act{val p=price.toIntOrNull()?:0;val n=limit.toIntOrNull()?:0;val m=minimum.toIntOrNull()?:0;val t=title.trim().ifEmpty{"بث مباشر"};if(t.length<2||n !in 1..maximumLimit||m !in 30..3600||(privateLive&&p !in 1..1000000))throw IllegalArgumentException("invalid_input");val row=api.request("/api/live","POST",JSONObject().put("title",t).put("private",privateLive).put("invite_only",inviteOnly).put("price_cents",p).put("max_viewers",n).put("minimum_seconds",m).put("gifts_enabled",gifts).put("mode",if(audioOnly)"audio"else"video").put("max_speakers",speakers.toInt()).put("gift_goal_cents",giftGoal.toIntOrNull()?:0).put("invite_names",JSONArray(invites.split(',', '،').map{it.trim().removePrefix("@")}.filter{it.isNotBlank()})));create=false;active=api.request("/api/live/${row.getString("id")}/admission","POST",JSONObject());activeId=row.getString("id")}})}
}

@Composable internal fun LiveRoomView(baseUrl:String,bootstrap:JSONObject,profile:((Int)->Unit)?=null,retry:()->Unit,finish:()->Unit){
    val transportTheme=if(MaterialTheme.colorScheme.background.luminance()<.25f)"dark"else"light"
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle;val owner=bootstrap.optBoolean("owner");val audioOnly=bootstrap.optString("mode")=="audio"
    var granted by remember{mutableStateOf(!owner)};var error by remember{mutableStateOf("")};var view by remember{mutableStateOf<WebView?>(null)}
    var requestingGuest by remember{mutableStateOf(false)}
    var requestedMode by remember{mutableStateOf(if(audioOnly)"audio"else"video")}
    // Keep native progress visible until both the room DOM and its media scripts have loaded.
    val roomTicket=bootstrap.optString("ticket")
    var pageReady by remember(roomTicket){mutableStateOf(false)}
    var pageFinished by remember(roomTicket){mutableStateOf(false)}
    var nativeStageStatus by remember(roomTicket){mutableStateOf("جارٍ تحميل واجهة الغرفة")}
    var bootFlags by remember(roomTicket){mutableIntStateOf(-1)}
    var scriptFailure by remember(roomTicket){mutableStateOf("")}
    fun bootFailureReason(flags:Int):String {
        val hint=if(scriptFailure.isBlank())"" else " · $scriptFailure"
        return when {
            flags<0->"D00: تعذر فحص JavaScript في WebView$hint"
            flags and 1==0->"D01: استجابة الخادم لا تتضمن عناصر واجهة الغرفة$hint"
            flags and 2==0->"D02: استجابة الغرفة لا تتضمن إعدادات جلسة البث$hint"
            flags and 4==0->"D03: مكتبة LiveKit غير متاحة داخل صفحة الغرفة$hint"
            flags and 8==0->"D04: ملفات تجهيز الصوت والفيديو لم تكتمل داخل WebView$hint"
            flags and 16==0->"D05: تعطل JavaScript قبل اكتمال تهيئة واجهة البث$hint"
            flags and 32==0->"D06: عناصر الغرفة موجودة لكن مساحة عرضها صفر$hint"
            else->"D07: الصفحة مهيأة لكن WebView لم يؤكد رسمها$hint"
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->
        val accepted=result[Manifest.permission.RECORD_AUDIO]==true&&(requestedMode=="audio"||result[Manifest.permission.CAMERA]==true)
        if(requestingGuest){if(accepted)runCatching{com.example.notifications.MediaSessionService.start(context,true)};view?.evaluateJavascript("window.dispatchEvent(new CustomEvent('publishpermissions',{detail:"+accepted+"}));",null);requestingGuest=false}
        else{granted=accepted;error="";if(!accepted)error=if(audioOnly)"اسمح بالميكروفون لبدء البث الصوتي."else"اسمح بالميكروفون والكاميرا لبدء البث."}
    }
    LaunchedEffect(Unit){if(owner){val list=if(audioOnly)arrayOf(Manifest.permission.RECORD_AUDIO)else arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA);if(list.all{ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})granted=true else permission.launch(list)}}
    // WebView.onPageFinished can precede deferred script execution and GPU compositing.
    // Wait for actual stage DOM readiness, then a visual frame, before removing the native loader.
    LaunchedEffect(granted,pageFinished,roomTicket){
        if(!granted || !pageFinished)return@LaunchedEffect
        repeat(25){
            if(pageReady || error.isNotBlank())return@LaunchedEffect
            // Only return bit flags; never read tokens, chat content or customer data.
            view?.evaluateJavascript(
                "(function(){var e=document.getElementById('stage');return (e?1:0)|(document.getElementById('config')?2:0)|(typeof LivekitClient!=='undefined'?4:0)|(typeof RelaxiaMedia!=='undefined'?8:0)|(window.relaxiaStageLoaded===true?16:0)|(e&&e.getBoundingClientRect().height>1?32:0);})()"
            ){raw->
                val flags=raw?.toIntOrNull() ?: -1
                bootFlags=flags
                if(flags==63 && !pageReady){
                    val current=view
                    current?.postVisualStateCallback(42L,object:WebView.VisualStateCallback(){
                        override fun onComplete(requestId:Long){pageReady=true}
                    })
                }
            }
            delay(950)
        }
        if(!pageReady && error.isBlank())error=bootFailureReason(bootFlags)
    }
    LaunchedEffect(granted,roomTicket){
        if(!granted)return@LaunchedEffect
        delay(32000)
        if(!pageReady && error.isBlank())error=bootFailureReason(bootFlags)
    }
    LaunchedEffect(pageReady,roomTicket){
        if(!pageReady)return@LaunchedEffect
        while(true){
            delay(3000)
            view?.evaluateJavascript("(function(){var s=document.getElementById('status');return (s&&s.textContent||'').slice(0,100)})()"){raw->
                val text=runCatching{JSONArray("[$raw]").optString(0)}.getOrDefault("")
                if(text.isNotBlank())nativeStageStatus=text
            }
        }
    }
    BackHandler{finish()}
    // Screenshots are allowed in Relaxia live rooms. Do not set FLAG_SECURE.
    DisposableEffect(Unit){onDispose{view?.loadUrl("about:blank");view?.destroy()}}
    DisposableEffect(granted){
        if(granted)runCatching{com.example.notifications.MediaSessionService.start(context,owner)}.onFailure{error="تعذّر تشغيل خدمة الجلسة في الخلفية."}
        onDispose{com.example.notifications.MediaSessionService.stop(context)}
    }
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,event->
        if(event==Lifecycle.Event.ON_STOP)view?.evaluateJavascript("window.setNativeBackground?.(true)",null)
        if(event==Lifecycle.Event.ON_START)view?.evaluateJavascript("window.setNativeBackground?.(false)",null)
    };lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
    Surface(Modifier.fillMaxSize(),color=Color(0xFF101820),contentColor=Color.White){Column(Modifier.fillMaxSize().safeDrawingPadding()){
        if(error.isNotBlank()){
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Icon(Icons.Outlined.ErrorOutline,"تعذر تحميل البث",Modifier.size(32.dp),tint=Color(0xFFF4BC97))
                Text(error,color=Color.White,style=MaterialTheme.typography.bodyLarge)
                if(owner&&!granted)TextButton({requestedMode=if(audioOnly)"audio"else"video";permission.launch(if(audioOnly)arrayOf(Manifest.permission.RECORD_AUDIO)else arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA))}){Text("إعادة طلب الأذونات")}
                Button(onClick=retry){Text("إعادة الدخول بتذكرة جديدة")}
                OutlinedButton(onClick=finish){Text("إنهاء والعودة إلى الغرف",color=Color.White)}
            }
        }
        if(granted&&error.isBlank())Box(Modifier.fillMaxSize()){
            AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->WebView(ctx).apply{
            view=this;setBackgroundColor(android.graphics.Color.rgb(23,21,23));settings.javaScriptEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=true;settings.databaseEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.mediaPlaybackRequiresUserGesture=false
            val allowedPath=Uri.parse(bootstrap.getString("url")).path
            addJavascriptInterface(object{@JavascriptInterface fun canOpenProfile():Boolean=profile!=null
            @JavascriptInterface fun openProfile(id:Int){android.os.Handler(android.os.Looper.getMainLooper()).post{if(trustedCallOrigin(url.orEmpty(),baseUrl)&&Uri.parse(url).path==allowedPath)profile?.invoke(id)}}
            @JavascriptInterface fun requestPublishPermissions(mode:String){android.os.Handler(android.os.Looper.getMainLooper()).post{
                if(trustedCallOrigin(url.orEmpty(),baseUrl)&&Uri.parse(url).path==allowedPath&&mode in listOf("audio","video")){
                    requestedMode=mode;val list=if(mode=="audio")arrayOf(Manifest.permission.RECORD_AUDIO)else arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
                    if(list.all{ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})evaluateJavascript("window.dispatchEvent(new CustomEvent('publishpermissions',{detail:true}));",null)
                    else{requestingGuest=true;permission.launch(list)}
                }
            }}
            @JavascriptInterface fun shareLive(text:String){android.os.Handler(android.os.Looper.getMainLooper()).post{if(trustedCallOrigin(url.orEmpty(),baseUrl)&&Uri.parse(url).path==allowedPath&&text.length<=1000){(context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("دعوة Relaxia",text))}}}
            @JavascriptInterface fun closeRoom(){android.os.Handler(android.os.Looper.getMainLooper()).post{if(trustedCallOrigin(url.orEmpty(),baseUrl)&&Uri.parse(url).path==allowedPath)finish()}}},"LayaliLive")
            webViewClient=object:WebViewClient(){
                private fun missingCriticalMedia(r:WebResourceRequest?):Boolean{
                    val path=r?.url?.path ?: return false
                    return trustedCallOrigin(r.url.toString(),baseUrl) &&
                        path.substringAfterLast('/') in setOf("live-stage.css","live-stage.js","live-media.js","livekit-client.umd.min.js")
                }
                override fun onReceivedHttpError(v:WebView?,r:WebResourceRequest?,response:WebResourceResponse?){
                    if(r?.isForMainFrame==true)error="رفض الخادم فتح غرفة البث (HTTP ${response?.statusCode}). ارجع إلى الغرف للحصول على تذكرة جديدة."
                    else if(missingCriticalMedia(r))error="ملفات تشغيل البث غير متاحة على الخادم (HTTP ${response?.statusCode}). يلزم تحديث خدمات البث."
                }
                override fun onReceivedError(v:WebView?,r:WebResourceRequest?,e:WebResourceError?){
                    if(r?.isForMainFrame==true)error="تعذّر الاتصال بصفحة غرفة البث. تحقق من اتصال الخادم."
                    else if(missingCriticalMedia(r))error="تعذّر تحميل ملفات محرك البث من الخادم. تحقق من نشر الملفات الثابتة."
                }
                override fun onPageFinished(v:WebView?,url:String?){
                    if(url==null||!trustedCallOrigin(url,baseUrl)||Uri.parse(url).path!=allowedPath){
                        error="انتقلت صفحة البث إلى عنوان غير متوقع. ارجع للغرف وحاول مجددًا."
                        return
                    }
                    pageFinished=true
                }
                override fun onRenderProcessGone(v:WebView?,detail:RenderProcessGoneDetail?):Boolean{
                    view=null
                    v?.destroy()
                    error="توقّف محرك عرض البث على الهاتف. ارجع إلى الغرف لفتح جلسة جديدة."
                    return true
                }
                override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?)=r?.url?.let{!trustedCallOrigin(it.toString(),baseUrl)||it.path!=allowedPath}?:true
                override fun onReceivedSslError(v:WebView?,h:SslErrorHandler?,e:android.net.http.SslError?){h?.cancel();error="شهادة أمان الخادم غير صالحة."}
            }
            webChromeClient=object:WebChromeClient(){
                override fun onConsoleMessage(msg:ConsoleMessage?):Boolean{
                    if(msg?.messageLevel()==ConsoleMessage.MessageLevel.ERROR){
                        // Console strings can include room tokens: classify only,
                        // never persist or expose the original JS error message.
                        val m=msg.message().orEmpty()
                        scriptFailure=when{
                            m.contains("Content Security Policy",true)||m.contains("Refused to load",true)->"CSP"
                            m.contains("SyntaxError",true)->"JavaScript syntax"
                            m.contains("ReferenceError",true)->"JavaScript reference"
                            m.contains("TypeError",true)->"JavaScript runtime"
                            m.contains("net::ERR",true)->"network resource"
                            else->"JavaScript runtime"
                        }
                    }
                    return false
                }
                override fun onPermissionRequest(r:PermissionRequest?){
                    if(r==null)return
                    if(!trustedCallOrigin(r.origin.toString(),baseUrl)){r.deny();return}
                    val accepted=r.resources.filter{
                        (it==PermissionRequest.RESOURCE_AUDIO_CAPTURE&&ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)||
                        (it==PermissionRequest.RESOURCE_VIDEO_CAPTURE&&requestedMode=="video"&&ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
                    }
                    if(accepted.isNotEmpty())r.grant(accepted.toTypedArray())else r.deny()
                }
            }
            val target=bootstrap.getString("url");if(trustedCallOrigin(target,baseUrl)&&allowedPath?.startsWith("/api/live/")==true&&allowedPath.endsWith("/room"))postUrl(target,("theme="+transportTheme+"&ticket="+URLEncoder.encode(bootstrap.getString("ticket"),"UTF-8")).toByteArray())else error="عنوان بث غير صالح."
            }})
            if(!pageReady){
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF101820)).padding(24.dp),
                    verticalArrangement=Arrangement.Center,
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator(color=Color(0xFF7DA5B8))
                    Spacer(Modifier.height(18.dp))
                    Text("جارٍ تحميل غرفة البث…",color=Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("يتم التحقق من ملفات واجهة الغرفة قبل الاتصال بوسائط البث.",color=Color(0xFFB7C6CF),style=MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick=retry){Text("إعادة الدخول بتذكرة جديدة",color=Color.White)}
                    TextButton(onClick=finish){Text("إنهاء البث والرجوع",color=Color.White)}
                }
            }
            // Native recovery strip remains visible even when the embedded WebView renderer goes blank.
            if(pageReady){
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .background(Color(0xD0101820)).padding(horizontal=8.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    Text(
                        if(audioOnly)"غرفة صوتية" else "بث فيديو",
                        fontWeight=FontWeight.SemiBold,
                        color=Color.White
                    )
                    Text(
                        nativeStageStatus,
                        modifier=Modifier.weight(1f),
                        color=Color(0xFFBED5DD),
                        maxLines=1
                    )
                    IconButton(onClick=retry){Icon(Icons.Outlined.Refresh,"إعادة الدخول",tint=Color.White)}
                    IconButton(onClick=finish){Icon(Icons.Outlined.Close,"إنهاء والعودة",tint=Color.White)}
                }
            }
        }else if(error.isBlank())Text("جارٍ تجهيز البث…",Modifier.padding(24.dp))
    }}
}

@Composable internal fun LiveBroadcastCard(row:JSONObject,open:()->Unit){ElevatedCard(onClick=open,modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    ChatAvatar(row.optString("owner_name"),48,row.optString("avatar_url"))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(row.optString("title"),style=MaterialTheme.typography.titleMedium);Text(row.optString("owner_name"));Text("${row.optInt("viewers")} مشاهد · "+(if(row.optBoolean("private"))"مدفوع: ${row.optLong("price_cents")}"else if(row.optBoolean("invite_only"))"بالدعوة"else"مجاني"),style=MaterialTheme.typography.bodySmall);Text((if(row.optString("mode")=="audio")"غرفة صوتية"else"بث فيديو")+" · ${row.optInt("max_speakers",1)} متحدثين كحد أقصى",style=MaterialTheme.typography.bodySmall)}
                    Icon(Icons.Outlined.PlayCircleOutline,null)
                }
            }
}

private class PreviewCameraResources {
    var device: android.hardware.camera2.CameraDevice? = null
    var session: android.hardware.camera2.CameraCaptureSession? = null
    var surface: android.view.Surface? = null
    var closed: Boolean = false

    fun close() {
        if (closed) return
        closed = true
        runCatching { session?.stopRepeating() }
        runCatching { session?.close() }
        runCatching { device?.close() }
        runCatching { surface?.release() }
        session = null
        device = null
        surface = null
    }
}

@Composable
internal fun CameraPreviewSurface(
    context: android.content.Context,
    facingFront: Boolean,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    val resources = remember(facingFront) { PreviewCameraResources() }
    val reportError = onError
    DisposableEffect(resources) {
        onDispose { resources.close() }
    }
    // A new TextureView is required for every camera switch. AndroidView.factory
    // is not re-executed by ordinary Compose recomposition.
    key(facingFront) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            if (resources.closed) return
                            try {
                                val manager = ctx.getSystemService(android.content.Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                                    ?: throw IllegalStateException("Camera service unavailable")
                                val facing = if (facingFront) android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
                                             else android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
                                val target = manager.cameraIdList.firstOrNull { id ->
                                    manager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) == facing
                                } ?: throw IllegalStateException("Requested camera unavailable")
                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    reportError("لم يُمنح إذن الكاميرا للمعاينة.")
                                    return
                                }
                                val configurations = manager.getCameraCharacteristics(target)
                                    .get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                val supported = configurations?.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                                val previewSize = supported?.minByOrNull {
                                    kotlin.math.abs(it.width - 1280) + kotlin.math.abs(it.height - 720)
                                }
                                if (previewSize != null) st.setDefaultBufferSize(previewSize.width, previewSize.height)
                                manager.openCamera(target, object : android.hardware.camera2.CameraDevice.StateCallback() {
                                    override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                                        if (resources.closed) { camera.close(); return }
                                        resources.device = camera
                                        try {
                                            val surface = android.view.Surface(st)
                                            resources.surface = surface
                                            val request = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW).apply {
                                                addTarget(surface)
                                            }
                                            camera.createCaptureSession(
                                                listOf(surface),
                                                object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                                                    override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                                                        if (resources.closed) { session.close(); return }
                                                        resources.session = session
                                                        try { session.setRepeatingRequest(request.build(), null, null) }
                                                        catch (_: Exception) { reportError("تعذّر عرض معاينة الكاميرا.") }
                                                    }
                                                    override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                                                        session.close()
                                                        reportError("تعذّر تجهيز معاينة الكاميرا.")
                                                    }
                                                },
                                                null
                                            )
                                        } catch (_: Exception) {
                                            resources.close()
                                            reportError("تعذّر تشغيل معاينة الكاميرا.")
                                        }
                                    }
                                    override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                                        camera.close()
                                        val wasActive = !resources.closed
                                        resources.close()
                                        if (wasActive) reportError("انقطع اتصال الكاميرا.")
                                    }
                                    override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                                        camera.close()
                                        resources.close()
                                        reportError("فشل فتح الكاميرا. تحقق من الجهاز وأعد المحاولة.")
                                    }
                                }, null)
                            } catch (_: Exception) {
                                resources.close()
                                reportError("الكاميرا غير متاحة للمعاينة. تحقق من أذوناتها.")
                            }
                        }
                        override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                            resources.close()
                            return true
                        }
                        override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                    }
                }
            }
        )
    }
}

@Composable internal fun LiveHostSetupScreen(
    onClose: () -> Unit,
    audioOnly: Boolean,
    onAudioOnlyChange: (Boolean) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    privateLive: Boolean,
    onPrivateLiveChange: (Boolean) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    inviteOnly: Boolean,
    onInviteOnlyChange: (Boolean) -> Unit,
    invites: String,
    onInvitesChange: (String) -> Unit,
    speakers: String,
    onSpeakersChange: (String) -> Unit,
    limit: String,
    onLimitChange: (String) -> Unit,
    minimum: String,
    onMinimumChange: (String) -> Unit,
    gifts: Boolean,
    onGiftsChange: (Boolean) -> Unit,
    giftGoal: String,
    onGiftGoalChange: (String) -> Unit,
    maximumLimit: Int,
    busy: Boolean,
    error: String,
    onStartStream: () -> Unit
){
    val context = LocalContext.current
    var facingFront by remember{mutableStateOf(true)}
    var hasCameraPermission by remember{
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
        hasCameraPermission = granted
    }
    var showAdvancedSettings by remember{mutableStateOf(false)}
    var showPriceDialog by remember{mutableStateOf(false)}
    var showInviteDialog by remember{mutableStateOf(false)}
    var previewError by remember { mutableStateOf("") }
    var starting by remember { mutableStateOf(false) }
    val previewLifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(previewLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(previewLifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> foreground = false
                else -> Unit
            }
        }
        previewLifecycle.addObserver(observer)
        onDispose { previewLifecycle.removeObserver(observer) }
    }
    LaunchedEffect(starting) {
        if (starting) {
            // Unmount/close the native camera before the WebView/LiveKit requests it.
            withFrameNanos { }
            delay(280)
            onStartStream()
        }
    }
    LaunchedEffect(busy) {
        if (!busy && starting) {
            // The parent closes this composable on success; reset only after a
            // failed request has returned control to the user.
            delay(150)
            if (!busy) starting = false
        }
    }
    LaunchedEffect(audioOnly, facingFront) { previewError = "" }

    LaunchedEffect(audioOnly){
        if(!audioOnly && !hasCameraPermission){
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler{onClose()}

    Box(Modifier.fillMaxSize().background(Color(0xFF101820))){
        // Live camera preview layer (or calm visualizer for audio-only)
        if(!audioOnly && hasCameraPermission && foreground && !starting){
            CameraPreviewSurface(
                context = context,
                facingFront = facingFront,
                modifier = Modifier.fillMaxSize(),
                onError = { previewError = it }
            )
            // Soft dark gradient overlay to ensure UI elements are legible
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xB3101820),
                        Color(0x33101820),
                        Color(0x80101820),
                        Color(0xE6101820)
                    )
                )
            ))
        } else {
            // Calm, refined Relaxia obsidian/ocean stage
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF131D26), Color(0xFF0F171F), Color(0xFF101820)))
            )){
                Box(Modifier.size(320.dp).align(Alignment.TopEnd).background(Brush.radialGradient(listOf(Color(0x2E3187A8), Color.Transparent)), CircleShape))
                Box(Modifier.size(260.dp).align(Alignment.BottomStart).background(Brush.radialGradient(listOf(Color(0x1AD7AD72), Color.Transparent)), CircleShape))
                if(audioOnly){
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally){
                        Box(Modifier.size(100.dp).background(Color(0x22FFFFFF), CircleShape), contentAlignment = Alignment.Center){
                            Icon(Icons.Filled.VolumeUp, null, tint = Color(0xFF3187A8), modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("غرفة صوتية", color = Color(0xFFF4F7F9), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("المايكروفون فقط سيكون مفعلًا", color = Color(0xFFA3B1BA), style = MaterialTheme.typography.bodySmall)
                    }
                } else if(!hasCameraPermission){
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally){
                        Icon(Icons.Filled.CameraAlt, null, tint = Color(0xFFA3B1BA), modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("إذن الكاميرا مطلوب للمعاينة", color = Color(0xFFF4F7F9), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }){
                            Text("منح الإذن", color = Color(0xFF3187A8))
                        }
                    }
                }
            }
        }

        // Top Navigation Bar
        Row(
            Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal=16.dp,vertical=8.dp),
            horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically
        ){
            IconButton(onClick=onClose, modifier=Modifier.size(40.dp).background(Color(0x55101820), CircleShape)){
                Icon(Icons.Filled.Close, "إغلاق", tint=Color.White)
            }
        }

        // Right-Side Controls: Camera flip and Settings
        Column(
            horizontalAlignment=Alignment.CenterHorizontally,
            modifier=Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(top=54.dp, end=16.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)
        ){
            if(!audioOnly && hasCameraPermission){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    IconButton(onClick={ facingFront = !facingFront }, modifier=Modifier.size(44.dp).background(Color(0x55101820), CircleShape)){
                        Icon(Icons.Filled.CameraAlt, "تبديل الكاميرا", tint=Color.White)
                    }
                    Text(if(facingFront)"أمامية" else "خلفية", color=Color(0xDDFFFFFF), style=MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                IconButton(onClick={ showAdvancedSettings = true }, modifier=Modifier.size(44.dp).background(Color(0x55101820), CircleShape)){
                    Icon(Icons.Filled.Settings, "إعدادات البث", tint=Color.White)
                }
                Text("إعدادات", color=Color(0xDDFFFFFF), style=MaterialTheme.typography.labelSmall)
            }
        }

        // Bottom Controls Area
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().safeDrawingPadding().padding(horizontal=18.dp, vertical=16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ){
            if(error.isNotBlank()){
                Text(error, color=Color(0xFFFF6B6B), style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(horizontal=8.dp))
            }
            if(previewError.isNotBlank() && !audioOnly){
                Text(previewError, color=Color(0xFFFFC078), style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(horizontal=8.dp))
            }

            // Mode Selector Capsule
            Row(
                Modifier.align(Alignment.CenterHorizontally).background(Color(0x77101820), RoundedCornerShape(30.dp)).padding(4.dp),
                horizontalArrangement=Arrangement.spacedBy(4.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(if(audioOnly) Color(0x44FFFFFF) else Color.Transparent)
                        .clickable{ onAudioOnlyChange(true) }.padding(horizontal=18.dp, vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Icon(Icons.Filled.VolumeUp, null, tint=Color.White, modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("غرفة صوتية", color=Color.White, fontWeight=if(audioOnly) FontWeight.Bold else FontWeight.Normal, style=MaterialTheme.typography.bodyMedium)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(if(!audioOnly) Color(0x44FFFFFF) else Color.Transparent)
                        .clickable{ onAudioOnlyChange(false) }.padding(horizontal=18.dp, vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Icon(Icons.Filled.Videocam, null, tint=Color.White, modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فيديو مباشر", color=Color.White, fontWeight=if(!audioOnly) FontWeight.Bold else FontWeight.Normal, style=MaterialTheme.typography.bodyMedium)
                }
            }

            // Stream Title Input
            OutlinedTextField(
                value=title,
                onValueChange={ onTitleChange(it.take(120)) },
                placeholder={ Text("عنوان البث (اختياري)", color=Color(0x99FFFFFF)) },
                leadingIcon={ Icon(Icons.Filled.Edit, null, tint=Color(0xAAFFFFFF)) },
                shape=RoundedCornerShape(24.dp),
                colors=OutlinedTextFieldDefaults.colors(
                    focusedContainerColor=Color(0x66101820),
                    unfocusedContainerColor=Color(0x4D101820),
                    focusedBorderColor=Color(0xFF3187A8),
                    unfocusedBorderColor=Color(0x33FFFFFF),
                    focusedTextColor=Color.White,
                    unfocusedTextColor=Color.White
                ),
                singleLine=true,
                modifier=Modifier.fillMaxWidth()
            )

            // Horizontally Scrollable Action Chips (Responsive on all screen widths)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                // Paid Admission Chip (Controls privateLive with price)
                FilterChip(
                    selected=privateLive,
                    onClick={ showPriceDialog = true },
                    label={ Text(if(privateLive)"دخول مدفوع: $price رصيد" else "دخول مدفوع") },
                    leadingIcon={ Icon(Icons.Filled.AttachMoney, null, modifier=Modifier.size(16.dp)) },
                    shape=RoundedCornerShape(20.dp),
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=Color(0x66277C9D),
                        selectedLabelColor=Color.White,
                        containerColor=Color(0x4D101820),
                        labelColor=Color(0xDDFFFFFF)
                    )
                )

                // Invite-Only Chip (Controls inviteOnly with invite list)
                FilterChip(
                    selected=inviteOnly,
                    onClick={ showInviteDialog = true },
                    label={ Text(if(inviteOnly)"للمدعوين فقط (مفعّل)" else "للمدعوين فقط") },
                    leadingIcon={ Icon(Icons.Filled.PersonAdd, null, modifier=Modifier.size(16.dp)) },
                    shape=RoundedCornerShape(20.dp),
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=Color(0x66277C9D),
                        selectedLabelColor=Color.White,
                        containerColor=Color(0x4D101820),
                        labelColor=Color(0xDDFFFFFF)
                    )
                )

                // Advanced Settings Chip
                FilterChip(
                    selected=gifts || speakers != "1",
                    onClick={ showAdvancedSettings = true },
                    label={ Text("خيارات متقدمة") },
                    leadingIcon={ Icon(Icons.Filled.Settings, null, modifier=Modifier.size(16.dp)) },
                    shape=RoundedCornerShape(20.dp),
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=Color(0x66277C9D),
                        selectedLabelColor=Color.White,
                        containerColor=Color(0x4D101820),
                        labelColor=Color(0xDDFFFFFF)
                    )
                )
            }

            // Start Live Broadcast Button (Calm Relaxia teal/gold gradient)
            Button(
                onClick={ starting = true },
                enabled=!busy && !starting && (audioOnly || hasCameraPermission),
                modifier=Modifier.fillMaxWidth().height(52.dp),
                shape=RoundedCornerShape(26.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent),
                contentPadding=PaddingValues()
            ){
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Color(0xFF277C9D), Color(0xFF1E5B74)))
                    ),
                    contentAlignment=Alignment.Center
                ){
                    Text(
                        if(busy || starting)"جارٍ البدء…" else "بدء البث ((•))",
                        color=Color.White,
                        style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold
                    )
                }
            }
        }
    }

    // Advanced Settings Dialog
    if(showAdvancedSettings){
        AlertDialog(
            onDismissRequest={showAdvancedSettings=false},
            title={Text("إعدادات البث المتقدمة")},
            text={
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text("عدد المتحدثين المسموح به:")
                    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        (if(audioOnly)listOf("1","5","10","15")else listOf("1","2","4","6","12")).forEach{n->
                            FilterChip(selected=speakers==n, onClick={onSpeakersChange(n)}, label={Text(n)})
                        }
                    }
                    OutlinedTextField(
                        value=limit,
                        onValueChange={onLimitChange(it.filter{c->c.isDigit()}.take(3))},
                        label={Text("أقصى عدد مشاهدين (حتى $maximumLimit)")}
                    )
                    OutlinedTextField(
                        value=minimum,
                        onValueChange={onMinimumChange(it.filter{c->c.isDigit()}.take(4))},
                        label={Text("مدة استحقاق الخدمة بالثواني (30 فأكثر)")}
                    )
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Switch(checked=gifts, onCheckedChange=onGiftsChange)
                        Spacer(Modifier.width(8.dp))
                        Text("إتاحة الهدايا في البث")
                    }
                    if(gifts){
                        OutlinedTextField(
                            value=giftGoal,
                            onValueChange={onGiftGoalChange(it.filter{c->c.isDigit()}.take(8))},
                            label={Text("هدف الهدايا (0 دون هدف)")}
                        )
                    }
                }
            },
            confirmButton={TextButton({showAdvancedSettings=false}){Text("تم")}}
        )
    }

    // Paid Admission Dialog
    if(showPriceDialog){
        AlertDialog(
            onDismissRequest={showPriceDialog=false},
            title={Text("الدخول المدفوع")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Switch(checked=privateLive, onCheckedChange=onPrivateLiveChange)
                        Spacer(Modifier.width(8.dp))
                        Text("تفعيل رسم الدخول من الرصيد")
                    }
                    if(privateLive){
                        OutlinedTextField(
                            value=price,
                            onValueChange={onPriceChange(it.filter{c->c.isDigit()}.take(7))},
                            label={Text("سعر الدخول من الرصيد")}
                        )
                    }
                }
            },
            confirmButton={TextButton({showPriceDialog=false}){Text("حفظ")}}
        )
    }

    // Invite-Only Dialog
    if(showInviteDialog){
        AlertDialog(
            onDismissRequest={showInviteDialog=false},
            title={Text("البث للمدعوين فقط")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Switch(checked=inviteOnly, onCheckedChange=onInviteOnlyChange)
                        Spacer(Modifier.width(8.dp))
                        Text("قصر الدخول على أسماء محددة")
                    }
                    if(inviteOnly){
                        OutlinedTextField(
                            value=invites,
                            onValueChange={onInvitesChange(it.take(1000))},
                            label={Text("أسماء المستخدمين مفصولة بفاصلة")}
                        )
                    }
                }
            },
            confirmButton={TextButton({showInviteDialog=false}){Text("حفظ")}}
        )
    }
}
