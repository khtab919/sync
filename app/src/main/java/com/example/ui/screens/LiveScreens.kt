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
    "live_update_required"->"خادمك ما زال بإصدار بث قديم. حدّث الخادم لتفعيل الضيوف وPK والبث الصوتي."
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
@Composable internal fun LiveLobby(api:LayaliApi,baseUrl:String,openProfile:(Int)->Unit,buy:()->Unit,back:()->Unit){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")}
    var allowed by remember{mutableStateOf(false)};var available by remember{mutableStateOf(false)};var maximumLimit by remember{mutableIntStateOf(25)};var busy by remember{mutableStateOf(false)};var create by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")};var privateLive by remember{mutableStateOf(false)};var inviteOnly by remember{mutableStateOf(false)};var invites by remember{mutableStateOf("")}
    var price by remember{mutableStateOf("100")};var limit by remember{mutableStateOf("25")};var minimum by remember{mutableStateOf("30")};var gifts by remember{mutableStateOf(false)}
    var audioOnly by remember{mutableStateOf(false)};var speakers by remember{mutableStateOf("1")};var giftGoal by remember{mutableStateOf("0")}
    var selected by remember{mutableStateOf<JSONObject?>(null)};var active by remember{mutableStateOf<JSONObject?>(null)};var activeId by remember{mutableStateOf("")}
    fun act(block:suspend()->Unit){if(busy)return;busy=true;error="";scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=liveError(e)}finally{busy=false}}}
    fun enter(row:JSONObject){act{active=api.request("/api/live/${row.getString("id")}/admission","POST",JSONObject().put("confirmed",true).put("price_cents",row.optLong("price_cents")));activeId=row.getString("id");selected=null}}
    LaunchedEffect(query){while(true){try{val result=api.request("/api/live?q="+URLEncoder.encode(query,"UTF-8"));if(result.optInt("stage_version")<2)throw IllegalStateException("live_update_required");val a=result.optJSONArray("items");rows=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)};allowed=result.optBoolean("can_broadcast");available=result.optBoolean("available");maximumLimit=result.optInt("max_viewers",25);error=""}catch(e:CancellationException){throw e}catch(e:Exception){allowed=false;available=false;rows=emptyList();error=liveError(e)};delay(5000)}}
    active?.let{room->LiveRoomView(baseUrl,room,profile={id->active=null;openProfile(id)}){val ending=room.optBoolean("owner");val id=activeId;active=null;if(ending)scope.launch{runCatching{api.request("/api/live/$id","POST",JSONObject().put("action","end"))}}};return}
    Scaffold(topBar={TopAppBar(title={Text("البث المباشر")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowForward,"رجوع")}},actions={TextButton(buy){Text("الرصيد")};if(allowed)IconButton({create=true},enabled=available&&!busy){Icon(Icons.Outlined.Videocam,"بدء بث")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{OutlinedTextField(query,{query=it.take(100)},label={Text("ابحث عن بث أو صاحب بث")},singleLine=true,modifier=Modifier.fillMaxWidth())}
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
            if(!available)item{Text("البث ينتظر جاهزية خادم الوسائط. لا توجد جلسات تجريبية.",style=MaterialTheme.typography.bodyMedium)}
            if(rows.isEmpty()&&error.isBlank()&&available)item{Text("لا توجد بثوث نشطة الآن.",style=MaterialTheme.typography.bodyLarge)}
            items(rows,key={it.getString("id")}){row->LiveBroadcastCard(row){selected=row}}
            item{Text("لا يُسجَّل البث ولا تتوفر إعادة مشاهدة داخل المنصة. لا يمكن ضمان منع التسجيل بأدوات خارجية.",style=MaterialTheme.typography.bodySmall)}
        }
    }
    selected?.let{row->AlertDialog(onDismissRequest={selected=null},title={Text(row.optString("title"))},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(row.optString("owner_name"));Text(if(row.optBoolean("private"))"سعر الدخول: ${row.optLong("price_cents")} من الرصيد"else if(row.optBoolean("invite_only"))"بث بالدعوة · مجاني"else"بث عام مجاني");Text(row.optString("refund_policy"),style=MaterialTheme.typography.bodySmall);TextButton({selected=null;openProfile(row.optInt("owner_id"))}){Text("عرض الملف الشخصي")}}},confirmButton={TextButton({enter(row)},enabled=!busy){Text(if(row.optBoolean("private"))"تأكيد الدخول وحجز المبلغ"else"مشاهدة")}},dismissButton={TextButton({selected=null}){Text("إلغاء")}})}
    if(create)AlertDialog(onDismissRequest={create=false},title={Text("إنشاء بث مباشر")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(title,{title=it.take(120)},label={Text("عنوان البث")})
        Row(verticalAlignment=Alignment.CenterVertically){Switch(audioOnly,{audioOnly=it});Text("بث صوتي فقط")}
        Text("عدد المتحدثين يشمل صاحب البث. طلبات الضيوف تحتاج موافقتك.",style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("1","2","4","6","12").forEach{n->FilterChip(speakers==n,{speakers=n},label={Text(n)})}}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Row(verticalAlignment=Alignment.CenterVertically){Switch(privateLive,{privateLive=it});Text("دخول مدفوع")}
        if(privateLive)OutlinedTextField(price,{price=it.filter{c->c.isDigit()}.take(7)},label={Text("سعر الدخول من الرصيد")})
        Row(verticalAlignment=Alignment.CenterVertically){Switch(inviteOnly,{inviteOnly=it});Text("للمدعوين فقط")}
        if(inviteOnly)OutlinedTextField(invites,{invites=it.take(1000)},label={Text("أسماء المستخدمين مفصولة بفاصلة")})
        OutlinedTextField(limit,{limit=it.filter{c->c.isDigit()}.take(3)},label={Text("أقصى عدد مشاهدين · حتى $maximumLimit")})
        OutlinedTextField(minimum,{minimum=it.filter{c->c.isDigit()}.take(4)},label={Text("مدة استحقاق الخدمة بالثواني · 30 على الأقل")})
        Row(verticalAlignment=Alignment.CenterVertically){Switch(gifts,{gifts=it});Text("إتاحة الهدايا المدفوعة")}
        if(gifts)OutlinedTextField(giftGoal,{giftGoal=it.filter{c->c.isDigit()}.take(8)},label={Text("هدف الهدايا · 0 دون هدف")})
        Text("تحصل على 80٪ من الإيراد المستحق. يعاد الحجز إذا انتهى البث قبل مدة الخدمة. البث لا يحفظ.",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton({act{val p=price.toIntOrNull()?:0;val n=limit.toIntOrNull()?:0;val m=minimum.toIntOrNull()?:0;if(title.trim().length<2||n !in 1..maximumLimit||m !in 30..3600||(privateLive&&p !in 1..1000000))throw IllegalArgumentException("invalid_input");val row=api.request("/api/live","POST",JSONObject().put("title",title.trim()).put("private",privateLive).put("invite_only",inviteOnly).put("price_cents",p).put("max_viewers",n).put("minimum_seconds",m).put("gifts_enabled",gifts).put("mode",if(audioOnly)"audio"else"video").put("max_speakers",speakers.toInt()).put("gift_goal_cents",giftGoal.toIntOrNull()?:0).put("invite_names",JSONArray(invites.split(',', '،').map{it.trim().removePrefix("@")}.filter{it.isNotBlank()})));create=false;active=api.request("/api/live/${row.getString("id")}/admission","POST",JSONObject());activeId=row.getString("id")}},enabled=!busy){Text("بدء البث")}},dismissButton={TextButton({create=false}){Text("إلغاء")}})
}

