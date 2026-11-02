package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.SmartServiceMediaView

@Composable
internal fun CalmProfileHeader(
    profile: UserProfile, title: String, username: String, coverUri: String,
    bookings: Int, services: Int, favorites: Int, light: Boolean, isAdmin: Boolean,
    onCover: () -> Unit, onAvatar: () -> Unit, onCopy: () -> Unit,
    onEdit: () -> Unit, onSettings: () -> Unit, onWallet: () -> Unit,
    onBookings: () -> Unit, onFavorites: () -> Unit, onMessages: () -> Unit,
    onStore: () -> Unit, onTheme: () -> Unit, onAdmin: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 254.dp)) {
            Box(Modifier.fillMaxWidth().height(220.dp).background(
                Brush.verticalGradient(listOf(colors.primaryContainer, colors.background))
            )) {
                if (coverUri.isNotBlank()) {
                    SmartServiceMediaView(imageUrl = coverUri, accentHex = 0xFF263D57,
                        secondaryHex = 0xFF142134, category = "", modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(Color.Transparent, colors.background)
                    )))
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = onSettings, shape = RoundedCornerShape(16.dp), color = colors.surface.copy(alpha = .95f)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Settings, "إعدادات الملف", tint = colors.onSurface)
                        }
                    }
                    Text("Layali", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                    Surface(onClick = onCover, shape = RoundedCornerShape(16.dp), color = colors.surface.copy(alpha = .95f)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AddPhotoAlternate, "تغيير صورة الغلاف", tint = colors.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Surface(onClick = onAvatar, shape = CircleShape, color = colors.surface,
                        border = BorderStroke(1.dp, colors.secondary.copy(alpha = .6f))) {
                        Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            FramedUserAvatar(avatarUri = profile.avatarUri,
                                fallbackInitial = title.take(1).ifBlank { "L" },
                                frameId = profile.selectedFrameId, level = profile.level,
                                avatarSize = 84.dp, showLevelBadge = false, isOnline = false)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                            color = colors.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (profile.cityName.isNotBlank() || profile.country.isNotBlank()) {
                            Text(listOf(profile.cityName, profile.country).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                        }
                        if (username.isNotBlank()) {
                            Surface(onClick = onCopy, shape = RoundedCornerShape(50), color = colors.surface) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(username, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Outlined.ContentCopy, "نسخ اسم المستخدم", Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                if (profile.bio.isNotBlank()) {
                    Text(profile.bio, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant)
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant), shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CalmProfileStat(bookings.toString(), "الحجوزات")
                    CalmProfileStat(services.toString(), "الخدمات")
                    CalmProfileStat(favorites.toString(), "المفضلة")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50)) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("تعديل الملف", fontSize = 16.sp)
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50), border = BorderStroke(1.dp, colors.outline)) {
                    Text("إعدادات الملف", fontSize = 16.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalmProfileShortcut("المفضلة", Icons.Outlined.FavoriteBorder, onFavorites, Modifier.weight(1f))
                CalmProfileShortcut("الحجز", Icons.Outlined.CalendarMonth, onStore, Modifier.weight(1f), selected = true)
                CalmProfileShortcut("محفظتي", Icons.Outlined.AccountBalanceWallet, onWallet, Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CalmProfileLink("حجوزاتي", Icons.Outlined.EventAvailable, onBookings)
                CalmProfileLink("الرسائل", Icons.Outlined.ChatBubbleOutline, onMessages)
                CalmProfileLink("الإطارات وإعدادات الحساب", Icons.Outlined.Style, onSettings)
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(if (light) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, null, tint = colors.secondary)
                        Text(if (light) "الوضع النهاري" else "الوضع الليلي", Modifier.weight(1f), fontSize = 17.sp)
                        Switch(checked = light, onCheckedChange = { onTheme() })
                    }
                }
                if (isAdmin) CalmProfileLink("إدارة التطبيق", Icons.Outlined.AdminPanelSettings, onAdmin)
            }
        }
    }
}

@Composable
private fun CalmProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalmProfileShortcut(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, selected: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(24.dp),
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant), shadowElevation = 2.dp) {
        Column(Modifier.heightIn(min = 104.dp).padding(vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(29.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CalmProfileLink(label: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = colors.onSurface, modifier = Modifier.size(26.dp))
            Text(label, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = colors.onSurfaceVariant)
        }
    }
}
