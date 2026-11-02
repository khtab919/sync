package com.example.platform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

internal fun parseCents(s: String): Long? =
    try {
        val normalized =
            s.map { if (it in '٠'..'٩') ('0'.code + (it.code - '٠'.code)).toChar() else it }
                .joinToString("")
                .replace('٫', '.')
        BigDecimal(normalized)
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
            .takeIf { it > 0 && it <= 100000000 }
    } catch (_: Exception) {
        null
    }

internal fun usd(cents: Long) = "${BigDecimal.valueOf(cents,2).toPlainString()} USD"

internal fun statusLabel(s: String) =
    mapOf(
        "pending" to "قيد المراجعة",
        "processing" to "قيد التحويل",
        "paid" to "تم التحويل",
        "approved" to "مقبول",
        "rejected" to "مرفوض",
        "cancelled" to "ملغى",
        "active" to "نشط",
        "banned" to "محظور",
        "suspended" to "حظر مؤقت",
        "deleted" to "محذوف",
        "confirmed" to "مؤكد",
    )[s] ?: s

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayaliPlatform() {
    val vm: PlatformViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var admin by rememberSaveable { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.error, state.notice) {
        (state.error ?: state.notice)?.let {
            snack.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.signedIn) { if (!state.signedIn) admin = false }
    BackHandler(admin) { admin = false }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme =
                darkColorScheme(
                    primary = Color(0xFFBDA7E7),
                    background = Color(0xFF080C16),
                    surface = Color(0xFF111827),
                    onSurface = Color(0xFFF1F4FA),
                    onSurfaceVariant = Color(0xFFB7C1D0),
                ),
            typography =
                Typography(
                    headlineLarge =
                        androidx.compose.ui.text.TextStyle(fontSize = 30.sp, lineHeight = 40.sp),
                    titleLarge =
                        androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
                    titleMedium =
                        androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
                    bodyLarge =
                        androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
                    bodyMedium =
                        androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
                    labelLarge =
                        androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
                ),
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snack) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (admin) "إدارة ليالي"
                                else listOf("ليالي", "الريلز", "المحفظة والأرباح", "حسابي")[tab]
                            )
                        },
                        navigationIcon = {
                            if (admin)
                                IconButton(onClick = { admin = false }) {
                                    Icon(Icons.Default.ArrowForward, "رجوع")
                                }
                        },
                        actions = {
                            IconButton(onClick = vm::refresh, enabled = !state.busy) {
                                Icon(Icons.Default.Refresh, "تحديث")
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!admin)
                        NavigationBar {
                            listOf("الرئيسية", "الريلز", "المحفظة", "حسابي").forEachIndexed { i, t
                                ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = {
                                        Icon(
                                            listOf(
                                                Icons.Default.Home,
                                                Icons.Default.PlayCircle,
                                                Icons.Default.AccountBalanceWallet,
                                                Icons.Default.Person,
                                            )[i],
                                            t,
                                        )
                                    },
                                    label = { Text(t, fontSize = 14.sp) },
                                )
                            }
                        }
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (admin && state.profile["isAdmin"] == true) AdminPanel(vm, state)
                    else
                        when (tab) {
                            0 -> ServicesPanel(vm, state)
                            1 -> LiveReels(vm, state)
                            2 ->
                                if (state.signedIn && state.profile.isNotEmpty())
                                    FinancePanel(vm, state)
                                else
                                    EmptyPanel(
                                        "محفظتك وأرباحك",
                                        "سجل الدخول من حسابي لعرض الرصيد وطلبات السحب.",
                                    )
                            3 ->
                                if (state.signedIn) AccountPanel(vm, state, { admin = true })
                                else LoginPanel(vm, state)
                        }
                }
            }
        }
    }
}

@Composable
internal fun EmptyPanel(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
internal fun MoneyField(value: String, onChange: (String) -> Unit) {
    Field(value, onChange, "المبلغ بالدولار — مثال 10.50")
}

@Composable
internal fun Primary(text: String, enabled: Boolean = true, click: () -> Unit) {
    Button(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun LoginPanel(vm: PlatformViewModel, state: PlatformState) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("أهلًا بك في ليالي", style = MaterialTheme.typography.headlineLarge)
            Text("شاهد الريلز بحرية. سجل الدخول لحفظ حسابك وإرسال الهدايا.")
        }
        item { Field(email, { email = it }, "البريد الإلكتروني", !state.busy) }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Primary(
                if (register) "إنشاء الحساب" else "تسجيل الدخول",
                !state.busy && email.isNotBlank() && password.length >= 6,
            ) {
                vm.login(email, password, register)
            }
        }
        item {
            TextButton(onClick = { register = !register }, enabled = !state.busy) {
                Text(if (register) "لدي حساب بالفعل" else "إنشاء حساب جديد")
            }
            TextButton(
                onClick = { vm.resetPassword(email) },
                enabled = email.isNotBlank() && !state.busy,
            ) {
                Text("نسيت كلمة المرور")
            }
        }
    }
}

