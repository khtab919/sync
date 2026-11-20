package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Account preview uses the same native Relaxia dashboard, avatar and profile
 * components as the main app. It is NOT an authentication/impersonation switch.
 * All data comes from the scoped, audited /api/admin/control/preview endpoint.
 * No viewer action can post, send messages, charge a wallet or change this user.
 */
@Composable
internal fun AdminAccountPreviewScreen(snapshot: JSONObject, onBack: () -> Unit) {
    val original = snapshot.optJSONObject("profile") ?: JSONObject()
    val person = remember(snapshot) {
        JSONObject(original.toString()).apply {
            put("selected_frame", original.optInt("frame"))
            put("reel_count", original.optInt("reels_count"))
            put("service_count", original.optInt("services_count"))
            put("followers_count", original.optInt("followers"))
            put("following_count", original.optInt("following"))
        }
    }
    val name = person.optString("display_name").ifBlank {
        person.optString("username").ifBlank { "حساب المستخدم" }
    }
    val wallet = snapshot.optJSONObject("wallet")
    val rooms = snapshot.optJSONArray("live_rooms").previewRows()
    val operations = snapshot.optJSONArray("operations").previewRows()
    val bookings = snapshot.optJSONArray("bookings").previewRows()
    val benefits = snapshot.optJSONArray("benefits").previewRows()
    val communication = snapshot.optJSONObject("communication")
    val timeline = remember(snapshot) {
        buildList {
            snapshot.optJSONArray("paid_photos").previewRows().forEach {
                add(JSONObject(it.toString()).put("kind", "photo"))
            }
            snapshot.optJSONArray("reels").previewRows().forEach {
                add(JSONObject(it.toString()).put("kind", "reel"))
            }
            snapshot.optJSONArray("services").previewRows().forEach {
                add(JSONObject(it.toString()).put("kind", "service"))
            }
        }.sortedByDescending { it.optString("created_at") }
    }
    var selectedTab by remember(person.optString("public_id")) { mutableIntStateOf(0) }
    var roomFilter by remember(person.optString("public_id")) { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }
    BackHandler { onBack() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "معلومات المعاينة")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "معاينة للقراءة فقط", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 98.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                RelaxiaHomeDashboard(
                                    profile = person,
                                    balanceCents = wallet?.optLong("balance_cents"),
                                    onProfile = { selectedTab = 4 },
                                    onWallet = { selectedTab = 4 },
                                    onLive = { roomFilter = "video" },
                                    onAudio = { roomFilter = "audio" }
                                )
                            }
                            if (roomFilter.isNotBlank()) {
                                item {
                                    PreviewSection(if (roomFilter == "audio") "الغرف الصوتية" else "البث المباشر") {
                                        TextButton(onClick = { roomFilter = "" }) { Text("عودة للمنشورات") }
                                    }
                                }
                                val relevant = rooms.filter {
                                    if (roomFilter == "audio") it.optString("mode") == "audio"
                                    else it.optString("mode") != "audio"
                                }
                                if (relevant.isEmpty()) item { PreviewEmpty("لا توجد غرف نشطة أو سابقة لهذا الحساب") }
                                items(relevant, key = { "live-" + it.optString("id") }) {
                                    PreviewCard(it.optString("title").ifBlank { "بث مباشر" }) {
                                        Text("الحالة: " + it.optString("status"))
                                        Text("المشاهدون: " + it.optInt("viewers"))
                                    }
                                }
                            } else {
                                item { PreviewSection("المجتمع") { Text("منشورات " + name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                                if (timeline.isEmpty()) item { PreviewEmpty("لم ينشر هذا الحساب محتوى بعد") }
                                items(timeline, key = { it.optString("kind") + "-" + it.optInt("id") }) {
                                    PreviewPublication(it, name, person)
                                }
                            }
                        }
                        1 -> {
                            item { PreviewSection("الريلز") {} }
                            val reels = timeline.filter { it.optString("kind") == "reel" }
                            if (reels.isEmpty()) item { PreviewEmpty("لا توجد ريلز منشورة") }
                            items(reels, key = { "reel-" + it.optInt("id") }) {
                                PreviewPublication(it, name, person)
                            }
                        }
                        2 -> {
                            item { PreviewSection("المتجر والخدمات") {} }
                            val services = timeline.filter { it.optString("kind") == "service" }
                            if (services.isEmpty()) item { PreviewEmpty("لا توجد خدمات منشورة") }
                            items(services, key = { "service-" + it.optInt("id") }) {
                                PreviewPublication(it, name, person)
                            }
                        }
                        3 -> {
                            item { PreviewSection("الرسائل") {} }
                            item {
                                PreviewCard("نشاط الرسائل") {
                                    Text("المرسلة: " + (communication?.optInt("sent") ?: 0))
                                    Text("المستلمة: " + (communication?.optInt("received") ?: 0))
                                    Text("نصوص المحادثات الخاصة غير معروضة في معاينة الإدارة.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        4 -> {
                            item {
                                ProfileHero(
                                    person = person,
                                    own = true,
                                    settings = { showInfo = true },
                                    editOrChat = { showInfo = true },
                                    frameOrFollow = { showInfo = true }
                                )
                            }
                            item { PreviewSection("منشوراتي والريلز والخدمات") {} }
                            if (timeline.isEmpty()) item { PreviewEmpty("لا يوجد محتوى منشور بعد") }
                            items(timeline, key = { it.optString("kind") + "-" + it.optInt("id") }) {
                                PreviewPublication(it, name, person)
                            }
                            item {
                                PreviewCard("نشاط الحساب") {
                                    Text("الحجوزات: " + bookings.size)
                                    Text("الامتيازات: " + benefits.size)
                                }
                            }
                            if (wallet != null) item {
                                PreviewCard("الرصيد") {
                                    Text("الرصيد الحالي: " + wallet.optLong("balance_cents") + " كوين")
                                    Text("المحجوز: " + wallet.optLong("reserved_cents") + " كوين")
                                    Text("العمليات المعروضة: " + operations.size)
                                }
                            }
                        }
                    }
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    listOf(
                        Triple("الرئيسية", Icons.Rounded.Home, 0),
                        Triple("الريلز", Icons.Rounded.SmartDisplay, 1),
                        Triple("المتجر", Icons.Rounded.Storefront, 2),
                        Triple("الرسائل", Icons.Rounded.ChatBubbleOutline, 3),
                        Triple("حسابي", Icons.Rounded.PersonOutline, 4)
                    ).forEach { (title, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index; roomFilter = "" },
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title, fontSize = 10.sp) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 90.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.ArrowBack, null) },
                text = { Text("العودة للإدارة") }
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("معاينة حساب " + name) },
            text = {
                Text("تظهر الواجهة كما يراها صاحب الحساب، لكنك لا تدخل بحسابه فعليًا. " +
                    "لا يمكن إرسال رسائل أو إنفاق الكوينز أو تعديل بياناته من المعاينة.")
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("فهمت") }
            }
        )
    }
}

