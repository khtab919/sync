package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal

internal fun commerceError(e: Exception): String = when(e.message) {
    "recipient_unavailable" -> "هذا الحساب غير متاح للمراسلة حاليًا. قد يكون معطلًا أو محذوفًا؛ ارجع إلى المحادثات."
    "message_request_conflict" -> "تعذّر تأكيد الرسالة بسبب تعارض في الطلب. لا تعِد دفع أي مبلغ قبل التحقق من سجل المحادثة."
    "invalid_body" -> "اكتب رسالة صالحة ثم أعد المحاولة."
    "server_unavailable" -> "تعذّر الاتصال بخادم Relaxia. تحقق من الإنترنت وحالة الخادم ثم حاول مجددًا."
    "server_error_401" -> "انتهت جلسة الحساب؛ سجّل الدخول مجددًا."
    "server_error_403" -> "لا تملك صلاحية تنفيذ هذا الإجراء."
    "server_error_404" -> "الصفحة أو الحساب المطلوب غير متاح حاليًا."
    "file_too_large" -> "الحد الأقصى للصورة أو الصوت 8MB، وللريلز 20MB."
    "invalid_image", "invalid_file", "invalid_media" -> "الملف غير مدعوم. اختر صورة JPG/PNG/WEBP أو فيديو MP4."
    "creator_account_required" -> "صلاحية النشر موقوفة لهذا الحساب. تواصل مع الدعم."
    "purchase_already_submitted" -> "تم تقديم هذا الطلب بالفعل. افتح طلباتي لمتابعة حالته."
    "frame_locked" -> "هذا الإطار مقفل. تابع تقدم مستواك لفتحه."
    "invalid_frame" -> "حدّث الخادم لتفعيل مجموعة الإطارات الجديدة."
    "transaction_already_submitted" -> "هذا التحويل مستخدم في طلب سابق."
    "invalid_transaction_hash" -> "أدخل رقم المعاملة TxID الكامل المكوّن من 64 خانة."
    "seller_request_pending" -> "طلب تفعيل البائع موجود بالفعل وقيد المراجعة."
    "already_seller" -> "حسابك مفعّل كبائع بالفعل."
    "live_not_configured", "live_server_unavailable" -> "خادم البث غير جاهز. تحقق من نشر التحديث."
    "invalid_booking_action" -> "تغيّرت حالة الطلب؛ حدّث الصفحة."
    "financial_review_required" -> "تحتاج هذه العملية مراجعة الإدارة."
    "paid_booking_required" -> "يلزم حجز جلسة وقبولها قبل بدء الاتصال المدفوع."
    "paid_price_changed" -> "تغيّر السعر؛ حدّث المحادثة وأكّد السعر الجديد."
    "message_unavailable" -> "المرفق غير متاح لهذا الحساب."
    "insufficient_balance", "insufficient_funds" -> "الرصيد المتاح لا يكفي."
    "nowpayments_not_configured" -> "بوابة NOWPayments تنتظر إعداد مفاتيح حساب التاجر."
    "invoice_creation_pending", "payment_creation_pending", "invoice_already_created" -> "هذه الفاتورة قيد التحقق. لا تنشئ تحويلًا جديدًا قبل مراجعة طلباتي أو الدعم."
    "invalid_payment_network", "invalid_payment_address", "invalid_payment_amount" -> "تعذّر التحقق من تفاصيل الدفع على شبكة Tron. لم يتم إنشاء تعليمات تحويل."
    "quote_expired" -> "انتهت مهلة هذا الطلب. أنشئ طلب شراء جديدًا."
    "provider_check_pending" -> "التحقق من البوابة لم يكتمل بعد. حاول لاحقًا."
    "update_app_for_wheel_rules" -> "حدّث التطبيق لعرض قواعد العجلة الجديدة قبل الدفع."
    "shamcash_not_configured" -> "بيانات استلام شام كاش غير مفعلة بعد."
    "syriatelcash_not_configured" -> "سيريتيل كاش لم يُفعّل بعد. يجب أن تضبط الإدارة سعر الصرف أولاً."
    "payment_quote_required" -> "أنشئ فاتورة من التطبيق قبل إرسال إثبات تحويل سيريتيل كاش."
    "invalid_transaction_reference" -> "أدخل رقم إشعار الدفع الصحيح."
    "wheel_configuration_changed" -> "تغيّر سعر اللفة أو قواعدها. حدّث الصفحة قبل الدفع."
    "wheel_unavailable" -> "العجلة غير مفعّلة حاليًا."
    else -> "تعذّر تنفيذ الطلب. تحقق من البيانات والاتصال وتحديث الخادم."
}

