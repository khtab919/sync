package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
    "invoice_creation_pending" -> "تعذّر تأكيد إنشاء الفاتورة. راجع الدعم قبل تكرار الدفع."
    "quote_expired" -> "انتهت مهلة هذا الطلب. أنشئ طلب شراء جديدًا."
    "provider_check_pending" -> "التحقق من البوابة لم يكتمل بعد. حاول لاحقًا."
    "update_app_for_wheel_rules" -> "حدّث التطبيق لعرض قواعد العجلة الجديدة قبل الدفع."
    "shamcash_not_configured" -> "بيانات استلام شام كاش غير مفعلة بعد."
    "invalid_transaction_reference" -> "أدخل رقم إشعار الدفع الصحيح."
    "wheel_configuration_changed" -> "تغيّر سعر اللفة أو قواعدها. حدّث الصفحة قبل الدفع."
    "wheel_unavailable" -> "العجلة غير مفعّلة حاليًا."
    else -> "تعذّر تنفيذ الطلب. تحقق من البيانات والاتصال وتحديث الخادم."
}

@Composable
internal fun CommerceScreen(api: LayaliApi, kind: String, back: () -> Unit) {
    if(kind == "شحن الرصيد") { CoinShopScreen(api, back); return }
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
        try { info = api.request(if(kind == "العجلة") "/api/promotion/wheel" else "/api/payment-info"); if(kind != "العجلة") refresh() }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { note = commerceError(e) }
        finally { busy = false }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(back, enabled = !busy) { Text("رجوع") }
            Text(kind, style = MaterialTheme.typography.headlineLarge)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
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
                    Text("سعر اللفة: ${BigDecimal.valueOf(info.optLong("cost_cents"), 2)} USD")
                    if(info.optBoolean("first_spin_guaranteed")) Text("أول لفة: جائزة ${BigDecimal.valueOf(info.optLong("first_reward_cents"),2)} دولار مضمونة.")
                    Text("يزيد سعر اللفة ${BigDecimal.valueOf(info.optLong("price_step_cents"),2)} دولار، حتى ${BigDecimal.valueOf(info.optLong("maximum_cost_cents"),2)} دولار.",style=MaterialTheme.typography.bodySmall)
                    if(!info.optBoolean("grand_unlocked")) Text("جائزة 100 دولار تُفتح بعد إنفاق ${BigDecimal.valueOf(info.optLong("grand_unlock_spend_cents"),2)} دولار تراكميًا. إنفاقك: ${BigDecimal.valueOf(info.optLong("spent_cents"),2)} دولار.",style=MaterialTheme.typography.bodySmall)
                    TextButton({ showRules = !showRules }) { Text("القواعد والاحتمالات") }
                    val prizes = info.optJSONArray("prizes")
                    if(showRules && prizes != null) for(i in 0 until prizes.length()) { val p = prizes.getJSONObject(i); Text("${if(p.optLong("amount_cents")==0L) "حظ أوفر · جرّب مرة أخرى" else BigDecimal.valueOf(p.optLong("amount_cents"),2).toPlainString()+" دولار"} · احتمال ${p.optString("chance_percent")}%") }
                    Button(onClick = { confirmSpin = true }, enabled = !busy) { Text("لف العجلة — خصم من الرصيد") }
                } else Text("العجلة غير مفعّلة حتى تحدد الإدارة سعر اللفة والجوائز واحتمالاتها.")
            }
            if(kind != "العجلة") {
                Text("طلباتي", style = MaterialTheme.typography.titleLarge)
                rows.forEach { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(when(r.optString("status")) { "pending" -> "قيد المراجعة"; "approved" -> "معتمد"; "rejected" -> "مرفوض"; else -> r.optString("status") })
                    if(r.has("amount_cents")) Text("${BigDecimal.valueOf(r.optLong("amount_cents"), 2)} USDT")
                    if(r.optString("review_note").isNotBlank()) Text(r.optString("review_note"))
                } } }
            }
        }
        if(confirmSpin) AlertDialog(onDismissRequest = { confirmSpin = false }, title = { Text("تأكيد اللفة المدفوعة") }, text = { Text(if(info.optBoolean("first_spin_guaranteed")) "سعر هذه اللفة ${BigDecimal.valueOf(info.optLong("cost_cents"),2)} دولار وجائزتها المضمونة ${BigDecimal.valueOf(info.optLong("first_reward_cents"),2)} دولار." else "سيُخصم ${BigDecimal.valueOf(info.optLong("cost_cents"),2)} دولار. قد تكون الجائزة صفرًا. قواعد فتح الجائزة الكبرى والأسعار المتزايدة معروضة قبل الدفع.") }, confirmButton = { TextButton(onClick = { confirmSpin = false; perform {
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
            note = if(result.optLong("prize_cents")==0L) "حظ أوفر · جرّب مرة أخرى" else "الجائزة: ${BigDecimal.valueOf(result.optLong("prize_cents"),2)} USD · الرصيد: ${BigDecimal.valueOf(result.optLong("balance_cents"),2)} USD"
        } }) { Text("تأكيد الخصم واللفة") } }, dismissButton = { TextButton({ confirmSpin = false }) { Text("إلغاء") } })
    }
}

