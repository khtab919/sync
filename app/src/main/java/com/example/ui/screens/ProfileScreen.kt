package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.PublishedService
import com.example.data.model.UserProfile
import com.example.ui.components.BrandActionBadgesRow
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.WingedHeartLogo
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.BrandRoyalPlum
import com.example.ui.theme.BrandSoftPearl
import com.example.ui.theme.DeepPlumText
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ObsidianNight
import com.example.util.LayaliNotificationHelper

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Royal Winged-Heart Profile Showcase Card (Matching Brand Board Phone Screen)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, BrandChampagneGold.copy(alpha = 0.7f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Top Decorative Brand Banner Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brand_identity),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            BrandDeepViolet.copy(alpha = 0.45f),
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
                                color = BrandDeepViolet.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandChampagneGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WingedHeartLogo(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الملف الشخصي الملكي VIP",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val isUserLoggedIn = isGoogleSignedIn || isSuperAdmin || userProfile.displayName.isNotBlank()
                            Surface(
                                onClick = {
                                    if (isUserLoggedIn) {
                                        showEditProfileDialog = true
                                    } else {
                                        onOpenGoogleSignIn()
                                    }
                                },
                                color = BrandDustyRose,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isUserLoggedIn) Icons.Default.Edit else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUserLoggedIn) "تعديل الملف" else "تسجيل الدخول",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Profile Avatar, Name, Bio, and Stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 76.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with Camera upload badge
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(104.dp)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_picker")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(3.dp, BrandChampagneGold, CircleShape)
                                    .background(BrandRoyalPlum),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile.avatarUri.isNotBlank()) {
                                    SmartServiceMediaView(
                                        imageUrl = userProfile.avatarUri,
                                        accentHex = 0xFFD6789B,
                                        secondaryHex = 0xFF47256B,
                                        category = "صورة شخصية",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    WingedHeartLogo(modifier = Modifier.size(68.dp))
                                }
                            }

                            Surface(
                                color = BrandDustyRose,
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color.White),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تغيير صورة الملف الشخصي",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val hasActiveAccount = isGoogleSignedIn || isSuperAdmin || userProfile.displayName.isNotBlank()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasActiveAccount) {
                                    userProfile.displayName.ifBlank { userEmail?.substringBefore("@") ?: "حسابي الشخصي" }
                                } else {
                                    "لا يوجد حساب مسجل حالياً"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasActiveAccount) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "موثق",
                                    tint = EmeraldVerified,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (hasActiveAccount && (userProfile.roleTitle.isNotBlank() || userProfile.telegramHandle.isNotBlank())) {
                            Text(
                                text = listOf(userProfile.roleTitle, userProfile.telegramHandle)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandDustyRose,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (!hasActiveAccount) {
                            Text(
                                text = "سجل دخولك أو أنشئ حسابك الجديد لحفظ بياناتك وحجوزاتك",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandChampagneGold,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (hasActiveAccount && userProfile.cityName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userProfile.cityName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (hasActiveAccount && userProfile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = userProfile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Availability Switch Row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (userProfile.isAvailableNow) EmeraldVerified else BrandChampagneGold.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (userProfile.isAvailableNow) EmeraldVerified else ErrorCrimson)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (userProfile.isAvailableNow) {
                                                "الحالة: متاح الآن لاستقبال الحجوزات والطلبات"
                                            } else {
                                                "الحالة: مشغول حالياً (الحجز المسبق فقط)"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "تواصل مباشر عبر تلجرام ${userProfile.telegramHandle}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        // Live Profile Stats (Customer vs Super Admin)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSuperAdmin) {
                                ProfileStatBox(
                                    modifier = Modifier.weight(1f),
                                    countLabel = "${publishedServices.size}",
                                    title = "عروض منشورة",
                                    onClick = onOpenAdminTab
                                )
                            }
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
                            if (isSuperAdmin) {
                                ProfileStatBox(
                                    modifier = Modifier.weight(1f),
                                    countLabel = "${blockedUsers.count { it.status == "active" }}",
                                    title = "المحظورين",
                                    onClick = onOpenAdminTab
                                )
                            }
                        }

                        if (isSuperAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenAdminTab,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandChampagneGold,
                                    contentColor = ObsidianNight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_super_admin_dashboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فتح لوحة التحكم الإدارية (Super Admin)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 3D Brand Action Badges Row (Matching Brand Board Mobile Screen)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الوصول السريع وخدمات التواصل المباشر",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BrandActionBadgesRow(
                        onFavoritesClick = onOpenFavoritesTab,
                        onSupportChatClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                        onDirectCallClick = onOpenExploreTab,
                        onCitiesClick = onOpenExploreTab
                    )
                }
            }
        }

        // 3. My Visual Showcase: Photos, Videos & Prices inside Profile (Super Admin Only)
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
                border = BorderStroke(1.dp, BrandDustyRose.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "معرض الصور والفيديو والأسعار في ملفي",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "اضغط على أي عرض لمشاهدة الفيديو 4K أو ألبوم الصور أو تعديل السعر",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandDustyRose,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة صورة للملف", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenAdminTab,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نشر فيديو وسعر جديد", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (userProfile.customPhotos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "الصور المرفوعة من جهازك (${userProfile.customPhotos.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(userProfile.customPhotos) { photoUri ->
                                Box(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.5.dp, BrandChampagneGold, RoundedCornerShape(16.dp))
                                ) {
                                    SmartServiceMediaView(
                                        imageUrl = photoUri,
                                        accentHex = 0xFFD6789B,
                                        secondaryHex = 0xFF47256B,
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
                                    .width(250.dp)
                                    .clickable { onOpenMediaViewer(service, true) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    ) {
                                        SmartServiceMediaView(
                                            imageUrl = service.imageUrl,
                                            accentHex = 0xFFD6789B,
                                            secondaryHex = 0xFF47256B,
                                            category = service.category,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = BrandDustyRose,
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
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "فيديو + صور",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = { onEditServicePrice(service) },
                                            color = BrandChampagneGold,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$${service.priceUsd}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = ObsidianNight,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل السعر",
                                                    tint = ObsidianNight,
                                                    modifier = Modifier.size(14.dp)
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
                                            color = BrandDustyRose
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

        // 4. Visual Identity Theme Switcher Card (Dark Theme vs Light & Soft Rose Theme)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "الهوية البصرية ونمط العرض (Dark / Soft Rose)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "اختر بين النسخة الداكنة الملكية أو النسخة الفاتحة والناعمة المعتمدة في الهوية البصرية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dark Theme Option Card
                        Surface(
                            onClick = { if (isSoftRoseLightTheme) onToggleThemeMode() },
                            color = BrandDeepViolet,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (!isSoftRoseLightTheme) 2.dp else 0.8.dp,
                                color = if (!isSoftRoseLightTheme) BrandChampagneGold else Color.White.copy(alpha = 0.2f)
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
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "النسخة الداكنة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dark Royal Violet",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandChampagneGold
                                )
                            }
                        }

                        // Soft Rose Light Theme Option Card
                        Surface(
                            onClick = { if (!isSoftRoseLightTheme) onToggleThemeMode() },
                            color = BrandSoftPearl,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (isSoftRoseLightTheme) 2.dp else 0.8.dp,
                                color = if (isSoftRoseLightTheme) BrandDustyRose else DeepPlumText.copy(alpha = 0.2f)
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
                                    tint = BrandDustyRose,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "النسخة الفاتحة والناعمة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepPlumText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Light & Soft Rose",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandDustyRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick User Blocking & Telegram Support Card inside Profile (Super Admin Only)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = ErrorCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الحظر السريع والإشعارات الفورية",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickBlockInput,
                            onValueChange = { quickBlockInput = it },
                            placeholder = { Text("اكتب اسم المستخدم أو @Handle لحظره...") },
                            singleLine = true,
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حظر", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (blockedUsers.isNotEmpty()) {
                        blockedUsers.take(4).forEach { blocked ->
                            val isBlocked = blocked.status == "active"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandDustyRose,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دعم تلجرام @RamaNUR", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onTriggerBroadcastNotification,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بث إشعار أندرويد", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        }

        // 6. Account & Authentication Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.35f))
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
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isSuperAdmin) {
                                        "حساب المسؤول الرئيسي متصل (${userEmail ?: "RamaNUR@gmail.com"})"
                                    } else if (isGoogleSignedIn) {
                                        "حساب متصل (${userEmail ?: "مزامنة سحابية نشطة"})"
                                    } else {
                                        "وضع تصفح العملاء VIP"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isSuperAdmin) {
                                        "صلاحية Super Admin مفعلة بالكامل"
                                    } else {
                                        "يمكنك تسجيل الدخول بالبريد الإلكتروني وكلمة المرور أو Google"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isGoogleSignedIn || isSuperAdmin) {
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ErrorCrimson)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خروج", color = ErrorCrimson)
                            }
                        }
                    }

                    Button(
                        onClick = onOpenGoogleSignIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandChampagneGold,
                            contentColor = ObsidianNight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_open_auth_screen_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGoogleSignedIn || isSuperAdmin) {
                                "تبديل الحساب / تسجيل الدخول بحساب آخر"
                            } else {
                                "تسجيل الدخول بالبريد الإلكتروني وكلمة المرور"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(userProfile.displayName) }
        var roleInput by remember { mutableStateOf(userProfile.roleTitle) }
        var cityInput by remember { mutableStateOf(userProfile.cityName) }
        var bioInput by remember { mutableStateOf(userProfile.bio) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل الملف الشخصي",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم الظاهر في الملف الشخصي") },
                        placeholder = { Text("أدخل اسمك الشخصي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("اللقب أو الوصف المختصر (اختياري)") },
                        placeholder = { Text("مثال: عضو VIP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("المدينة (مثال: دمشق، حلب، إسطنبول، أنطاليا)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("نبذة شخصية (اختياري)") },
                        minLines = 3,
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
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandDustyRose,
                        contentColor = Color.White
                    )
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء")
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, BrandChampagneGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.titleLarge,
                color = BrandChampagneGold,
                fontWeight = FontWeight.Bold
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