@Composable
internal fun CommerceScreen(api: LayaliApi, kind: String, back: () -> Unit) {
    if(kind == "شحن الرصيد") { RelaxiaTopupMethodsScreen(api, back); return }
    val isWheel = kind == "العجلة" || kind == "يا نصيب"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf(JSONObject()) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("10") }
    var tx by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf("") }
    var receiptUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var wheelKey by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var wheelRotation by remember { mutableFloatStateOf(0f) }
    var showRules by remember { mutableStateOf(false) }
    var confirmSpin by remember { mutableStateOf(false) }
    suspend fun refresh() {
        val result = api.request(if(kind == "شحن الرصيد") "/api/topups" else "/api/seller-requests")
        val a = result.optJSONArray("items")
        rows = if(a == null) emptyList() else (0 until a.length()).map { a.getJSONObject(it) }
    }
    fun perform(block: suspend () -> Unit) {
        if(busy) return
        busy = true; note = ""
        scope.launch { try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { note = commerceError(e) } finally { busy = false } }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) perform { val result = api.upload(context, uri, "receipt"); receipt = result.getString("id"); receiptUrl = result.getString("url"); note = "تم رفع صورة الإيصال." }
    }
    LaunchedEffect(kind) {
        busy = true
        try { info = api.request(if(isWheel) "/api/promotion/wheel" else "/api/payment-info"); if(!isWheel) refresh() }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { note = commerceError(e) }
        finally { busy = false }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(back, enabled = !busy) { Text("رجوع") }
            Text(if(isWheel) "يا نصيب" else kind, style = MaterialTheme.typography.headlineLarge)
            if(busy) LuxuryLoadingStatus("جارٍ التحميل")
            if(note.isNotBlank()) Text(note, color = MaterialTheme.colorScheme.primary)
            if(kind == "شحن الرصيد") {
                Text("USDT · BEP20 فقط", style = MaterialTheme.typography.titleLarge)
                Text("عروض الشحن: 25 USDT مع 4% رصيد إضافي، و50 مع 6%، و100 مع 8%. المكافأة تعتمد مع الدفع.")
                Text("يُضاف الرصيد بعد تحقق الإدارة من التحويل الفعلي. صورة الإيصال وحدها لا تكفي للاعتماد. 1 USDT = 1 دولار رصيد داخلي.")
                ReferenceImage(info.optString("qr_url"), Modifier.fillMaxWidth().height(245.dp))
                Text(info.optString("address"), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("USDT BEP20", info.optString("address"))); note = "تم نسخ عنوان المحفظة." }, enabled = info.has("address")) { Text("نسخ العنوان") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("10", "25", "50").forEach { p -> FilterChip(price == p, { price = p }, label = { Text("$p USDT") }) } }
                OutlinedTextField(price, { price = it }, label = { Text("المبلغ المحوّل USDT") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tx, { tx = it.trim() }, label = { Text("رقم المعاملة TxID") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text(if(receipt.isBlank()) "رفع صورة إثبات الدفع" else "استبدال صورة الإيصال") }
                if(receiptUrl.isNotBlank()) ReferenceImage(receiptUrl, Modifier.fillMaxWidth().height(160.dp))
                Button(onClick = { perform {
                    val value = BigDecimal(price.trim()).movePointRight(2).intValueExact()
                    api.request("/api/topups", "POST", JSONObject().put("amount_cents", value).put("transaction_hash", tx).put("receipt_id", receipt))
                    receipt = ""; receiptUrl = ""; tx = ""; refresh(); note = "أُرسل طلب الشحن إلى الإدارة. لم يُعتمد الرصيد بعد."
                } }, enabled = !busy && receipt.isNotBlank() && tx.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("إرسال الدفع للتحقق") }
            } else if(kind == "تفعيل البائع") {
                Text("النشر متاح لجميع الحسابات النشطة دون تفعيل بائع. هذه الاستمارة اختيارية لطلب مراجعة وضع الحساب لدى الإدارة.")
                OutlinedTextField(description, { description = it.take(1000) }, label = { Text("نوع المحتوى والخدمات التي تريد تقديمها") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                Button(onClick = { perform { api.request("/api/seller-requests", "POST", JSONObject().put("description", description)); description = ""; refresh(); note = "تم تقديم طلبك للمراجعة." } }, enabled = !busy && description.trim().length >= 10) { Text("تقديم طلب تفعيل البائع") }
            } else {
                RewardWheel(info.optJSONArray("prizes"), wheelRotation)
                Text("لفة مدفوعة من الرصيد. راجع السعر واحتمالات الجوائز قبل التأكيد.")
                if(info.optBoolean("enabled")) {
                    Text("سعر اللفة: ${info.optLong("cost_cents")} كوين")
                    if(info.optBoolean("first_spin_guaranteed")) Text("أول لفة: جائزة ${info.optLong("first_reward_cents")} كوين مضمونة.")
                    if(info.optLong("price_step_cents") > 0L) Text("يزيد سعر اللفة ${info.optLong("price_step_cents")} كوين حتى ${info.optLong("maximum_cost_cents")} كوين.",style=MaterialTheme.typography.bodySmall)
                    else Text("سعر اللفة ثابت: ${info.optLong("cost_cents")} كوين.",style=MaterialTheme.typography.bodySmall)
                    if(info.optLong("grand_unlock_spend_cents") > 0L && !info.optBoolean("grand_unlocked")) Text("بعض الجوائز تُفتح بعد إنفاق ${info.optLong("grand_unlock_spend_cents")} كوين. إنفاقك: ${info.optLong("spent_cents")} كوين.",style=MaterialTheme.typography.bodySmall)
                    TextButton({ showRules = !showRules }) { Text("القواعد والاحتمالات") }
                    val prizes = info.optJSONArray("prizes")
                    if(showRules && prizes != null) for(i in 0 until prizes.length()) { val p = prizes.getJSONObject(i); Text("${if(p.optLong("amount_cents")==0L) "بدون جائزة · حاول مجددًا" else "${p.optLong("amount_cents")} كوين"} · احتمال ${p.optString("chance_percent")}%") }
                    Button(onClick = { confirmSpin = true }, enabled = !busy) { Text("لف العجلة — خصم من الرصيد") }
                } else Text("العجلة غير مفعّلة حتى تحدد الإدارة سعر اللفة والجوائز واحتمالاتها.")
            }
            if(!isWheel) {
                Text("طلباتي", style = MaterialTheme.typography.titleLarge)
                rows.forEach { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(when(r.optString("status")) { "pending" -> "قيد المراجعة"; "approved" -> "معتمد"; "rejected" -> "مرفوض"; else -> r.optString("status") })
                    if(r.has("amount_cents")) Text("${r.optLong("amount_cents")} كوين")
                    if(r.optString("review_note").isNotBlank()) Text(r.optString("review_note"))
                } } }
            }
        }
        if(confirmSpin) AlertDialog(onDismissRequest = { confirmSpin = false }, title = { Text("تأكيد اللفة المدفوعة") }, text = { Text(if(info.optBoolean("first_spin_guaranteed")) "سعر هذه اللفة ${info.optLong("cost_cents")} كوين وجائزتها المضمونة ${info.optLong("first_reward_cents")} كوين." else "سيُخصم ${info.optLong("cost_cents")} كوين من رصيدك. قد لا تحصل على جائزة؛ راجع احتمالات الجوائز قبل التأكيد.") }, confirmButton = { TextButton(onClick = { confirmSpin = false; perform {
            val result = api.request("/api/promotion/wheel", "POST", JSONObject().put("request_key", wheelKey).put("rules_version",2).put("config_version", info.optString("config_version")))
            val prizes = info.optJSONArray("prizes")
            var accumulated = 0f
            var center = 0f
            if(prizes != null) for(i in 0 until prizes.length()) {
                val prize = prizes.getJSONObject(i)
                val sweep = prize.optString("chance_percent").toFloatOrNull()?.times(3.6f) ?: 0f
                if(prize.optLong("amount_cents") == result.optLong("prize_cents")) { center = accumulated + sweep / 2f; break }
                accumulated += sweep
            }
            wheelRotation += 1800f + ((360f - center - wheelRotation % 360f + 360f) % 360f)
            wheelKey = java.util.UUID.randomUUID().toString()
            info = api.request("/api/promotion/wheel")
            note = if(result.optLong("prize_cents")==0L) "بدون جائزة · حاول مجددًا" else "الجائزة: ${result.optLong("prize_cents")} كوين · الرصيد: ${result.optLong("balance_cents")} كوين"
        } }) { Text("تأكيد الخصم واللفة") } }, dismissButton = { TextButton({ confirmSpin = false }) { Text("إلغاء") } })
    }
}