@Composable
internal fun SellerPublishScreen(api: LayaliApi, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var terms by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("خدمة") }
    var media by remember { mutableStateOf(JSONObject()) }
    var reel by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    fun perform(block: suspend () -> Unit) { if(busy) return; busy = true; scope.launch { try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { note = commerceError(e) } finally { busy = false } } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if(uri != null) perform { media = api.upload(context, uri, if(reel) "reel" else "offer"); note = "تم رفع الملف." } }
    Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(back, enabled = !busy) { Text("رجوع") }
        Text("مساحة النشر", style = MaterialTheme.typography.headlineLarge)
        Text("النشر متاح فوراً لجميع الحسابات النشطة · محتوى 18+ غير صريح")
        Row { FilterChip(!reel, { reel = false; media = JSONObject() }, label = { Text("خدمة أو منتج") }, enabled = !busy); Spacer(Modifier.width(8.dp)); FilterChip(reel, { reel = true; media = JSONObject() }, label = { Text("ريلز") }, enabled = !busy) }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(note.isNotBlank()) Text(note)
        if(!reel) {
            OutlinedTextField(title, { title = it.take(150) }, label = { Text("عنوان العرض") }, modifier = Modifier.fillMaxWidth())
            if(!reel){OutlinedTextField(duration,{duration=it.filter{c->c.isDigit()}.take(4)},label={Text("مدة الخدمة بالدقائق")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(terms,{terms=it.take(600)},label={Text("شروط الخدمة والإلغاء")},modifier=Modifier.fillMaxWidth())}
            OutlinedTextField(description, { description = it.take(600) }, label = { Text("وصف العرض") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(price, { price = it }, label = { Text("السعر بالدولار") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Row { listOf("خدمة", "جلسة", "منتج").forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c) }) } }
        }
        OutlinedButton(onClick = { picker.launch(if(reel) "video/mp4" else "image/*") }, enabled = !busy) { Text(if(reel) "اختيار فيديو MP4 من الهاتف · حتى 20MB" else "رفع صورة من الهاتف · حتى 8MB") }
        if(media.has("id") && !reel) ReferenceImage(media.optString("url"), Modifier.fillMaxWidth().height(200.dp))
        Button(onClick = { perform {
            val result = if(reel) api.request("/api/reels", "POST", JSONObject().put("media_id", media.getString("id")))
            else api.request("/api/services", "POST", JSONObject().put("title", title).put("description", description).put("price_cents", BigDecimal(price).movePointRight(2).intValueExact()).put("category", category).put("image_id", media.getString("id")).put("duration_minutes",duration.toInt()).put("terms",terms))
            if(result.optString("status") == "published") {
                media = JSONObject(); title = ""; description = ""; price = ""
                note = "تم نشر المحتوى بنجاح."
            } else {
                note = "تم حفظ الطلب، لكن ظهوره لم يتأكد بعد. تحقق من تحديث الخادم."
            }
        } }, enabled = !busy && media.has("id") && (reel || (title.length >= 3 && description.length >= 10 && price.isNotBlank()))) { Text("نشر الآن") }
    } }
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
    val palette = listOf(androidx.compose.ui.graphics.Color(0xFF277C9D), androidx.compose.ui.graphics.Color(0xFF3187A8), androidx.compose.ui.graphics.Color(0xFFDCECF2), androidx.compose.ui.graphics.Color(0xFF153E53))
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
                    val text = if(prizes != null && prizes.length() > 0) { val amount=prizes.getJSONObject(i).optLong("amount_cents");if(amount==0L)"حظ أوفر"else BigDecimal.valueOf(amount,2).toPlainString() } else "✦"
                    drawContext.canvas.nativeCanvas.drawText(text, center.x + kotlin.math.cos(angle).toFloat() * size.width * .32f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .32f + 5.dp.toPx(), paint)
                }
                start += sweep
            }
            drawCircle(androidx.compose.ui.graphics.Color(0xFFF5E9D8), radius = size.width * .10f)
        }
        Text("▼", color = androidx.compose.ui.graphics.Color(0xFFD7AD72), style = MaterialTheme.typography.headlineMedium)
    }
}
