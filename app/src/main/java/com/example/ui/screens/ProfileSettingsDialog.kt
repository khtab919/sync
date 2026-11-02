package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarFrameCatalog
import com.example.data.model.UserProfile
import com.example.ui.components.FramedUserAvatar
import com.example.ui.theme.DeepTealPrimary

private enum class ProfileSettingsPage(val title: String) {
    HOME("إعدادات الحساب"),
    CHAT("الرسائل والتسعير"),
    FRAMES("إطارات الملف الشخصي"),
    APPEARANCE("المظهر"),
    ACCOUNT("الحساب والأمان")
}

@Composable
fun ProfileSettingsDialog(
    userProfile: UserProfile,
    lightTheme: Boolean,
    isSignedIn: Boolean,
    isAdmin: Boolean,
    walletBalanceUsd: Int,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onToggleTheme: () -> Unit,
    onUpdateChatPricing: (Int, Boolean) -> Unit,
    onSelectFrame: (String) -> Unit,
    onOpenWallet: () -> Unit,
    onChangePassword: () -> Unit,
    onSignOut: () -> Unit
) {
    var page by remember { mutableStateOf(ProfileSettingsPage.HOME) }
    var priceText by remember(userProfile.chatPriceUsd) {
        mutableStateOf(userProfile.chatPriceUsd.toString())
    }
    var paidChat by remember(userProfile.isPaidChatEnabled) {
        mutableStateOf(userProfile.isPaidChatEnabled)
    }
    val price = priceText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page != ProfileSettingsPage.HOME) {
                    IconButton(onClick = { page = ProfileSettingsPage.HOME }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة")
                    }
                }
                Text(page.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (page) {
                    ProfileSettingsPage.HOME -> {
                        item {
                            Text(
                                "إعدادات واضحة ومنفصلة حتى لا تتزاحم الخيارات في صفحة الملف الشخصي.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        item { SettingsEntry("المعلومات الشخصية", "الاسم، النبذة، المدينة واسم المستخدم", Icons.Default.Edit) { onDismiss(); onEditProfile() } }
                        item { SettingsEntry("الرسائل المدفوعة", if (userProfile.isPaidChatEnabled) "$${userProfile.chatPriceUsd} لفتح المحادثة" else "الدردشة مجانية", Icons.Default.ChatBubbleOutline) { page = ProfileSettingsPage.CHAT } }
                        item { SettingsEntry("الإطارات والمستويات", "ستة تصاميم متدرجة لكل مستوى", Icons.Default.AutoAwesome) { page = ProfileSettingsPage.FRAMES } }
                        item { SettingsEntry("المظهر", if(lightTheme) "الوضع النهاري" else "الوضع الليلي", Icons.Default.Palette) { page = ProfileSettingsPage.APPEARANCE } }
                        item { SettingsEntry("المحفظة", "رصيدك الحالي $${walletBalanceUsd}", Icons.Default.AccountBalanceWallet) { onDismiss(); onOpenWallet() } }
                        item { SettingsEntry("الحساب والأمان", "كلمة المرور وتسجيل الخروج", Icons.Default.Lock) { page = ProfileSettingsPage.ACCOUNT } }
                    }
                    ProfileSettingsPage.CHAT -> {
                        item {
                            Text("حددي مبلغاً ثابتاً لفتح المحادثة معك. يظهر السعر قبل محاولة المراسلة.", style = MaterialTheme.typography.bodyMedium)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = paidChat, onCheckedChange = { paidChat = it }, modifier = Modifier.testTag("settings_paid_chat_switch"))
                                Text("تفعيل الرسائل المدفوعة", fontWeight = FontWeight.Medium)
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it.filter(Char::isDigit).take(4) },
                                label = { Text("سعر فتح الدردشة بالدولار") },
                                supportingText = { Text("من 1 إلى 5000 دولار • مرة واحدة عند فتح المحادثة") },
                                isError = paidChat && (price == null || price !in 1..5000),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("settings_chat_price_input")
                            )
                        }
                        item {
                            Text(
                                "يجب تأكيد الدفع من الخادم قبل فتح الرسائل. رفع صورة إيصال وحده لا يعني نجاح الدفع.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    ProfileSettingsPage.FRAMES -> {
                        item {
                            Text("تتطور أشكال الإطارات مع المستوى: معدني، وردي، هندسي، نجمي، متوهج ثم ألماسي. الحركة مخصصة للعرض الكبير لتوفير البطارية.", style = MaterialTheme.typography.bodyMedium)
                        }
                        items(AvatarFrameCatalog.frames, key = { it.id }) { frame ->
                            val unlocked = isAdmin || userProfile.level >= frame.minLevel
                            val selected = frame.id == userProfile.selectedFrameId
                            Surface(
                                onClick = { if (unlocked) onSelectFrame(frame.id) },
                                enabled = unlocked,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(if(selected) 1.6.dp else 0.8.dp, if(selected) Color(frame.accentHex) else MaterialTheme.colorScheme.outlineVariant),
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth().testTag("select_avatar_frame_${frame.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FramedUserAvatar(
                                        avatarUri = userProfile.avatarUri,
                                        fallbackInitial = userProfile.displayName.take(1),
                                        frameId = frame.id,
                                        level = frame.minLevel,
                                        avatarSize = 46.dp,
                                        showLevelBadge = false
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(frame.nameAr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        Text(frame.subtitleAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if(selected) "مفعّل" else if (unlocked) "متاح للتفعيل" else "يفتح في المستوى ${frame.minLevel}", fontSize = 12.sp, color = Color(frame.primaryHex))
                                    }
                                }
                            }
                        }
                    }
                    ProfileSettingsPage.APPEARANCE -> {
                        item {
                            Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = lightTheme, onCheckedChange = { onToggleTheme() }, modifier = Modifier.testTag("settings_theme_switch"))
                                Column {
                                    Text("الوضع النهاري", fontWeight = FontWeight.Medium)
                                    Text(if(lightTheme) "مفعّل" else "الوضع الليلي مفعّل", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    ProfileSettingsPage.ACCOUNT -> {
                        item { SettingsEntry("تعديل الملف الشخصي", "تحديث البيانات العامة", Icons.Default.Edit) { onDismiss(); onEditProfile() } }
                        item { SettingsEntry("كلمة المرور والنسخ المحلي", "إعدادات حماية الحساب", Icons.Default.Lock) { onDismiss(); onChangePassword() } }
                        if (isSignedIn) {
                            item {
                                HorizontalDivider()
                                TextButton(onClick = { onDismiss(); onSignOut() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("تسجيل الخروج", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (page == ProfileSettingsPage.CHAT) {
                Button(
                    onClick = {
                        onUpdateChatPricing((price ?: userProfile.chatPriceUsd).coerceIn(1, 5000), paidChat)
                        page = ProfileSettingsPage.HOME
                    },
                    enabled = !paidChat || (price != null && price in 1..5000),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTealPrimary)
                ) { Text("حفظ تسعير الرسائل") }
            } else {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        }
    )
}

@Composable
private fun SettingsEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
