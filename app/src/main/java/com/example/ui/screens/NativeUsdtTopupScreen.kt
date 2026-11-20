package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.LayaliApi
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

private val NightBlue = Color(0xFF0B1528)
private val PanelBlue = Color(0xFF13223D)
private val PanelBorder = Color(0xFF243B64)
private val ActionBlue = Color(0xFF1864D9)
private val SoftText = Color(0xFFB1C2DF)

@Composable
internal fun NativeUsdtTopupScreen(api: LayaliApi, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var usdAmount by rememberSaveable { mutableIntStateOf(10) }
    var quoteText by rememberSaveable { mutableStateOf("") }
    var paymentText by rememberSaveable { mutableStateOf("") }
    var paymentVisible by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var showOrders by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var balance by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var gatewayStatus by remember { mutableStateOf("waiting") }
    var remaining by remember { mutableLongStateOf(1800L) }
    val quote = remember(quoteText) { runCatching { JSONObject(quoteText) }.getOrNull() }
    val payment = remember(paymentText) { runCatching { JSONObject(paymentText) }.getOrNull() }
    val quoteId = quote?.optString("id").orEmpty()
    val payAddress = payment?.optString("pay_address").orEmpty()
    val payAmount = payment?.optString("pay_amount").orEmpty()

    fun copy(value: String, label: String) {
        if (value.isBlank()) return
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(label, value))
        notice = "تم النسخ"
    }
    fun runAction(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        notice = ""
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice = commerceError(e) }
            finally { busy = false }
        }
    }
    suspend fun refreshWallet() {
        balance = api.request("/api/wallet").optLong("available_cents")
    }
    suspend fun verify() {
        if (quoteId.isBlank()) return
        val state = api.request("/api/nowpayments/status/" + quoteId)
        gatewayStatus = state.optString("status", "waiting")
        if (state.optBoolean("credited")) {
            completed = true
            refreshWallet()
            notice = "تم تأكيد الدفع وإضافة الكوينز بنجاح."
        } else if (state.optBoolean("review_required")) {
            notice = "طلب الدفع قيد المراجعة. لا يتم إضافة الرصيد قبل التحقق."
        } else if (gatewayStatus in listOf("expired", "failed", "refunded")) {
            notice = "انتهى هذا الطلب أو تعذر الدفع. راجع طلباتي."
        }
    }
    LaunchedEffect(Unit) {
        try { refreshWallet() }
        catch (_: Exception) { notice = "تعذّر تحديث الرصيد" }
    }
    LaunchedEffect(quoteText) {
        val expiry = quote?.optString("expires_at").orEmpty()
        val deadline = runCatching { Instant.parse(expiry).toEpochMilli() }.getOrDefault(0L)
        if (deadline == 0L) {
            remaining = 1800L
        } else {
            while (true) {
                remaining = ((deadline - System.currentTimeMillis() + 999L) / 1000L)
                    .coerceAtLeast(0L)
                if (remaining == 0L) break
                delay(1000)
            }
        }
    }
    LaunchedEffect(quoteId, paymentVisible, completed) {
        if (!paymentVisible || completed || quoteId.isBlank()) return@LaunchedEffect
        while (!completed && paymentVisible) {
            try { verify() }
            catch (_: Exception) { gatewayStatus = "checking" }
            delay(12000)
        }
    }
    BackHandler(paymentVisible) { paymentVisible = false }

    Surface(Modifier.fillMaxSize(), color = NightBlue, contentColor = Color.White) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (paymentVisible) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { paymentVisible = false }) {
                        Icon(Icons.Rounded.ArrowForward, "رجوع")
                    }
                    Spacer(Modifier.weight(1f))
                    Text("الدفع الآمن", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(48.dp))
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepHeader("1", "أرسل بالضبط", onHelp = { showHelp = true })
                    TopupPanel {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = { copy(payAmount, "USDT amount") },
                                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue)) {
                                Icon(Icons.Rounded.ContentCopy, null)
                                Spacer(Modifier.width(7.dp))
                                Text("نسخ")
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(payAmount, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("USDT", color = SoftText)
                            }
                        }
                    }
                    StepHeader("2", "إلى هذا العنوان")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF46536A)) {
                            Text("الشبكة: Tron  |  USDT فقط",
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                    TopupPanel {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("كود QR للعنوان", color = SoftText)
                            val qr = remember(payAddress) { makeQr(payAddress) }
                            if (qr != null) {
                                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                                    Image(qr.asImageBitmap(), "رمز QR لعنوان الدفع",
                                        Modifier.size(156.dp).padding(10.dp))
                                }
                            } else Text("تعذّر إنشاء رمز QR. انسخ العنوان بالأسفل.")
                            HorizontalDivider(color = PanelBorder)
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { copy(payAddress, "USDT TRC20 address") },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF090E19), contentColor = Color.White)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("نسخ")
                                }
                                Text(payAddress, modifier = Modifier.weight(1f),
                                    fontSize = 13.sp, textAlign = TextAlign.Start,
                                    lineHeight = 20.sp)
                            }
                        }
                    }
                    if (completed) Text("تم تأكيد الدفع وإضافة الرصيد",
                        color = Color(0xFF4DE6A0), fontWeight = FontWeight.Bold)
                    else Text(
                        when (gatewayStatus) {
                            "confirming", "confirmed", "sending" ->
                                "تم رصد التحويل. بانتظار اكتمال تأكيد الشبكة."
                            "checking" -> "جارٍ إعادة الاتصال للتحقق من الدفع"
                            else -> "يتم التحقق تلقائيًا من التحويل"
                        },
                        Modifier.fillMaxWidth(), color = SoftText,
                        textAlign = TextAlign.Center
                    )
                    Text("صلاحية الفاتورة: %02d:%02d".format(Locale.US, remaining / 60, remaining % 60),
                        Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = SoftText)
                    OutlinedButton(onClick = { runAction { verify() } },
                        enabled = !busy && !completed,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (busy) "جارٍ التحقق…" else "التحقق الآن")
                    }
                    if (notice.isNotBlank()) Text(notice, color = SoftText)
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, null,
                            tint = Color(0xFF3DD89B))
                        Spacer(Modifier.width(6.dp))
                        Text("دفع آمن • اتصال مشفر", color = SoftText)
                    }
                    if (completed) Button(onClick = { paymentVisible = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue)) {
                        Text("العودة إلى رصيدي")
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = back) { Icon(Icons.Rounded.Close, "إغلاق") }
                    Text("شحن الكوينز", Modifier.weight(1f),
                        textAlign = TextAlign.Center, fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = back) { Icon(Icons.Rounded.ArrowForward, "رجوع") }
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { runAction {
                        val data = api.request("/api/topups").optJSONArray("items")
                        orders = if (data == null) emptyList()
                            else (0 until data.length()).mapNotNull { data.optJSONObject(it) }
                        showOrders = true
                    } }, enabled = !busy) {
                        Icon(Icons.Rounded.ReceiptLong, null)
                        Spacer(Modifier.width(5.dp))
                        Text("طلباتي")
                    }
                    TopupPanel {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Toll, null, tint = Color(0xFFF0B95C),
                                modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f),
                                horizontalAlignment = Alignment.End) {
                                Text("رصيدك الحالي", color = SoftText)
                                Text(balance.toString() + " كوين", fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    LockedPaymentRow("طريقة الدفع", "USDT · شبكة Tron (TRC20)",
                        Icons.Rounded.Lock)
                    TopupPanel {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF169C69),
                                shape = RoundedCornerShape(9.dp)) {
                                Text("= " + (usdAmount * 100) + " كوين",
                                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبلغ", color = SoftText)
                                Text("$" + String.format(Locale.US, "%.2f", usdAmount.toDouble()),
                                    fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 25, 50).forEach { option ->
                            FilterChip(
                                selected = option == usdAmount,
                                onClick = { usdAmount = option },
                                label = { Text("$" + option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text("الحد الأدنى: 1 USD • الدفع عبر USDT (TRC20) فقط",
                        color = SoftText, style = MaterialTheme.typography.bodySmall)
                    Text("صلاحية الفاتورة: 30:00 بعد إنشاء الطلب",
                        color = SoftText, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (payment != null && !completed && payAddress.isNotBlank() &&
                            quote?.optInt("amount_cents") == usdAmount * 100) {
                            paymentVisible = true
                        } else runAction {
                            val q = api.request("/api/topup-quotes", "POST",
                                JSONObject().put("method", "nowpayments")
                                    .put("amount_cents", usdAmount * 100))
                            val p = api.request("/api/nowpayments/direct", "POST",
                                JSONObject().put("quote_id", q.getString("id")))
                            require(p.optString("pay_currency") == "usdttrc20" &&
                                p.optString("pay_address").startsWith("T") &&
                                p.optString("pay_amount").isNotBlank()) {
                                "invalid_direct_payment"
                            }
                            quoteText = q.toString()
                            paymentText = p.toString()
                            paymentVisible = true
                        }
                    }, enabled = !busy && !completed,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue),
                        shape = RoundedCornerShape(16.dp)) {
                        Text(if (busy) "جارٍ إنشاء فاتورة USDT…" else
                            "متابعة الدفع — " + String.format(Locale.US, "%.2f USD", usdAmount.toDouble()),
                            fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { runAction {
                        if (quoteId.isBlank()) {
                            notice = "لا يوجد طلب شحن قيد التحقق."
                        } else verify()
                    } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("التحقق من وصول الرصيد")
                    }
                    Text("يُضاف الرصيد بعد تأكيد بوابة الدفع.",
                        color = SoftText, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)
                    if (notice.isNotBlank()) Text(notice, color = SoftText)
                }
            }
        }
    }

    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text("طريقة الدفع") },
        text = { Text("انسخ مبلغ USDT الدقيق وعنوان محفظة Tron من الفاتورة، ثم حوّل عبر شبكة TRC20 فقط. يُضاف الرصيد تلقائيًا بعد تأكيد الدفع.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("فهمت") } }
    )

    if (showOrders) {
        AlertDialog(
            onDismissRequest = { showOrders = false },
            title = { Text("طلباتي") },
            text = {
                Column(Modifier.heightIn(max = 370.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (orders.isEmpty()) Text("لا توجد طلبات محفوظة")
                    orders.forEach {
                        Text(it.optLong("amount_cents").toString() + " كوين · " +
                            it.optString("review_note", it.optString("status")))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrders = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun StepHeader(number: String, title: String, onHelp: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onHelp != null) TextButton(onClick = onHelp) { Text("كيف يعمل هذا؟") }
        Spacer(Modifier.weight(1f))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Surface(color = ActionBlue, shape = RoundedCornerShape(40.dp)) {
            Text(number, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun TopupPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PanelBlue,
        border = BorderStroke(1.dp, PanelBorder),
        content = content
    )
}

@Composable
private fun LockedPaymentRow(label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TopupPanel {
        Row(Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Color(0xFF64B1F6))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(label, color = SoftText, fontSize = 12.sp)
                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

/** QR is derived locally from the actual provider address, never a static sample. */
private fun makeQr(value: String): Bitmap? {
    if (!value.matches(Regex("T[1-9A-HJ-NP-Za-km-z]{33}"))) return null
    return runCatching {
        val size = 256
        val bits = QRCodeWriter().encode(
            value, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until size) for (x in 0 until size) {
                setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE)
            }
        }
    }.getOrNull()
}