@Composable internal fun LiveRoomView(baseUrl:String,bootstrap:JSONObject,profile:((Int)->Unit)?=null,finish:()->Unit){
    val transportTheme=if(MaterialTheme.colorScheme.background.luminance()<.25f)"dark"else"light"
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle;val owner=bootstrap.optBoolean("owner");val audioOnly=bootstrap.optString("mode")=="audio"
    var granted by remember{mutableStateOf(!owner)};var error by remember{mutableStateOf("")};var view by remember{mutableStateOf<WebView?>(null)}
    var requestingGuest by remember{mutableStateOf(false)}
    var requestedMode by remember{mutableStateOf(if(audioOnly)"audio"else"video")}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->
        val accepted=result[Manifest.permission.RECORD_AUDIO]==true&&(requestedMode=="audio"||result[Manifest.permission.CAMERA]==true)
        if(requestingGuest){view?.evaluateJavascript("window.dispatchEvent(new CustomEvent('publishpermissions',{detail:"+accepted+"}));",null);requestingGuest=false}
        else{granted=accepted;if(!accepted)error=if(audioOnly)"اسمح بالميكروفون لبدء البث الصوتي."else"اسمح بالميكروفون والكاميرا لبدء البث."}
    }
    LaunchedEffect(Unit){if(owner){val list=if(audioOnly)arrayOf(Manifest.permission.RECORD_AUDIO)else arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA);if(list.all{ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED})granted=true else permission.launch(list)}}
    BackHandler{finish()}
    DisposableEffect(Unit){val window=(context as? android.app.Activity)?.window;val already=window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE)!=0;window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE);onDispose{view?.loadUrl("about:blank");view?.destroy();if(!already)window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)}}
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP&&(context as? android.app.Activity)?.isChangingConfigurations!=true)finish()};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
    Surface(Modifier.fillMaxSize(),color=Color(0xFF101820),contentColor=Color.White){Column(Modifier.fillMaxSize().safeDrawingPadding()){
        if(error.isNotBlank()){Text(error,Modifier.padding(20.dp));TextButton(finish){Text("رجوع")}}
        if(granted&&error.isBlank())AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->WebView(ctx).apply{
            view=this;setBackgroundColor(android.graphics.Color.rgb(23,21,23));settings.javaScriptEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=false;settings.cacheMode=WebSettings.LOAD_NO_CACHE;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.mediaPlaybackRequiresUserGesture=false
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
                override fun onReceivedHttpError(v:WebView?,r:WebResourceRequest?,response:WebResourceResponse?){if(r?.isForMainFrame==true)error="تعذر فتح غرفة البث من الخادم (HTTP ${response?.statusCode}). ارجع وأعد الدخول للحصول على تذكرة جديدة."}
                override fun onReceivedError(v:WebView?,r:WebResourceRequest?,e:WebResourceError?){if(r?.isForMainFrame==true)error="تعذر تحميل غرفة البث. تحقق من الاتصال بالخادم."}
                override fun onPageFinished(v:WebView?,url:String?){if(url!=null&&trustedCallOrigin(url,baseUrl)&&Uri.parse(url).path==allowedPath)v?.evaluateJavascript("typeof LivekitClient !== 'undefined' && typeof connect === 'function'"){ready->if(ready=="false")error="لم تُحمّل ملفات محرك البث. يلزم فحص ملفات الخادم؛ لم يبدأ الفيديو."}}
                override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?)=r?.url?.let{!trustedCallOrigin(it.toString(),baseUrl)||it.path!=allowedPath}?:true;override fun onReceivedSslError(v:WebView?,h:SslErrorHandler?,e:android.net.http.SslError?){h?.cancel();error="شهادة أمان الخادم غير صالحة."}}
            webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(r:PermissionRequest?){if(r==null)return;if(!trustedCallOrigin(r.origin.toString(),baseUrl)){r.deny();return};val accepted=r.resources.filter{(it==PermissionRequest.RESOURCE_AUDIO_CAPTURE&&ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)||(it==PermissionRequest.RESOURCE_VIDEO_CAPTURE&&ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)};if(accepted.size==r.resources.size&&accepted.isNotEmpty())r.grant(accepted.toTypedArray())else r.deny()}}
            val target=bootstrap.getString("url");if(trustedCallOrigin(target,baseUrl)&&allowedPath?.startsWith("/api/live/")==true&&allowedPath.endsWith("/room"))postUrl(target,("theme="+transportTheme+"&ticket="+URLEncoder.encode(bootstrap.getString("ticket"),"UTF-8")).toByteArray())else error="عنوان بث غير صالح."
        }})else if(error.isBlank())Text("جارٍ تجهيز البث…",Modifier.padding(24.dp))
    }}
}

@Composable internal fun LiveBroadcastCard(row:JSONObject,open:()->Unit){ElevatedCard(onClick=open,modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    ChatAvatar(row.optString("owner_name"),48,row.optString("avatar_url"))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(row.optString("title"),style=MaterialTheme.typography.titleMedium);Text(row.optString("owner_name"));Text("${row.optInt("viewers")} مشاهد · "+(if(row.optBoolean("private"))"مدفوع: ${row.optLong("price_cents")}"else if(row.optBoolean("invite_only"))"بالدعوة"else"مجاني"),style=MaterialTheme.typography.bodySmall);Text((if(row.optString("mode")=="audio")"بث صوتي"else"بث فيديو")+" · ${row.optInt("max_speakers",1)} متحدثين كحد أقصى",style=MaterialTheme.typography.bodySmall)}
                    Icon(Icons.Outlined.PlayCircleOutline,null)
                }
            }
}
