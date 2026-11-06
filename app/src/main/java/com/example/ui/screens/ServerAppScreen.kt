package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import com.example.ui.auth.AdultWelcomeScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@Composable
fun ServerAppScreen(baseUrl: String) {
    val api = remember(baseUrl) { LayaliApi(baseUrl) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var policyAccepted by rememberSaveable { mutableStateOf(false) }
    var signedIn by remember { mutableStateOf(false) }
    var register by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf("الخدمات") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var wallet by remember { mutableStateOf(JSONObject()) }
    var profile by remember { mutableStateOf(JSONObject()) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var birth by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var peer by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var withdrawalKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var reportType by rememberSaveable { mutableStateOf("service") }
    var reportId by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }

    suspend fun load() {
        val path = when (tab) {
            "الخدمات" -> "/api/services"
            "الحجوزات" -> "/api/bookings"
            "الريلز" -> "/api/reels"
            "المحفظة" -> "/api/wallet"
            "حسابي" -> "/api/me"
            "الرسائل" -> if (peer.toIntOrNull() != null) "/api/messages/$peer" else return
            else -> return
        }
        val response = api.request(path)
        if (tab == "المحفظة") wallet = response
        else if (tab == "حسابي") profile = response
        else {
            val array = response.getJSONArray("items")
            items = (0 until array.length()).map { array.getJSONObject(it) }
        }
    }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        notice = ""
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice = friendlyError(e.message) }
            finally {
                if (api.token == null) signedIn = false
                busy = false
            }
        }
    }
    LaunchedEffect(tab, signedIn) {
        items = emptyList()
        if (signedIn) perform { load() }
    }
    if (!policyAccepted) {
        AdultWelcomeScreen { policyAccepted = true }
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Layali", style = MaterialTheme.typography.headlineLarge)
                Text("للبالغين 18+ · محتوى غير صريح", style = MaterialTheme.typography.bodyMedium)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodyLarge)
                if (!signedIn) {
                    Text(if (register) "إنشاء حساب" else "تسجيل الدخول", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    if (register) {
                        OutlinedTextField(email, { email = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(birth, { birth = it }, label = { Text("تاريخ الميلاد YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                        Text("بإنشاء الحساب تؤكد صحة تاريخ الميلاد وموافقتك على ضوابط المحتوى التي وافقت عليها في شاشة البداية.")
                    }
                    Button(enabled = !busy, onClick = { perform {
                        val payload = JSONObject().put("username", username).put("password", password)
                        if (register) payload.put("email", email).put("date_of_birth", birth).put("accept_non_explicit_policy", true)
                        api.request(if (register) "/api/register" else "/api/login", "POST", payload)
                        password = ""
                        signedIn = true
                    } }) { Text(if (register) "إنشاء الحساب والموافقة" else "دخول") }
                    TextButton(enabled = !busy, onClick = { register = !register }) { Text(if (register) "لدي حساب" else "إنشاء حساب جديد") }
                } else {
                    listOf(listOf("الخدمات", "الحجوزات", "الريلز"), listOf("الرسائل", "المحفظة", "حسابي"), listOf("بلاغ", "نشر خدمة")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { name -> FilterChip(selected = tab == name, enabled = !busy, onClick = { tab = name }, label = { Text(name) }) }
                        }
                    }
                    Text(tab, style = MaterialTheme.typography.titleLarge)
                    if (tab in listOf("الخدمات", "الحجوزات", "الريلز")) {
                        TextButton(enabled = !busy, onClick = { perform { load() } }) { Text("تحديث") }
                        if (items.isEmpty() && !busy) Text("لا توجد عناصر متاحة حاليًا.")
                        items.forEach { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(item.optString("title", "#${item.optString("id")}"), style = MaterialTheme.typography.titleMedium)
                                    if (tab == "الخدمات") {
                                        Text(item.optString("description"))
                                        Text("${item.optInt("price_cents") / 100.0} USD · المذيعة #${item.optInt("owner_id")}")
                                        Button(enabled = !busy, onClick = { perform {
                                            api.request("/api/bookings", "POST", JSONObject().put("service_id", item.getInt("id")))
                                            tab = "الحجوزات"
                                            notice = "تم تسجيل الطلب بانتظار الدفع. لم تُخصم أموال."
                                        } }) { Text("طلب حجز") }
                                    } else if (tab == "الحجوزات") {
                                        Text("${item.optInt("price_cents") / 100.0} USD")
                                        Text(if (item.optString("status") == "awaiting_payment") "بانتظار الدفع" else item.optString("status"))
                                    } else {
                                        Button(onClick = {
                                            val uri = Uri.parse(item.optString("video_url"))
                                            if (uri.scheme == "https") context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }) { Text("مشاهدة الريل") }
                                    }
                                }
                            }
                        }
                    }
                    if (tab == "الرسائل") {
                        OutlinedTextField(peer, { peer = it.filter(Char::isDigit) }, label = { Text("رقم حساب المستلم") })
                        TextButton(enabled = !busy, onClick = { perform { load() } }) { Text("فتح المحادثة / تحديث") }
                        items.asReversed().forEach { Text("#${it.optInt("sender_id")}: ${it.optString("body")}") }
                        OutlinedTextField(message, { message = it }, label = { Text("رسالتك") })
                        Button(enabled = !busy && peer.toIntOrNull() != null && message.isNotBlank(), onClick = { perform {
                            api.request("/api/messages/$peer", "POST", JSONObject().put("body", message))
                            message = ""
                            load()
                        } }) { Text("إرسال") }
                    }
                    if (tab == "نشر خدمة") {
                        Text("يتطلب حساب مذيعة موثّقًا. تظهر الخدمة بعد مراجعة الإدارة.")
                        OutlinedTextField(title, { title = it }, label = { Text("العنوان") })
                        OutlinedTextField(description, { description = it }, label = { Text("الوصف") })
                        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("السعر بالسنت الأمريكي، 200 = دولاران") })
                        Button(enabled = !busy && price.toIntOrNull() != null, onClick = { perform {
                            api.request("/api/services", "POST", JSONObject().put("title", title).put("description", description).put("price_cents", price.toInt()))
                            title = ""; description = ""; price = ""
                            notice = "أُرسلت الخدمة للمراجعة."
                        } }) { Text("إرسال للمراجعة") }
                    }
                    if (tab == "المحفظة") {
                        Text("الرصيد المتاح: ${wallet.optLong("available_cents") / 100.0} USD")
                        Text("المبلغ المحجوز: ${wallet.optLong("reserved_cents") / 100.0} USD")
                        Text("الدفع والتحويل الفعلي غير متاحين بعد. طلب السحب يحجز مبلغًا من رصيد حقيقي متاح فقط.")
                        OutlinedTextField(price, { price = it.filter(Char::isDigit); withdrawalKey = UUID.randomUUID().toString() }, label = { Text("مبلغ السحب بالسنت") })
                        OutlinedTextField(destination, { destination = it; withdrawalKey = UUID.randomUUID().toString() }, label = { Text("جهة الاستلام") })
                        Button(enabled = !busy && price.toIntOrNull() != null, onClick = { perform {
                            api.request("/api/withdrawals", "POST", JSONObject().put("amount_cents", price.toInt()).put("destination", destination).put("request_key", withdrawalKey))
                            withdrawalKey = UUID.randomUUID().toString()
                            price = ""; destination = ""
                            load(); notice = "سُجّل طلب السحب. لم يتم تحويل الأموال بعد."
                        } }) { Text("طلب سحب") }
                        TextButton(enabled = !busy, onClick = { perform { load() } }) { Text("تحديث الرصيد") }
                    }
                    if (tab == "حسابي") {
                        Text("${profile.optString("username")} · رقم الحساب ${profile.optInt("id")}")
                        Text(if (profile.optBoolean("identity_verified")) "الهوية موثقة" else "الهوية غير موثقة")
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl.trimEnd('/') + "/admin/"))) }) { Text("لوحة المسؤول") }
                    }
                    if (tab == "بلاغ") {
                        Text("النوع: service للخدمة، reel للريل، user للحساب، message للرسالة")
                        OutlinedTextField(reportType, { reportType = it }, label = { Text("نوع البلاغ") })
                        OutlinedTextField(reportId, { reportId = it.filter(Char::isDigit) }, label = { Text("رقم العنصر") })
                        OutlinedTextField(reason, { reason = it }, label = { Text("سبب البلاغ") })
                        Button(enabled = !busy && reportId.toIntOrNull() != null, onClick = { perform {
                            api.request("/api/reports", "POST", JSONObject().put("target_type", reportType).put("target_id", reportId.toInt()).put("reason", reason))
                            reason = ""; notice = "وصل البلاغ إلى الإدارة."
                        } }) { Text("إرسال البلاغ") }
                    }
                    TextButton(enabled = !busy, onClick = { perform {
                        api.request("/api/logout", "POST", JSONObject())
                        signedIn = false; items = emptyList(); wallet = JSONObject(); profile = JSONObject()
                    } }) { Text("تسجيل الخروج") }
                }
            }
        }
    }
}

