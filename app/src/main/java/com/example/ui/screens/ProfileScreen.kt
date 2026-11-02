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
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.viewmodel.WalletTransaction

private enum class ProfileMediaTab(
    val label: String,
    val icon: ImageVector
) {
    REELS("الريلز", Icons.Default.PlayArrow),
    VIDEOS("Videos", Icons.Default.Videocam),
    PHOTOS("الصور", Icons.Default.GridOn),
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
    walletBalanceUsd: Int = 0,
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
    var coverImageUri by rememberSaveable { mutableStateOf("") }

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
                durationLabel = "",
                imageUrl = pub.imageUrl,
                category = pub.category,
                singlePriceUsd = pub.priceUsd.toInt(),
                isExclusiveLocked = false,
                accentHex = 0xFF0D9488L,
                secondaryHex = 0xFF09111EL,
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
                accentHex = 0xFFB9A477L,
                secondaryHex = 0xFF151A23L,
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
                accentHex = 0xFF0D9488L,
                secondaryHex = 0xFF09111EL
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

    val formattedUsername = userProfile.username.let {
        if (it.isBlank() || it.startsWith("@")) it else "@$it"
    }

    val hasActiveAccount = isGoogleSignedIn || isSuperAdmin || userProfile.displayName.isNotBlank()
    val displayTitle = if (hasActiveAccount) {
        userProfile.displayName.substringBefore("•").trim().ifBlank {
            userEmail?.substringBefore("@") ?: "حسابي"
        }
    } else {
        "مرحباً بك"
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "profile_header_section") {
            CalmProfileHeader(
                profile = userProfile, title = displayTitle, username = formattedUsername,
                coverUri = coverImageUri, bookings = bookingsCount,
                services = publishedServices.size, favorites = favoritesCount,
                light = isSoftRoseLightTheme, isAdmin = isSuperAdmin,
                onCover = { coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onAvatar = { avatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onCopy = { copyTextToClipboard(context, "Username", formattedUsername) },
                onEdit = { showEditProfileDialog = true }, onSettings = { showSettingsModal = true },
                onWallet = onOpenWalletTab, onBookings = onOpenBookingsTab,
                onFavorites = onOpenFavoritesTab, onMessages = onOpenChatTab,
                onStore = onOpenExploreTab, onTheme = onToggleThemeMode, onAdmin = onOpenAdminTab
            )
        }
        item(key = "profile_content_tabs") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("منشوراتي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(ProfileMediaTab.REELS, ProfileMediaTab.PHOTOS, ProfileMediaTab.POSTS)) { tab ->
                        FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab },
                            label = { Text(tab.label, fontSize = 15.sp) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("profile_tab_${tab.name.lowercase()}"))
                    }
                }
            }
        }
        if (selectedTab == ProfileMediaTab.PHOTOS) {
            item {
                TextButton(onClick = { showcasePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(horizontal = 20.dp)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة صورة")
                }
            }
        }
        if (selectedTab == ProfileMediaTab.POSTS) {
            item { Text("المنشورات المدفوعة متاحة للمعاينة. النشر والشراء ينتظران تفعيل الخادم.",
                modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (currentTabItems.isEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.GridOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("لا توجد منشورات بعد", style = MaterialTheme.typography.titleMedium)
                        Text("ستظهر صورك وخدماتك المنشورة هنا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(currentTabItems.chunked(2)) { rowItems ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    ProfileMediaGridTile(item = item, isPinned = pinnedClipIds.contains(item.id),
                        isLocked = item.isExclusiveLocked, isWideTile = true,
                        onClick = { handleOpenOrUnlockMedia(item) }, onTogglePin = { handleTogglePin(item) },
                        modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    selectedLockedItemForPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedLockedItemForPurchase = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Exclusive VIP",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "فتح المحتوى الحصري",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${item.title} (${item.durationLabel})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "يمكنك شراء هذا الفيديو منفرداً لمشاهدته فوراً، أو الاشتراك الشهري لفتح جميع الفيديوهات والألبومات الحصرية بلا حدود.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "شراء هذا الفيديو منفرداً",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
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
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "اشتراك VIP (فتح كل المحتوى)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "رصيد محفظتك الحالي: $$walletBalanceUsd",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("شحن المحفظة", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLockedItemForPurchase = null }) {
                    Text("إغلاق", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "اشتراك VIP الحصري • $displayTitle",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• فتح جميع فيديوهات وألبومات قسم Exclusive بلا حدود\n• أولوية في الرد والمراسلة الخاصة\n• شارة داعم ملكية أرجوانية في الملف الشخصي",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قيمة الاشتراك الشهري: $$VIP_MONTHLY_SUBSCRIPTION_USD (رصيدك: $$walletBalanceUsd)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
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
                    Text("إلغاء", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("نشر محتوى مدفوع", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("المعاينة متاحة للجميع، أما المحتوى الكامل فيُحفظ بسجل خاص لا يستطيع الزوار قراءته مباشرة.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
                        Text("تنبيه: تحصيل المبلغ وفتح المحتوى للمشتري لن يُفعّلا قبل ربط تحقق الدفع والصلاحيات في الخادم.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        var usernameInput by remember { mutableStateOf(userProfile.username) }
        var roleInput by remember { mutableStateOf(userProfile.roleTitle) }
        var cityInput by remember { mutableStateOf(userProfile.cityName) }
        var bioInput by remember { mutableStateOf(userProfile.bio) }
        var chatPriceInput by remember { mutableStateOf(userProfile.chatPriceUsd.toString()) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل الملف الشخصي",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم المستخدم (@username)", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("المدينة", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("النبذة التعريفية", fontSize = 11.sp) },
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp),
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "قاعدة بيانات الحسابات وتغيير كلمة المرور",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(10.dp),
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
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
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.background
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
                            shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
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
                    Text("حفظ البنية وإغلاق", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
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
                                MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
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
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                        .clickable { onUnpin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "إلغاء التثبيت",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "مثبت #$rank",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.background,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "مقفل",
                    tint = MaterialTheme.colorScheme.secondary,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.viewsLabel == "—" || item.viewsLabel == "خاص") "" else "${item.viewsLabel} مشاهدة",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.7.dp,
            color = if (isLocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
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
                                MaterialTheme.colorScheme.background.copy(alpha = if (isLocked) 0.55f else 0.25f),
                                MaterialTheme.colorScheme.background.copy(alpha = if (isLocked) 0.65f else 0.1f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
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
                            if (isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                        )
                        .clickable { onTogglePin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "تثبيت في أفضل 3",
                        tint = if (isPinned) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                }

                if (item.tab != ProfileMediaTab.PHOTOS) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.durationLabel,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Center Lock / Unlock Indicator for Exclusive & Paid Videos
            if (isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "محتوى حصري",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.tab == ProfileMediaTab.POSTS) "بسعر • ${item.singlePriceUsd}" else "فتح • ${item.singlePriceUsd}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.viewsLabel == "—" || item.viewsLabel == "خاص") "" else "${item.viewsLabel} مشاهدة",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