@Composable
private fun PreviewSection(title: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun PreviewPublication(item: JSONObject, name: String, person: JSONObject) {
    val kind = item.optString("kind")
    val title = when (kind) {
        "service" -> item.optString("title").ifBlank { "خدمة" }
        "photo" -> item.optString("caption").ifBlank { "صورة" }
        else -> item.optString("caption").ifBlank { "ريلز" }
    }
    val media = when (kind) {
        "service" -> item.optString("image_url")
        "photo" -> item.optString("media_url").ifBlank { item.optString("preview_url") }
        else -> item.optString("preview_url")
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReferenceAvatar(name, person.optInt("selected_frame"), 42,
                    person.optString("avatar_url"))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(when (kind) {
                        "photo" -> "صورة منشورة"
                        "reel" -> "ريلز"
                        else -> "خدمة"
                    }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.BookmarkBorder, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (media.startsWith("https://")) {
                ReferenceImage(media, Modifier.fillMaxWidth().height(245.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                if (kind == "service") {
                    Text(item.optString("description"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                }
                if (kind == "photo" || kind == "reel") {
                    val cost = item.optInt("price_cents")
                    if (cost > 0) Text("محتوى مدفوع · " + cost + " كوين",
                        color = MaterialTheme.colorScheme.primary)
                }
                if (kind == "service") {
                    Text("السعر: " + item.optLong("price_cents") + " كوين",
                        color = MaterialTheme.colorScheme.primary)
                }
                if (!item.optBoolean("approved", true) || !item.optBoolean("active", true)) {
                    Text("غير منشور للعامة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun JSONArray?.previewRows(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

@Composable
private fun PreviewEmpty(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(22.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PreviewCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
