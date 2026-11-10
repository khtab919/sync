package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.api.MessageInbox
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.api.LayaliApi
import com.example.data.api.SecureSessionStore
import com.example.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.util.UUID

private val destinations = listOf("تصفح", "ريلز", "العرض", "دردشة", "حسابي")
private val destinationIcons = listOf(Icons.Outlined.Home, Icons.Outlined.SmartDisplay, Icons.Outlined.Storefront, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.PersonOutline)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ServerAppScreen(baseUrl: String) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("layali_appearance", 0) }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark", false)) }
    val api = remember(baseUrl) { LayaliApi(baseUrl, SecureSessionStore(context.applicationContext, baseUrl.trimEnd('/'))) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var activeCall by rememberSaveable { mutableStateOf("") }
    var incomingCall by remember { mutableStateOf<JSONObject?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var signedIn by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(true) }
    var remembered by remember { mutableStateOf(false) }
    var resumeRevision by remember { mutableIntStateOf(0) }
    var securityDialog by remember { mutableStateOf(false) }
    var nextReels by remember { mutableStateOf("") }
    var directReel by remember { mutableStateOf("") }
    var savedReels by remember { mutableStateOf(false) }
    var moreReels by remember { mutableStateOf(false) }
    var register by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(-1f) }
    var submitting by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var contentError by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(JSONObject()) }
    var services by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var creators by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf(JSONObject()) }
    var creator by remember { mutableStateOf(JSONObject()) }
    val chatState = rememberSaveableStateHolder()
    var purchaseReturn by rememberSaveable { mutableStateOf("") }
    var walletAccountId by remember { mutableIntStateOf(0) }
    var walletLoading by remember { mutableStateOf(false) }
    var walletError by remember { mutableStateOf("") }
    var wallet by remember { mutableStateOf(JSONObject()) }
    var accountMatches by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var searchReels by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedReelId by remember { mutableIntStateOf(0) }
    var showCreationSheet by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("الكل") }
    var searchCategory by rememberSaveable { mutableStateOf("الكل") }
    var followingOnly by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var profileGender by rememberSaveable { mutableStateOf("") }
    var adultConfirmed by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var offerKind by rememberSaveable { mutableStateOf("خدمة") }
    var peerId by rememberSaveable { mutableStateOf("") }
    var peerName by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var avatarUrl by rememberSaveable { mutableStateOf("") }
    var profileUsername by rememberSaveable { mutableStateOf("") }
    var profileEmail by rememberSaveable { mutableStateOf("") }
    var chatPrice by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var requestKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var reportKind by rememberSaveable { mutableStateOf("service") }
    var reportId by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }

    fun open(route: String) { page = route; notice = ""; contentError = ""; query = ""; searchCategory = "الكل" }
    fun buyCredits() { purchaseReturn = page; open("شحن الرصيد") }
    fun back() { if (page == "شحن الرصيد" && purchaseReturn.isNotBlank()) { page = purchaseReturn; purchaseReturn = "" } else { page = "" }; query = ""; notice = "" }
    fun perform(action: suspend () -> Unit) {
        if (submitting) return
        submitting = true
        notice = ""
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice = friendlyError(e.message) }
            finally { if (api.token == null) { signedIn = false; remembered = false } else if (!signedIn) remembered = true; submitting = false }
        }
    }
    fun finishInternalCall() {
        val id = activeCall
        activeCall = ""
        if (id.isNotBlank()) scope.launch { runCatching { api.request("/api/calls/$id", "POST", JSONObject().put("action", "end")) } }
    }
    fun openInternalCall(id: String) { perform {
        val row = api.request("/api/calls/$id")
        if (row.optInt("callee_id") == profile.optInt("id") && row.optString("status") == "ringing") api.request("/api/calls/$id", "POST", JSONObject().put("action", "accept"))
        if (row.optString("status") in listOf("ringing", "accepted")) { incomingCall = null; activeCall = id }
        else notice = "هذه المكالمة انتهت بالفعل. يمكنك بدء مكالمة جديدة."
    } }
    fun startChat(id: Int, name: String) { peerId = id.toString(); peerName = name; message = ""; open("محادثة") }
    val avatarPicker=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()){uri->
        if(uri!=null)perform{
            if(api.request("/health/").optInt("avatar_upload_version")<1)throw IllegalStateException("avatar_update_required")
            val uploaded=api.upload(context,uri,"avatar")
            profile=api.request("/api/me","PATCH",JSONObject().put("avatar_id",uploaded.getString("id")))
            avatarUrl=profile.optString("avatar_url");notice="تم رفع صورة الملف وحفظها."
        }
    }
    val reelPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) perform {
            // Upload/approval are enforced by the backend, never client-side.
            val uploaded = try{api.upload(context, uri, "reel"){value->scope.launch{uploadProgress=value}}}finally{uploadProgress=-1f}
            val result = api.request("/api/reels", "POST", JSONObject().put("media_id", uploaded.getString("id")))
            if (result.optString("status") != "published") throw IllegalStateException("reel_publish_unconfirmed")
            page = ""; tab = 1; revision++
            notice = "تم نشر الريل بنجاح."
        }
    }
    fun editProfile() {
        profileGender=profile.optString("gender").takeIf { it in listOf("male","female") } ?: ""
        displayName = profile.optString("display_name", profile.optString("username"))
        bio = profile.optString("bio")
        avatarUrl = profile.optString("avatar_url")
        profileUsername = profile.optString("username")
        profileEmail = profile.optString("email")
        chatPrice = (profile.optInt("chat_price_cents") / 100.0).toString()
        open("تعديل الملف")
    }
    fun showService(item: JSONObject) { selected = item; open("تفاصيل العرض") }
    fun showCreator(id: Int) {
        if (id == profile.optInt("id")) { page = ""; tab = 4; return }
        if (id <= 0) { notice = "هذا الملف غير متاح حاليًا."; return }
        perform {
            creator = api.request("/api/creators/$id")
            open("صفحة المذيعة")
            // Refresh public offers for the selected creator.
            revision++
        }
    }
    LaunchedEffect(query,page,tab,signedIn){
        if(!signedIn||(if(page.isBlank())destinations[tab]else page) !in listOf("تصفح","صنّاع المحتوى")||query.trim().length<2){
            accountMatches=emptyList();searchReels=emptyList();return@LaunchedEffect
        }
        delay(300)
        try{
            val term=query.trim().removePrefix("@")
            accountMatches=api.request("/api/accounts/search?q="+java.net.URLEncoder.encode(term,"UTF-8")).rows()
            searchReels=api.request("/api/reels?q="+java.net.URLEncoder.encode(term,"UTF-8")).rows()
            services=api.request("/api/services?q="+java.net.URLEncoder.encode(term,"UTF-8")).rows()
        }catch(e:CancellationException){throw e}catch(e:Exception){accountMatches=emptyList();searchReels=emptyList()}
    }
    LaunchedEffect(api, resumeRevision) {
        restoring = true
        try {
            if (api.token != null || api.restoreSession()) {
                profile = api.request("/api/me")
                // Admins use the same app login and enter the in-app MFA screen.
                page = if (profile.optBoolean("is_admin")) "لوحة الإدارة" else ""
                signedIn = true
                remembered = true
                notice = ""
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notice = friendlyError(e.message); remembered = api.token != null }
        finally { restoring = false }
    }
    val pushPermission=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    val pushRoute by com.example.notifications.NotificationRoutes.pending.collectAsState()
    LaunchedEffect(signedIn,foreground,restoring,profile.optInt("id")) {
        if(restoring)return@LaunchedEffect
        com.example.notifications.NotificationRoutes.account(context,if(signedIn)profile.optInt("id")else 0)
        if(signedIn && foreground){
            if(android.os.Build.VERSION.SDK_INT>=33){
                val pushPrefs=context.getSharedPreferences("relaxia_push",0)
                if(!pushPrefs.getBoolean("asked",false)){
                    pushPrefs.edit().putBoolean("asked",true).apply()
                    pushPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            runCatching { com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                context.getSharedPreferences("relaxia_push",0).edit().putString("token",token).apply()
                scope.launch { runCatching { api.request("/api/push/devices","POST",JSONObject().put("token",token)) } }
            } }
        }
    }
    LaunchedEffect(pushRoute,signedIn) {
        val route=pushRoute
        if(signedIn && route!=null){
            if(route["account_id"]==profile.optInt("id").toString() || route["category"] in listOf("reel","profile","live_link")){
                when(route["category"]){
                    "reel" -> {directReel=route["target"].orEmpty();page="";tab=1;revision++}
                    "profile" -> route["target"]?.toIntOrNull()?.let{showCreator(it)}
                    "live_link" -> open("البث المباشر")
                    "messages" -> route["target"]?.toIntOrNull()?.let{startChat(it,"")}
                    "calls" -> { incomingCall=runCatching{api.request("/api/calls").optJSONObject("incoming")}.getOrNull() }
                    "bookings" -> open("الحجوزات")
                    "wallet" -> open("المحفظة")
                    "live" -> open("البث المباشر")
                }
            }
            com.example.notifications.NotificationRoutes.pending.value=null
        }
    }
    if(securityDialog)AccountSecurityDialog(api,!signedIn,{securityDialog=false}){signedIn=false;remembered=false;profile=JSONObject();page=""}
    val screen = page.ifBlank { destinations[tab] }
    // Android back from a full-screen Reel must return home, never exit the app.
    BackHandler(enabled = signedIn && (page.isNotBlank() || tab == 1)) {
        if (screen == "ريلز") {
            page = ""; tab = 0; query = ""; contentError = ""; notice = ""
        } else back()
    }
    LaunchedEffect(signedIn, profile.optInt("id"), screen, revision) {
        if (!signedIn) return@LaunchedEffect
        if (screen !in listOf("تصفح", "حسابي", "دردشة", "المحفظة")) return@LaunchedEffect
        walletLoading = true; walletError = ""
        val accountId = profile.optInt("id")
        try { wallet = api.request("/api/wallet"); walletAccountId = accountId }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { walletError = "تعذّر تحديث الرصيد"; if (api.token == null) signedIn = false }
        finally { walletLoading = false }
    }
    LaunchedEffect(screen, signedIn, revision) {
        if (!signedIn) return@LaunchedEffect
        loading = true; contentError = ""; rows = emptyList()
        try {
            when (screen) {
                "تصفح", "العرض", "صفحة المذيعة" -> {
                    services = api.request("/api/services").rows()
                    creators = api.request("/api/creators").rows()
                    if (screen == "صفحة المذيعة") creator = (creators+accountMatches).firstOrNull { it.optInt("id") == creator.optInt("id") } ?: creator
                }
                "حسابي" -> { profile = api.request("/api/me"); services = api.request("/api/services").rows() }
                "ريلز" -> { val result=api.request("/api/reels"+(if(directReel.isNotBlank())"?reel_id=$directReel"else if(savedReels)"?saved=1"else ""));rows=result.rows();directReel="";nextReels=result.optString("next_cursor").takeUnless{it=="null"}.orEmpty() }
                "دردشة" -> rows = api.request("/api/conversations").rows()
                "محادثة جديدة" -> creators = api.request("/api/creators").rows()
                "محادثة" -> rows = api.request("/api/messages/$peerId").rows()
                "المحفوظات" -> rows = api.request("/api/saved").rows()
                "الحجوزات" -> rows = api.request("/api/bookings").rows()
                "المحفظة" -> { rows = api.request("/api/withdrawals").rows() }
                "الدعم وخدمة العملاء" -> rows = api.request("/api/support").rows()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            contentError = if (screen == "دردشة" && e.message == "server_unavailable")
                "تعذّر تحميل المحادثات من الخادم. اضغط «إعادة المحاولة»."
                else friendlyError(e.message)
            if (api.token == null) signedIn = false
        }
        finally { loading = false }
    }

    LaunchedEffect(signedIn, foreground, profile.optInt("id")) {
        if(!signedIn || !foreground)return@LaunchedEffect
        val inbox=MessageInbox(context,api.origin,profile.optInt("id"))
        while(true){
            try {
                val messageVersion=api.request("/health/").optInt("messaging_version")
                if(messageVersion>=3)api.request("/api/presence","POST",JSONObject())
                if(messageVersion>=2){
                    val received=api.request("/api/delivery/incoming").rows()
                    if(received.isNotEmpty()){
                        withContext(Dispatchers.IO){inbox.save(received)}
                        api.request("/api/delivery/acknowledge","POST",JSONObject().put("ids",org.json.JSONArray(received.map{it.optInt("id")})))
                    }
                }
            }catch(e:CancellationException){throw e}catch(e:Exception){}
            delay(4000)
        }
    }
    LaunchedEffect(signedIn, foreground, profile.optInt("id"), activeCall,screen) {
        if (!signedIn || !foreground || activeCall.isNotBlank() || screen=="البث المباشر") { incomingCall = null; return@LaunchedEffect }
        try { if (api.request("/health/").optInt("calls_version") < 1) return@LaunchedEffect } catch (e: Exception) { return@LaunchedEffect }
        while (true) {
            try { incomingCall = api.request("/api/calls").optJSONObject("incoming") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { incomingCall = null }
            delay(3000)
        }
    }
    DisposableEffect(incomingCall?.optString("id"), foreground) {
        val tone = if(incomingCall != null && foreground) runCatching { android.media.RingtoneManager.getRingtone(context, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)) }.getOrNull() else null
        runCatching { tone?.play() }
        onDispose { runCatching { tone?.stop() } }
    }
    MyApplicationTheme(darkTheme = dark) {
        if (restoring) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(20.dp))
                    Text("جاري فتح حسابك…")
                }
            }
            return@MyApplicationTheme
        }
        if (!signedIn && remembered) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("أهلًا بعودتك", style = MaterialTheme.typography.headlineLarge)
                    Text("دخولك محفوظ. تعذّر الاتصال بالخادم الآن، ولا تحتاج لإعادة كتابة كلمة المرور.")
                    if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.error)
                    ReferencePrimary("إعادة المحاولة", !submitting) { resumeRevision++ }
                    TextButton(onClick = { perform { api.forgetSession(); remembered = false; notice = "" } }, enabled = !submitting) { Text("استخدام حساب آخر") }
                }
            }
            return@MyApplicationTheme
        }
        if (!signedIn) {
            ReferenceLogin(register, submitting, notice, username, password, email, adultConfirmed,
                { username = it }, { password = it }, { email = it }, { adultConfirmed = it },
                { register = !register; notice = "" }, { perform {
                    // Existing accounts (including administrators) must not be blocked by
                    // the *registration* version handshake.
                    val body = JSONObject().put("email",email.trim()).put("password",password)
                    if (register) {
                        api.requireSimpleRegistration()
                        body.put("gender",gender).put("display_name",username.trim()).put("adult_confirmed", adultConfirmed).put("accept_non_explicit_policy", true)
                        if (email.isNotBlank()) body.put("email", email.trim())
                    }
                    api.request(if (register) "/api/register" else "/api/login", "POST", body)
                    profile = api.request("/api/me")
                    password = ""; signedIn = true; tab = 0
                    page = if (profile.optBoolean("is_admin")) "لوحة الإدارة" else ""
                    if (!api.sessionSaved) notice = "تم الدخول. تعذّر حفظ الجلسة على هذا الجهاز، وقد تحتاج للدخول مجددًا عند إغلاق التطبيق."
                } }, gender=gender, onGender={gender=it},onForgot={securityDialog=true})
            return@MyApplicationTheme
        }
        if (signedIn && profile.optBoolean("account_restricted")) {
            AccountNotices(api){perform{api.request("/api/logout","POST",JSONObject().put("push_token",context.getSharedPreferences("relaxia_push",0).getString("token","")));signedIn=false;remembered=false;profile=JSONObject();page=""}}
            return@MyApplicationTheme
        }
        if (activeCall.isNotBlank() && signedIn) {
            InternalCallScreen(api, baseUrl, activeCall) { finishInternalCall() }
            return@MyApplicationTheme
        }
        incomingCall?.let { incoming -> AlertDialog(onDismissRequest = {}, title = { Text(if(incoming.optString("mode") == "video") "مكالمة فيديو واردة" else "مكالمة صوتية واردة") }, text = { Text(incoming.optString("caller_name")) }, confirmButton = {
            TextButton(onClick = { openInternalCall(incoming.getString("id")) }, enabled = !submitting) { Text("قبول") }
        }, dismissButton = {
            TextButton(onClick = { perform { api.request("/api/calls/${incoming.getString("id")}", "POST", JSONObject().put("action", "reject")); incomingCall = null } }, enabled = !submitting) { Text("رفض") }
        }) }
        if (screen=="لوحة الإدارة" && profile.optBoolean("is_admin")) {
            AdminCentre(api,profile.optString("display_name"),profile.optInt("id")){back()}
            return@MyApplicationTheme
        }
        if (screen in listOf("شحن الرصيد", "تفعيل البائع", "العجلة")) {
            CommerceScreen(api, screen) { page = purchaseReturn; purchaseReturn = ""; notice = ""; revision++ }
            return@MyApplicationTheme
        }
        if(screen=="التنبيهات"){AccountNotices(api){back()};return@MyApplicationTheme}
        if(screen=="الحسابات المحظورة"){BlockedAccounts(api){back()};return@MyApplicationTheme}
        if(screen=="سجل العمليات"){FinancialHistory(api){back()};return@MyApplicationTheme}
        if(screen=="الحجوزات"){BookingHistory(api){back();revision++};return@MyApplicationTheme}
        if(screen=="البث المباشر"){LiveLobby(api,baseUrl,{showCreator(it)},{buyCredits()}){back();revision++};return@MyApplicationTheme}
        if (screen == "إضافة عرض") {
            SellerPublishScreen(api) { back(); revision++ }
            return@MyApplicationTheme
        }
        if (screen == "محادثة" && peerId.toIntOrNull() != null) {
            chatState.SaveableStateProvider("conversation-$peerId") {
                ConversationScreen(api, peerId.toInt(), peerName, profile.optInt("id"), message, { message = it }, { buyCredits() }, { openInternalCall(it) }, isSeller=profile.optBoolean("publishing_allowed")) { back(); revision++ }
            }
            return@MyApplicationTheme
        }
        Scaffold(
            floatingActionButton = {
                if (page.isBlank() && tab == 0) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { showCreationSheet = true },
                            containerColor = Color(0xFF277C9D),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, "إنشاء محتوى", Modifier.size(22.dp))
                        }
                        SupportFloatingButton()
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (screen != "حسابي" && screen != "صفحة المذيعة" && (screen != "ريلز" || rows.isEmpty())) TopAppBar(
                    title = {
                        if (screen == "تصفح") Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Image(painterResource(R.drawable.relaxia_logo), "شعار Relaxia", Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)))
                            Text("Relaxia", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        } else Text(if (screen == "محادثة") peerName.ifBlank { "محادثة" } else screen, fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = { if (page.isNotBlank()) IconButton(onClick = { back() }) { Icon(Icons.Outlined.ArrowForward, "رجوع") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        if (screen in listOf("العرض", "دردشة")) IconButton(onClick={buyCredits()}){Icon(Icons.Outlined.AccountBalanceWallet,"الرصيد",Modifier.size(22.dp))}
                        when (screen) {
                            "تصفح" -> HomeToolbarActions({open("البث المباشر")},{buyCredits()},{open("المحفوظات")},{revision++})
                            "العرض" -> {
                                IconButton(onClick = { open("إضافة عرض") }) { Icon(Icons.Outlined.AddCircleOutline, "إضافة عرض") }
                                IconButton(onClick = { open("الحجوزات") }) { Icon(Icons.Outlined.ShoppingBag, "حجوزاتي") }
                            }
                            "دردشة" -> IconButton(onClick = { open("محادثة جديدة") }) { Icon(Icons.Outlined.EditNote, "محادثة جديدة") }
                        }
                        if ((screen in destinations && screen != "تصفح") || screen in listOf("المحفوظات", "الحجوزات", "المحفظة", "محادثة", "الدعم وخدمة العملاء"))
                            IconButton(enabled = !loading, onClick = { revision++ }) { Icon(Icons.Outlined.Refresh, "تحديث") }
                    }
                )
            },
            bottomBar = {
                if (page.isBlank() && (screen != "ريلز" || rows.isEmpty())) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.heightIn(min = 72.dp)) {
                    destinations.forEachIndexed { index, name -> NavigationBarItem(selected = tab == index,
                        onClick = { tab = index; query = ""; category = "الكل"; searchCategory = "الكل"; notice = "" },
                        icon = { Icon(destinationIcons[index], null, Modifier.size(24.dp)) }, label = { Text(name, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary)) }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (loading || submitting) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                if (notice.isNotBlank() || contentError.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(notice.ifBlank { contentError }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (contentError.isNotBlank()) TextButton(onClick={revision++}, enabled=!loading) { Text("إعادة المحاولة") }
                    }
                }
                if(uploadProgress>=0f)LinearProgressIndicator(progress={uploadProgress},modifier=Modifier.fillMaxWidth())
                if (screen == "ريلز" && rows.isNotEmpty()) {
                    val pager = rememberPagerState(pageCount = { rows.size })
                    LaunchedEffect(rows, selectedReelId) {
                        if(selectedReelId > 0){
                            val index = rows.indexOfFirst { it.optInt("id") == selectedReelId }
                            if(index >= 0){ pager.scrollToPage(index); selectedReelId = 0 }
                        }
                    }
                    LaunchedEffect(pager.currentPage,rows.size,nextReels){
                        if(pager.currentPage>=rows.size-3 && nextReels.isNotBlank() && !moreReels){
                            moreReels=true
                            try{val result=api.request("/api/reels?before="+nextReels+(if(savedReels)"&saved=1"else ""));rows=(rows+result.rows()).distinctBy{it.optInt("id")};nextReels=result.optString("next_cursor").takeUnless{it=="null"}.orEmpty()}
                            catch(e:Exception){contentError="تعذّر تحميل المزيد من الريلز."}
                            finally{moreReels=false}
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                        VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                            val row = rows[index]
                            ReelItemView(
                                row = row,
                                active = foreground && pager.currentPage == index,
                                onChat = { startChat(row.optInt("owner_id"), row.optString("owner_name")) },
                                onCreator = { showCreator(row.optInt("owner_id")) },
                                api = api,
                                onHidden = {rows=rows.filterNot{it.optInt("id")==row.optInt("id")}},
                                viewerId = profile.optInt("id")
                            )
                        }
                        Text(
                            "الريلز",
                            modifier = Modifier.align(Alignment.TopCenter)
                                .padding(top = 20.dp)
                                .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { tab = 0; page = ""; notice = ""; revision++ },
                            modifier = Modifier.align(Alignment.TopStart).padding(start=12.dp, top=12.dp)
                                .background(Color.Black.copy(alpha=0.48f), CircleShape)
                        ) { Icon(Icons.Outlined.ArrowForward, "العودة للتصفح", tint=Color.White) }
                    }
                } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (screen) {
                        "تصفح" -> {
                            item { SearchField(query, { query = it }, "ابحث عن حساب أو ريل أو عرض") }
                            item {
                                LazyRow(contentPadding=PaddingValues(horizontal=18.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                    items(listOf("الكل","الحسابات","الريلز","العروض")) { label ->
                                        FilterChip(selected=searchCategory==label, onClick={searchCategory=label}, label={Text(label)})
                                    }
                                }
                            }
                            if(searchCategory in listOf("الكل","الحسابات"))
                            items(accountMatches,key={"account-"+it.optInt("id")}){person->OutlinedCard(onClick={showCreator(person.optInt("id"))},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(person.optString("display_name"),style=MaterialTheme.typography.titleMedium);Text("@"+person.optString("username")+" · #"+person.optString("public_id"),style=LocalTextStyle.current.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr))}}}
                            if (query.trim().length >= 2 && searchCategory in listOf("الكل","الريلز")) {
                                items(searchReels, key={"search-reel-"+it.optInt("id")}){ reel ->
                                    Surface(onClick={selectedReelId=reel.optInt("id");tab=1;query="";searchCategory="الكل"},
                                        modifier=Modifier.padding(horizontal=18.dp).fillMaxWidth(),
                                        color=MaterialTheme.colorScheme.surfaceVariant, shape=RoundedCornerShape(16.dp)){
                                        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
                                            Icon(Icons.Outlined.SmartDisplay,"ريل",tint=MaterialTheme.colorScheme.primary)
                                            Text("ريل من "+reel.optString("owner_name"),Modifier.weight(1f))
                                            Icon(Icons.Outlined.PlayArrow,"مشاهدة")
                                        }
                                    }
                                }
                            }
                            if(searchCategory in listOf("الكل","الحسابات")) item {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item { Story("مساحتك", profile.optInt("selected_frame"), profile.optString("avatar_url")) { tab = 4 } }
                                    items(creators.filter{it.optInt("id")!=profile.optInt("id")}) { person -> Story(person.optString("display_name"), person.optInt("selected_frame"), person.optString("avatar_url")) { creator = person; open("صفحة المذيعة") } }
                                }
                            }
                            item { Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { followingOnly = false }) { Text("لك", fontSize = 18.sp, fontWeight = if (!followingOnly) FontWeight.Bold else FontWeight.Normal) }
                                TextButton(onClick = { followingOnly = true }) { Text("أتابعهم", color = if (followingOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            } }
                            val ids = creators.filter { it.optBoolean("following") }.map { it.optInt("id") }.toSet()
                            val visible = if (searchCategory in listOf("الكل","العروض")) services.filter {
                                (!followingOnly || it.optInt("owner_id") in ids) &&
                                (it.optString("title") + it.optString("owner_name")).contains(query, true)
                            } else emptyList()
                            if (visible.isEmpty() && !loading && contentError.isBlank() && query.isBlank() && searchCategory in listOf("الكل","العروض")) item { ReferenceEmpty(Icons.Outlined.Explore, "مساحتك لاكتشاف الجديد", "ستظهر هنا العروض التي ينشرها المستخدمون.") }
                            items(visible, key = { it.optInt("id") }) { service ->
                                FeedCard(service, { showCreator(service.optInt("owner_id")) }, { showService(service) }, { kind -> perform {
                                    val updated = api.request("/api/services/${service.optInt("id")}/reaction", "POST", JSONObject().put("kind", kind).put("enabled", !service.optBoolean(if (kind == "like") "liked" else "saved")))
                                    services = services.map { if (it.optInt("id") == updated.optInt("id")) updated else it }
                                } }, { startChat(service.optInt("owner_id"), service.optString("owner_name")) })
                            }
                        }
                        "العرض", "المحفوظات" -> {
                            item { SearchField(query, { query = it }, "ابحث عن خدمة أو جلسة أو منتج") }
                            item { LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("الكل", "خدمة", "منتج", "جلسة")) { label -> FilterChip(category == label, onClick = { category = label }, label = { Text(label) }) }
                            } }
                            val visible = (if (screen == "المحفوظات") rows else services).filter { (category == "الكل" || it.optString("category") == category) && it.optString("title").contains(query, true) }
                            if (visible.isEmpty() && !loading && contentError.isBlank()) item { ReferenceEmpty(Icons.Outlined.Storefront, "لا توجد عروض بعد", "العروض المعتمدة تظهر هنا. يمكنك العودة لاحقًا أو تحديث الصفحة.") }
                            items(visible.chunked(2)) { pair -> Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { service -> Box(Modifier.weight(1f)) { OfferCard(service) { showService(service) } } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            } }
                        }
                        "تفاصيل العرض" -> {
                            item { ReferenceImage(selected.optString("image_url"), Modifier.fillMaxWidth().height(290.dp)) }
                            item { Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(selected.optString("category", "خدمة"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selected.optString("title"), style = MaterialTheme.typography.headlineLarge)
                                Text(usd(selected.optLong("price_cents")), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Text(selected.optString("description"), style = MaterialTheme.typography.bodyLarge)
                                ReferenceRow(Icons.Outlined.PersonOutline, selected.optString("owner_name"), "عرض الملف الشخصي") { showCreator(selected.optInt("owner_id")) }
                                ServiceBookingButton(api,selected){open("الحجوزات");notice="حُجز المبلغ بانتظار قبول مقدم الخدمة."}
                                OutlinedButton(onClick = { reportKind = "service"; reportId = selected.optInt("id").toString(); open("إرسال بلاغ") }, modifier = Modifier.fillMaxWidth()) { Text("الإبلاغ عن العرض") }
                            } }
                        }
                        "دردشة" -> {
                            item { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("رسائلك", Modifier.weight(1f), fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                Text("${rows.size} محادثة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            } }
                            item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { open("محادثة جديدة") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.AddComment, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text("محادثة جديدة") }
                                OutlinedButton(onClick = { message = ""; open("الدعم وخدمة العملاء") }, shape = RoundedCornerShape(16.dp)) { Text("الدعم") }
                            } }
                            if (rows.isEmpty() && !loading && contentError.isBlank()) item { ReferenceEmpty(Icons.Outlined.ChatBubbleOutline, "ابدأ حديثًا جديدًا", "اختر حسابًا وابدأ برسالة أو صورة أو تسجيل صوتي.") }
                            items(rows, key = { it.optInt("id") }) { person -> Box(Modifier.padding(horizontal = 20.dp)) { ConversationTile(person) { startChat(person.optInt("id"), person.optString("display_name")) } } }
                        }
                        "محادثة جديدة" -> {
                            item { SearchField(query, { query = it }, "ابحث عن اسم المذيعة") }
                            items(creators.filter { (it.optString("display_name")+it.optString("username")+it.optString("public_id")).contains(query.removePrefix("@"), true) }, key = { it.optInt("id") }) { person -> Box(Modifier.padding(horizontal = 20.dp)) {
                                Surface(onClick = { startChat(person.optInt("id"), person.optString("display_name")) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ReferenceAvatar(person.optString("display_name"), person.optInt("selected_frame"), 48, person.optString("avatar_url"))
                                        Text(person.optString("display_name"), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Outlined.ChatBubbleOutline, "فتح المحادثة")
                                    }
                                }
                            } }
                            item { FormColumn {
                                Text("أو افتح محادثة برقم الحساب", style = MaterialTheme.typography.titleMedium)
                                Field(peerId, { peerId = it.filter(Char::isDigit) }, "رقم حساب المستلم", numeric = true)
                                ReferencePrimary("فتح المحادثة", peerId.toIntOrNull() != null) { startChat(peerId.toInt(), "حساب #$peerId") }
                            } }
                        }
                        "محادثة", "الدعم وخدمة العملاء" -> {
                            if (rows.isEmpty() && !loading && contentError.isBlank()) item { ReferenceEmpty(if (screen == "محادثة") Icons.Outlined.ChatBubbleOutline else Icons.Outlined.SupportAgent, "هنا تبدأ المحادثة", "الرسائل تُرسل إلى الخادم. لا توجد ردود تلقائية أو حسابات تجريبية.") }
                            items(rows.asReversed()) { row -> Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val mine = screen != "محادثة" || row.optInt("sender_id") == profile.optInt("id")
                                Surface(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 4.dp else 18.dp, bottomEnd = if (mine) 18.dp else 4.dp), color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, modifier = Modifier.align(if (mine) Alignment.Start else Alignment.End).widthIn(max = 300.dp)) { Text(row.optString("body"), Modifier.padding(14.dp)) }
                                if (row.optString("reply").isNotBlank()) Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.align(Alignment.End).widthIn(max = 300.dp)) { Text(row.optString("reply"), Modifier.padding(14.dp)) }
                            } }
                            item { FormColumn {
                                Field(message, { message = it }, "اكتب رسالتك", lines = 4)
                                ReferencePrimary("إرسال", !submitting && message.isNotBlank()) { perform {
                                    api.request(if (screen == "محادثة") "/api/messages/$peerId" else "/api/support", "POST", JSONObject().put("body", message))
                                    message = ""; revision++
                                } }
                            } }
                        }
                        "حسابي", "صفحة المذيعة" -> {
                            val own = screen == "حسابي"
                            val person = if (own) profile else creator
                            item { ProfileHero(person, own, { if (own) open("الإعدادات") else back() }, { if (own) editProfile() else startChat(person.optInt("id"), person.optString("display_name")) }, {
                                if (own) open("إطاراتي") else perform {
                                    creator = api.request("/api/creators/${person.optInt("id")}/follow", "POST", JSONObject().put("enabled", !person.optBoolean("following")))
                                    creators = creators.map { if (it.optInt("id") == creator.optInt("id")) creator else it }
                                }
                            }) }
                            if (own) item { Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { WalletSummary(if(walletAccountId == profile.optInt("id") && wallet.has("available_cents")) wallet.optLong("available_cents") else null, walletLoading, walletError, { buyCredits() }, { open("المحفظة") }, { revision++ }) } }
                            item { Box(Modifier.padding(horizontal = 20.dp)) { ReferenceCard {
                                ReferenceRow(Icons.Outlined.ChatBubbleOutline, if (own) "سعر التحدث معي" else "سعر المحادثة", if (person.optInt("chat_price_cents") == 0) "محادثة مجانية" else "${usd(person.optLong("chat_price_cents"))} · تتطلب التحقق من الدفع") { if (own) editProfile() else startChat(person.optInt("id"), person.optString("display_name")) }
                            } } }
                            if (own) item { FormColumn {
                                if(profile.optBoolean("is_admin"))ReferenceRow(Icons.Outlined.Settings,"لوحة الإدارة"){open("لوحة الإدارة")}
                                ReferenceRow(Icons.Outlined.ShoppingBag,"نشاطي","الحجوزات والبث وسجل العمليات"){open("نشاطي")}
                                ReferenceRow(Icons.Outlined.Notifications,"الإشعارات والاعتراضات"){open("التنبيهات")}
                                ReferenceRow(Icons.Outlined.BookmarkBorder,"الريلز المحفوظة"){savedReels=true;page="";tab=1;revision++}
                                ReferenceRow(Icons.Outlined.Lock,"أمان الحساب"){securityDialog=true}
                                ReferenceRow(Icons.Outlined.Notifications,"إعدادات إشعارات الهاتف"){context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))}
                                Text("الإصدار "+com.example.BuildConfig.VERSION_NAME,style=MaterialTheme.typography.bodySmall)
                                ReferenceRow(Icons.Outlined.Settings,"الإعدادات والمساعدة"){open("الإعدادات")}
                            } }
                            item { Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (own) "عروضي" else "العروض", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium); if (own) TextButton(onClick = { open("إضافة عرض") }) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("إضافة") } } }
                            val offers = services.filter { it.optInt("owner_id") == person.optInt("id") }
                            if (offers.isEmpty() && !loading) item { ReferenceEmpty(Icons.Outlined.Storefront, "اعرض خدمة، جلسة، أو منتجًا", "تظهر العروض المنشورة هنا مباشرة.") }
                            items(offers) { offer -> Box(Modifier.padding(horizontal = 20.dp)) { OfferCard(offer) { showService(offer) } } }


                        }
                        "تعديل الملف" -> item { FormColumn {
                            var advanced by rememberSaveable { mutableStateOf(false) }
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReferenceAvatar(displayName, profile.optInt("selected_frame"), 142, avatarUrl)
                                Button(
                                    onClick = { avatarPicker.launch("image/*") },
                                    enabled = !submitting,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF104BB5))
                                ) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("تغيير الصورة") }
                                if (avatarUrl.isNotBlank()) TextButton(
                                    onClick = { perform {
                                        profile = api.request("/api/me", "PATCH", JSONObject().put("avatar_url", ""))
                                        avatarUrl = ""; notice = "تم حذف صورة الملف."
                                    } }, enabled = !submitting
                                ) { Text("حذف الصورة") }
                                if (submitting) LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                            Field(displayName, { displayName = it.take(100) }, "الاسم الظاهر")
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Badge, null)
                                    Column { Text("رقم الحساب: ${profile.optString("public_id", "—")}",
                                        fontWeight = FontWeight.SemiBold)
                                        Text("معرّف ثابت لا يمكن تعديله", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            GenderChoice(profileGender, { profileGender = it })
                            Field(bio, { bio = it.take(150) }, "نبذة عني · ${bio.length}/150", lines = 3)
                            TextButton(onClick = { advanced = !advanced }) {
                                Icon(if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                                Spacer(Modifier.width(6.dp))
                                Text("بيانات الحساب والإعدادات المتقدمة")
                            }
                            if (advanced) {
                                Field(profileUsername, { profileUsername = it }, "اسم المستخدم الاختياري @username")
                                Text("اسم المستخدم يختلف عن رقم الحساب الثابت.", style = MaterialTheme.typography.bodySmall)
                                Field(profileEmail, { profileEmail = it }, "البريد الإلكتروني لتسجيل الدخول")
                                if (profile.optBoolean("publishing_allowed"))
                                    Field(chatPrice, { chatPrice = it }, "رسوم المحادثة بالدولار · 0 للمجانية", numeric = true)
                            }
                            ReferencePrimary("حفظ التعديلات", !submitting && displayName.isNotBlank()) { perform {
                                val body = JSONObject().put("display_name", displayName.trim())
                                    .put("bio", bio.trim()).put("avatar_url", avatarUrl)
                                if (profileGender.isNotBlank()) body.put("gender", profileGender)
                                if (advanced) {
                                    body.put("username", profileUsername).put("email", profileEmail.trim())
                                    if (profile.optBoolean("publishing_allowed")) body.put("chat_price_cents", cents(chatPrice))
                                }
                                profile = api.request("/api/me", "PATCH", body)
                                page = ""; tab = 4; notice = "تم حفظ التعديلات."
                            } }
                        } }
                        "إطاراتي" -> item {
                            val frameArray = profile.optJSONArray("owned_frames")
                            val owned = if (frameArray == null) setOf(0)
                                else (0 until frameArray.length()).map { frameArray.optInt(it) }.toSet()
                            RelaxiaFramePicker(profile.optInt("selected_frame"),
                                profile.optString("display_name"), profile.optString("avatar_url"),
                                profile.optInt("level", 1), owned) { frame -> perform {
                                profile = api.request("/api/me", "PATCH", JSONObject().put("selected_frame", frame))
                                notice = "تم تجهيز الإطار."
                            } }
                        }
                        "نشاطي" -> item { FormColumn {
                            ReferenceRow(Icons.Outlined.Sensors,"البث المباشر"){open("البث المباشر")}
                            ReferenceRow(Icons.Outlined.ShoppingBag,"حجوزاتي"){open("الحجوزات")}
                            ReferenceRow(Icons.Outlined.ReceiptLong,"سجل العمليات"){open("سجل العمليات")}
                            ReferenceRow(Icons.Outlined.BookmarkBorder,"المحفوظات"){open("المحفوظات")}
                            ReferenceRow(Icons.Outlined.Casino,"عجلة الحظ المدفوعة"){open("العجلة")}
                        } }
                        "الإعدادات" -> item { FormColumn {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("الوضع الليلي", Modifier.weight(1f)); Switch(dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() }) }
                            ReferenceRow(Icons.Outlined.SupportAgent,"الدعم وخدمة العملاء"){message="";open("الدعم وخدمة العملاء")}
                            ReferenceRow(Icons.Outlined.Block,"الحسابات المحظورة"){open("الحسابات المحظورة")}
                            ReferenceRow(Icons.Outlined.Info,"من نحن"){open("من نحن")}
                            ReferenceRow(Icons.Outlined.PersonOutline, "تعديل الملف الشخصي") { editProfile() }
                            ReferenceRow(Icons.Outlined.Shield, "الخصوصية وضوابط المحتوى") { open("سياسة الخصوصية") }
                            ReferenceRow(Icons.Outlined.Flag, "إرسال بلاغ") { open("إرسال بلاغ") }
                            ReferenceRow(Icons.Outlined.Logout, "تسجيل الخروج") { perform { api.request("/api/logout", "POST", JSONObject().put("push_token",context.getSharedPreferences("relaxia_push",0).getString("token",""))); signedIn = false; services = emptyList(); creators = emptyList(); rows = emptyList(); profile = JSONObject(); page = "" } }
                        } }
                        "من نحن", "سياسة الخصوصية" -> item { FormColumn {
                            Text(if (screen == "من نحن") "Relaxia" else "خصوصيتك وحدودك أولًا", style = MaterialTheme.typography.headlineLarge)
                            Text(if (screen == "من نحن") "مساحة عربية للبالغين لاكتشاف صنّاع المحتوى وخدماتهم والتواصل معهم، بمحتوى غير صريح وواجهات هادئة وواضحة." else "تُحفظ بيانات حسابك ورسائلك وحجوزاتك على خادم Relaxia. يُمنع المحتوى الجنسي الصريح والعري والتحرش ومشاركة بيانات الآخرين دون إذن. الريلز متاحة للحسابات المسجلة، والخدمات تُراجع قبل ظهورها. لا تضع معلومات دفع حساسة داخل المحادثات.", style = MaterialTheme.typography.bodyLarge)
                        } }
                        "الحجوزات" -> {
                            if (rows.isEmpty() && !loading && contentError.isBlank()) item { ReferenceEmpty(Icons.Outlined.ShoppingBag, "لم تبدأ حجوزاتك بعد", "تصفح العروض وسجّل طلب الخدمة المناسب لك.") }
                            items(rows) { row -> Box(Modifier.padding(horizontal = 18.dp)) { ReferenceCard { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(row.optString("service__title", "حجز #${row.optString("id").take(8)}"), style = MaterialTheme.typography.titleMedium)
                                Text(usd(row.optLong("price_cents")), fontWeight = FontWeight.Bold)
                                Text(if (row.optString("status") == "awaiting_payment") "بانتظار الدفع · لم يُؤكَّد الحجز بعد" else row.optString("status"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } } } }
                        }
                        "إضافة عرض" -> item { FormColumn {
                            Text("اعرض خدمة أو جلسة أو منتجًا", style = MaterialTheme.typography.headlineMedium)
                            Text("يمكن لجميع الحسابات النشطة نشر الخدمات مباشرة. يمكن للإدارة إخفاء المحتوى المخالف لاحقاً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("خدمة", "منتج", "جلسة").forEach { kind -> FilterChip(offerKind == kind, { offerKind = kind }, label = { Text(kind) }) } }
                            Field(title, { title = it }, "عنوان العرض")
                            Field(description, { description = it }, "وصف العرض", lines = 4)
                            Field(price, { price = it }, "السعر بالدولار", numeric = true)
                            Field(image, { image = it }, "رابط صورة HTTPS · اختياري")
                            ReferencePrimary("نشر الآن", !submitting) { perform {
                                val result = api.request("/api/services", "POST", JSONObject().put("title", title).put("description", description).put("price_cents", cents(price)).put("category", offerKind).put("image_url", image))
                                if(result.optString("status") == "published") {
                                    title = ""; description = ""; price = ""; image = ""; page = ""; tab = 2; revision++
                                    notice = "تم نشر عرضك بنجاح."
                                } else {
                                    notice = "تم حفظ العرض، لكن النشر لم يتأكد. تحقق من تحديث الخادم."
                                }
                            } }
                        } }
                        "المحفظة" -> {
                            item { FormColumn {
                                ReferenceCard { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("الرصيد المتاح", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(usd(wallet.optLong("available_cents")), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                    Text("محجوز: ${usd(wallet.optLong("reserved_cents"))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("نقاط ترويجية منفصلة: ${wallet.optLong("promotional_points")} · غير قابلة للسحب",style=MaterialTheme.typography.bodySmall)
                                } }
                                ReferenceRow(Icons.Outlined.ReceiptLong,"سجل العمليات والإيصالات"){open("سجل العمليات")}
                                ReferencePrimary("شراء رصيد", !submitting) { buyCredits() }
                                Text("الشحن يعتمد بعد تحقق الإدارة من التحويل. طلب السحب يحجز الرصيد للمراجعة ولا ينفّذ تحويلًا تلقائيًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Field(price, { price = it; requestKey = UUID.randomUUID().toString() }, "مبلغ السحب بالدولار", numeric = true)
                                Field(destination, { destination = it; requestKey = UUID.randomUUID().toString() }, "جهة استلام الأموال")
                                ReferencePrimary("طلب سحب", !submitting) { perform {
                                    api.request("/api/withdrawals", "POST", JSONObject().put("amount_cents", cents(price)).put("destination", destination).put("request_key", requestKey))
                                    price = ""; destination = ""; requestKey = UUID.randomUUID().toString(); revision++; notice = "سُجّل الطلب للمراجعة. لم يتم تحويل الأموال."
                                } }
                                Text("طلبات السحب", style = MaterialTheme.typography.titleLarge)
                            } }
                            items(rows) { row -> Box(Modifier.padding(horizontal = 22.dp)) { ReferenceCard { Text("${usd(row.optLong("amount_cents"))} · ${if (row.optString("status") == "pending") "قيد المراجعة" else row.optString("status")}", Modifier.padding(18.dp)) } } }
                        }
                        "إرسال بلاغ" -> item { FormColumn {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("service" to "عرض", "reel" to "ريل", "user" to "حساب", "message" to "رسالة").forEach { (kind, label) -> FilterChip(reportKind == kind, { reportKind = kind }, label = { Text(label) }) } }
                            Field(reportId, { reportId = it.filter(Char::isDigit) }, "رقم العنصر", numeric = true)
                            Field(reason, { reason = it }, "سبب البلاغ", lines = 4)
                            ReferencePrimary("إرسال البلاغ", !submitting && reportId.toIntOrNull() != null) { perform {
                                api.request("/api/reports", "POST", JSONObject().put("target_type", reportKind).put("target_id", reportId.toInt()).put("reason", reason)); reason = ""; notice = "وصل البلاغ إلى الإدارة."
                            } }
                        } }
                        "ريلز" -> if (!loading && contentError.isBlank()) item { ReferenceEmpty(Icons.Outlined.SmartDisplay, "شاهد اللحظات", "الريلز المنشورة ستظهر هنا فور رفعها.") }
                    }
                }
            }
        }
        if (showCreationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreationSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "إنشاء محتوى جديد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "اختر ما تريد نشره في مجتمع Relaxia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = {
                            showCreationSheet = false
                            reelPicker.launch("video/mp4")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF277C9D).copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.SmartDisplay, null, tint = Color(0xFF277C9D), modifier = Modifier.size(22.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("نشر ريلز", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("شارك فيديو قصير مع الجمهور من المعرض مباشرة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        onClick = {
                            showCreationSheet = false
                            open("إضافة عرض")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD7AD72).copy(alpha = 0.18f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Storefront, null, tint = Color(0xFFD7AD72), modifier = Modifier.size(22.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("عرض شيء للبيع", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("أضف خدمة، جلسة، أو منتجاً مع تحديد السعر والصور", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        onClick = {
                            showCreationSheet = false
                            open("البث المباشر")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE53935).copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Sensors, null, tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("بدء بث مباشر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("تجهيز الكاميرا والصوت وبدء بث تفاعلي للجمهور", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun cents(value: String): Int {
    val decimal = value.trim().toBigDecimalOrNull() ?: error("invalid_input")
    return try { decimal.multiply(BigDecimal(100)).intValueExact().also { require(it >= 0) } }
    catch (_: Exception) { error("invalid_input") }
}

@Composable
private fun FormColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable
private fun Field(value: String, change: (String) -> Unit, label: String, lines: Int = 1, numeric: Boolean = false) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), minLines = lines, maxLines = if (lines == 1) 1 else 6, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface))
}
@Composable
private fun SearchField(value: String, change: (String) -> Unit, hint: String) {
    OutlinedTextField(value, change, placeholder = { Text(hint, style = MaterialTheme.typography.bodyMedium) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f), focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f)))
}
@Composable
private fun Story(name: String, frame: Int, imageUrl: String = "", click: () -> Unit) {
    Column(Modifier.width(68.dp).clickable(onClick = click).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ReferenceAvatar(name, frame, 56, imageUrl)
        Text(name.substringBefore(' '), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
    }
}
@Composable
private fun FeedCard(service: JSONObject, owner: () -> Unit, details: () -> Unit, react: (String) -> Unit, chat: () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) { ReferenceCard {
        Row(Modifier.fillMaxWidth().padding(14.dp).clickable(onClick = owner), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReferenceAvatar(service.optString("owner_name"), service.optInt("owner_frame"), 52, service.optString("owner_avatar_url"))
            Column(Modifier.weight(1f)) { Text(service.optString("owner_name"), fontWeight = FontWeight.SemiBold); Text(service.optString("category", "خدمة"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { react("save") }) { Icon(if (service.optBoolean("saved")) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, "حفظ العرض") }
        }
        ReferenceImage(service.optString("image_url"), Modifier.fillMaxWidth().aspectRatio(1.14f).clip(RoundedCornerShape(15.dp)).clickable(onClick = details))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { react("like") }) { Icon(if (service.optBoolean("liked")) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "إعجاب", tint = if (service.optBoolean("liked")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            Text(service.optInt("like_count").toString(), fontSize = 12.sp)
            IconButton(onClick = chat) { Icon(Icons.Outlined.ChatBubbleOutline, "محادثة صاحب الحساب") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = details) { Text("عرض التفاصيل") }
        }
        Text(service.optString("title"), Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.SemiBold)
        Text(service.optString("description"), Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 8.dp), maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}
@Composable
private fun OfferCard(service: JSONObject, click: () -> Unit) {
    ReferenceCard(onClick = click) {
        ReferenceImage(service.optString("image_url"), Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(service.optString("category", "خدمة"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(service.optString("title"), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(usd(service.optLong("price_cents")), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ProfileHero(
    person: JSONObject, own: Boolean,
    settings: () -> Unit, editOrChat: () -> Unit, frameOrFollow: () -> Unit
) {
    val context = LocalContext.current
    val blue = Color(0xFF104BB5)
    val navy = Color(0xFF08295F)
    Column(Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(blue, navy)),
            RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
        .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (own) "حسابي" else "الملف الشخصي",
                modifier = Modifier.weight(1f), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = settings) {
                Icon(if (own) Icons.Outlined.Settings else Icons.Outlined.ArrowForward,
                    if (own) "الإعدادات" else "رجوع", tint = Color.White)
            }
        }
        ReferenceAvatar(person.optString("display_name", person.optString("username")),
            person.optInt("selected_frame"), 128, person.optString("avatar_url"))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(person.optString("display_name", "المستخدم"),
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val genderMark = when(person.optString("gender")) {
                "male" -> "♂"
                "female" -> "♀"
                else -> ""
            }
            if(genderMark.isNotBlank()) Text(genderMark, fontSize = 23.sp, color = Color(0xFFB8DDFF))
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val id = person.optString("public_id", "—")
            Text("ID: $id", color = Color.White.copy(alpha = .85f), fontSize = 15.sp)
            if(id != "—") IconButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Relaxia ID", id))
                }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.ContentCopy, "نسخ رقم الحساب",
                    tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
        if(person.optBoolean("identity_verified")) Text("✓ حساب موثق",
            color = Color(0xFFB6DBFF), fontSize = 12.sp)
        if(person.optString("bio").isNotBlank()) Text(person.optString("bio"),
            color = Color.White.copy(alpha = .85f), maxLines = 2,
            overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
        Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(vertical = 14.dp, horizontal = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("الريلز" to person.optInt("reel_count"),
                    "العروض" to person.optInt("service_count"),
                    "المتابعون" to person.optInt("followers_count"),
                    "أتابع" to person.optInt("following_count")).forEach { (label, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), color = Color.White, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text(label, color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(onClick = editOrChat, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White,
                    contentColor = navy)) {
                Icon(if (own) Icons.Outlined.Edit else Icons.Outlined.ChatBubbleOutline,
                    null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if(own) "تعديل الملف" else "محادثة", maxLines = 1, fontSize = 13.sp)
            }
            OutlinedButton(onClick = frameOrFollow,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=.65f))) {
                Icon(if(own) Icons.Outlined.WorkspacePremium else Icons.Outlined.PersonAdd,
                    null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if(own) "إطاراتي" else if(person.optBoolean("following"))
                    "إلغاء المتابعة" else "متابعة", fontSize = 13.sp, maxLines = 1)
            }
        }
        if (own && person.has("level")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = Color(0xFFE3C88D))
                Spacer(Modifier.width(8.dp))
                Text("المستوى ${person.optInt("level")}", color = Color.White,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${person.optLong("xp_points")} XP", color = Color.White.copy(alpha=.84f),
                    fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = {
                    val current = person.optLong("xp_points")
                    val cap = person.optLong("next_level_xp", 200L)
                    if(person.optInt("level") >= 100) 1f
                    else ((current - (cap - 200)).toFloat() / 200f).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Color(0xFFD3ECFF), trackColor = Color.White.copy(alpha=.2f)
            )
        }
    }
}

@Composable
internal fun ReferenceLogin(register: Boolean, busy: Boolean, notice: String, username: String, password: String, email: String, adultConfirmed: Boolean, onUsername: (String) -> Unit, onPassword: (String) -> Unit, onEmail: (String) -> Unit, onAdultConfirmed: (Boolean) -> Unit, toggle: () -> Unit, submit: () -> Unit, gender:String="", onGender:(String)->Unit={},onForgot:()->Unit={}) {
    var showPassword by remember { mutableStateOf(false) }
    val ready=!busy && email.trim().contains("@") && password.isNotBlank() && (!register||(username.trim().length>=2&&password.length>=8&&adultConfirmed&&gender in listOf("male","female")))
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
        LazyColumn(Modifier.fillMaxSize().imePadding(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
            item{Column(Modifier.fillMaxWidth().background(WisalInk,RoundedCornerShape(bottomEnd=40.dp)).statusBarsPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.relaxia_logo),"شعار Relaxia",Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.weight(1f));Text("18+",color=Color.White.copy(alpha=.8f))}
                Text("Relaxia",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text("لحظات أقرب، بطريقتك.",color=Color.White.copy(alpha=.85f),fontSize=16.sp)
            }}
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Text(if(register)"مساحتك تبدأ هنا"else"أهلًا بعودتك",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                Text(if(register)"أنشئ حسابك بخطوات بسيطة."else"ادخل ببريدك الإلكتروني وكلمة المرور.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(!register) Text("المسؤول يدخل من الشاشة نفسها بحسابه الإداري؛ تظهر لوحة الإدارة بعد الدخول والتحقق الثنائي.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(notice.isNotBlank())Text(notice,color=MaterialTheme.colorScheme.error)
                if(register)OutlinedTextField(username,onUsername,label={Text("الاسم الظاهر")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
                OutlinedTextField(email,onEmail,label={Text("البريد الإلكتروني")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next))
                OutlinedTextField(password,onPassword,label={Text("كلمة المرور")},visualTransformation=if(showPassword)VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={IconButton({showPassword=!showPassword}){Icon(if(showPassword)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(showPassword)"إخفاء كلمة المرور"else"إظهار كلمة المرور")}},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={if(ready)submit()}))
                if(register){
                    GenderChoice(gender,onGender)
                    Text("8 خانات أو أكثر. يمكنك اختيار اسم المستخدم لاحقًا من ملفك.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Checkbox(adultConfirmed,onAdultConfirmed);Text("أؤكد أن عمري 18 عامًا أو أكثر وأوافق على ضوابط المحتوى غير الصريح.",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)}
                }
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(!register)TextButton(onClick=onForgot,enabled=!busy){Text("نسيت كلمة المرور؟")}
                ReferencePrimary(if(register)"إنشاء حساب"else"دخول",ready){submit()}
                TextButton(toggle,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)){Text(if(register)"لديك حساب؟ تسجيل الدخول"else"جديد هنا؟ أنشئ حسابًا")}
            }}
        }
    }
}

private fun formatReelTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun ReelItemView(
    row: JSONObject,
    active: Boolean,
    onChat: () -> Unit,
    onCreator: () -> Unit,
    api: LayaliApi,
    viewerId: Int,
    onHidden: () -> Unit = {}
) {
    val url = row.optString("video_url")
    val context = LocalContext.current
    var isPlaying by remember(url) { mutableStateOf(active) }
    var playbackError by remember(url) { mutableStateOf<String?>(null) }
    var buffering by remember(url) { mutableStateOf(true) }
    var retryKey by remember(url) { mutableIntStateOf(0) }
    var muted by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableLongStateOf(0L) }
    var positionMs by remember(url) { mutableLongStateOf(0L) }
    var following by remember(row.optInt("owner_id")) { mutableStateOf(row.optBoolean("following")) }
    var followBusy by remember { mutableStateOf(false) }
    var followError by remember { mutableStateOf("") }
    var liked by remember(row.optInt("id")) { mutableStateOf(row.optBoolean("liked")) }
    var likeCount by remember(row.optInt("id")) { mutableIntStateOf(row.optInt("like_count")) }
    var likeBusy by remember(row.optInt("id")) { mutableStateOf(false) }
    var likeError by remember(row.optInt("id")) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var actions by remember { mutableStateOf(false) }
    var saved by remember(row.optInt("id")) { mutableStateOf(row.optBoolean("saved")) }
    var actionBusy by remember { mutableStateOf(false) }
    fun preference(action:String,enabled:Boolean){
        if(actionBusy)return
        actionBusy=true
        scope.launch{try{api.request("/api/reels/${row.optInt("id")}/action","POST",JSONObject().put("action",action).put("enabled",enabled));if(action=="hidden")onHidden()else saved=enabled;actions=false}catch(e:Exception){likeError="تعذّر حفظ اختيارك."}finally{actionBusy=false}}
    }
    if(actions)AlertDialog(onDismissRequest={actions=false},title={Text("خيارات الريل")},text={Column{
        TextButton(enabled=!actionBusy,onClick={preference("saved",!saved)}){Text(if(saved)"إزالة من المحفوظات"else"حفظ الريل")}
        TextButton(enabled=!actionBusy,onClick={preference("hidden",true)}){Text("غير مهتم بهذا الريل")}
    }},confirmButton={TextButton(onClick={actions=false}){Text("إغلاق")}})


    LaunchedEffect(active) { isPlaying = active }
    // A player is owned by the current page only. Swiping releases its decoder.
    val player = remember(url, active, retryKey) {
        if (active && url.startsWith("https://")) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }
        } else null
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "تعذّر تشغيل الفيديو (رمز ${error.errorCode})."
                buffering = false
            }
            override fun onRenderedFirstFrame() { buffering = false }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }
    LaunchedEffect(player, active, isPlaying, muted) {
        player?.playWhenReady = active && isPlaying && playbackError == null
        player?.volume = if (muted) 0f else 1f
    }
    // All timing shown in the UI comes from the actual decoder timeline.
    LaunchedEffect(player, active) {
        while (active && player != null) {
            val length = player.duration
            durationMs = if (length > 0 && length < 24 * 60 * 60 * 1000L) length else 0L
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(250)
        }
    }
    LaunchedEffect(player, retryKey, active) {
        if (player != null && active) {
            delay(15000)
            if (buffering && playbackError == null) {
                playbackError = "تأخر ظهور الفيديو. تأكد من الشبكة وأعد المحاولة."
                player.pause()
            }
        }
    }

    LaunchedEffect(active,player){
        var watched=0
        while(active && watched<180){
            delay(1000)
            if(player?.isPlaying==true)watched++
            if(watched==3 || (watched>0 && watched%15==0))runCatching{api.request("/api/reels/${row.optInt("id")}/action","POST",JSONObject().put("action","watch").put("seconds",watched))}
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { viewContext ->
                    LayoutInflater.from(viewContext).inflate(
                        R.layout.reel_video_player, null, false
                    ) as PlayerView
                },
                modifier = Modifier.fillMaxSize(),
                update = { it.player = player }
            )
        }
        // TextureView, not SurfaceView: Compose actions stay in front of the video.
        Box(Modifier.fillMaxSize().clickable {
            if (playbackError == null) isPlaying = !isPlaying
        })

        // Gradient scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x33000000),
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x88000000),
                            Color(0xE6101820)
                        )
                    )
                )
        )

        if(active && (playbackError != null || !url.startsWith("https://"))) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Outlined.ErrorOutline, "خطأ تشغيل", tint = Color.White, modifier = Modifier.size(36.dp))
                Text(
                    playbackError ?: "رابط الفيديو غير صالح.",
                    color = Color.White, fontSize = 15.sp
                )
                if(url.startsWith("https://")) Button(onClick = {
                    playbackError = null
                    buffering = true
                    retryKey++
                }) { Text("إعادة المحاولة") }
            }
        } else if(active && buffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // Real player controls; no invented duration, likes or viewer counts.
        if (active && playbackError == null && !buffering) {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Slider(
                    value = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        if (durationMs > 0) {
                            val next = (durationMs * fraction).toLong()
                            player?.seekTo(next)
                            positionMs = next
                        }
                    },
                    enabled = durationMs > 0,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, "تشغيل أو إيقاف", tint = Color.White)
                    }
                    Text(
                        "${formatReelTime(positionMs)} / ${formatReelTime(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    IconButton(onClick = { muted = !muted }) {
                        Icon(if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp, if (muted) "تشغيل الصوت" else "كتم الصوت", tint = Color.White)
                    }
                }
            }
        }

        // Center Play icon when paused
        if (!isPlaying) {
            Surface(
                color = Color(0x66000000),
                shape = CircleShape,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // Publisher Meta Overlay (Bottom Start)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 16.dp, end = 16.dp, bottom = 106.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onCreator)
            ) {
                ReferenceAvatar(
                    row.optString("owner_name"),
                    row.optInt("selected_frame"),
                    36,
                    row.optString("avatar_url")
                )
                Text(
                    row.optString("owner_name"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.optBoolean("identity_verified")) {
                    Icon(Icons.Outlined.Verified, "حساب موثق", tint = Color(0xFF9DCDE4), modifier = Modifier.size(17.dp))
                }
            }
            val handle = row.optString("owner_username")
            if (handle.isNotBlank()) Text("@$handle", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
            if (viewerId > 0 && row.optInt("owner_id") != viewerId && row.optInt("owner_id") > 0) {
                OutlinedButton(
                    onClick = {
                        if (!followBusy) {
                            followBusy = true
                            followError = ""
                            scope.launch {
                                try {
                                    val result = api.request(
                                        "/api/creators/${row.optInt("owner_id")}/follow",
                                        "POST",
                                        JSONObject().put("enabled", !following)
                                    )
                                    following = result.optBoolean("following")
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (e: Exception) { followError = "تعذّرت المتابعة" }
                                finally { followBusy = false }
                            }
                        }
                    },
                    enabled = !followBusy,
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 3.dp)
                ) { Text(if (following) "إلغاء المتابعة" else "متابعة", color = Color.White, fontSize = 12.sp) }
            }
            if (followError.isNotBlank()) Text(followError, color = Color(0xFFFFC5C5), fontSize = 11.sp)
        }

        IconButton(onClick={actions=true},modifier=Modifier.align(Alignment.TopEnd).padding(top=16.dp,end=12.dp)){Icon(Icons.Outlined.MoreVert,"خيارات الريل",tint=Color.White)}
        // Floating Action Buttons (Bottom End)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        if (!likeBusy) {
                            likeBusy = true
                            likeError = ""
                            scope.launch {
                                try {
                                    val result = api.request(
                                        "/api/reels/${row.optInt("id")}/like", "POST",
                                        JSONObject().put("enabled", !liked)
                                    )
                                    liked = result.optBoolean("liked")
                                    likeCount = result.optInt("like_count")
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { likeError = "تعذّر تسجيل الإعجاب" }
                                finally { likeBusy = false }
                            }
                        }
                    },
                    enabled = !likeBusy,
                    modifier = Modifier.size(45.dp).background(Color(0x66000000), CircleShape)
                ) {
                    Icon(if (liked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        "إعجاب بالريل", tint = if (liked) Color(0xFFFF6B80) else Color.White)
                }
                Text(likeCount.toString(), color = Color.White, fontSize = 11.sp)
                if (likeError.isNotBlank()) Text(likeError, color = Color(0xFFFFB3BA), fontSize = 9.sp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onChat)
            ) {
                Surface(
                    color = Color(0x66000000),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            "محادثة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("محادثة", color = Color.White, fontSize = 10.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Relaxia\n${row.optString("share_url",api.origin+"/s/reel/"+row.optInt("id"))}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة"))
                }
            ) {
                Surface(
                    color = Color(0x66000000),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Share,
                            "مشاركة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("مشاركة", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

private fun friendlyError(code: String?): String = when (code) {
    "authentication_required", "invalid_session" -> "انتهت جلسة الدخول. أدخل بريدك الإلكتروني وكلمة المرور للمتابعة."
    "invalid_credentials" -> "البريد الإلكتروني أو كلمة المرور غير صحيحين. إذا لم تُنشئ حسابًا بعد، اضغط «أنشئ حسابًا»."
    "invalid_username" -> "اسم المستخدم يبدأ بحرف إنجليزي ويحتوي 3–30 خانة من الحروف والأرقام والنقطة أو الشرطة السفلية."
    "invalid_password" -> "اختر كلمة مرور من 8 خانات أو أكثر. لا يلزم رمز خاص أو أحرف كبيرة."
    "password_too_common" -> "كلمة المرور شائعة جدًا. اختر كلمة مختلفة يسهل عليك تذكّرها."
    "invalid_display_name" -> "أدخل اسمًا ظاهرًا من حرفين على الأقل."
    "email_unavailable" -> "هذا البريد مرتبط بحساب آخر. استخدم بريدًا مختلفًا أو سجّل الدخول."
    "protected_username" -> "اسم مستخدم المالك محجوز ولا يمكن تغييره."
    "invalid_email" -> "البريد الإلكتروني إلزامي. أدخل عنوانًا صحيحًا مثل name@example.com."
    "invalid_input" -> "إحدى البيانات غير صحيحة. راجع الحقول ثم حاول مجددًا."
    "server_update_required" -> "يلزم تحديث خادم Relaxia لتفعيل الاسم والبريد الإلزاميين والميزات الجديدة."
    "server_unavailable" -> "الخادم غير متاح الآن. حاول بعد قليل؛ لا تغيّر كلمة المرور."
    "adult_policy_required" -> "فعّل تأكيد 18+ والموافقة على ضوابط المحتوى."
    "verified_creator_required", "creator_account_required" -> "صلاحية النشر موقوفة لهذا الحساب. تواصل مع الدعم."
    "identity_verification_required" -> "خادم التطبيق ما زال بالنسخة القديمة. حدّثه لتفعيل عرض الريلز للجميع."
    "paid_chat_requires_verified_payment" -> "المحادثة مدفوعة. الدفع غير مفعّل بعد، ولم تُرسل الرسالة."
    "insufficient_funds" -> "رصيدك المتاح لا يكفي لطلب السحب."
    "too_many_attempts" -> "محاولات كثيرة. حاول بعد 15 دقيقة."
    "username_unavailable" -> "اسم المستخدم مستخدم بالفعل أو محجوز. اختر اسمًا آخر."
    "conflict" -> "الطلب موجود بالفعل. حدّث الصفحة قبل إعادة المحاولة."
    "invalid_chat_price" -> "أدخل سعر محادثة صحيحًا، أو صفرًا للمحادثة المجانية."
    "invalid_image_url" -> "أدخل رابط صورة صحيحًا يبدأ بـ https://."
    "avatar_update_required" -> "الخادم لا يدعم رفع صورة الملف بعد. يلزم تحديث الخادم المطابق للتطبيق."
    "invalid_avatar" -> "تعذر إرفاق الصورة بهذا الحساب. أعد رفعها من المعرض."
    "invalid_media" -> "اختر ملف فيديو MP4 صالحًا مع بيانات الفيديو كاملة."
    "file_too_large" -> "حجم الملف كبير. الحد الأقصى للريلز 20 ميغابايت."
    "storage_limit" -> "بلغ الحساب حد التخزين المسموح. احذف المحتوى غير المطلوب."
    "reel_publish_unconfirmed" -> "رُفع الفيديو، لكن تأكيد النشر لم يصل. تحقق من قائمة الريلز."
    "invalid_gender" -> "اختر الجنس: ذكر أو أنثى."
    "recipient_unavailable", "creator_unavailable" -> "الحساب غير متاح حاليًا."
    else -> "تعذّر الاتصال بالخادم. تحقق من الإنترنت ثم أعد المحاولة."
}

internal fun genderLabel(value:String)=when(value){"male"->"ذكر";"female"->"أنثى";else->"غير محدد"}
@Composable
internal fun GenderChoice(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("الجنس")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("male", "female").forEach { choice ->
                FilterChip(selected = value == choice, onClick = { onChange(choice) }, label = { Text(genderLabel(choice)) })
            }
        }
    }
}

@Composable
internal fun HomeToolbarActions(live:()->Unit,wallet:()->Unit,saved:()->Unit,refresh:()->Unit){
    var expanded by remember{mutableStateOf(false)}
    TextButton(live){Icon(Icons.Outlined.Sensors,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("مباشر")}
    Box {
        IconButton({expanded=true}){Icon(Icons.Outlined.MoreVert,"خيارات التصفح")}
        DropdownMenu(expanded,{expanded=false}){
            DropdownMenuItem(text={Text("المحفظة والرصيد")},onClick={expanded=false;wallet()})
            DropdownMenuItem(text={Text("المحفوظات")},onClick={expanded=false;saved()})
            DropdownMenuItem(text={Text("تحديث المحتوى")},onClick={expanded=false;refresh()})
        }
    }
}