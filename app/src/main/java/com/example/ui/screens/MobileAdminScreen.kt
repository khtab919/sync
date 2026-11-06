package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.net.URLEncoder
import java.util.UUID

private fun JSONObject.adminItems(): List<JSONObject> {
    val a = optJSONArray("items") ?: return emptyList()
    return (0 until a.length()).map { a.getJSONObject(it) }
}
private fun cents(value: Long) = BigDecimal.valueOf(value, 2).toPlainString() + " USD"

@Composable
internal fun MobileAdminScreen(api: LayaliApi, adminName: String, logout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var section by remember { mutableStateOf("الحسابات") }
    var query by remember { mutableStateOf("") }
    var offset by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var accounts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var content by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var creditKey by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var confirmation by remember { mutableStateOf<JSONObject?>(null) }
    fun message(e: Exception): String = when(e.message) {
        "administrator_required", "invalid_session", "authentication_required" -> "لا تملك الجلسة صلاحية الإدارة. سجّل الدخول بحساب المسؤول."
        "protected_account" -> "هذا الحساب الإداري محمي من هذا الإجراء."
        "credit_request_conflict" -> "هذا الطلب استُخدم سابقًا ببيانات مختلفة. أعد فتح الحساب لبدء طلب جديد."
        "account_unavailable" -> "الحساب غير موجود."
        else -> "تعذّر تنفيذ الطلب. تحقق من الاتصال وتحديث الخادم ثم أعد المحاولة."
    }
    fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = ""; success = ""
        scope.launch {
            try { block() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error = message(e) }
            finally { busy = false }
        }
    }
    fun viewAccount(id: Int) { perform {
        selected = api.request("/api/admin/accounts/$id")
        amount = ""; reason = ""; creditKey = UUID.randomUUID().toString()
    } }
    LaunchedEffect(section, offset, revision) {
        loading = true; error = ""
        try {
            if (api.request("/health/").optInt("mobile_admin_version") < 1) {
                error = "يلزم تحديث الخادم لتفعيل الإدارة داخل التطبيق."
                accounts = emptyList(); content = emptyList()
            } else if (section == "الحسابات") {
                val result = api.request("/api/admin/accounts?q=${URLEncoder.encode(query.trim(), "UTF-8")}&offset=$offset")
                accounts = result.adminItems(); total = result.optInt("total")
            } else content = api.request("/api/admin/moderation").adminItems()
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { accounts = emptyList(); content = emptyList(); error = message(e) }
        finally { loading = false }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("إدارة Relaxia", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("مرحبًا، $adminName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = logout, enabled = !busy) { Text("تسجيل الخروج") }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("الحسابات", "مراجعة المحتوى").forEach { name ->
                        FilterChip(selected = section == name, onClick = { selected = null; section = name; offset = 0 }, label = { Text(name) }, enabled = !busy)
                    }
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (success.isNotBlank()) Text(success, color = MaterialTheme.colorScheme.primary)
                if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = { revision++ }, enabled = !busy && !loading) { Text("تحديث") }
            }
            val account = selected
            if (account != null && section == "الحسابات") {
                item {
                    TextButton(onClick = { selected = null; revision++ }, enabled = !busy) { Text("العودة إلى الحسابات") }
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RelaxiaAvatar(account.optString("display_name"), account.optInt("selected_frame"), 82, account.optString("avatar_url"))
                        Text(account.optString("display_name"), style = MaterialTheme.typography.titleLarge)
                        Text("#${account.optInt("id")} · ${account.optString("username")}")
                        Text(account.optString("email"))
                        Text(account.optString("bio"))
                        Text(if (account.optBoolean("active")) "الحساب نشط" else "الحساب محظور")
                        Text(if (account.optBoolean("creator")) "النشر والظهور في الاستكشاف مفعّلان" else "صلاحية النشر غير مفعّلة")
                        Text("الرصيد: ${cents(account.optLong("balance_cents"))}")
                        Text("المحجوز: ${cents(account.optLong("reserved_cents"))}")
                        if (!account.optBoolean("protected")) {
                            OutlinedButton(onClick = { confirmation = JSONObject().put("action", if (account.optBoolean("active")) "ban" else "unban").put("label", if (account.optBoolean("active")) "حظر الحساب" else "إلغاء الحظر") }, enabled = !busy) { Text(if (account.optBoolean("active")) "حظر الحساب" else "إلغاء الحظر") }
                            OutlinedButton(onClick = { confirmation = JSONObject().put("action", if (account.optBoolean("creator")) "creator_off" else "creator_on").put("label", if (account.optBoolean("creator")) "إيقاف صلاحية النشر" else "تفعيل النشر والاستكشاف") }, enabled = !busy) { Text(if (account.optBoolean("creator")) "إيقاف صلاحية النشر" else "تفعيل النشر والاستكشاف") }
                        }
                    } }
                }
                item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("إضافة رصيد", style = MaterialTheme.typography.titleLarge)
                        Text("رصيد داخلي بالدولار؛ كل عملية محفوظة مع اسم المسؤول والسبب.")
                        OutlinedTextField(amount, { amount = it }, label = { Text("المبلغ بالدولار") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(reason, { reason = it.take(300) }, label = { Text("سبب الإضافة") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val value = runCatching { BigDecimal(amount.trim()).movePointRight(2).longValueExact() }.getOrNull()
                            if (value == null || value !in 1..1000000 || reason.trim().length < 3) error = "أدخل مبلغًا بين 0.01 و10000 دولار بخانتين عشريتين كحد أقصى، وسببًا واضحًا."
                            else confirmation = JSONObject().put("action", "credit").put("amount_cents", value).put("reason", reason.trim()).put("request_key", creditKey).put("label", "إضافة ${cents(value)}")
                        }, enabled = !busy) { Text("مراجعة الإضافة") }
                    } }
                }
                item {
                    Text("منشورات الحساب", style = MaterialTheme.typography.titleLarge)
                    val services = account.optJSONArray("services")
                    if (services != null) for (i in 0 until services.length()) { val s = services.getJSONObject(i); Text("${s.optString("title")} · ${if(s.optBoolean("approved")) "معتمد" else "بانتظار الاعتماد"}") }
                    val reels = account.optJSONArray("reels")
                    if (reels != null) Text("عدد الريلز: ${reels.length()}")
                }
            } else if (section == "الحسابات") {
                item {
                    OutlinedTextField(query, { query = it.take(100) }, label = { Text("ابحث بالاسم أو البريد") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { offset = 0; revision++ }, enabled = !busy && !loading) { Text("بحث") }
                    Text("$total حساب")
                }
                items(accounts, key = { it.optInt("id") }) { row ->
                    OutlinedCard(onClick = { viewAccount(row.optInt("id")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(row.optString("display_name"), style = MaterialTheme.typography.titleMedium)
                            Text(row.optString("email"))
                            Text(if(row.optBoolean("active")) "نشط · معاينة وإدارة" else "محظور · معاينة وإدارة")
                        }
                    }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { offset = (offset - 50).coerceAtLeast(0) }, enabled = offset > 0 && !loading && !busy) { Text("السابق") }
                    OutlinedButton(onClick = { offset += 50 }, enabled = offset + 50 < total && !loading && !busy) { Text("التالي") }
                } }
            } else {
                if(content.isEmpty() && !loading) item { Text("لا يوجد محتوى للمراجعة حاليًا.") }
                items(content, key = { it.optString("kind") + it.optInt("id") }) { row ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(row.optString("title"), style = MaterialTheme.typography.titleMedium)
                        Text("${row.optString("owner")} · ${if (row.optBoolean("approved")) "معتمد" else "غير معتمد"}")
                        if (row.optString("url").startsWith("https://")) TextButton(onClick = { runCatching { uriHandler.openUri(row.optString("url")) } }) { Text("معاينة الوسائط") }
                        OutlinedButton(onClick = { confirmation = JSONObject().put("kind", row.optString("kind")).put("id", row.optInt("id")).put("approved", !row.optBoolean("approved")).put("label", if(row.optBoolean("approved")) "إخفاء المحتوى" else "اعتماد المحتوى") }, enabled = !busy) { Text(if(row.optBoolean("approved")) "إخفاء" else "اعتماد") }
                    } }
                }
            }
        }
        confirmation?.let { data -> AlertDialog(onDismissRequest = { if(!busy) confirmation = null }, title = { Text(data.optString("label")) }, text = { Text("تأكيد تنفيذ الإجراء؟ سيُسجل باسم حسابك الإداري." ) }, confirmButton = {
            TextButton(onClick = {
                confirmation = null
                perform {
                    if(data.has("kind")) { api.request("/api/admin/moderation", "POST", data); revision++ }
                    else {
                        selected = api.request("/api/admin/accounts/${selected!!.optInt("id")}", "POST", data)
                        if(data.optString("action") == "credit") { amount = ""; reason = ""; creditKey = UUID.randomUUID().toString() }
                    }
                    success = "تم تنفيذ الإجراء بنجاح."
                }
            }, enabled = !busy) { Text("تأكيد") }
        }, dismissButton = { TextButton(onClick = { confirmation = null }, enabled = !busy) { Text("إلغاء") } }) }
    }
}
