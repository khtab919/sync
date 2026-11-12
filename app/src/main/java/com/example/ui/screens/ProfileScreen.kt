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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RegisteredAccountEntity
import com.example.data.model.AvatarFrameCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.PublishedService
import com.example.data.model.UserProfile
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.copyTextToClipboard
import com.example.ui.theme.DeepTealPrimary
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.MutedLuxuryGold
import com.example.ui.theme.RoyalNavyBackground
import com.example.ui.theme.RoyalNavyCard
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.SoftWhiteText
import com.example.ui.viewmodel.WalletTransaction

private enum class ProfileMediaTab(
    val label: String,
    val icon: ImageVector
) {
    REELS("Reels", Icons.Default.PlayArrow),
    VIDEOS("Videos", Icons.Default.Videocam),
    PHOTOS("Photos", Icons.Default.GridOn),
    EXCLUSIVE("Exclusive", Icons.Default.AutoAwesome),
    POSTS("منشورات", Icons.Default.Lock)
}

private data class ProfileMediaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val tab: ProfileMediaTab,
    val viewsLabel: String,
    val durationLabel: String,
    val imageUrl: String,
    val category: String,
    val singlePriceUsd: Int,
    val isExclusiveLocked: Boolean,
    val accentHex: Long,
    val secondaryHex: Long,
    val sourcePublished: PublishedService? = null
)