private fun friendlyError(code: String?): String = when (code) {
    "authentication_required", "invalid_session", "invalid_credentials" -> "تعذّر تسجيل الدخول. تحقق من بياناتك أو سجّل الدخول مجددًا."
    "adult_policy_required" -> "يجب أن يكون عمرك 18 عامًا أو أكثر وأن توافق على ضوابط المحتوى."
    "verified_creator_required" -> "هذه الخدمة متاحة للمذيعات بعد توثيق الهوية واعتماد الإدارة."
    "identity_verification_required" -> "مشاهدة الريلز تتطلب توثيق الهوية لدى الإدارة."
    "paid_chat_requires_verified_payment" -> "هذه المحادثة مدفوعة. لم يُفعّل التحقق من الدفع بعد، ولم تُرسل الرسالة."
    "insufficient_funds" -> "رصيدك المتاح لا يكفي لطلب السحب."
    "too_many_attempts" -> "محاولات كثيرة. حاول بعد 15 دقيقة."
    "username_unavailable", "conflict" -> "اسم المستخدم أو الطلب مستخدم بالفعل."
    "invalid_input", "invalid_password", "invalid_email", "invalid_username" -> "تحقق من الحقول. كلمة المرور الجديدة يجب ألا تقل عن 12 حرفًا وتكون غير شائعة."
    else -> "تعذّر إتمام الطلب. تحقق من الاتصال والبيانات ثم حاول مجددًا."
}