private val PublishNavy = Color(0xFF0F2342)
private val PublishSelected = Color(0xFFE8EEF5)
private val PublishLine = Color(0xFF8390A3)
private val PublishHint = Color(0xFF61718B)

/**
 * One real publishing workflow for both entry points. The two drafts and uploaded
 * media IDs stay separate when switching tabs, preventing an image from being
 * submitted as a reel (or vice versa).
 */
@Composable
internal fun SellerPublishScreen(api: LayaliApi, initialReel:Boolean=false, initialPhoto:Boolean=false, returnToRoom:(()->Unit)?=null, accountId:Int=0, onHome:()->Unit={}, onView:(Boolean,JSONObject)->Unit={_,_->}, back:()->Unit) {
    var mode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if(initialPhoto)"photo" else if(initialReel)"reel" else "service") }
    key(mode,accountId){ PublicationEditor(api,mode,accountId,returnToRoom,{mode=it},onHome,onView,back) }
}

@Composable
private fun PublicationEditor(api:LayaliApi, mode:String, accountId:Int, returnToRoom:(()->Unit)?, changeMode:(String)->Unit, onHome:()->Unit, onView:(Boolean,JSONObject)->Unit, back:()->Unit) {
    val photo=mode=="photo"
    val reel=mode=="reel"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drafts=remember(accountId){context.getSharedPreferences("publish_draft_"+accountId+"_"+mode,Context.MODE_PRIVATE)}
    var publishedJson by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var privatePost by remember { mutableStateOf(drafts.getBoolean("private",false)) }
    var allowComments by remember { mutableStateOf(drafts.getBoolean("comments",true)) }
    var coverId by remember { mutableStateOf(drafts.getString("cover","").orEmpty()) }
    var clipStart by remember { mutableStateOf(drafts.getString("start","").orEmpty()) }
    var clipEnd by remember { mutableStateOf(drafts.getString("end","").orEmpty()) }
    var captureUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var publishKey by remember { mutableStateOf(drafts.getString("publishKey",null)?:java.util.UUID.randomUUID().toString()) }
    var showPhoto by remember { mutableStateOf(false) }
    LaunchedEffect(privatePost,allowComments,coverId,clipStart,clipEnd,publishKey){
        drafts.edit().putBoolean("private",privatePost).putBoolean("comments",allowComments).putString("cover",coverId)
            .putString("start",clipStart).putString("end",clipEnd).putString("publishKey",publishKey).apply()
    }
    var previewVideo by remember { mutableStateOf(false) }
    var videoSource by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("videoSource","").orEmpty()) }
    var title by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("title","").orEmpty()) }
    var description by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("description","").orEmpty()) }
    var caption by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("caption","").orEmpty()) }
    var reelPaid by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getBoolean("reelPaid",false)) }
    var reelPrice by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("reelPrice","").orEmpty()) }
    var duration by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("duration","30").orEmpty()) }
    var terms by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("terms","").orEmpty()) }
    var price by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("price","").orEmpty()) }
    var category by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("category","خدمة").orEmpty()) }
    var offerJson by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("offerJson","{}").orEmpty()) }
    val offerMedia = remember(offerJson) { JSONObject(offerJson) }
    var offerUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("offerUri","").orEmpty()) }
    var reelJson by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("reelJson","{}").orEmpty()) }
    val reelMedia = remember(reelJson) { JSONObject(reelJson) }
    var reelUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(drafts.getString("reelUri","").orEmpty()) }

    var busy by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(-1f) }
    var note by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var noteIsError by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        note = ""
        noteIsError = false
        scope.launch {
            try { block() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { note = commerceError(e); noteIsError = true }
            finally { uploadProgress = -1f; busy = false }
        }
    }
    fun uploadDraft(uri: android.net.Uri, chosenReel: Boolean) {
        perform {
            uploadProgress = 0f
            val uploaded = api.upload(context, uri, if(chosenReel) "reel" else if(photo)"paid_photo" else "offer",
                if(chosenReel)clipStart.toIntOrNull()?:0 else 0,if(chosenReel)clipEnd.toIntOrNull()?:0 else 0) { uploadProgress = it }
            require(uploaded.optString("id").isNotBlank())
            if(chosenReel) { reelJson = uploaded.toString(); reelUri = "" }
            else { offerJson = uploaded.toString(); offerUri = "" }
            note = if(chosenReel) "تم رفع الفيديو. راجع الوصف ثم انشر." else "تم رفع الصورة."
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            if(reel) { videoSource=uri.toString(); reelUri=uri.toString();reelJson="{}";clipStart="";clipEnd="" }
            else { offerUri=uri.toString(); offerJson="{}";uploadDraft(uri,false) }
        }
    }
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)perform {coverId=api.upload(context,uri,"offer").getString("id");note="تم اختيار صورة الغلاف"}
    }
    fun captured(ok:Boolean){
        if(!ok||captureUri.isBlank())return
        if(reel){videoSource=captureUri;reelUri=captureUri;reelJson="{}";clipStart="";clipEnd=""}
        else{offerUri=captureUri;offerJson="{}";uploadDraft(android.net.Uri.parse(captureUri),false)}
    }
    val captureVideo=rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()){captured(it)}
    val capturePhoto=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){captured(it)}
    fun launchCamera(){
        runCatching {
            val directory=java.io.File(context.cacheDir,"publish-camera").apply{mkdirs()}
            val file=java.io.File.createTempFile("capture-",if(reel)".mp4" else ".jpg",directory)
            val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".publish.files",file)
            captureUri=uri.toString()
            if(reel)captureVideo.launch(uri) else capturePhoto.launch(uri)
        }.onFailure{note="تعذّر فتح الكاميرا. يمكنك اختيار ملف من المعرض.";noteIsError=true}
    }
    val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted)launchCamera() else {note="يلزم إذن الكاميرا للتصوير.";noteIsError=true}
    }
    LaunchedEffect(title,description,caption,reelPaid,reelPrice,duration,terms,price,category,offerJson,reelJson,reelUri,offerUri,videoSource){
        drafts.edit().putString("title",title).putString("description",description).putString("caption",caption).putString("reelPrice",reelPrice).putString("duration",duration).putString("terms",terms).putString("price",price).putString("category",category).putString("offerJson",offerJson).putString("reelJson",reelJson).putString("reelUri",reelUri).putString("offerUri",offerUri).putString("videoSource",videoSource).putBoolean("reelPaid",reelPaid).apply()
    }
    if(previewVideo && videoSource.isNotBlank()){
        AlertDialog(onDismissRequest={previewVideo=false}, title={Text("معاينة الريلز")},text={
            androidx.compose.ui.viewinterop.AndroidView(factory={ctx->android.widget.VideoView(ctx).apply{
                val controls=android.widget.MediaController(ctx);controls.setAnchorView(this);setMediaController(controls)
                setVideoURI(android.net.Uri.parse(videoSource));setOnPreparedListener{start()}
            }},modifier=Modifier.fillMaxWidth().height(260.dp))
        },confirmButton={TextButton({previewVideo=false}){Text("إغلاق")}})
    }
    if(publishedJson.isNotEmpty()){
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
            Icon(Icons.Rounded.CheckCircle,null,Modifier.size(64.dp),tint=PublishNavy)
            Text("تم النشر بنجاح!",style=MaterialTheme.typography.headlineMedium)
            Text(if(privatePost)"المحتوى ظاهر لك وحدك." else "أصبح محتواك متاحًا للمشاهدة.")
            Spacer(Modifier.height(16.dp))
            if(showPhoto && photo) ReferenceImage(JSONObject(publishedJson).optJSONObject("item")?.optString("media_url").orEmpty(),Modifier.fillMaxWidth().height(220.dp))
            Button({if(photo)showPhoto=true else onView(reel,JSONObject(publishedJson))},modifier=Modifier.fillMaxWidth()){Text("مشاهدة المنشور")}
            OutlinedButton(onHome,modifier=Modifier.fillMaxWidth()){Text("العودة إلى الرئيسية")}
            TextButton({publishedJson="";note=""}){Text("إنشاء منشور آخر")}
        }
        return
    }
    val pendingUri = if(reel) reelUri else offerUri
    val selectedMedia = if(reel) reelMedia else offerMedia
    val validMoney = runCatching {
        (price.trim().toIntOrNull() ?: 0) > 0
    }.getOrDefault(false)
    val validOffer = title.trim().length >= 3 && description.trim().length >= 10 &&
        duration.toIntOrNull() in 1..1440 && validMoney
    val validReelPrice = !reelPaid || reelPrice.toIntOrNull() in 1..1000000
    val publishEnabled = !busy && selectedMedia.has("id") &&
        (if(reel||photo) validReelPrice else validOffer)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                    .padding(horizontal = 18.dp)
            ) {
                if(returnToRoom!=null) TextButton(returnToRoom,modifier=Modifier.height(36.dp)){Text("↩ العودة إلى الغرفة الصوتية",fontSize=12.sp)}
                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "إنشاء محتوى",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = back, enabled = !busy,
                        modifier = Modifier.align(Alignment.CenterStart).size(48.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "رجوع", tint = PublishNavy)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PublishSegment("خدمة أو منتج", !reel&&!photo, !busy, Modifier.weight(1f)) {
                        changeMode("service")
                    }
                    PublishSegment("ريلز", reel, !busy, Modifier.weight(1f)) {
                        changeMode("reel")
                    }
                    PublishSegment("صورة", photo, !busy, Modifier.weight(1f)){changeMode("photo")}
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    if(note.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if(noteIsError) MaterialTheme.colorScheme.errorContainer
                                else PublishSelected
                        ) {
                            Text(
                                note, Modifier.fillMaxWidth().padding(12.dp),
                                color = if(noteIsError) MaterialTheme.colorScheme.onErrorContainer
                                    else PublishNavy, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if(reel||photo) {
                        Text(
                            "النشر متاح فوراً لجميع الحسابات النشطة · محتوى 18+ غير صريح",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        PublishMediaDropzone(
                            selected = selectedMedia.has("id"), isReel = reel, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(112.dp)
                        ) { picker.launch(arrayOf(if(reel)"video/mp4" else "image/*")) }
                        if(photo && offerMedia.has("id")){
                            ReferenceImage(offerMedia.optString("url"),Modifier.fillMaxWidth().height(114.dp))
                            TextButton({offerJson="{}";offerUri=""},enabled=!busy){Text("إزالة الصورة")}
                        }
                        if(reel && videoSource.isNotBlank()){
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                PublishInput(clipStart,{clipStart=it.filter(Char::isDigit).take(3);reelJson="{}";reelUri=videoSource},"البداية بالثواني",numeric=true,modifier=Modifier.weight(1f),enabled=!busy)
                                PublishInput(clipEnd,{clipEnd=it.filter(Char::isDigit).take(3);reelJson="{}";reelUri=videoSource},"النهاية بالثواني",numeric=true,modifier=Modifier.weight(1f),enabled=!busy)
                            }
                            Row {
                                TextButton({previewVideo=true},enabled=!busy){Text("معاينة الفيديو")}
                                TextButton({videoSource="";reelJson="{}";reelUri="";note=""},enabled=!busy){Text("إزالة الفيديو")}
                            }
                        }
                        Text("${caption.length} / 500",style=MaterialTheme.typography.labelSmall)
                        PublishInput(
                            value = caption, onChange = { caption = it.take(500) },
                            label = "وصف المحتوى",
                            hint = "اكتب عنوان الريلز أو وصفاً قصيراً...",
                            modifier = Modifier.fillMaxWidth(), lines = 2, enabled = !busy
                        )
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Switch(checked=reelPaid,onCheckedChange={reelPaid=it},enabled=!busy)
                            Text("محتوى مدفوع · معاينة مموّهة قبل الشراء",
                                modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        }
                        if(reelPaid)PublishInput(
                            value=reelPrice,
                            onChange={reelPrice=it.filter(Char::isDigit).take(7)},
                            label="سعر الفتح بالكوينز",numeric=true,
                            modifier=Modifier.fillMaxWidth(),enabled=!busy
                        )
                    } else {
                        PublishInput(
                            value = title, onChange = { title = it.take(150) },
                            label = "عنوان العرض",
                            modifier = Modifier.fillMaxWidth(), enabled = !busy
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PublishInput(
                                value = duration,
                                onChange = { duration = it.filter(Char::isDigit).take(4) },
                                label = "المدة (دقيقة)", numeric = true,
                                modifier = Modifier.weight(1f), enabled = !busy
                            )
                            PublishInput(
                                value = price,
                                onChange = { price = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                                label = "السعر بالكوينز", numeric = true,
                                modifier = Modifier.weight(1f), enabled = !busy
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("خدمة", "جلسة", "منتج").forEach { kind ->
                                PublishSegment(
                                    kind, category == kind, !busy, Modifier.weight(1f)
                                ) { category = kind }
                            }
                        }
                        PublishInput(
                            value = description, onChange = { description = it.take(600) },
                            label = "وصف العرض", lines = 3,
                            modifier = Modifier.fillMaxWidth(), enabled = !busy
                        )
                        PublishInput(
                            value = terms, onChange = { terms = it.take(600) },
                            label = "شروط الخدمة والإلغاء", lines = 2,
                            modifier = Modifier.fillMaxWidth(), enabled = !busy
                        )
                        PublishMediaDropzone(
                            selected = offerMedia.has("id"), isReel = false, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(112.dp)
                        ) { picker.launch(arrayOf("image/*")) }
                        if(offerMedia.has("id")) {
                            val imageUrl = offerMedia.optString("url")
                            if(imageUrl.isNotBlank())
                                ReferenceImage(imageUrl, Modifier.fillMaxWidth().height(150.dp))
                        }
                    }
                    OutlinedButton(onClick={cameraPermission.launch(android.Manifest.permission.CAMERA)},enabled=!busy){Text("التصوير بالكاميرا")}
                    if(reel) TextButton({coverPicker.launch(arrayOf("image/*"))},enabled=!busy){Text(if(coverId.isBlank())"اختيار صورة الغلاف" else "استبدال صورة الغلاف")}
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("خصوصية النشر",Modifier.weight(1f))
                        FilterChip(!privatePost,{privatePost=false},enabled=!busy,label={Text("الجميع")})
                        FilterChip(privatePost,{privatePost=true},enabled=!busy,label={Text("أنا فقط")})
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){Text("السماح بالتعليقات",Modifier.weight(1f));Switch(allowComments,{allowComments=it},enabled=!busy)}
                    if(pendingUri.isNotBlank() && !busy) {
                        OutlinedButton({ uploadDraft(android.net.Uri.parse(pendingUri),reel) }) { Text(if(noteIsError)"إعادة محاولة الرفع" else "رفع الملف المختار") }
                    }
                    Spacer(Modifier.height(6.dp))

                Column(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if(busy) {
                        if(uploadProgress >= 0f) {
                            Text("رفع الملف · ${(uploadProgress*100).toInt()}٪")
                            LinearProgressIndicator(
                                progress = { uploadProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Button(
                        onClick = {
                            perform {
                                val response = if(reel||photo) {
                                    api.request(
                                        if(reel)"/api/reels" else "/api/paid-posts", "POST",
                                        JSONObject().put("media_id", selectedMedia.getString("id")).put("owner_only",privatePost).put("comments_allowed",allowComments).put("cover_id",coverId)
                                            .put("caption", caption.trim())
                                            .put("price_cents", if(reelPaid)reelPrice.toInt()else 0)
                                    )
                                } else {
                                    api.request(
                                        "/api/services", "POST",
                                        JSONObject().put("publish_key",publishKey).put("owner_only",privatePost).put("comments_allowed",allowComments).put("title", title.trim())
                                            .put("description", description.trim())
                                            .put("price_cents", price.trim().toInt())
                                            .put("category", category)
                                            .put("image_id", offerMedia.getString("id"))
                                            .put("duration_minutes", duration.toInt())
                                            .put("terms", terms.trim())
                                    )
                                }
                                if(response.optString("status") == "published") {
                                    publishedJson=response.toString();showPhoto=false;publishKey=java.util.UUID.randomUUID().toString();coverId="";clipStart="";clipEnd=""
                                    if(photo){caption="";reelPaid=false;reelPrice=""}
                                    if(reel) { videoSource="";reelJson = "{}"; caption = "";reelPaid=false;reelPrice="" }
                                    else {
                                        offerJson = "{}"; title = ""; description = ""
                                        price = ""; duration = "30"; terms = ""
                                    }
                                    note = ""
                                } else {
                                    noteIsError = true
                                    note = "تم إرسال الطلب، لكن النشر لم يتأكد بعد. تحقق من تحديث الخادم."
                                }
                            }
                        },
                        enabled = publishEnabled,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PublishNavy, contentColor = Color.White,
                            disabledContainerColor = PublishNavy.copy(alpha = .3f)
                        ),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(
                            if(uploadProgress>=0f) "جارٍ رفع الملف…" else if(busy) "جارٍ النشر…" else if(noteIsError && publishEnabled) "إعادة محاولة النشر" else "نشر الآن",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun PublishSegment(
    title: String, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(49.dp),
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if(selected) PublishSelected else PublishLine
        ),
        color = if(selected) Color(0xFF214D9A) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if(selected) Color.White else MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            Icon(if(title=="ريلز") Icons.Rounded.SmartDisplay else if(title=="صورة") Icons.Rounded.PhotoCamera else Icons.Rounded.Storefront,null,Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                title, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PublishInput(
    value: String, onChange: (String) -> Unit,
    label: String, modifier: Modifier,
    hint: String = "", lines: Int = 1, numeric: Boolean = false, enabled: Boolean = true
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1) },
        placeholder = { if(hint.isNotBlank()) Text(hint) },
        shape = RoundedCornerShape(18.dp),
        minLines = lines, maxLines = if(lines == 1) 1 else 5,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = if(numeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = PublishLine.copy(alpha = .55f),
            focusedBorderColor = PublishNavy
        )
    )
}

@Composable
private fun PublishMediaDropzone(
    selected: Boolean, isReel: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onPick: () -> Unit
) {
    val radius = 22.dp
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    color = PublishLine,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10.dp.toPx(), 8.dp.toPx())
                        )
                    )
                )
            }
            .clickable(enabled = enabled, onClick = onPick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PublishSelected,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if(isReel) Icons.Rounded.SmartDisplay else Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp), tint = PublishNavy
                    )
                }
            }
            Text(
                when {
                    selected && isReel -> "تم رفع الفيديو · اضغط للاستبدال"
                    selected -> "تم رفع الصورة · اضغط للاستبدال"
                    isReel -> "اختيار فيديو MP4 من الهاتف · حتى 96MB"
                    else -> "رفع صورة من الهاتف · حتى 8MB"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = PublishNavy, fontWeight = FontWeight.Medium
            )
            if(selected) Icon(
                Icons.Rounded.CheckCircle, "الملف مرفوع",
                tint = Color(0xFF2E795F), modifier = Modifier.size(18.dp)
            )
        }
    }
}
@Composable
internal fun SupportFloatingButton() {
    var open by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    FloatingActionButton(onClick = { open = true }) { Text("الدعم", Modifier.padding(horizontal = 12.dp)) }
    if(open) AlertDialog(onDismissRequest = { open = false }, title = { Text("تواصل مع الدعم") }, text = { Text("اختر وسيلة التواصل. ستفتح المحادثة لتكتب رسالتك وترسلها بنفسك.") }, confirmButton = {
        TextButton(onClick = { open = false; runCatching { uri.openUri("https://wa.me/201145781363") } }) { Text("واتساب") }
    }, dismissButton = { TextButton(onClick = { open = false; runCatching { uri.openUri("https://t.me/RamaNUR") } }) { Text("تلجرام") } })
}


