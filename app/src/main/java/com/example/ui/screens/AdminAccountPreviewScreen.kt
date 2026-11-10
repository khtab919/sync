package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Independent, read-only destination. The back control never scrolls off screen. */
@Composable
internal fun AdminAccountPreviewScreen(snapshot: JSONObject, onBack: () -> Unit) {
    val person = snapshot.optJSONObject("profile") ?: JSONObject()
    val displayName = person.optString("display_name").ifBlank {
        person.optString("username", "مستخدم")
    }
    val services = snapshot.optJSONArray("services")
    val benefits = snapshot.optJSONArray("benefits")
    val bookings = snapshot.optJSONArray("bookings")
    val communication = snapshot.optJSONObject("communication")
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 3.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الإدارة / الحسابات / المعاينة", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("معاينة الحساب", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onBack) { Text("رجوع للحساب") }
                    }
                    Text("قراءة فقط · لا يمكن التصرف باسم المستخدم أو الاطلاع على رسائله الخاصة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            RelaxiaAvatar(displayName, person.optInt("frame"),
                                84, person.optString("avatar_url"))
                            Text(displayName, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("#" + person.optString("public_id", "—") +
                                " · @" + person.optString("username"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RelaxiaLevelBadge(person.optInt("level", 1))
                                val vip = person.optInt("vip_tier")
                                if (vip in 1..5) {
                                    Text("VIP $vip", style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (person.optString("bio").isNotBlank()) Text(person.optString("bio"))
                        }
                    }
                }
                item {
                    AdminPreviewInformation("حالة الحساب") {
                        Text(if (person.optBoolean("publishing_allowed")) "النشر متاح" else "النشر مقيد")
                        Text(if (person.optBoolean("identity_verified")) "حساب موثق" else "غير موثق")
                        Text(when(person.optString("gender")) {
                            "male" -> "ذكر"; "female" -> "أنثى"; else -> "غير محدد"
                        })
                    }
                }
                item { Text("الخدمات", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold) }
                if (services == null || services.length() == 0) item {
                    Text("لا توجد خدمات منشورة لهذا الحساب",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else items((0 until services.length()).map { services.getJSONObject(it) }) { service ->
                    AdminPreviewInformation(service.optString("title")) {
                        Text("السعر: " + service.optLong("price_cents") + " سنت" +
                            " · المدة: " + service.optInt("duration_minutes") + " دقيقة")
                    }
                }
                item { Text("المكافآت والامتيازات", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold) }
                if (benefits == null || benefits.length() == 0) item {
                    Text("لا توجد مكافآت مؤقتة نشطة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else items((0 until benefits.length()).map { benefits.getJSONObject(it) }) { benefit ->
                    val type = when (benefit.optString("kind")) {
                        "vip" -> "VIP"; "level" -> "مستوى"; "frame" -> "إطار"; else -> "مكافأة"
                    }
                    AdminPreviewInformation("$type " + benefit.optInt("value")) {
                        Text("تنتهي: " + benefit.optString("expires_at"),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    AdminPreviewInformation("ملخص النشاط") {
                        Text("الحجوزات الظاهرة: " + (bookings?.length() ?: 0))
                        Text("الرسائل المرسلة: " + (communication?.optInt("sent") ?: 0))
                        Text("الرسائل المستلمة: " + (communication?.optInt("received") ?: 0))
                        Text("لا تُعرض محتويات الرسائل أو المكالمات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val wallet = snapshot.optJSONObject("wallet")
                if (wallet != null) item {
                    AdminPreviewInformation("المحفظة · للعرض فقط") {
                        Text("المتاح: " + wallet.optLong("balance_cents") + " سنت")
                        Text("المحجوز: " + wallet.optLong("reserved_cents") + " سنت")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPreviewInformation(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}
