package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
    val reels = snapshot.optJSONArray("reels")
    val liveRooms = snapshot.optJSONArray("live_rooms")
    val operations = snapshot.optJSONArray("operations")
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
                    Text("معاينة محتوى الحساب الحقيقي · للقراءة فقط، بلا انتحال أو اطلاع على الرسائل الخاصة",
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
                            Text("المتابعون: " + person.optInt("followers") +
                                "   •   يتابع: " + person.optInt("following"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("الريلز: " + person.optInt("reels_count") +
                                "   •   الخدمات: " + person.optInt("services_count"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("الخبرة: " + person.optLong("xp_points") +
                                " XP   •   المستوى المكتسب: " + person.optInt("earned_level", 1),
                                style = MaterialTheme.typography.bodySmall)
                            Text("الإطارات المملوكة: " + (person.optJSONArray("owned_frames")?.length() ?: 0),
                                style = MaterialTheme.typography.bodySmall)

                        }
                    }
                }
                item {
                    AdminPreviewInformation("حالة الحساب") {
                        Text(if (person.optBoolean("publishing_allowed")) "النشر متاح" else "النشر مقيد")
                        Text(if (person.optBoolean("identity_verified")) "حساب موثق" else "غير موثق")
                        Text(if (person.optBoolean("broadcast_allowed")) "البث متاح" else "البث موقوف")
                        Text(if (person.optBoolean("creator")) "صانع محتوى" else "حساب عادي")
                        Text("تاريخ التسجيل: " + person.optString("created_at"))
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
                        val picture = service.optString("image_url")
                        if (picture.startsWith("https://")) {
                            ReferenceImage(picture, Modifier.fillMaxWidth().height(160.dp))
                        }
                        Text(service.optString("description"))
                        Text("النوع: " + service.optString("category") +
                            " · السعر: " + service.optLong("price_cents") + " كوين" +
                            " · المدة: " + service.optInt("duration_minutes") + " دقيقة")
                        Text(if (service.optBoolean("active")) "الخدمة نشطة" else "الخدمة موقوفة",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { Text("الريلز والمنشورات", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold) }
                if(reels==null || reels.length()==0) item {
                    Text("لا توجد ريلز لهذا الحساب",color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else items((0 until reels.length()).map{reels.getJSONObject(it)}){reel->
                    AdminPreviewInformation("ريلز #"+reel.optInt("id")){
                        Text(reel.optString("caption").ifBlank{"دون وصف"})
                        Text("الإعجابات: "+reel.optInt("likes")+
                            " · "+(if(reel.optBoolean("approved")) "منشور" else "قيد المراجعة"),
                            style=MaterialTheme.typography.bodySmall)
                        Text("تاريخ النشر: "+reel.optString("created_at"),
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
                item { Text("الغرف والبثوث", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold) }
                if(liveRooms==null || liveRooms.length()==0)item{
                    Text("لم ينشئ الحساب بثًا بعد",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }else items((0 until liveRooms.length()).map{liveRooms.getJSONObject(it)}){live->
                    AdminPreviewInformation(live.optString("title").ifBlank{"بث مباشر"}){
                        Text((if(live.optString("mode")=="audio") "صوتي" else "فيديو")+
                            " · الحالة: "+live.optString("status"))
                        Text("المشاهدون: "+live.optInt("viewers")+
                            " · "+live.optString("created_at"),style=MaterialTheme.typography.bodySmall)
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
                item { Text("الحجوزات", style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.SemiBold) }
                if(bookings==null || bookings.length()==0)item{
                    Text("لا توجد حجوزات مسجلة",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }else items((0 until bookings.length()).map{bookings.getJSONObject(it)}){booking->
                    AdminPreviewInformation("حجز #"+booking.optString("id").take(8)){
                        Text("الحالة: "+booking.optString("status")+
                            " · القيمة: "+booking.optLong("price_cents")+" كوين")
                    }
                }
                val wallet = snapshot.optJSONObject("wallet")
                if (wallet != null) item {
                    AdminPreviewInformation("المحفظة · للعرض فقط") {
                        Text("الرصيد: " + wallet.optLong("balance_cents") + " كوين")
                        Text("المحجوز: " + wallet.optLong("reserved_cents") + " كوين")
                        Text("المتاح: " + (wallet.optLong("balance_cents") -
                            wallet.optLong("reserved_cents")) + " كوين")
                    }
                }
                if(operations!=null&&operations.length()>0){
                    item{Text("السجل المالي · للمسؤول المخوّل فقط",
                        style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)}
                    items((0 until operations.length()).map{operations.getJSONObject(it)}){op->
                        AdminPreviewInformation(op.optString("kind")){
                            Text("المبلغ: "+op.optLong("amount_cents")+" كوين"+
                                " · الحالة: "+op.optString("status"))
                            Text(op.optString("created_at"),
                                style=MaterialTheme.typography.bodySmall)
                        }
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
