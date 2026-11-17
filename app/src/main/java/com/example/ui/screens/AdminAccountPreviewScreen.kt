package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

/** The customer's actual profile and activity in a read-only administrative viewport.
 * The administrator's own session and permissions never become those of the customer.
 * A persistent floating action returns to the same administrative account detail.
 */
@Composable
internal fun AdminAccountPreviewScreen(snapshot: JSONObject, onBack: () -> Unit) {
    val person = snapshot.optJSONObject("profile") ?: JSONObject()
    val name = person.optString("display_name").ifBlank { person.optString("username", "مستخدم") }
    val services = snapshot.optJSONArray("services").previewRows()
    val reels = snapshot.optJSONArray("reels").previewRows()
    val rooms = snapshot.optJSONArray("live_rooms").previewRows()
    val bookings = snapshot.optJSONArray("bookings").previewRows()
    val benefits = snapshot.optJSONArray("benefits").previewRows()
    val operations = snapshot.optJSONArray("operations").previewRows()
    val wallet = snapshot.optJSONObject("wallet")
    val communication = snapshot.optJSONObject("communication")
    val tabs = buildList {
        addAll(listOf("حسابي", "الريلز", "الخدمات", "البثوث", "الحجوزات", "الامتيازات"))
        if (wallet != null) add("الرصيد")
    }
    var selected by remember(person.optString("public_id")) { mutableStateOf("حسابي") }
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { listState.scrollToItem(0) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                // This is intentionally a user-style profile, not an admin data table.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Relaxia", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("معاينة آمنة", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { tab ->
                        FilterChip(selected == tab, onClick = { selected = tab },
                            label = { Text(tab) })
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                        top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                RelaxiaAvatar(name, person.optInt("frame"), 92,
                                    person.optString("avatar_url"))
                                Text(name, style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold)
                                Text("@" + person.optString("username") +
                                    "  ·  #" + person.optString("public_id"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RelaxiaLevelBadge(person.optInt("level", 1))
                                    val vip = person.optInt("vip_tier")
                                    if (vip in 1..5) Text("VIP $vip", fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                if(person.optString("bio").isNotBlank()) Text(person.optString("bio"))
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly) {
                                    PreviewCounter("المتابعون", person.optInt("followers"))
                                    PreviewCounter("يتابع", person.optInt("following"))
                                    PreviewCounter("الريلز", person.optInt("reels_count"))
                                    PreviewCounter("الخدمات", person.optInt("services_count"))
                                }
                            }
                        }
                    }

                    when(selected) {
                        "حسابي" -> {
                            item { PreviewCard("الملف الشخصي") {
                                Text("الخبرة: " + person.optLong("xp_points") + " XP")
                                Text("المستوى المكتسب: " + person.optInt("earned_level", 1))
                                Text("الإطارات المملوكة: " +
                                    (person.optJSONArray("owned_frames")?.length() ?: 0))
                                Text("التوثيق: " + if(person.optBoolean("identity_verified")) "موثق" else "غير موثق")
                                Text("النشر: " + if(person.optBoolean("publishing_allowed")) "متاح" else "مقيد")
                                Text("البث: " + if(person.optBoolean("broadcast_allowed")) "متاح" else "مقيد")
                            } }
                            item { PreviewCard("النشاط") {
                                Text("البثوث: " + rooms.size)
                                Text("الحجوزات: " + bookings.size)
                                Text("الرسائل: " + (communication?.optInt("sent") ?: 0) +
                                    " مرسلة · " + (communication?.optInt("received") ?: 0) + " مستلمة")
                                Text("محتوى الرسائل والمكالمات الخاصة غير متاح في المعاينة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } }
                        }
                        "الريلز" -> {
                            if(reels.isEmpty()) item { PreviewEmpty("لا توجد ريلز منشورة") }
                            items(reels, key = { "reel-" + it.optInt("id") }) { reel ->
                                PreviewCard(reel.optString("caption").ifBlank { "ريلز" }) {
                                    Text("الإعجابات: " + reel.optInt("likes"))
                                    Text(if(reel.optBoolean("approved")) "منشور" else "غير منشور")
                                    Text(reel.optString("created_at"),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        "الخدمات" -> {
                            if(services.isEmpty()) item { PreviewEmpty("لا توجد خدمات") }
                            items(services, key = { "service-" + it.optInt("id") }) { service ->
                                PreviewCard(service.optString("title")) {
                                    val image = service.optString("image_url")
                                    if (image.startsWith("https://"))
                                        ReferenceImage(image, Modifier.fillMaxWidth().height(170.dp))
                                    Text(service.optString("description"))
                                    Text("السعر: " + service.optLong("price_cents") + " كوين")
                                    Text("المدة: " + service.optInt("duration_minutes") + " دقيقة")
                                    Text(if(service.optBoolean("active")) "متاحة" else "غير متاحة")
                                }
                            }
                        }
                        "البثوث" -> {
                            if(rooms.isEmpty()) item { PreviewEmpty("لا توجد بثوث لهذا الحساب") }
                            items(rooms, key = { "room-" + it.optString("id") }) { room ->
                                PreviewCard(room.optString("title").ifBlank { "غرفة بث" }) {
                                    Text(if(room.optString("mode")=="audio") "غرفة صوتية" else "بث فيديو")
                                    Text("الحالة: " + room.optString("status") +
                                        " · المشاهدون: " + room.optInt("viewers"))
                                }
                            }
                        }
                        "الحجوزات" -> {
                            if(bookings.isEmpty()) item { PreviewEmpty("لا توجد حجوزات") }
                            items(bookings, key = { "booking-" + it.optString("id") }) { booking ->
                                PreviewCard("حجز #" + booking.optString("id").take(8)) {
                                    Text("الحالة: " + booking.optString("status"))
                                    Text("القيمة: " + booking.optLong("price_cents") + " كوين")
                                }
                            }
                        }
                        "الامتيازات" -> {
                            if(benefits.isEmpty()) item { PreviewEmpty("لا توجد امتيازات حالية") }
                            items(benefits, key = { "benefit-" + it.optString("id") }) { benefit ->
                                PreviewCard(when(benefit.optString("kind")) {
                                    "vip" -> "VIP"; "level" -> "مستوى"; "frame" -> "إطار"
                                    else -> "ميزة"
                                }) {
                                    Text("القيمة: " + benefit.optInt("value"))
                                    Text("تنتهي: " + benefit.optString("expires_at"))
                                }
                            }
                        }
                        "الرصيد" -> {
                            if(wallet != null) {
                                item { PreviewCard("المحفظة · عرض فقط") {
                                    Text("الرصيد: " + wallet.optLong("balance_cents") + " كوين")
                                    Text("المتاح: " + (wallet.optLong("balance_cents") -
                                        wallet.optLong("reserved_cents")) + " كوين")
                                    Text("المحجوز: " + wallet.optLong("reserved_cents") + " كوين")
                                } }
                                items(operations, key = { "op-" + it.optString("id") }) { op ->
                                    PreviewCard(op.optString("kind")) {
                                        Text("المبلغ: " + op.optLong("amount_cents") + " كوين")
                                        Text("الحالة: " + op.optString("status"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.ArrowBack, contentDescription = null) },
                text = { Text("العودة للإدارة") }
            )
        }
    }
}

private fun JSONArray?.previewRows(): List<JSONObject> =
    if(this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

@Composable
private fun PreviewCounter(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewEmpty(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PreviewCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