@Composable
private fun AccountPanel(vm: PlatformViewModel, state: PlatformState, openAdmin: () -> Unit) {
    var name by
        rememberSaveable(state.profile.str("uid"), state.profile.str("displayName")) {
            mutableStateOf(state.profile.str("displayName"))
        }
    var frame by
        rememberSaveable(state.profile.str("uid"), state.profile.str("frameId")) {
            mutableStateOf(state.profile.str("frameId").ifBlank { "platinum" })
        }
    var publish by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RoyalAvatar(name, frame)
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
        }
        if (state.profile.str("warning").isNotBlank())
            item {
                Panel {
                    Text("تنبيه من الإدارة")
                    Text(state.profile.str("warning"))
                }
            }
        item {
            Panel {
                Field(name, { name = it }, "الاسم الظاهر")
                Text("إطار الحساب", style = MaterialTheme.typography.titleMedium)
                listOf(
                        "platinum" to "بلاتين • نقاء ملكي",
                        "sapphire" to "ياقوت • تاج الليل",
                        "imperial" to "إمبراطوري • ذهب وجمشت",
                    )
                    .forEach { (id, title) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = frame == id, onClick = { frame = id })
                            Text(title)
                        }
                    }
                Primary("حفظ الملف", !state.busy && name.isNotBlank()) {
                    vm.perform(
                        "updateProfile",
                        mapOf("displayName" to name, "frameId" to frame),
                        "تم حفظ الملف",
                    )
                }
            }
        }
        if (state.profile.str("role") == "creator" || state.profile["isAdmin"] == true)
            item { Primary("نشر ريلز", !state.busy) { publish = true } }
        if (state.profile["isAdmin"] == true)
            item {
                OutlinedButton(
                    onClick = openAdmin,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text("لوحة الإدارة")
                }
            }
        item {
            Panel {
                Text("حجوزاتي", style = MaterialTheme.typography.titleLarge)
                if (state.bookings.isEmpty()) Text("لا توجد حجوزات بعد")
                state.bookings.forEach { b ->
                    Text(
                        "${b.str("title")} • ${b.str("date")}\n${statusLabel(b.str("status"))} • ${usd(b.num("totalCents"))}"
                    )
                    HorizontalDivider()
                }
            }
        }
        item { TextButton(onClick = vm::signOut, enabled = !state.busy) { Text("تسجيل الخروج") } }
    }
    if (publish) PublishDialog(vm, state) { publish = false }
}

@Composable
private fun ServicesPanel(vm: PlatformViewModel, state: PlatformState) {
    var selected by remember { mutableStateOf<Record?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("ليلتك تبدأ هنا", style = MaterialTheme.typography.headlineLarge)
            Text(
                "عروض منشورة من الإدارة، بتفاصيل وأسعار واضحة.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.services.isEmpty())
            item {
                EmptyPanel(
                    "لا توجد عروض منشورة حاليًا",
                    "لن نعرض عروضًا أو تقييمات تجريبية. يمكنك مشاهدة الريلز أو العودة لاحقًا.",
                )
            }
        items(state.services, key = { it.str("id") }) { s ->
            Panel {
                Text(s.str("title"), style = MaterialTheme.typography.titleLarge)
                Text(s.str("cityName"))
                Text(s.str("description"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${usd(s.num("priceUsd")*100)} للشخص",
                    style = MaterialTheme.typography.titleMedium,
                )
                Primary("طلب حجز", state.signedIn && !state.busy) { selected = s }
                if (!state.signedIn) Text("سجل الدخول لإرسال طلب الحجز")
            }
        }
    }
    selected?.let { s ->
        var date by rememberSaveable { mutableStateOf("") }
        var guests by rememberSaveable { mutableStateOf("1") }
        val requestId = remember { UUID.randomUUID().toString() }
        AlertDialog(
            onDismissRequest = { if (!state.busy) selected = null },
            title = { Text(s.str("title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(date, { date = it }, "تاريخ الحجز YYYY-MM-DD")
                    Field(guests, { guests = it }, "عدد الأشخاص (1–15)")
                    Text("الإجمالي: ${usd(s.num("priceUsd")*100*(guests.toLongOrNull()?:0))}")
                    Text("الطلب بانتظار تأكيد التوافر من الإدارة. لا يُخصم رصيد عند تقديمه.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = {
                        vm.perform(
                            "bookService",
                            mapOf(
                                "serviceId" to s.str("id"),
                                "date" to date,
                                "guests" to (guests.toIntOrNull() ?: 0),
                                "expectedCents" to
                                    s.num("priceUsd") * 100 * (guests.toLongOrNull() ?: 0),
                                "requestId" to requestId,
                            ),
                            "تم إرسال طلب الحجز",
                            after = { selected = null },
                        )
                    },
                ) {
                    Text("إرسال الطلب")
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }, enabled = !state.busy) { Text("إلغاء") }
            },
        )
    }
}