private const val VIP_MONTHLY_SUBSCRIPTION_USD = 29

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    walletBalanceUsd: Int = 250,
    walletTransactions: List<WalletTransaction> = emptyList(),
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
    onPurchaseContentWithWallet: (amountUsd: Int, itemTitle: String) -> Boolean = { _, _ -> false },
    onPublishPaidPost: (title: String, preview: String, body: String, priceUsd: Int) -> Unit = { _, _, _, _ -> },
    onShowMessage: (String) -> Unit = {},
    isSuperAdmin: Boolean = false,
    savedAccounts: List<RegisteredAccountEntity> = emptyList(),
    lastInfrastructureSyncLabel: String = "محفوظ في SQLite + السحابة",
    onSaveInfrastructureNow: () -> Unit = {},
    onChangeAccountPassword: (currentPassword: String, newPassword: String, requireCurrent: Boolean) -> Boolean = { _, _, _ -> true }
) {
    val context = LocalContext.current

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showPaidPostDialog by remember { mutableStateOf(false) }
    var showSubscribeVipModal by remember { mutableStateOf(false) }
    var showPasswordAndDbModal by remember { mutableStateOf(false) }
    var selectedLockedItemForPurchase by remember { mutableStateOf<ProfileMediaItem?>(null) }

    var isFollowing by rememberSaveable { mutableStateOf(false) }
    var isSubscribedVip by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(ProfileMediaTab.REELS) }
    var coverImageUri by rememberSaveable { mutableStateOf("aleppo_palace_nights") }

    val unlockedVideoIds = remember { mutableStateListOf<String>() }
    val pinnedClipIds = remember {
        mutableStateListOf<String>()
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateAvatar(uri.toString())
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coverImageUri = uri.toString()
        }
    }

    val showcasePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAddShowcasePhoto(uri.toString())
        }
    }

    // Build curated media items across Reels, Videos, Photos, and Exclusive tabs
    val allProfileMedia = remember(publishedServices, userProfile.customPhotos) {
        val dynamicReels = publishedServices.filter { it.category != "منشور مدفوع" }.mapIndexed { idx, pub ->
            ProfileMediaItem(
                id = "pub_reel_${pub.id}",
                title = pub.title,
                subtitle = pub.cityName,
                tab = ProfileMediaTab.REELS,
                viewsLabel = "—",
                durationLabel = "00:45",
                imageUrl = pub.imageUrl,
                category = pub.category,
                singlePriceUsd = pub.priceUsd.toInt().coerceAtLeast(15),
                isExclusiveLocked = false,
                accentHex = 0xFF792D45L,
                secondaryHex = 0xFF4C2037L,
                sourcePublished = pub
            )
        }

        val paidPosts = publishedServices.filter { it.category == "منشور مدفوع" }.map { pub ->
            ProfileMediaItem(
                id = "paid_post_${pub.id}",
                title = pub.title,
                subtitle = "معاينة • ${pub.priceUsd}",
                tab = ProfileMediaTab.POSTS,
                viewsLabel = "خاص",
                durationLabel = "مقفل",
                imageUrl = pub.imageUrl,
                category = pub.category,
                singlePriceUsd = pub.priceUsd.toInt(),
                isExclusiveLocked = true,
                accentHex = 0xFFD7AD72L,
                secondaryHex = 0xFF4C2037L,
                sourcePublished = pub
            )
        }

        val customPhotos = userProfile.customPhotos.mapIndexed { idx, uriStr ->
            ProfileMediaItem(
                id = "custom_photo_$idx",
                title = "صورة مضافة #${idx + 1}",
                subtitle = userProfile.cityName,
                tab = ProfileMediaTab.PHOTOS,
                viewsLabel = "—",
                durationLabel = "صورة",
                imageUrl = uriStr,
                category = "سهرات فنية",
                singlePriceUsd = 0,
                isExclusiveLocked = false,
                accentHex = 0xFF792D45L,
                secondaryHex = 0xFF4C2037L
            )
        }

        dynamicReels + paidPosts + customPhotos
    }

    val pinnedItems = remember(allProfileMedia, pinnedClipIds.toList()) {
        pinnedClipIds.mapNotNull { id -> allProfileMedia.find { it.id == id } }.take(3)
    }

    val currentTabItems = remember(allProfileMedia, selectedTab) {
        allProfileMedia.filter { it.tab == selectedTab }
    }

    val formattedUsername = userProfile.username.ifBlank { "@layla_vip" }.let {
        if (it.startsWith("@")) it else "@$it"
    }

    val hasActiveAccount = isGoogleSignedIn || isSuperAdmin || userProfile.displayName.isNotBlank()
    val displayTitle = if (hasActiveAccount) {
        userProfile.displayName.substringBefore("•").trim().ifBlank {
            userEmail?.substringBefore("@") ?: "ليلى الملكية"
        }
    } else {
        "ليلى • Layla VIP"
    }

    val handleOpenOrUnlockMedia: (ProfileMediaItem) -> Unit = { item ->
        if (item.tab == ProfileMediaTab.POSTS) {
            // Never charge a wallet locally or expose private body data.
            onShowMessage("هذه معاينة لمنشور مدفوع. فتح المحتوى متوقف حتى تفعيل التحقق من الدفع عبر الخادم.")
        } else {
        val isUnlocked = !item.isExclusiveLocked || isSubscribedVip || unlockedVideoIds.contains(item.id)
        if (isUnlocked) {
            val pub = item.sourcePublished ?: PublishedService(
                id = item.id,
                title = item.title,
                cityName = item.subtitle,
                country = "سوريا",
                category = item.category,
                description = item.title,
                priceUsd = item.singlePriceUsd.toLong().coerceAtLeast(50L),
                imageUrl = item.imageUrl,
                videoUrl = item.title,
                mediaType = if (item.tab == ProfileMediaTab.PHOTOS) "image" else "both"
            )
            onOpenMediaViewer(pub, item.tab != ProfileMediaTab.PHOTOS)
        } else {
            selectedLockedItemForPurchase = item
        }
        }
    }

    val handleTogglePin: (ProfileMediaItem) -> Unit = { item ->
        if (pinnedClipIds.contains(item.id)) {
            pinnedClipIds.remove(item.id)
            onShowMessage("تم إلغاء تثبيت المقطع")
        } else {
            if (pinnedClipIds.size >= 3) {
                pinnedClipIds.removeAt(pinnedClipIds.lastIndex)
            }
            pinnedClipIds.add(0, item.id)
            onShowMessage("تم تثبيت المقطع في أفضل 3 مقاطع 📌")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalNavyBackground)
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // ====================================================================
        // 1. COVER PHOTO + OVERLAPPING AVATAR + NAME + GOLDEN LEVEL BADGE + BIO
        // ====================================================================
        item(key = "profile_header_section") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(182.dp)
                ) {
                    // Cover Banner Image (138.dp height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                            .clickable {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        SmartServiceMediaView(
                            imageUrl = coverImageUri,
                            accentHex = 0xFF792D45L,
                            secondaryHex = 0xFF4C2037L,
                            category = "سهرات فنية",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Smooth Cover Vignette
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            RoyalNavyBackground.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            RoyalNavyBackground
                                        )
                                    )
                                )
                        )

                        // Top Action Icons Over Cover (Small, balanced, non-crowded)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Settings & Account Gear Icon
                                IconButton(
                                    onClick = { showSettingsModal = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(RoyalNavyBackground.copy(alpha = 0.6f))
                                        .testTag("profile_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "الإعدادات",
                                        tint = SoftWhiteText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Edit Profile Icon
                                IconButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(RoyalNavyBackground.copy(alpha = 0.6f))
                                        .testTag("edit_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الملف",
                                        tint = SoftWhiteText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Compact Wallet Pill in Cover Corner
                                Surface(
                                    onClick = onOpenWalletTab,
                                    color = RoyalNavyBackground.copy(alpha = 0.68f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.8.dp, MutedLuxuryGold.copy(alpha = 0.55f)),
                                    modifier = Modifier.testTag("profile_independent_wallet_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MutedLuxuryGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$$walletBalanceUsd",
                                            fontSize = 11.sp,
                                            color = SoftWhiteText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Cover Photo Change Pill
                            Surface(
                                onClick = {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                color = RoyalNavyBackground.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تغيير الغلاف",
                                        tint = SoftWhiteText.copy(alpha = 0.85f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الغلاف",
                                        fontSize = 10.sp,
                                        color = SoftWhiteText.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Overlapping Centered Profile Avatar with Luxury Multi-Layer Jewel Frame
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_picker")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(RoyalNavyBackground)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FramedUserAvatar(
                                avatarUri = userProfile.avatarUri,
                                fallbackInitial = displayTitle.take(1).ifBlank { "L" },
                                frameId = userProfile.selectedFrameId,
                                level = userProfile.level,
                                avatarSize = 72.dp,
                                showLevelBadge = true,
                                isOnline = userProfile.isAvailableNow
                            )
                        }

                        Surface(
                            color = DeepTealPrimary,
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, RoyalNavyBackground),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "تغيير الصورة",
                                    tint = SoftWhiteText,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name + Royal Amethyst LEVEL Badge (Hard-earned prestige badge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    val activeFrame = remember(userProfile.selectedFrameId) {
                        AvatarFrameCatalog.findById(userProfile.selectedFrameId)
                    }
                    // Royal Prestige LEVEL Badge
                    Surface(
                        onClick = {
                            onShowMessage(
                                "المستوى الحالي LEVEL ${userProfile.level} (${userProfile.xpPoints}/${userProfile.nextLevelXp} XP) • يرتفع المستوى فقط عبر الحجوزات الموثقة وشحن الرصيد"
                            )
                        },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(activeFrame.accentHex)),
                        modifier = Modifier.testTag("boost_profile_xp_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(activeFrame.primaryHex).copy(alpha = 0.32f),
                                            Color(activeFrame.secondaryHex).copy(alpha = 0.32f)
                                        )
                                    )
                                )
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(activeFrame.accentHex),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LEVEL ${userProfile.level} • ${activeFrame.badgeLabel}",
                                fontSize = 10.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = displayTitle,
                        fontSize = 16.sp,
                        color = SoftWhiteText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // @username + City
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clickable { copyTextToClipboard(context, "Username", formattedUsername) }
                        .testTag("profile_username_badge")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = SlateSecondaryText,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formattedUsername,
                        fontSize = 12.sp,
                        color = SlateSecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    if (userProfile.cityName.isNotBlank()) {
                        Text(
                            text = "• ${userProfile.cityName.substringBefore("•").trim()}",
                            fontSize = 11.sp,
                            color = SlateSecondaryText
                        )
                    }
                }

                // Concise Bio
                val bioText = userProfile.bio.ifBlank {
                    "صانعة محتوى وسهرات VIP حصرية ✨ • حجوزات خاصة ومقاطع 4K"
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bioText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = SoftWhiteText.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ====================================================================
                // ====================================================================
                // 2. REAL STATS ROW: FOLLOWERS, FOLLOWING, REELS, OFFERS (إحصائيات حقيقية بالكامل)
                // ====================================================================
                val followersDisplay = "—" // No verified follower count in this legacy profile model
                val reelsDisplay = "—" // Services are not reels; never miscount them
                val offersTotalCount = publishedServices.count { it.category != "منشور مدفوع" }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("profile_level_xp_card"),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalStatItem(
                        value = followersDisplay,
                        label = "المتابعون"
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    )

                    MinimalStatItem(
                        value = "—",
                        label = "أتابع"
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    )

                    MinimalStatItem(
                        value = reelsDisplay,
                        label = "الريلز"
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    )

                    MinimalStatItem(
                        value = offersTotalCount.toString(),
                        label = "العروض"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ====================================================================
                // 3. ACTION BUTTONS: FOLLOW, SUBSCRIBE, MESSAGE (أزرار المتابعة والاشتراك والمراسلة)
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Message Button (مراسلة)
                    Surface(
                        onClick = onOpenChatTab,
                        color = RoyalNavyCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(36.dp)
                            .testTag("profile_message_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = SoftWhiteText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "مراسلة",
                                fontSize = 12.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    // Subscribe VIP Button (اشتراك)
                    Surface(
                        onClick = { showSubscribeVipModal = true },
                        color = if (isSubscribedVip) MutedLuxuryGold.copy(alpha = 0.22f) else MutedLuxuryGold,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MutedLuxuryGold),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(36.dp)
                            .testTag("profile_subscribe_vip_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSubscribedVip) MutedLuxuryGold else RoyalNavyBackground,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSubscribedVip) "مشترك VIP ✓" else "اشتراك • $$VIP_MONTHLY_SUBSCRIPTION_USD",
                                fontSize = 11.sp,
                                color = if (isSubscribedVip) MutedLuxuryGold else RoyalNavyBackground,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Follow Button (متابعة)
                    Surface(
                        onClick = {
                            isFollowing = !isFollowing
                            onShowMessage(if (isFollowing) "تمت المتابعة بنجاح ✓" else "تم إلغاء المتابعة")
                        },
                        color = if (isFollowing) RoyalNavyCard else DeepTealPrimary,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFollowing) DeepTealPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(36.dp)
                            .testTag("profile_follow_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (isFollowing) DeepTealPrimary else SoftWhiteText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFollowing) "متابَع" else "متابعة",
                                fontSize = 12.sp,
                                color = if (isFollowing) DeepTealPrimary else SoftWhiteText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ====================================================================
                // 3.5. LUXURY AVATAR FRAMES & HARD-EARNED LEVEL PROGRESSION CARD
                // ====================================================================
                val xpRatio = (userProfile.xpPoints.toFloat() / userProfile.nextLevelXp.coerceAtLeast(1).toFloat())
                    .coerceIn(0.03f, 1f)
                val remainingXp = (userProfile.nextLevelXp - userProfile.xpPoints).coerceAtLeast(0)

                Surface(
                    color = RoyalNavyCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, MutedLuxuryGold.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Top Row: XP Remaining + Level Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${userProfile.xpPoints} / ${userProfile.nextLevelXp} XP (متبقي $remainingXp XP)",
                                fontSize = 10.sp,
                                color = SlateSecondaryText,
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "الإطارات الملكية الفاخرة • LEVEL ${userProfile.level}",
                                    fontSize = 11.sp,
                                    color = SoftWhiteText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MutedLuxuryGold,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sleek Amethyst & Sapphire XP Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(RoyalNavyBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(xpRatio)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(DeepTealPrimary, MutedLuxuryGold)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Luxury Frames Showcase
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AvatarFrameCatalog.frames, key = { it.id }) { frameStyle ->
                                val isSelected = userProfile.selectedFrameId == frameStyle.id
                                val isUnlocked = isSuperAdmin || userProfile.level >= frameStyle.minLevel
                                val framePrimary = Color(frameStyle.primaryHex)
                                val frameAccent = Color(frameStyle.accentHex)

                                Surface(
                                    onClick = { onSelectAvatarFrame(frameStyle.id) },
                                    color = if (isSelected) {
                                        framePrimary.copy(alpha = 0.18f)
                                    } else {
                                        RoyalNavyBackground.copy(alpha = 0.75f)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.3.dp else 0.7.dp,
                                        color = if (isSelected) {
                                            frameAccent
                                        } else if (isUnlocked) {
                                            framePrimary.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    ),
                                    modifier = Modifier.width(104.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        FramedUserAvatar(
                                            avatarUri = userProfile.avatarUri,
                                            fallbackInitial = displayTitle.take(1).ifBlank { "L" },
                                            frameId = frameStyle.id,
                                            level = frameStyle.minLevel,
                                            avatarSize = 36.dp,
                                            showLevelBadge = false
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = frameStyle.nameAr,
                                            fontSize = 9.sp,
                                            color = if (isUnlocked) SoftWhiteText else SlateSecondaryText,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Surface(
                                            color = when {
                                                isSelected -> framePrimary
                                                isUnlocked -> DeepTealPrimary.copy(alpha = 0.22f)
                                                else -> RoyalNavyCard
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!isUnlocked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = SlateSecondaryText,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                }
                                                Text(
                                                    text = when {
                                                        isSelected -> "مفعّل ✓"
                                                        isUnlocked -> "تفعيل"
                                                        else -> "LV.${frameStyle.minLevel}"
                                                    },
                                                    fontSize = 8.sp,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isUnlocked -> DeepTealPrimary
                                                        else -> SlateSecondaryText
                                                    },
                                                    fontWeight = FontWeight.ExtraBold
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
        }

        // ====================================================================
        // 4. PINNED TOP 3 CLIPS (إمكانية تثبيت أفضل 3 مقاطع)
        // ====================================================================
        if (pinnedItems.isNotEmpty()) {
            item(key = "profile_pinned_top_3") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pinnedItems.size}/3 مثبت",
                            fontSize = 10.sp,
                            color = SlateSecondaryText
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "أفضل 3 مقاطع مثبتة",
                                fontSize = 12.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = MutedLuxuryGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pinnedItems.forEachIndexed { index, clip ->
                            val isLocked = clip.isExclusiveLocked && !isSubscribedVip && !unlockedVideoIds.contains(clip.id)
                            PinnedClipCard(
                                item = clip,
                                rank = index + 1,
                                isLocked = isLocked,
                                onClick = { handleOpenOrUnlockMedia(clip) },
                                onUnpin = { handleTogglePin(clip) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat((3 - pinnedItems.size).coerceAtLeast(0)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 5. MEDIA TABS BAR: Reels | Videos | Photos | Exclusive
        // ====================================================================
        item(key = "profile_content_tabs") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ProfileMediaTab.entries) { tab ->
                        val isSelected = selectedTab == tab
                        val activeColor = if (tab == ProfileMediaTab.EXCLUSIVE) MutedLuxuryGold else DeepTealPrimary

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(88.dp)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp)
                                .testTag("profile_tab_${tab.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) activeColor else SlateSecondaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) SoftWhiteText else SlateSecondaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(2.dp)
                                    .background(if (isSelected) activeColor else Color.Transparent)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        // ====================================================================
        // 6. EXCLUSIVE / PPV BANNER (When on Exclusive or Videos tab)
        // ====================================================================
        if (selectedTab == ProfileMediaTab.EXCLUSIVE && !isSubscribedVip) {
            item(key = "exclusive_unlock_banner") {
                Surface(
                    onClick = { showSubscribeVipModal = true },
                    color = RoyalNavyCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, MutedLuxuryGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MutedLuxuryGold,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "اشتراك $$VIP_MONTHLY_SUBSCRIPTION_USD",
                                fontSize = 10.sp,
                                color = RoyalNavyBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "افتح كل المحتوى الحصري أو اشترِ أي فيديو منفرداً",
                                fontSize = 11.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "فيديوهات 4K خاصة • جلسات VIP • ألبومات حصرية",
                                fontSize = 10.sp,
                                color = SlateSecondaryText
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == ProfileMediaTab.POSTS) {
            item(key = "profile_paid_post_publish") {
                Surface(
                    onClick = { showPaidPostDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = RoyalNavyCard,
                    border = BorderStroke(1.dp, MutedLuxuryGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).testTag("create_paid_post_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MutedLuxuryGold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إنشاء منشور مدفوع", fontSize = 15.sp, color = SoftWhiteText, fontWeight = FontWeight.Bold)
                            Text("معاينة عامة ومحتوى خاص بسعر تختارينه", fontSize = 12.sp, color = SlateSecondaryText)
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 7. TAB CONTENT GRID (3-Column for Reels/Photos, 2-Column for Videos/Exclusive)
        // ====================================================================
        item(key = "profile_tab_grid_${selectedTab.name}") {
            val columns = if (selectedTab == ProfileMediaTab.VIDEOS || selectedTab == ProfileMediaTab.EXCLUSIVE || selectedTab == ProfileMediaTab.POSTS) 2 else 3
            val rows = currentTabItems.chunked(columns)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == ProfileMediaTab.PHOTOS) {
                    // Subtle Add Photo Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = {
                                showcasePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            color = RoyalNavyCard,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = DeepTealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إضافة صورة للمعرض",
                                    fontSize = 11.sp,
                                    color = SoftWhiteText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    Surface(
                        color = RoyalNavyCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = SlateSecondaryText)
                            Text("لا توجد منشورات في هذا القسم بعد", fontSize = 14.sp, color = SoftWhiteText)
                            Text("ستظهر المنشورات الحقيقية هنا بعد نشرها.", fontSize = 12.sp, color = SlateSecondaryText)
                        }
                    }
                }
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { mediaItem ->
                            val isPinned = pinnedClipIds.contains(mediaItem.id)
                            val isLocked = mediaItem.tab == ProfileMediaTab.POSTS ||
                                (mediaItem.isExclusiveLocked &&
                                !isSubscribedVip &&
                                !unlockedVideoIds.contains(mediaItem.id))

                            ProfileMediaGridTile(
                                item = mediaItem,
                                isPinned = isPinned,
                                isLocked = isLocked,
                                isWideTile = columns == 2,
                                onClick = { handleOpenOrUnlockMedia(mediaItem) },
                                onTogglePin = { handleTogglePin(mediaItem) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat((columns - rowItems.size).coerceAtLeast(0)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 8. COMPACT FOOTER BAR FOR CREATOR SETTINGS, FRAMES & AUTH
        // ====================================================================
        item(key = "profile_compact_footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showSettingsModal = true },
                    color = RoyalNavyCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_chat_pricing_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعديل الإعدادات",
                            fontSize = 11.sp,
                            color = DeepTealPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "تسعير الدردشة ($${userProfile.chatPriceUsd}) • الإطارات • الحساب",
                                fontSize = 11.sp,
                                color = SlateSecondaryText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = SoftWhiteText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Infrastructure Database & Password Management Card
                Surface(
                    onClick = { showPasswordAndDbModal = true },
                    color = RoyalNavyCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.9.dp, MutedLuxuryGold.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_database_infrastructure_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = EmeraldVerified.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.8.dp, EmeraldVerified.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${savedAccounts.size.coerceAtLeast(1)} حساب محفوظ",
                                    fontSize = 9.sp,
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "قاعدة بيانات الحسابات • حفظ البنية التحتية وكلمة السر",
                                    fontSize = 11.sp,
                                    color = SoftWhiteText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MutedLuxuryGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "الحالة: $lastInfrastructureSyncLabel • تشفير PBKDF2-HMAC-SHA256",
                            fontSize = 10.sp,
                            color = SlateSecondaryText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPasswordAndDbModal = true },
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(0.8.dp, MutedLuxuryGold),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("open_change_password_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MutedLuxuryGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تغيير كلمة السر",
                                    fontSize = 10.sp,
                                    color = SoftWhiteText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onSaveInfrastructureNow,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepTealPrimary,
                                    contentColor = SoftWhiteText
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("save_infrastructure_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "حفظ البنية التحتية",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Direct quick buttons for Auth & Super Admin so testTags are always mounted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_avatar_frames_card"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenGoogleSignIn,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("profile_open_auth_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DeepTealPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isGoogleSignedIn || isSuperAdmin) "إعدادات الحساب" else "تسجيل الدخول",
                            fontSize = 11.sp,
                            color = SoftWhiteText
                        )
                    }

                    if (isSuperAdmin) {
                        Button(
                            onClick = onOpenAdminTab,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutedLuxuryGold,
                                contentColor = RoyalNavyBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("open_super_admin_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لوحة الإدارة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ====================================================================
    // MODAL 1: PURCHASE SINGLE VIDEO OR SUBSCRIBE VIP FOR EXCLUSIVE CONTENT
    // ====================================================================
    selectedLockedItemForPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedLockedItemForPurchase = null },
            containerColor = RoyalNavyCard,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MutedLuxuryGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Exclusive VIP",
                            fontSize = 10.sp,
                            color = MutedLuxuryGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "فتح المحتوى الحصري",
                        fontSize = 15.sp,
                        color = SoftWhiteText,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${item.title} (${item.durationLabel})",
                        fontSize = 13.sp,
                        color = SoftWhiteText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "يمكنك شراء هذا الفيديو منفرداً لمشاهدته فوراً، أو الاشتراك الشهري لفتح جميع الفيديوهات والألبومات الحصرية بلا حدود.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = SlateSecondaryText,
                        textAlign = TextAlign.End
                    )

                    // Option A: Single Video Purchase
                    Surface(
                        onClick = {
                            if (onPurchaseContentWithWallet(item.singlePriceUsd, "شراء فيديو: ${item.title}")) {
                                unlockedVideoIds.add(item.id)
                                selectedLockedItemForPurchase = null
                                onShowMessage("تم شراء الفيديو وفتحه بنجاح ✓")
                            } else {
                                selectedLockedItemForPurchase = null
                                onShowMessage("رصيد المحفظة غير كافٍ، يرجى شحن الرصيد أولاً")
                                onOpenWalletTab()
                            }
                        },
                        color = DeepTealPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${item.singlePriceUsd}",
                                fontSize = 13.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "شراء هذا الفيديو منفرداً",
                                fontSize = 12.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Option B: Full VIP Subscription
                    Surface(
                        onClick = {
                            if (onPurchaseContentWithWallet(VIP_MONTHLY_SUBSCRIPTION_USD, "اشتراك VIP شامل • $displayTitle")) {
                                isSubscribedVip = true
                                selectedLockedItemForPurchase = null
                                onShowMessage("تم تفعيل اشتراك VIP وفتح جميع المقاطع الحصرية ✓")
                            } else {
                                selectedLockedItemForPurchase = null
                                onShowMessage("رصيد المحفظة غير كافٍ، يرجى شحن الرصيد أولاً")
                                onOpenWalletTab()
                            }
                        },
                        color = MutedLuxuryGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MutedLuxuryGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$$VIP_MONTHLY_SUBSCRIPTION_USD / شهرياً",
                                fontSize = 12.sp,
                                color = MutedLuxuryGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "اشتراك VIP (فتح كل المحتوى)",
                                fontSize = 12.sp,
                                color = MutedLuxuryGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "رصيد محفظتك الحالي: $$walletBalanceUsd",
                        fontSize = 11.sp,
                        color = SlateSecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedLockedItemForPurchase = null
                    onOpenWalletTab()
                }) {
                    Text("شحن المحفظة", fontSize = 12.sp, color = DeepTealPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLockedItemForPurchase = null }) {
                    Text("إغلاق", fontSize = 12.sp, color = SlateSecondaryText)
                }
            }
        )
    }

    // ====================================================================
    // MODAL 2: VIP SUBSCRIPTION DIALOG
    // ====================================================================
    if (showSubscribeVipModal) {
        AlertDialog(
            onDismissRequest = { showSubscribeVipModal = false },
            containerColor = RoyalNavyCard,
            title = {
                Text(
                    text = "اشتراك VIP الحصري • $displayTitle",
                    fontSize = 15.sp,
                    color = MutedLuxuryGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• فتح جميع فيديوهات وألبومات قسم Exclusive بلا حدود\n• أولوية في الرد والمراسلة الخاصة\n• شارة داعم ملكية أرجوانية في الملف الشخصي",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = SoftWhiteText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قيمة الاشتراك الشهري: $$VIP_MONTHLY_SUBSCRIPTION_USD (رصيدك: $$walletBalanceUsd)",
                        fontSize = 11.sp,
                        color = SlateSecondaryText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSubscribedVip) {
                            showSubscribeVipModal = false
                        } else if (onPurchaseContentWithWallet(VIP_MONTHLY_SUBSCRIPTION_USD, "اشتراك VIP الشهري")) {
                            isSubscribedVip = true
                            showSubscribeVipModal = false
                            onShowMessage("تم تفعيل اشتراك VIP الحصري بنجاح ✨")
                        } else {
                            showSubscribeVipModal = false
                            onShowMessage("رصيد المحفظة غير كافٍ، يرجى شحن الرصيد عبر شام كاش أو USDT")
                            onOpenWalletTab()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedLuxuryGold,
                        contentColor = RoyalNavyBackground
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isSubscribedVip) "مفعّل حالياً ✓" else "تأكيد الاشتراك ($$VIP_MONTHLY_SUBSCRIPTION_USD)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubscribeVipModal = false }) {
                    Text("إلغاء", fontSize = 12.sp, color = SlateSecondaryText)
                }
            }
        )
    }

    if (showPaidPostDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postPreview by remember { mutableStateOf("") }
        var postBody by remember { mutableStateOf("") }
        var postPrice by remember { mutableStateOf("5") }
        val parsedPrice = postPrice.toIntOrNull()
        val canPublish = postTitle.trim().length in 3..150 &&
            postPreview.trim().length in 10..300 &&
            postBody.trim().length in 20..5000 &&
            parsedPrice != null && parsedPrice in 1..1000

        AlertDialog(
            onDismissRequest = { showPaidPostDialog = false },
            containerColor = RoyalNavyCard,
            title = { Text("نشر محتوى مدفوع", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftWhiteText) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("المعاينة متاحة للجميع، أما المحتوى الكامل فيُحفظ بسجل خاص لا يستطيع الزوار قراءته مباشرة.", color = SlateSecondaryText, fontSize = 13.sp)
                    }
                    item {
                        OutlinedTextField(
                            value = postTitle, onValueChange = { postTitle = it.take(150) },
                            label = { Text("عنوان المنشور") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("paid_post_title")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = postPreview, onValueChange = { postPreview = it.take(300) },
                            label = { Text("المعاينة العامة") }, minLines = 2, maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("paid_post_preview")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = postBody, onValueChange = { postBody = it.take(5000) },
                            label = { Text("النص الكامل المدفوع") }, minLines = 4, maxLines = 8,
                            modifier = Modifier.fillMaxWidth().testTag("paid_post_body")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = postPrice, onValueChange = { postPrice = it.filter(Char::isDigit).take(4) },
                            label = { Text("السعر بالدولار (1–1000)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = parsedPrice == null || parsedPrice !in 1..1000,
                            modifier = Modifier.fillMaxWidth().testTag("paid_post_price")
                        )
                    }
                    item {
                        Text("تنبيه: تحصيل المبلغ وفتح المحتوى للمشتري لن يُفعّلا قبل ربط تحقق الدفع والصلاحيات في الخادم.", color = MutedLuxuryGold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = canPublish,
                    onClick = {
                        onPublishPaidPost(postTitle.trim(), postPreview.trim(), postBody.trim(), parsedPrice ?: 5)
                        showPaidPostDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTealPrimary)
                ) { Text("نشر المعاينة", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPaidPostDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // MODAL 3: Full, navigable settings; each category has its own screen.
    // ============================================================================
    if (showSettingsModal) {
        ProfileSettingsDialog(
            userProfile = userProfile,
            lightTheme = isSoftRoseLightTheme,
            isSignedIn = isGoogleSignedIn || isSuperAdmin,
            isAdmin = isSuperAdmin,
            walletBalanceUsd = walletBalanceUsd,
            onDismiss = { showSettingsModal = false },
            onEditProfile = { showEditProfileDialog = true },
            onToggleTheme = onToggleThemeMode,
            onUpdateChatPricing = onUpdateChatPricing,
            onSelectFrame = onSelectAvatarFrame,
            onOpenWallet = onOpenWalletTab,
            onChangePassword = { showPasswordAndDbModal = true },
            onSignOut = onSignOut
        )
    }

    // ============================================================================
    // ====================================================================
    // MODAL 4: EDIT PROFILE DIALOG
    // ====================================================================
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(userProfile.displayName) }
        var usernameInput by remember { mutableStateOf(userProfile.username.ifBlank { "@layla_vip" }) }
        var roleInput by remember { mutableStateOf(userProfile.roleTitle) }
        var cityInput by remember { mutableStateOf(userProfile.cityName) }
        var bioInput by remember { mutableStateOf(userProfile.bio) }
        var chatPriceInput by remember { mutableStateOf(userProfile.chatPriceUsd.toString()) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = RoyalNavyCard,
            title = {
                Text(
                    text = "تعديل الملف الشخصي",
                    fontSize = 15.sp,
                    color = SoftWhiteText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم المستخدم (@username)", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username_input")
                    )
                    OutlinedTextField(
                        value = chatPriceInput,
                        onValueChange = { chatPriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("سعر الدردشة ($)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("المدينة", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("النبذة التعريفية", fontSize = 11.sp) },
                        minLines = 2,
                        shape = RoundedCornerShape(18.dp),
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
                        containerColor = DeepTealPrimary,
                        contentColor = SoftWhiteText
                    )
                ) {
                    Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء", fontSize = 12.sp, color = SlateSecondaryText)
                }
            }
        )
    }

    // ====================================================================
    // MODAL 5: INFRASTRUCTURE DATABASE & CHANGE PASSWORD DIALOG
    // ====================================================================
    if (showPasswordAndDbModal) {
        var currentPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }
        var directRecoveryMode by remember { mutableStateOf(false) }
        var modalError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordAndDbModal = false },
            containerColor = RoyalNavyCard,
            title = {
                Text(
                    text = "قاعدة بيانات الحسابات وتغيير كلمة المرور",
                    fontSize = 14.sp,
                    color = SoftWhiteText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = RoyalNavyBackground,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "الحساب النشط: $formattedUsername • LEVEL ${userProfile.level}",
                                fontSize = 11.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "قاعدة البيانات: Room SQLite + Cloud Firestore (/users)",
                                fontSize = 10.sp,
                                color = EmeraldVerified
                            )
                            Text(
                                text = "إجمالي الحسابات المحفوظة: ${savedAccounts.size.coerceAtLeast(1)} حساب",
                                fontSize = 10.sp,
                                color = SlateSecondaryText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !directRecoveryMode,
                            onClick = {
                                directRecoveryMode = false
                                modalError = null
                            },
                            label = { Text("تأكيد كلمة السر الحالية", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepTealPrimary,
                                selectedLabelColor = SoftWhiteText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = directRecoveryMode,
                            onClick = {
                                directRecoveryMode = true
                                modalError = null
                            },
                            label = { Text("تعيين مباشر للحساب", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MutedLuxuryGold,
                                selectedLabelColor = RoyalNavyBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!directRecoveryMode) {
                        OutlinedTextField(
                            value = currentPassInput,
                            onValueChange = {
                                currentPassInput = it
                                modalError = null
                            },
                            label = { Text("كلمة المرور الحالية", fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_current_password_input")
                        )
                    }

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = {
                            newPassInput = it
                            modalError = null
                        },
                        label = { Text("كلمة المرور الجديدة (6 خانات فأكثر)", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = {
                            confirmPassInput = it
                            modalError = null
                        },
                        label = { Text("تأكيد كلمة المرور الجديدة", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_confirm_password_input")
                    )

                    if (modalError != null) {
                        Text(
                            text = modalError ?: "",
                            fontSize = 11.sp,
                            color = ErrorCrimson,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassInput.trim().length < 6) {
                            modalError = "يجب أن تتكون كلمة المرور الجديدة من 6 خانات على الأقل"
                            return@Button
                        }
                        if (newPassInput != confirmPassInput) {
                            modalError = "كلمتا المرور الجديدتان غير متطابقتين"
                            return@Button
                        }
                        val ok = onChangeAccountPassword(
                            currentPassInput,
                            newPassInput,
                            !directRecoveryMode
                        )
                        if (ok) {
                            showPasswordAndDbModal = false
                        } else {
                            modalError = "كلمة المرور الحالية غير صحيحة"
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedLuxuryGold,
                        contentColor = RoyalNavyBackground
                    ),
                    modifier = Modifier.testTag("profile_save_new_password_button")
                ) {
                    Text("حفظ كلمة السر الجديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSaveInfrastructureNow()
                        showPasswordAndDbModal = false
                    }
                ) {
                    Text("حفظ البنية وإغلاق", fontSize = 11.sp, color = DeepTealPrimary)
                }
            }
        )
    }
}

@Composable
private fun MinimalStatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            color = SoftWhiteText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SlateSecondaryText
        )
    }
}

@Composable
private fun PinnedClipCard(
    item: ProfileMediaItem,
    rank: Int,
    isLocked: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = RoyalNavyCard,
        border = BorderStroke(0.7.dp, MutedLuxuryGold.copy(alpha = 0.45f)),
        modifier = modifier.height(132.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SmartServiceMediaView(
                imageUrl = item.imageUrl,
                accentHex = item.accentHex,
                secondaryHex = item.secondaryHex,
                category = item.category,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalNavyBackground.copy(alpha = 0.35f),
                                Color.Transparent,
                                RoyalNavyBackground.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Top Row: Pin Badge + Unpin Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(RoyalNavyBackground.copy(alpha = 0.65f))
                        .clickable { onUnpin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "إلغاء التثبيت",
                        tint = MutedLuxuryGold,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Surface(
                    color = MutedLuxuryGold,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "مثبت #$rank",
                        fontSize = 8.sp,
                        color = RoyalNavyBackground,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "مقفل",
                    tint = MutedLuxuryGold,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                )
            }

            // Bottom Views & Title
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.title,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = SoftWhiteText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.viewsLabel == "—" || item.viewsLabel == "خاص") "" else "${item.viewsLabel} مشاهدة",
                    fontSize = 9.sp,
                    color = SlateSecondaryText
                )
            }
        }
    }
}

@Composable
private fun ProfileMediaGridTile(
    item: ProfileMediaItem,
    isPinned: Boolean,
    isLocked: Boolean,
    isWideTile: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = RoyalNavyCard,
        border = BorderStroke(
            width = 0.7.dp,
            color = if (isLocked) MutedLuxuryGold.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(if (isWideTile) 128.dp else 142.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SmartServiceMediaView(
                imageUrl = item.imageUrl,
                accentHex = item.accentHex,
                secondaryHex = item.secondaryHex,
                category = item.category,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay (darker when locked)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalNavyBackground.copy(alpha = if (isLocked) 0.55f else 0.25f),
                                RoyalNavyBackground.copy(alpha = if (isLocked) 0.65f else 0.1f),
                                RoyalNavyBackground.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Top Row: Pin toggle button + Duration/Price Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin / Unpin button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPinned) MutedLuxuryGold else RoyalNavyBackground.copy(alpha = 0.6f)
                        )
                        .clickable { onTogglePin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "تثبيت في أفضل 3",
                        tint = if (isPinned) RoyalNavyBackground else SoftWhiteText.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                }

                if (item.tab != ProfileMediaTab.PHOTOS) {
                    Surface(
                        color = RoyalNavyBackground.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.durationLabel,
                            fontSize = 9.sp,
                            color = SoftWhiteText,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Center Lock / Unlock Indicator for Exclusive & Paid Videos
            if (isLocked) {
                Surface(
                    color = RoyalNavyBackground.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, MutedLuxuryGold),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "محتوى حصري",
                            tint = MutedLuxuryGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.tab == ProfileMediaTab.POSTS) "بسعر • ${item.singlePriceUsd}" else "فتح • ${item.singlePriceUsd}",
                            fontSize = 10.sp,
                            color = MutedLuxuryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (item.isExclusiveLocked) {
                // Unlocked Exclusive badge
                Surface(
                    color = EmeraldVerified.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "مفتوح",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "مفتوح ✓",
                            fontSize = 9.sp,
                            color = EmeraldVerified,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Title & View Count
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(7.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.title,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = SoftWhiteText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.viewsLabel == "—" || item.viewsLabel == "خاص") "" else "${item.viewsLabel} مشاهدة",
                    fontSize = 9.sp,
                    color = SlateSecondaryText
                )
            }
        }
    }
}