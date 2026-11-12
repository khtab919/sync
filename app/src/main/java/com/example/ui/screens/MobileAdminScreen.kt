package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
internal fun MobileAdminScreen(api: LayaliApi, adminName: String, liveOnly:Boolean=false, allowed:List<String>?=null, exit:(()->Unit)?=null, logout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var section by remember { mutableStateOf(allowed?.firstOrNull()?:if(liveOnly)"البث المباشر"else"الحسابات") }
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
    var wheelConfig by remember { mutableStateOf(JSONObject()) }
    var wheelCost by remember { mutableStateOf("") }
    var wheelBank by remember { mutableStateOf("") }
    var wheelPrizes by remember { mutableStateOf("") }
    var gatewayConfigured by remember { mutableStateOf(false) }
    var paymentAccount by remember { mutableStateOf("") }
    var paymentCurrency by remember { mutableStateOf("USD") }
    var paymentRate by remember { mutableStateOf("1") }
    var paymentInstructions by remember { mutableStateOf("") }
    var paymentEnabled by remember { mutableStateOf(false) }
    var wheelStep by remember { mutableStateOf("") }
    var wheelMaximum by remember { mutableStateOf("") }
    var firstReward by remember { mutableStateOf("") }
    var grandUnlock by remember { mutableStateOf("") }
    var wheelEnabled by remember { mutableStateOf(false) }
    var reviewNote by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    var requests by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var endedLiveReports by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var liveRoom by remember{mutableStateOf<JSONObject?>(null)}
    var payout by remember{mutableStateOf<JSONObject?>(null)}
    var payoutReference by remember{mutableStateOf("")}
    var payoutVerified by remember{mutableStateOf(false)}
    var refundBooking by remember{mutableStateOf<JSONObject?>(null)}
    var refundReason by remember{mutableStateOf("")}
    var livePriceTarget by remember{mutableStateOf<JSONObject?>(null)}
    var livePriceInput by remember{mutableStateOf("")}
    var liveStop by remember{mutableStateOf<JSONObject?>(null)}
    var confirmation by remember { mutableStateOf<JSONObject?>(null) }
    var administrativeReason by remember { mutableStateOf("") }
    suspend fun securePost(path:String,payload:JSONObject):JSONObject {
        if(administrativeReason.trim().length<3)throw IllegalArgumentException("reason_required")
        return api.request(path,"POST",payload.put("reason",administrativeReason.trim()))
    }
    fun message(e: Exception): String = when(e.message) {
        "administrator_required", "invalid_session", "authentication_required" -> "لا تملك الجلسة صلاحية الإدارة. سجّل الدخول بحساب المسؤول."
        "protected_account" -> "هذا الحساب الإداري محمي من هذا الإجراء."
        "credit_request_conflict" -> "هذا الطلب استُخدم سابقًا ببيانات مختلفة. أعد فتح الحساب لبدء طلب جديد."
        "independent_approval_required" -> "سُجل طلب موافقة مستقلة. عد إلى مركز الإدارة وافتح الموافقات المالية."
        "admin_mfa_required" -> "عد لمركز الإدارة وأعد التحقق بالمصادقة الثنائية."
        "reason_required" -> "اكتب سبب الإجراء في أعلى الصفحة أولًا."
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
            } else if (section == "العجلة") {
                wheelConfig = api.request("/api/admin/wheel")
                wheelCost = BigDecimal.valueOf(wheelConfig.optLong("cost_cents"),2).toPlainString()
                wheelBank = BigDecimal.valueOf(wheelConfig.optLong("bankroll_cents"),2).toPlainString()
                wheelEnabled = wheelConfig.optBoolean("enabled")
                wheelStep = BigDecimal.valueOf(wheelConfig.optLong("price_step_cents"),2).toPlainString()
                wheelMaximum = BigDecimal.valueOf(wheelConfig.optLong("maximum_cost_cents"),2).toPlainString()
                firstReward = BigDecimal.valueOf(wheelConfig.optLong("first_reward_cents"),2).toPlainString()
                grandUnlock = BigDecimal.valueOf(wheelConfig.optLong("grand_unlock_spend_cents"),2).toPlainString()
                val prizes = wheelConfig.optJSONArray("prizes")
                wheelPrizes = if(prizes == null) "" else (0 until prizes.length()).joinToString("\n") { i -> val p = prizes.getJSONObject(i); "${BigDecimal.valueOf(p.optLong("amount_cents"),2)}:${p.optInt("weight")}" }
            } else if (section == "الدفع") {
                val data=api.request("/api/admin/payment-settings")
                gatewayConfigured=data.optBoolean("nowpayments_configured");paymentAccount=data.optString("account");paymentCurrency=data.optString("currency","USD");paymentRate=data.optString("rate","1");paymentInstructions=data.optString("instructions");paymentEnabled=data.optBoolean("enabled")
            } else if(section=="البث المباشر") {
                val data=api.request("/api/admin/live");content=data.adminItems()
                val reports=data.optJSONArray("pending_reports")
                endedLiveReports=if(reports==null)emptyList()else (0 until reports.length()).map{reports.getJSONObject(it)}
            }
            else if(section=="الحجوزات")content=api.request("/api/admin/bookings").adminItems()
            else if(section=="السحب")content=api.request("/api/admin/payouts").adminItems()
            else if (section == "الطلبات") requests = api.request("/api/admin/requests").adminItems()
            else content = api.request("/api/admin/moderation").adminItems()
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { accounts = emptyList(); content = emptyList(); error = message(e) }
        finally { loading = false }
    }
    BackHandler(enabled = liveRoom != null || selected != null || exit != null) {
        when {
            liveRoom != null -> liveRoom = null
            selected != null -> selected = null
            else -> exit?.invoke()
        }
    }
    liveRoom?.let { room ->
        Box(Modifier.fillMaxSize()) {
            LiveRoomView(
                baseUrl=api.origin,
                bootstrap=room,
                retry={
                    val parts=android.net.Uri.parse(room.optString("url")).pathSegments
                    val id=parts.getOrNull(parts.size-2)
                    liveRoom=null
                    if(!id.isNullOrBlank())perform{
                        liveRoom=api.request("/api/live/$id/admission","POST",JSONObject())
                    }
                },
                finish={liveRoom=null}
            )
            Surface(Modifier.safeDrawingPadding().padding(12.dp), shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
                TextButton(onClick = { liveRoom = null }) { Text("← العودة إلى إدارة البث") }
            }
        }
        return
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("إدارة Relaxia", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("مرحبًا، $adminName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if(exit!=null)TextButton(onClick=exit){Text("العودة لمركز الإدارة")}
                TextButton(onClick = logout, enabled = !busy) { Text("تسجيل الخروج") }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (allowed?:if(liveOnly)listOf("البث المباشر")else listOf("الحسابات", "البث المباشر", "الحجوزات", "السحب", "المحتوى", "الطلبات", "العجلة", "الدفع")).forEach { name ->
                        FilterChip(selected = section == name, onClick = { selected = null; section = name; offset = 0 }, label = { Text(name) }, enabled = !busy)
                    }
                }
                OutlinedTextField(administrativeReason,{administrativeReason=it.take(300)},label={Text("سبب الإجراء الإداري")},modifier=Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (success.isNotBlank()) Text(success, color = MaterialTheme.colorScheme.primary)
                if (loading || busy) LuxuryLoadingStatus("جارٍ التحميل")
                TextButton(onClick = { revision++ }, enabled = !busy && !loading) { Text("تحديث") }
            }
            if(section=="البث المباشر") {
                if(content.isEmpty())item{Text("لا توجد بثوث جارية.")}
                items(content,key={it.getString("id")}){row->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(row.optString("title"),style=MaterialTheme.typography.titleMedium);Text(row.optString("owner_name"));Text("${row.optInt("viewers")} مشاهد · "+if(row.optBoolean("private"))"خاص: ${row.optLong("price_cents")}"else"عام مجاني")
                    val reports=row.optJSONArray("reports");if(reports!=null)for(i in 0 until reports.length())Text("بلاغ: "+reports.getJSONObject(i).optString("reason"),color=MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        TextButton({perform{liveRoom=securePost("/api/live/${row.getString("id")}/admission",JSONObject())}},enabled=!busy){Text("مشاهدة للإشراف")}
                        Text((if(row.optString("mode")=="audio")"بث صوتي"else"بث فيديو")+" · ${row.optInt("max_speakers",1)} مقاعد · هدايا: ${row.optLong("gift_total_cents")}")
                    TextButton({liveStop=row},enabled=!busy){Text("إيقاف البث")}
                        if(reports!=null&&reports.length()>0)TextButton({perform{securePost("/api/admin/live",JSONObject().put("action","resolve").put("id",row.getString("id")));revision++}},enabled=!busy){Text("تمت المراجعة")}
                    }
                    if(row.optBoolean("private")&&!liveOnly)TextButton({livePriceTarget=row;livePriceInput=row.optLong("price_cents").toString()}){Text("تعديل السعر قبل البيع")}
                    Text("لا توجد تسجيلات أو إعادة مشاهدة للتحقيق لاحقًا.",style=MaterialTheme.typography.bodySmall)
                }}}
            }
            if(section=="البث المباشر"&&endedLiveReports.isNotEmpty()){
                item{Text("بلاغات بانتظار المراجعة",style=MaterialTheme.typography.titleMedium);Text("بيانات البلاغ فقط؛ لا توجد تسجيلات للبث.",style=MaterialTheme.typography.bodySmall)}
                items(endedLiveReports,key={"report-"+it.optLong("id")}){report->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(report.optString("title"));Text(report.optString("owner_name"));Text(report.optString("reason"))
                    Text(report.optString("reporter_name")+" · "+report.optString("created_at"),style=MaterialTheme.typography.bodySmall)
                    TextButton({perform{securePost("/api/admin/live",JSONObject().put("action","resolve").put("id",report.getString("broadcast_id")));revision++}},enabled=!busy){Text("تمت مراجعة بلاغات الجلسة")}
                }}}
            }
            if(section=="الحجوزات"){
                items(content,key={it.getString("id")}){row->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(row.optString("title"));Text(row.optString("buyer")+" ← "+row.optString("owner"));Text(cents(row.optLong("price_cents")));Text(operationLabel(row.optString("status")))
                    if(row.optString("payment_status")=="held")TextButton({refundBooking=row;refundReason=""},enabled=!busy){Text("مراجعة استرداد الحجز")}
                }}}
            }
            if(section=="السحب"){
                if(content.isEmpty())item{Text("لا توجد طلبات سحب.")}
                items(content,key={it.getString("id")}){row->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(row.optString("name"));Text(cents(row.optLong("amount_cents")));Text(row.optString("destination"));Text(operationLabel(row.optString("status")));Text(row.optString("payout_reference"))
                    if(row.optString("status")=="pending")Button({payout=row;payoutReference="";payoutVerified=false},enabled=!busy){Text("مراجعة طلب السحب")}
                }}}
            }
            val account = selected
            if (account != null && section == "الحسابات") {
                item {
                    TextButton(onClick = { selected = null; revision++ }, enabled = !busy) { Text("العودة إلى الحسابات") }
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RelaxiaAvatar(account.optString("display_name"), account.optInt("selected_frame"), 82, account.optString("avatar_url"))
                        Text(account.optString("display_name"), style = MaterialTheme.typography.titleLarge)
                        Text("#${account.optString("public_id","—")} · ${account.optString("username")}")
                        Text(account.optString("email"))
                        Text("الجنس: "+genderLabel(account.optString("gender")))
                        if(!account.optBoolean("protected"))TextButton({perform{selected=securePost("/api/admin/accounts/${account.optInt("id")}",JSONObject().put("action",if(account.optBoolean("live_moderator"))"moderator_off"else"moderator_on"))}},enabled=!busy){Text(if(account.optBoolean("live_moderator"))"إلغاء إشراف البث"else"منح إشراف البث فقط")}
                        Text(account.optString("bio"))
                        Text(if (account.optBoolean("active")) "الحساب نشط" else "الحساب محظور")
                        Text(if (account.optBoolean("publishing_allowed")) "النشر والظهور في الاستكشاف مفعّلان" else "صلاحية النشر غير مفعّلة")
                        if(!account.optBoolean("protected"))TextButton({perform{selected=securePost("/api/admin/accounts/${account.optInt("id")}",JSONObject().put("action",if(account.optBoolean("broadcast_allowed"))"broadcast_off"else"broadcast_on"))}},enabled=!busy){Text(if(account.optBoolean("broadcast_allowed"))"تعطيل صلاحية البث"else"السماح بالبث للحساب")}
                        Text("الرصيد: ${cents(account.optLong("balance_cents"))}")
                        Text("المحجوز: ${cents(account.optLong("reserved_cents"))}")
                        if (!account.optBoolean("protected")) {
                            OutlinedButton(onClick = { confirmation = JSONObject().put("action", if (account.optBoolean("active")) "ban" else "unban").put("label", if (account.optBoolean("active")) "حظر الحساب" else "إلغاء الحظر") }, enabled = !busy) { Text(if (account.optBoolean("active")) "حظر الحساب" else "إلغاء الحظر") }
                            OutlinedButton(onClick = { confirmation = JSONObject().put("action", if (account.optBoolean("publishing_allowed")) "publishing_off" else "publishing_on").put("label", if (account.optBoolean("publishing_allowed")) "إيقاف صلاحية النشر" else "تفعيل النشر والاستكشاف") }, enabled = !busy) { Text(if (account.optBoolean("publishing_allowed")) "إيقاف صلاحية النشر" else "تفعيل النشر والاستكشاف") }
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
            } else if (section == "العجلة") {
                item {
                    Text("إعدادات العجلة المدفوعة", style = MaterialTheme.typography.titleLarge)
                    Text("غير مفعّلة افتراضيًا. كل جائزة واحتمالها يظهران للمستخدم قبل الدفع. احتياطي الجوائز منفصل ويُحدّث مع كل لفة.")
                    Row { Switch(wheelEnabled, { wheelEnabled = it }, enabled = !busy); Text("تفعيل العجلة") }
                    OutlinedTextField(wheelCost, { wheelCost = it }, label = { Text("سعر اللفة بالدولار") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(wheelStep, { wheelStep=it }, label={Text("زيادة السعر بعد كل لفة بالدولار")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(wheelMaximum, { wheelMaximum=it }, label={Text("أعلى سعر للفة بالدولار")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(firstReward, { firstReward=it }, label={Text("جائزة أول لفة المضمونة بالدولار")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(grandUnlock, { grandUnlock=it }, label={Text("إنفاق مطلوب لفتح جائزة 100 دولار")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(wheelBank, { wheelBank = it }, label = { Text("احتياطي الجوائز بالدولار") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(wheelPrizes, { wheelPrizes = it }, label = { Text("كل سطر: الجائزة بالدولار:الوزن") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                    Text("مجموع الأوزان يحدد نسب الاحتمالات. الوزن 20 من مجموع 100 يعني احتمال 20%. الجائزة صفر تعني خسارة سعر اللفة.")
                    Button(onClick = { perform {
                        val list = org.json.JSONArray()
                        wheelPrizes.lines().filter { it.isNotBlank() }.forEach { line -> val parts = line.split(":"); require(parts.size == 2); list.put(JSONObject().put("amount_cents", BigDecimal(parts[0].trim()).movePointRight(2).intValueExact()).put("weight", parts[1].trim().toInt())) }
                        securePost("/api/admin/wheel",JSONObject().put("enabled", wheelEnabled).put("cost_cents", BigDecimal(wheelCost).movePointRight(2).intValueExact()).put("bankroll_cents", BigDecimal(wheelBank).movePointRight(2).longValueExact()).put("expected_bankroll_cents", wheelConfig.optLong("bankroll_cents")).put("prizes", list).put("price_step_cents",BigDecimal(wheelStep).movePointRight(2).intValueExact()).put("maximum_cost_cents",BigDecimal(wheelMaximum).movePointRight(2).intValueExact()).put("first_reward_cents",BigDecimal(firstReward).movePointRight(2).intValueExact()).put("grand_unlock_spend_cents",BigDecimal(grandUnlock).movePointRight(2).intValueExact()))
                        revision++; success = "تم حفظ إعدادات العجلة."
                    } }, enabled = !busy && !loading) { Text("حفظ الإعدادات") }
                }
            } else if (section == "الدفع") {
                item {
                    Text(if(gatewayConfigured)"NOWPayments: مفاتيح الخادم مضبوطة"else"NOWPayments: ينتظر مفاتيح حساب التاجر",style=MaterialTheme.typography.titleMedium)
                    Text("مفاتيح البوابة تضبط على الخادم وتبقى سرية. اختبر دفعة حقيقية قبل الإطلاق.",style=MaterialTheme.typography.bodySmall)
                    Text("استلام شام كاش",style=MaterialTheme.typography.titleLarge)
                    Row { Switch(paymentEnabled,{paymentEnabled=it},enabled=!busy);Text("تفعيل وسيلة الدفع") }
                    OutlinedTextField(paymentAccount,{paymentAccount=it.take(160)},label={Text("عنوان محفظة الاستلام")},modifier=Modifier.fillMaxWidth())
                    Row { listOf("USD","SYP").forEach{c->FilterChip(paymentCurrency==c,{paymentCurrency=c;if(c=="USD")paymentRate="1"},label={Text(c)})} }
                    if(paymentCurrency=="SYP")OutlinedTextField(paymentRate,{paymentRate=it},label={Text("ليرة سورية لكل دولار من الرصيد")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(paymentInstructions,{paymentInstructions=it.take(500)},label={Text("تعليمات الدفع")},modifier=Modifier.fillMaxWidth())
                    Button(onClick={perform { securePost("/api/admin/payment-settings",JSONObject().put("enabled",paymentEnabled).put("account",paymentAccount).put("currency",paymentCurrency).put("rate",paymentRate).put("instructions",paymentInstructions));success="تم حفظ بيانات الاستلام." }},enabled=!busy){Text("حفظ")}
                }
            } else if (section == "الطلبات") {
                if(requests.isEmpty() && !loading) item { Text("لا توجد طلبات دفع أو بائعين.") }
                items(requests, key = { it.optString("kind") + it.optString("id") }) { row ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(when(row.optString("kind")){"topup"->"طلب شحن رصيد";"gateway"->"دفعة NOWPayments";else->"طلب تفعيل بائع"}, style = MaterialTheme.typography.titleLarge)
                        Text("${row.optString("username")} · #${row.optString("public_id","—")}")
                        Text(when(row.optString("status")) { "pending" -> "قيد المراجعة"; "approved" -> "معتمد"; else -> if(row.optString("kind")=="gateway") row.optString("status") else "مرفوض" })
                        if(row.optString("kind") == "topup") {
                            Text("المحوّل: ${cents(row.optLong("amount_cents"))} · نقاط ترويجية: ${row.optLong("bonus_cents")}")
                            Text("${row.optString("method","usdt")} · المطلوب: ${row.optString("payment_amount")} ${row.optString("payment_currency")}")
                            if(row.optBoolean("late_submission"))Text("إيصال أُرسل بعد انتهاء مهلة الطلب — راجع وقت التحويل.")
                            Text(row.optString("transaction_hash"))
                            ReferenceImage(row.optString("receipt_url"), Modifier.fillMaxWidth().height(220.dp))
                            if(row.optString("method","usdt")=="usdt") TextButton(onClick = { runCatching { uriHandler.openUri("https://bscscan.com/tx/" + row.optString("transaction_hash")) } }) { Text("فحص التحويل على BscScan") }
                        } else if(row.optString("kind")=="gateway") {
                            Text("${cents(row.optLong("amount_cents"))} · نقاط ترويجية ${row.optLong("bonus_cents")}")
                            Text("رقم البوابة: ${row.optString("provider_id")}")
                            Text("الاعتماد آلي بعد التحقق من إشعار البوابة. لا يعتمد بمجرد صورة دفع.",style=MaterialTheme.typography.bodySmall)
                        } else Text(row.optString("description"))
                        if(row.optString("status") == "pending" && row.optString("kind")!="gateway") Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("approve" to "اعتماد", "reject" to "رفض").forEach { (decision, label) ->
                                OutlinedButton(onClick = { reviewNote = ""; verified = false; confirmation = JSONObject().put("request_kind", row.optString("kind")).put("id", row.optString("id")).put("decision", decision).put("label", "$label الطلب") }, enabled = !busy) { Text(label) }
                            }
                        } else Text(row.optString("review_note"))
                    } }
                }
            } else if(section=="المحتوى") {
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
        confirmation?.let { data -> AlertDialog(onDismissRequest = { if(!busy) confirmation = null }, title = { Text(data.optString("label")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("تأكيد تنفيذ الإجراء؟ سيُسجل باسم حسابك الإداري.")
            if(data.has("request_kind")) {
                OutlinedTextField(reviewNote, { reviewNote = it.take(300) }, label = { Text("سبب القرار / تفاصيل التحقق") })
                if(data.optString("request_kind") == "topup" && data.optString("decision") == "approve") {
                    Text("تحقق من وصول المبلغ الصحيح إلى الحساب الصحيح عبر وسيلة الدفع المختارة. الإيصال وحده لا يثبت الدفع.")
                    Row { Checkbox(verified, { verified = it }); Text("تحققت من وصول التحويل فعليًا") }
                }
            }
        } }, confirmButton = {
            TextButton(onClick = {
                confirmation = null
                perform {
                    if(data.has("request_kind")) {
                        val body = JSONObject().put("kind", data.optString("request_kind")).put("id", if(data.optString("request_kind") == "seller") data.optString("id").toInt() else data.optString("id")).put("decision", data.optString("decision")).put("review_note", reviewNote).put("payment_verified", verified)
                        securePost("/api/admin/requests",body); revision++
                    } else if(data.has("kind")) { securePost("/api/admin/moderation",data); revision++ }
                    else {
                        selected = securePost("/api/admin/accounts/${selected!!.optInt("id")}",data)
                        if(data.optString("action") == "credit") { amount = ""; reason = ""; creditKey = UUID.randomUUID().toString() }
                    }
                    success = "تم تنفيذ الإجراء بنجاح."
                }
            }, enabled = !busy && (!data.has("request_kind") || (reviewNote.trim().length >= 3 && (data.optString("request_kind") != "topup" || data.optString("decision") != "approve" || verified)))) { Text("تأكيد") }
        }, dismissButton = { TextButton(onClick = { confirmation = null }, enabled = !busy) { Text("إلغاء") } }) }
    }
    liveStop?.let{row->AlertDialog(onDismissRequest={liveStop=null},title={Text("إيقاف البث للجميع")},text={Text("سيُغلق البث وتُعاد الحجوزات المالية التي لم تستوفِ مدة الخدمة. لا يُحفظ فيديو.")},confirmButton={TextButton({liveStop=null;perform{securePost("/api/admin/live",JSONObject().put("action","end").put("id",row.getString("id")));revision++}}){Text("إيقاف")}},dismissButton={TextButton({liveStop=null}){Text("إلغاء")}})}
    payout?.let{row->AlertDialog(onDismissRequest={payout=null},title={Text("التحقق من السحب")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(row.optString("name"));Text(cents(row.optLong("amount_cents")));Text(row.optString("destination"));Text("نفّذ التحويل الخارجي أولًا ثم سجّل مرجعه. هذا الزر لا يرسل الأموال تلقائيًا.");OutlinedTextField(payoutReference,{payoutReference=it.take(300)},label={Text("مرجع التحويل المنفذ")});Row{Checkbox(payoutVerified,{payoutVerified=it});Text("تحققت من تنفيذ التحويل")}}},confirmButton={TextButton({payout=null;perform{securePost("/api/admin/payouts",JSONObject().put("id",row.getString("id")).put("action","paid").put("paid_verified",true).put("payout_reference",payoutReference));revision++}},enabled=payoutVerified&&payoutReference.length>=6&&!busy){Text("اعتماد السحب")}},dismissButton={TextButton({payout=null;perform{securePost("/api/admin/payouts",JSONObject().put("id",row.getString("id")).put("action","reject"));revision++}},enabled=!busy){Text("رفض وإعادة الحجز")}})}

    refundBooking?.let{row->AlertDialog(onDismissRequest={refundBooking=null},title={Text("استرداد الحجز")},text={Column{Text(row.optString("title"));Text(cents(row.optLong("price_cents")));OutlinedTextField(refundReason,{refundReason=it.take(300)},label={Text("سبب القرار بعد المراجعة")})}},confirmButton={TextButton({refundBooking=null;perform{securePost("/api/admin/bookings",JSONObject().put("id",row.getString("id")).put("action","refund").put("reason",refundReason));revision++}},enabled=refundReason.trim().length>=3&&!busy){Text("إعادة المبلغ")}},dismissButton={TextButton({refundBooking=null}){Text("إلغاء")}})}
    livePriceTarget?.let{row->AlertDialog(onDismissRequest={livePriceTarget=null},title={Text("سعر دخول البث")},text={Column{Text("يمكن التعديل قبل بيع أي تذكرة فقط، حفاظًا على السعر المتفق عليه.");OutlinedTextField(livePriceInput,{livePriceInput=it.filter{c->c.isDigit()}.take(7)},label={Text("السعر من الرصيد")})}},confirmButton={TextButton({livePriceTarget=null;perform{securePost("/api/admin/live",JSONObject().put("id",row.getString("id")).put("action","price").put("price_cents",livePriceInput.toLong()));revision++}},enabled=(livePriceInput.toLongOrNull()?:0L) in 1L..1000000L&&!busy){Text("حفظ")}},dismissButton={TextButton({livePriceTarget=null}){Text("إلغاء")}})}

}