@Composable
private fun RewardWheel(prizes: org.json.JSONArray?, rotation: Float) {
    val animated by androidx.compose.animation.core.animateFloatAsState(rotation, androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "wheel")
    val palette = listOf(androidx.compose.ui.graphics.Color(0xFF792D45), androidx.compose.ui.graphics.Color(0xFFA04B63), androidx.compose.ui.graphics.Color(0xFFF0E0E3), androidx.compose.ui.graphics.Color(0xFF4C2037))
    Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        androidx.compose.foundation.Canvas(Modifier.size(280.dp)) {
            val ring = 9.dp.toPx()
            drawCircle(androidx.compose.ui.graphics.Color(0xFFD7AD72))
            val count = prizes?.length()?.takeIf { it > 0 } ?: 6
            var start = -90f + animated
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 14.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
            for(i in 0 until count) {
                val sweep = if(prizes != null && prizes.length() > 0) (prizes.getJSONObject(i).optString("chance_percent").toFloatOrNull() ?: 0f) * 3.6f else 360f / count
                drawArc(palette[i % palette.size], start, sweep, true, topLeft = androidx.compose.ui.geometry.Offset(ring,ring), size = androidx.compose.ui.geometry.Size(size.width - ring*2, size.height - ring*2))
                if(sweep >= 15f) {
                    val angle = Math.toRadians((start + sweep / 2).toDouble())
                    val text = if(prizes != null && prizes.length() > 0) { val amount=prizes.getJSONObject(i).optLong("amount_cents");if(amount==0L)"بدون جائزة"else amount.toString() } else "✦"
                    drawContext.canvas.nativeCanvas.drawText(text, center.x + kotlin.math.cos(angle).toFloat() * size.width * .32f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .32f + 5.dp.toPx(), paint)
                }
                start += sweep
            }
            drawCircle(androidx.compose.ui.graphics.Color(0xFFF5E9D8), radius = size.width * .10f)
        }
        Text("▼", color = androidx.compose.ui.graphics.Color(0xFFD7AD72), style = MaterialTheme.typography.headlineMedium)
    }
}
