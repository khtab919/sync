package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.NumberFormat

private val adminNavy = Color(0xFF07142F)
private val adminDeep = Color(0xFF101F41)
private val adminRoyal = Color(0xFF174BA9)
private val adminGold = Color(0xFFE6BD72)
private val adminMuted = Color(0xFFB9C7E0)
private val adminPink = Color(0xFFDC85B6)

private data class AdminTile(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val destination: String
)

private fun formatAdminNumber(number: Long) =
    NumberFormat.getIntegerInstance().format(number)

@Composable
internal fun AdminRoyalHeader(
    name: String,
    title: String,
    onMenu: () -> Unit,
    onNotices: (() -> Unit)?,
    onBack: () -> Unit
) {
    Surface(
        color = adminNavy,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.background(
                Brush.verticalGradient(listOf(Color(0xFF102A61), adminNavy))
            ).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Relaxia", color = adminGold,
                        fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("لوحة الإدارة", color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = adminMuted, fontSize = 12.sp)
                }
                if (onNotices != null) IconButton(onClick = onNotices) {
                    Icon(Icons.Outlined.Notifications, "الإشعارات",
                        tint = adminGold)
                }
                IconButton(onClick = onMenu) {
                    Icon(Icons.Outlined.Menu, "قائمة الإدارة", tint = Color.White)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "العودة للتطبيق", tint = Color.White)
                }
            }
            if (title != "الرئيسية") {
                Text(title, color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AdminStatTile(tile: AdminTile, stats: JSONObject, onClick: () -> Unit) {
    val present = stats.has(tile.key) && !stats.isNull(tile.key)
    val amount = if (present) {
        if (tile.key.endsWith("_cents")) {
            val cents = stats.optLong(tile.key)
            "${formatAdminNumber(cents / 100)}.${(cents % 100).toString().padStart(2, '0')} $"
        } else formatAdminNumber(stats.optLong(tile.key))
    } else "—"
    Surface(
        onClick = onClick,
        color = adminDeep,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, tile.color.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(tile.icon, contentDescription = null, tint = tile.color,
                    modifier = Modifier.size(26.dp))
                Spacer(Modifier.weight(1f))
            }
            Text(amount, color = Color.White, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            Text(tile.label, color = adminMuted,
                fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun AdminRoyalDashboard(
    stats: JSONObject,
    period: String,
    onPeriod: (String) -> Unit,
    allowed: (String) -> Boolean,
    onSection: (String) -> Unit,
    updatedAt: String,
    onRefresh: () -> Unit
) {
    val tiles = listOf(
        AdminTile("users", "إجمالي المستخدمين", Icons.Outlined.People,
            Color(0xFF7DB8FF), "الحسابات"),
        AdminTile("online", "المتصلون الآن", Icons.Outlined.Person,
            Color(0xFF5DD6B1), "الحسابات"),
        AdminTile("live", "البثوث الجارية", Icons.Outlined.Videocam,
            adminPink, "العمليات الحالية"),
        AdminTile("revenue_cents", "الإيراد المستحق", Icons.Outlined.Paid,
            adminGold, "الموافقات المالية")
    ).filter { it.key != "revenue_cents" || allowed("finance.view") }

    Column(verticalArrangement = Arrangement.spacedBy(17.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("نظرة عامة", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.weight(1f), color = adminNavy)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, "تحديث الإحصائيات",
                    tint = adminRoyal)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, label) in listOf("day" to "اليوم", "week" to "الأسبوع",
                "month" to "الشهر")) {
                FilterChip(selected = period == value, onClick = { onPeriod(value) },
                    label = { Text(label) })
            }
        }
        // Equal-height tiles, responsive to portrait phone width.
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                row.forEach { tile ->
                    Box(Modifier.weight(1f)) {
                        AdminStatTile(tile, stats) { onSection(tile.destination) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("إحصاءات مباشرة من الخادم", color = adminNavy,
            fontWeight = FontWeight.SemiBold)
        val highlights = listOf(
            Triple("pending_reports", "بلاغات تنتظر المراجعة", "الدعم والبلاغات"),
            Triple("pending_cases", "طلبات الدعم", "الدعم والبلاغات"),
            Triple("bookings", "حجوزات الفترة", "العمليات الحالية"),
            Triple("pending_withdrawals", "طلبات السحب", "الموافقات المالية")
        )
        for ((key, label, destination) in highlights) {
            if (!stats.has(key)) continue
            Surface(onClick = { onSection(destination) },
                shape = RoundedCornerShape(15.dp), color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE0E7F5))) {
                Row(Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), color = adminNavy)
                    Text(formatAdminNumber(stats.optLong(key)), color = adminRoyal,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ChevronLeft, null, tint = adminRoyal,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
        if (updatedAt.isNotBlank()) Text("آخر تحديث: $updatedAt",
            color = Color(0xFF687991), fontSize = 11.sp)
        Text("جميع الأرقام مأخوذة من بيانات فعلية. لا توجد رسوم أو أنشطة تجريبية.",
            color = Color(0xFF687991), fontSize = 11.sp)
    }
}

@Composable
internal fun AdminRoyalBottomNavigation(
    current: String,
    allowed: (String) -> Boolean,
    onNavigate: (String) -> Unit
) {
    val links = listOf(
        Triple("الرئيسية", "dashboard.view", Icons.Outlined.Home),
        Triple("الحسابات", "users.view", Icons.Outlined.People),
        Triple("العمليات الحالية", "live.view", Icons.Outlined.Videocam),
        Triple("الموافقات المالية", "finance.view", Icons.Outlined.AccountBalanceWallet)
    ).filter { allowed(it.second) }
    if (links.isEmpty()) return
    Surface(color = adminNavy, shadowElevation = 9.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            links.forEach { (title, _, icon) ->
                val selected = current == title
                Column(Modifier.weight(1f).clickable { onNavigate(title) }
                    .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(icon, contentDescription = title,
                        tint = if (selected) adminGold else adminMuted)
                    Text(
                        if (title == "الموافقات المالية") "المال" else if (title == "العمليات الحالية") "البث" else title,
                        color = if (selected) Color.White else adminMuted,
                        fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun AdminRoyalMenu(
    sections: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sections.forEach { (title, _) ->
            val selected = title == current
            Surface(
                onClick = { onSelect(title) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Color(0xFF1D4389) else adminNavy,
                border = BorderStroke(1.dp,
                    if (selected) adminGold else Color.White.copy(alpha = 0.12f))
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChevronLeft, null, tint = adminGold)
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = Color.White, fontWeight =
                        if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
