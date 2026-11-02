package com.example.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun FinancePanel(vm: PlatformViewModel, state: PlatformState) {
    var form by remember { mutableStateOf("") }
    val creator = state.profile.str("role") == "creator"
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Panel {
                Text("رصيد المحفظة", style = MaterialTheme.typography.titleMedium)
                Text(
                    usd(state.profile.num("balanceCents")),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Primary("إرفاق إيصال إيداع", !state.busy) { form = "deposit" }
                Text("لن يُضاف الرصيد حتى تتحقق الإدارة من التحويل.")
            }
        }
        if (creator) {
            item {
                Panel {
                    Text("أرباحك • حصتك 80%", style = MaterialTheme.typography.titleLarge)
                    Text("متاح للسحب", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        usd(state.profile.num("availableCents")),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text("محجوز لطلبات السحب: ${usd(state.profile.num("heldCents"))}")
                    Text("إجمالي الأرباح: ${usd(state.profile.num("lifetimeCents"))}")
                    Primary("طلب سحب", !state.busy && state.profile.num("availableCents") > 0) {
                        form = "withdraw"
                    }
                    Text(
                        "مدة الوصول المتوقعة من ساعة إلى 24 ساعة من تقديم الطلب. تظهر هنا حالة الطلب وإيصال التحويل عند اكتماله."
                    )
                }
            }
            item { Text("طلبات السحب", style = MaterialTheme.typography.titleLarge) }
            if (state.history.rows("withdrawals").isEmpty()) item { Text("لا توجد طلبات سحب") }
            items(state.history.rows("withdrawals"), key = { it.str("id") }) { r ->
                WithdrawalCard(vm, state, r)
            }
            item {
                Panel {
                    Text("تفاصيل الأرباح", style = MaterialTheme.typography.titleLarge)
                    if (state.history.rows("earnings").isEmpty()) Text("لا توجد أرباح بعد")
                    state.history.rows("earnings").forEach { r ->
                        Text(
                            "قيمة الهدية ${usd(r.num("grossCents"))}\nحصتك ${usd(r.num("creatorCents"))} • المنصة ${usd(r.num("platformCents"))}"
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        item { Text("الإيداعات", style = MaterialTheme.typography.titleLarge) }
        items(state.history.rows("deposits"), key = { it.str("id") }) { d ->
            Panel {
                Text("${usd(d.num("amountCents"))} • ${statusLabel(d.str("status"))}")
                Text(d.str("reference"))
                if (d.str("reason").isNotBlank()) Text(d.str("reason"))
                ReceiptButton(vm, "deposits", d.str("id"))
            }
        }
    }
    if (form.isNotBlank()) FinanceDialog(vm, state, form) { form = "" }
}

@Composable
internal fun ReceiptButton(vm: PlatformViewModel, kind: String, id: String) {
    val scope = rememberCoroutineScope()
    val handler = LocalUriHandler.current
    var busy by remember { mutableStateOf(false) }
    TextButton(
        enabled = !busy,
        onClick = {
            busy = true
            scope.launch {
                try {
                    val url = vm.call("receipt", mapOf("kind" to kind, "id" to id)).str("url")
                    if (url.isNotBlank()) handler.openUri(url)
                    else vm.fail("لم يُرفق إيصال التحويل بعد")
                } catch (e: Exception) {
                    vm.fail(e.localizedMessage ?: "تعذر فتح الإيصال")
                } finally {
                    busy = false
                }
            }
        },
    ) {
        Text("عرض الإيصال")
    }
}

@Composable
private fun WithdrawalCard(vm: PlatformViewModel, state: PlatformState, r: Record) {
    Panel {
        Text(
            "${usd(r.num("amountCents"))} • ${statusLabel(r.str("status"))}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("رقم الطلب: ${r.str("id")}")
        Text("${r.str("beneficiary")} • ${r.str("method")}\n${r.str("destination")}")
        Text(
            "تاريخ الطلب: ${DateFormat.getDateTimeInstance().format(Date(r.num("requestedAtMs")))}"
        )
        if (r.str("status") in listOf("pending", "processing")) {
            Text(
                "الموعد المتوقع: ${DateFormat.getDateTimeInstance().format(Date(r.num("expectedByMs")))}"
            )
            if (System.currentTimeMillis() > r.num("expectedByMs"))
                Text(
                    "تجاوز الطلب المدة المتوقعة؛ تواصل مع الإدارة برقم الطلب.",
                    color = MaterialTheme.colorScheme.error,
                )
        }
        if (r.str("reason").isNotBlank()) Text("ملاحظة الإدارة: ${r.str("reason")}")
        if (r.str("status") == "paid") {
            Text("مرجع التحويل: ${r.str("reference")}")
            ReceiptButton(vm, "withdrawals", r.str("id"))
        }
        if (r.str("status") == "pending")
            TextButton(
                enabled = !state.busy,
                onClick = {
                    vm.perform(
                        "processWithdrawal",
                        mapOf(
                            "id" to r.str("id"),
                            "action" to "cancel",
                            "reason" to "إلغاء بطلب صاحبة الحساب",
                        ),
                        "أُلغي الطلب وأعيد المبلغ إلى الأرباح المتاحة",
                    )
                },
            ) {
                Text("إلغاء الطلب وإعادة المبلغ")
            }
    }
}

@Composable
private fun FinanceDialog(
    vm: PlatformViewModel,
    state: PlatformState,
    kind: String,
    close: () -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("SHAM_CASH") }
    var destination by rememberSaveable { mutableStateOf("") }
    var beneficiary by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val deposit = kind == "deposit"
    val target = state.settings.str(if (method == "SHAM_CASH") "shamCash" else "usdt")
    AlertDialog(
        onDismissRequest = { if (!state.busy) close() },
        title = { Text(if (deposit) "إيصال الإيداع" else "طلب سحب الأرباح") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyField(amount, { amount = it })
                MethodSelector(method) { method = it }
                if (deposit) {
                    Text(
                        if (target.isBlank()) "لم تضبط الإدارة وجهة الإيداع بعد"
                        else "حوّل إلى:\n$target"
                    )
                    Field(reference, { reference = it }, "مرجع التحويل")
                    OutlinedButton(enabled = !state.busy, onClick = { picker.launch("image/*") }) {
                        Text(if (uri == null) "إرفاق صورة الإيصال" else "تم اختيار الإيصال • تغيير")
                    }
                } else {
                    Field(beneficiary, { beneficiary = it }, "اسم المستفيدة")
                    Field(
                        destination,
                        { destination = it },
                        if (method == "SHAM_CASH") "حساب شام كاش" else "عنوان USDT على شبكة BEP-20",
                    )
                    Text(
                        "المبلغ من حصتك الصافية (80%)؛ لا تُخصم عمولة المنصة مرة ثانية. الوصول المتوقع خلال 1–24 ساعة."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.busy && (!deposit || (uri != null && target.isNotBlank())),
                onClick = {
                    val cents = parseCents(amount)
                    if (cents == null) vm.fail("أدخل مبلغًا صحيحًا")
                    else if (deposit)
                        vm.perform(
                            "submitDeposit",
                            mapOf(
                                "amountCents" to cents,
                                "method" to method,
                                "reference" to reference,
                                "requestId" to id,
                            ),
                            "أُرسل الإيصال للمراجعة",
                            uri,
                            "depositReceipts",
                            close,
                        )
                    else
                        vm.perform(
                            "requestWithdrawal",
                            mapOf(
                                "amountCents" to cents,
                                "method" to method,
                                "destination" to destination,
                                "beneficiary" to beneficiary,
                                "requestId" to id,
                            ),
                            "تم تقديم طلب السحب وحجز المبلغ",
                            after = close,
                        )
                },
            ) {
                Text(if (state.busy) "جارٍ الإرسال…" else "تأكيد الطلب")
            }
        },
        dismissButton = { TextButton(onClick = close, enabled = !state.busy) { Text("إلغاء") } },
    )
}

@Composable
internal fun MethodSelector(method: String, change: (String) -> Unit) {
    Column {
        listOf("SHAM_CASH" to "شام كاش", "USDT_BEP20" to "USDT • BEP-20").forEach { (id, name) ->
            Row {
                RadioButton(selected = method == id, onClick = { change(id) })
                Text(name, Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
internal fun PublishDialog(vm: PlatformViewModel, state: PlatformState, close: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    AlertDialog(
        onDismissRequest = { if (!state.busy) close() },
        title = { Text("نشر ريلز") },
        text = {
            Column {
                Field(title, { title = it }, "عنوان الفيديو")
                OutlinedButton(onClick = { picker.launch("video/*") }, enabled = !state.busy) {
                    Text(if (uri == null) "اختيار فيديو" else "تم اختيار الفيديو")
                }
                Text("MP4 أو WebM حتى 100 ميغابايت. يظهر للجميع دون إثبات هوية.")
            }
        },
        confirmButton = {
            TextButton(
                enabled = uri != null && title.isNotBlank() && !state.busy,
                onClick = {
                    vm.perform(
                        "publishReel",
                        mapOf("title" to title, "requestId" to id),
                        "تم نشر الريلز",
                        uri,
                        "reelUploads",
                        close,
                    )
                },
            ) {
                Text("نشر")
            }
        },
        dismissButton = { TextButton(onClick = close, enabled = !state.busy) { Text("إلغاء") } },
    )
}
