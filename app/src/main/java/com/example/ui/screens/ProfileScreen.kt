package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AvatarFrameCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.PublishedService
import com.example.data.model.UserProfile
import com.example.ui.components.BrandActionBadgesRow
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.HeroBannerCanvas
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.WingedHeartLogo
import com.example.ui.components.copyTextToClipboard
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnowGraySurface
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.TitleDarkText
import com.example.ui.theme.VioletBlueVip
import com.example.util.LayaliNotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    isGoogleSignedIn: Boolean,
    userEmail: String?,
    publishedServices: List<PublishedService>,
    bookingsCount: Int,
    reviewsCount: Int,
    favoritesCount: Int,
    blockedUsers: List<BlockedUser>,
    isSoftRoseLightTheme: Boolean,
    onToggleThemeMode: () -> Unit,
    onUpdateProfile: (displayName: String, bio: String, cityName: String, roleTitle: String, isAvailable: Boolean) -> Unit,
    onUpdateUsername: (String) -> Unit = {},
    onSelectAvatarFrame: (String) -> Unit = {},
    onUpdateChatPricing: (priceUsd: Int, isEnabled: Boolean) -> Unit = { _, _ -> },
    onGainProfileXp: (Int) -> Unit = {},
    onOpenChatTab: () -> Unit = {},
    onOpenWalletTab: () -> Unit = {},
    onUpdateAvatar: (String) -> Unit,
    onAddShowcasePhoto: (String) -> Unit,
    onOpenMediaViewer: (PublishedService, Boolean) -> Unit,
    onEditServicePrice: (PublishedService) -> Unit,
    onOpenAdminTab: () -> Unit,
    onOpenBookingsTab: () -> Unit,
    onOpenFavoritesTab: () -> Unit,
    onOpenExploreTab: () -> Unit,
    onQuickBlockUser: (identifier: String, reason: String) -> Unit,
    onToggleBlockStatus: (blockId: String, currentStatus: String) -> Unit,
    onTriggerBroadcastNotification: () -> Unit,
    onOpenGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    isSuperAdmin: Boolean = false
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var quickBlockInput by remember { mutableStateOf("") }
    var customChatPriceText by remember(userProfile.chatPriceUsd) {
        mutableStateOf(userProfile.chatPriceUsd.toString())
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateAvatar(uri.toString())
        }
    }

    val showcasePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAddShowcasePhoto(uri.toString())
        }
    }

    val activeFrame = remember(userProfile.selectedFrameId) {
        AvatarFrameCatalog.findById(userProfile.selectedFrameId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overhauled Royal Profile Hero Card (Framed Avatar + Level + @username + Stats)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, SovereignGold.copy(alpha = 0.55f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Top Hero Banner Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp)
                    ) {
                        HeroBannerCanvas(modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            CharcoalNight.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )

                        // Top Right & Left Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CharcoalNight.copy(alpha = 0.86f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SovereignGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WingedHeartLogo(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lv.${userProfile.level} • ${activeFrame.nameAr}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SovereignGold,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = { showEditProfileDialog = true },
                                color = SovereignGold,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = CharcoalNight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تعديل الملف و @الاسم",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CharcoalNight,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Framed Avatar + Display Name + @username + Bio + Stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_picker")
                        ) {
                            FramedUserAvatar(
                                avatarUri = userProfile.avatarUri,
                                fallbackInitial = userProfile.displayName.ifBlank { "V" },
                                frameId = userProfile.selectedFrameId,
                                level = userProfile.level,
                                avatarSize = 100.dp,
                                showLevelBadge = true,
                                isOnline = userProfile.isAvailableNow
                            )

                            Surface(
                                color = SovereignGold,
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, CharcoalNight),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تغيير صورة الملف الشخصي",
                                        tint = CharcoalNight,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val hasActiveAccount = isGoogleSignedIn || isSuperAdmin || userProfile.displayName.isNotBlank()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasActiveAccount) {
                                    userProfile.displayName.ifBlank { userEmail?.substringBefore("@") ?: "عضو ليالي الملكي" }
                                } else {
                                    "عضو نادي ليالي الملكي VIP"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثق",
                                tint = VioletBlueVip,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dedicated @username Pill Badge ("اجعل هناك اسم مستخدم")
                        val formattedUsername = userProfile.username.ifBlank { "@layali_vip" }.let {
                            if (it.startsWith("@")) it else "@$it"
                        }
                        Surface(
                            onClick = {
                                copyTextToClipboard(context, "Username", formattedUsername)
                            },
                            color = SovereignGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("profile_username_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ اسم المستخدم",
                                    tint = SovereignGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formattedUsername,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = SovereignGold,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Paid Chat Price Badge + Role Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = onOpenChatTab,
                                color = VioletBlueVip.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, VioletBlueVip)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = VioletBlueVip,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "سعر الدردشة معي: $${userProfile.chatPriceUsd}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VioletBlueVip,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (userProfile.cityName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = SovereignGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = userProfile.cityName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SubtitleGrayText
                                    )
                                }
                            }
                        }

                        if (userProfile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = userProfile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SubtitleGrayText,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Availability Switch Row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(if (userProfile.isAvailableNow) EmeraldVerified else ErrorCrimson)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (userProfile.isAvailableNow) {
                                                "الحالة: متصل الآن للدردشة والحجوزات"
                                            } else {
                                                "الحالة: الحجز المسبق فقط"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "اسم المستخدم: $formattedUsername • إطار: ${activeFrame.nameAr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SubtitleGrayText
                                        )
                                    }
                                }

                                Switch(
                                    checked = userProfile.isAvailableNow,
                                    onCheckedChange = { checked ->
                                        onUpdateProfile(
                                            userProfile.displayName,
                                            userProfile.bio,
                                            userProfile.cityName,
                                            userProfile.roleTitle,
                                            checked
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = EmeraldVerified
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Profile Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileStatBox(
                                modifier = Modifier.weight(1f),
                                countLabel = "Lv.${userProfile.level}",
                                title = "المستوى",
                                onClick = { onGainProfileXp(250) }
                            )
                            ProfileStatBox(
                                modifier = Modifier.weight(1f),
                                countLabel = "$bookingsCount",
                                title = "حجوزاتي",
                                onClick = onOpenBookingsTab
                            )
                            ProfileStatBox(
                                modifier = Modifier.weight(1f),
                                countLabel = "$favoritesCount",
                                title = "المفضلة",
                                onClick = onOpenFavoritesTab
                            )
                            ProfileStatBox(
                                modifier = Modifier.weight(1f),
                                countLabel = "$reviewsCount",
                                title = "تقييماتي",
                                onClick = onOpenExploreTab
                            )
                        }

                        if (isSuperAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LuxuryGradientButton(
                                text = "فتح لوحة التحكم الإدارية (Super Admin)",
                                icon = Icons.Default.AdminPanelSettings,
                                onClick = onOpenAdminTab,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "open_super_admin_dashboard_button"
                            )
                        }
                    }
                }
            }
        }

        // 2. Profile Level & XP Progression Card ("اجعل لفل واطارات للملف الشخصي")
        item {
            val progressRatio = (userProfile.xpPoints.toFloat() / userProfile.nextLevelXp.coerceAtLeast(1).toFloat())
                .coerceIn(0.08f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("profile_level_xp_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SovereignGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SovereignGold)
                        ) {
                            Text(
                                text = "${userProfile.xpPoints} / ${userProfile.nextLevelXp} XP",
                                style = MaterialTheme.typography.labelLarge,
                                color = SovereignGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مستوى الحساب الملكي (Lv.${userProfile.level})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SovereignGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = SovereignGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onGainProfileXp(300) },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SovereignGold),
                            modifier = Modifier.testTag("boost_profile_xp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SovereignGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ترقية المستوى (+300 XP)",
                                color = SovereignGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "كل حجز أو إيداع موثق يرفع لفل حسابك",
                            style = MaterialTheme.typography.labelMedium,
                            color = SubtitleGrayText
                        )
                    }
                }
            }
        }

        // 3. Avatar Frames Studio ("اجعل لفل واطارات للملف الشخصي")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("profile_avatar_frames_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = VioletBlueVip.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${AvatarFrameCatalog.frames.size} إطارات ملكية",
                                style = MaterialTheme.typography.labelMedium,
                                color = VioletBlueVip,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "إطارات الملف الشخصي الملكية",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "اختر الإطار الذي يظهر حول صورتك في الملف الشخصي والدردشة والريلز:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtitleGrayText
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(AvatarFrameCatalog.frames, key = { it.id }) { frameItem ->
                            val isSelected = userProfile.selectedFrameId == frameItem.id
                            val framePrimary = Color(frameItem.primaryHex)

                            Surface(
                                onClick = { onSelectAvatarFrame(frameItem.id) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) {
                                    framePrimary.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) framePrimary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .width(146.dp)
                                    .testTag("select_avatar_frame_${frameItem.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FramedUserAvatar(
                                        avatarUri = userProfile.avatarUri,
                                        fallbackInitial = userProfile.displayName.ifBlank { "V" },
                                        frameId = frameItem.id,
                                        level = frameItem.minLevel.coerceAtLeast(userProfile.level),
                                        avatarSize = 62.dp,
                                        showLevelBadge = true
                                    )

                                    Text(
                                        text = frameItem.nameAr,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        color = if (isSelected) framePrimary else CharcoalNight,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isSelected) "مفعّل الآن ✓" else "تفعيل الإطار",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) CharcoalNight else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Paid Chat Pricing Configuration Card ("اجعل بإمكان اي شخص تسعير الدردشه معه")
        item {
            val presetChatPrices = listOf(5, 10, 15, 25, 50, 100)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("profile_chat_pricing_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, VioletBlueVip.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = userProfile.isPaidChatEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateChatPricing(userProfile.chatPriceUsd, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldVerified
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "تسعير الدردشة الخاصة معي ($)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = SovereignGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = "يمكن لأي شخص تحديد سعر فتح الدردشة الخاصة معه. السعر الحالي لحسابك (${userProfile.username}): $${userProfile.chatPriceUsd}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtitleGrayText
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetChatPrices.forEach { price ->
                            val isSelected = userProfile.chatPriceUsd == price
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    customChatPriceText = price.toString()
                                    onUpdateChatPricing(price, true)
                                },
                                label = {
                                    Text(
                                        text = "$$price",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold,
                                    selectedLabelColor = CharcoalNight
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val parsed = (customChatPriceText.toIntOrNull() ?: 15).coerceIn(1, 5000)
                                onUpdateChatPricing(parsed, true)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignGold,
                                contentColor = CharcoalNight
                            ),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("حفظ السعر", fontWeight = FontWeight.ExtraBold)
                        }

                        OutlinedTextField(
                            value = customChatPriceText,
                            onValueChange = { input ->
                                customChatPriceText = input.filter { it.isDigit() }
                            },
                            label = { Text("سعر مخصص للدردشة ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LuxuryGradientButton(
                            text = "فتح قسم الدردشة",
                            icon = Icons.Default.Chat,
                            isEmeraldStyle = true,
                            onClick = onOpenChatTab,
                            modifier = Modifier.weight(1f)
                        )
                        LuxuryGradientButton(
                            text = "شام كاش & USDT",
                            icon = Icons.Default.AttachMoney,
                            isEmeraldStyle = false,
                            onClick = onOpenWalletTab,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Quick Access Badges Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الوصول السريع وخدمات التواصل المباشر",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BrandActionBadgesRow(
                        onFavoritesClick = onOpenFavoritesTab,
                        onSupportChatClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                        onDirectCallClick = onOpenChatTab,
                        onCitiesClick = onOpenExploreTab
                    )
                }
            }
        }

        // 6. Super Admin Showcase inside Profile
        if (isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "معرض الصور والفيديو والأسعار (Super Admin)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط على أي عرض لمشاهدة الفيديو 4K أو تعديل السعر",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SubtitleGrayText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showcasePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SovereignGold,
                                    contentColor = CharcoalNight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة صورة", fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onOpenAdminTab,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, VioletBlueVip),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = VioletBlueVip,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نشر عرض جديد",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        if (userProfile.customPhotos.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(userProfile.customPhotos) { photoUri ->
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    ) {
                                        SmartServiceMediaView(
                                            imageUrl = photoUri,
                                            accentHex = 0xFFF59E0B,
                                            secondaryHex = 0xFF09111E,
                                            category = "صورة مرفوعة",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(publishedServices, key = { "prof_pub_${it.id}" }) { service ->
                                Card(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .clickable { onOpenMediaViewer(service, true) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(124.dp)
                                        ) {
                                            SmartServiceMediaView(
                                                imageUrl = service.imageUrl,
                                                accentHex = 0xFFF59E0B,
                                                secondaryHex = 0xFF09111E,
                                                category = service.category,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                color = SovereignGold,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircleFilled,
                                                        contentDescription = null,
                                                        tint = CharcoalNight,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "4K",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CharcoalNight,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = { onEditServicePrice(service) },
                                                color = CharcoalNight.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$${service.priceUsd}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = SovereignGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل السعر",
                                                        tint = VioletBlueVip,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = service.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${service.cityName} • ${service.category}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SubtitleGrayText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Theme Mode Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "نمط الهوية البصرية (لؤلؤي نهاري / ياقوتي ليلي)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر بين الهوية اللؤلؤية الدافئة أو الكحلي الملكي المرصع بالذهب والزمرد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtitleGrayText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { if (!isSoftRoseLightTheme) onToggleThemeMode() },
                            color = PureWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSoftRoseLightTheme) 2.dp else 1.dp,
                                color = if (isSoftRoseLightTheme) SovereignGold else LightBorderGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = CoralPinkPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اللؤلؤي الذهبي",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TitleDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Royal Pearl Light",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SubtitleGrayText
                                )
                            }
                        }

                        Surface(
                            onClick = { if (isSoftRoseLightTheme) onToggleThemeMode() },
                            color = CharcoalNight,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (!isSoftRoseLightTheme) 2.dp else 1.dp,
                                color = if (!isSoftRoseLightTheme) SovereignGold else Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = SovereignGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "الياقوتي الليلي",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Midnight Sapphire VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignGold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Super Admin Quick Block & Broadcast Card
        if (isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = ErrorCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الحظر السريع والإشعارات الفورية",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = quickBlockInput,
                                onValueChange = { quickBlockInput = it },
                                placeholder = { Text("اسم المستخدم لحظره...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (quickBlockInput.trim().isNotEmpty()) {
                                        onQuickBlockUser(quickBlockInput.trim(), "حظر مباشر من الملف الشخصي")
                                        quickBlockInput = ""
                                    }
                                },
                                enabled = quickBlockInput.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("حظر", maxLines = 1)
                            }
                        }

                        if (blockedUsers.isNotEmpty()) {
                            blockedUsers.take(4).forEach { blocked ->
                                val isBlocked = blocked.status == "active"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${blocked.blockedIdentifier} (${if (isBlocked) "محظور" else "غير محظور"})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isBlocked) ErrorCrimson else EmeraldVerified
                                    )
                                    TextButton(onClick = { onToggleBlockStatus(blocked.id, blocked.status) }) {
                                        Text(
                                            text = if (isBlocked) "فك الحظر" else "إعادة الحظر",
                                            color = VioletBlueVip
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SovereignGold,
                                    contentColor = CharcoalNight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دعم @RamaNUR", fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onTriggerBroadcastNotification,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, VioletBlueVip),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = VioletBlueVip,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بث إشعار", color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // 9. Account & Authentication Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SovereignGold,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isSuperAdmin) {
                                        "حساب المسؤول الرئيسي (${userEmail ?: "RamaNUR@gmail.com"})"
                                    } else if (isGoogleSignedIn) {
                                        "حساب متصل (${userEmail ?: "مزامنة سحابية نشطة"})"
                                    } else {
                                        "وضع تصفح العملاء VIP"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSuperAdmin) {
                                        "صلاحية Super Admin مفعلة بالكامل"
                                    } else {
                                        "سجل دخولك لمزامنة الحجوزات والمحفظة"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SubtitleGrayText
                                )
                            }
                        }

                        if (isGoogleSignedIn || isSuperAdmin) {
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, ErrorCrimson)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خروج", color = ErrorCrimson, maxLines = 1)
                            }
                        }
                    }

                    LuxuryGradientButton(
                        text = if (isGoogleSignedIn || isSuperAdmin) {
                            "تبديل الحساب / تسجيل الدخول بحساب آخر"
                        } else {
                            "تسجيل الدخول بالبريد الإلكتروني وكلمة المرور"
                        },
                        icon = Icons.Default.CheckCircle,
                        onClick = onOpenGoogleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_open_auth_screen_button"
                    )
                }
            }
        }
    }

    // Complete Profile & @username Edit Dialog
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(userProfile.displayName) }
        var usernameInput by remember { mutableStateOf(userProfile.username.ifBlank { "@layali_vip" }) }
        var roleInput by remember { mutableStateOf(userProfile.roleTitle) }
        var cityInput by remember { mutableStateOf(userProfile.cityName) }
        var bioInput by remember { mutableStateOf(userProfile.bio) }
        var chatPriceInput by remember { mutableStateOf(userProfile.chatPriceUsd.toString()) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل الملف الشخصي واسم المستخدم",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم الظاهر في الملف الشخصي") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم المستخدم (@username)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username_input")
                    )
                    OutlinedTextField(
                        value = chatPriceInput,
                        onValueChange = { chatPriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("سعر الدردشة معي بالدولار ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("المدينة (حلب، دمشق، إسطنبول...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("اللقب أو الوصف المختصر") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("نبذة شخصية") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(
                            nameInput,
                            bioInput,
                            cityInput,
                            roleInput,
                            userProfile.isAvailableNow
                        )
                        onUpdateUsername(usernameInput)
                        val parsedPrice = (chatPriceInput.toIntOrNull() ?: userProfile.chatPriceUsd).coerceIn(1, 5000)
                        onUpdateChatPricing(parsedPrice, userProfile.isPaidChatEnabled)
                        showEditProfileDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignGold,
                        contentColor = CharcoalNight
                    )
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء", color = SubtitleGrayText)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatBox(
    modifier: Modifier = Modifier,
    countLabel: String,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.titleLarge,
                color = SovereignGold,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
