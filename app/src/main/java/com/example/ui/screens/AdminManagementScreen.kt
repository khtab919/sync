package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.LayaliCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.Booking
import com.example.data.model.PublishedService
import com.example.data.model.Venue
import com.example.security.MiddlewareResult
import com.example.security.SuperAdminSecurityManager
import com.example.ui.components.InteractiveVideoShowcaseCard
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.WingedHeartLogo
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WalletTransaction
import com.example.util.LayaliNotificationHelper

private enum class AdminControlSection(val titleAr: String) {
    SERVICES("الخدمات والمنشورات"),
    BOOKINGS("إدارة الحجوزات"),
    WALLET("المحفظة والدفع"),
    SECURITY_BLOCK("الحظر والأمان"),
    ADMIN_SETTINGS("إعدادات المسؤول")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminManagementScreen(
    adminSessionToken: String?,
    publishedServicesState: UiState<List<PublishedService>>,
    catalogVenues: List<Venue>,
    deletedCatalogCount: Int,
    bookingsState: UiState<List<Booking>>,
    walletBalanceUsd: Int,
    walletTransactions: List<WalletTransaction>,
    blockedUsersState: UiState<List<BlockedUser>>,
    isSoftRoseLightTheme: Boolean,
    isSubmitting: Boolean,
    onToggleThemeMode: () -> Unit,
    onPublishService: (
        title: String,
        cityName: String,
        country: String,
        category: String,
        priceUsd: Int,
        description: String,
        imageUri: String,
        videoUrl: String
    ) -> Unit,
    onUpdateServicePrice: (serviceId: String, newPriceUsd: Int) -> Unit,
    onUpdateServiceDetails: (
        serviceId: String,
        newTitle: String,
        newCityName: String,
        newCountry: String,
        newCategory: String,
        newPriceUsd: Int,
        newDescription: String
    ) -> Unit,
    onDeletePublishedService: (serviceId: String) -> Unit,
    onUpdateCatalogVenuePrice: (venueId: String, newPriceUsd: Int) -> Unit,
    onDeleteCatalogVenue: (venueId: String) -> Unit,
    onRestoreDeletedCatalogVenues: () -> Unit,
    onAdminUpdateBookingStatus: (bookingId: String, newStatus: String) -> Unit,
    onAdminDeleteBooking: (bookingId: String) -> Unit,
    onAdminAdjustWalletBalance: (amountDeltaUsd: Int, reasonNote: String) -> Unit,
    onBlockUser: (identifier: String, reason: String, cityScope: String) -> Unit,
    onToggleBlockStatus: (blockedId: String, currentStatus: String) -> Unit,
    onDeleteBlockedRecord: (blockedId: String) -> Unit,
    onTriggerInstantNotification: (title: String, city: String, priceUsd: Int, category: String) -> Unit,
    onNavigateToPublicStore: () -> Unit = {},
    onSuperAdminSignOut: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateToPublicStore)
    val context = LocalContext.current

    // 1. Route Protection & API Middleware Check (403 Forbidden for non-Super-Admin access)
    val middlewareStatus = remember(adminSessionToken) {
        SuperAdminSecurityManager.requireSuperAdminMiddleware(adminSessionToken)
    }

    if (middlewareStatus is MiddlewareResult.Forbidden403) {
        Forbidden403RouteGuardScreen(
            errorTitle = middlewareStatus.errorTitle,
            errorMessage = middlewareStatus.message,
            onReturnToPublicHome = onNavigateToPublicStore
        )
        return
    }

    // Authenticated Super Admin Control Panel State
    var credentialSnapshot by remember {
        mutableStateOf(SuperAdminSecurityManager.getCredentialSnapshot(context))
    }
    var activeSection by rememberSaveable { mutableStateOf(AdminControlSection.SERVICES) }

    // New Service / Post Form State
    var serviceTitle by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("حلب") }
    var selectedCountry by remember { mutableStateOf("سوريا") }
    var selectedCategory by remember { mutableStateOf("سهرات فنية") }
    var priceUsdText by remember { mutableStateOf("50") }
    var serviceDescription by remember { mutableStateOf("") }
    var videoTitleInput by remember {
        mutableStateOf("عرض فيديو 4K خاص للأجنحة الملكية والسهرات والسبا")
    }
    var selectedVisualPreset by remember { mutableStateOf("palace_hall") }
    var customGalleryUri by remember { mutableStateOf<String?>(null) }

    // Edit Service Dialog State
    var editingService by remember { mutableStateOf<PublishedService?>(null) }
    var editingCatalogVenue by remember { mutableStateOf<Venue?>(null) }

    // Wallet Admin Adjustment State
    var walletAdjustAmountText by remember { mutableStateOf("50") }
    var walletAdjustNote by remember { mutableStateOf("شحن رصيد مباشر من إدارة التطبيق") }

    // Block User State
    var blockIdentifierInput by remember { mutableStateOf("") }
    var blockReasonInput by remember { mutableStateOf("مخالفة سياسة الحجز أو إزعاج المراكز") }
    var blockCityScopeInput by remember { mutableStateOf("جميع المدن السورية والتركية") }

    // Super Admin Credential Update State
    var currentPasswordInput by remember { mutableStateOf("") }
    var newAdminEmailInput by remember(credentialSnapshot.adminEmail) {
        mutableStateOf(credentialSnapshot.adminEmail)
    }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmNewPasswordInput by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var credentialUpdateFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customGalleryUri = uri.toString()
        }
    }

    val publishedServices = when (publishedServicesState) {
        is UiState.Success -> publishedServicesState.data
        else -> emptyList()
    }

    val bookingsList = when (bookingsState) {
        is UiState.Success -> bookingsState.data
        else -> emptyList()
    }

    val blockedUsers = when (blockedUsersState) {
        is UiState.Success -> blockedUsersState.data
        else -> emptyList()
    }

    val visualPresets = remember {
        listOf(
            "palace_hall" to "قصر السهرات والقدود",
            "card_lounge" to "لاونج العزت الفام",
            "card_spa" to "سبا ومساج ملكي",
            "card_vip_night" to "بطاقة فنية وزيارات",
            "brand_dark_showcase" to "النسخة الداكنة VIP",
            "brand_light_showcase" to "النسخة الفاتحة والناعمة"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("super_admin_control_panel"),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Super Admin Top Security & Role Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, BrandChampagneGold)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            WingedHeartLogo(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "لوحة التحكم الإدارية",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = BrandChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = EmeraldVerified.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, EmeraldVerified)
                                    ) {
                                        Text(
                                            text = "Super Admin",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "الحساب النشط: ${credentialSnapshot.adminEmail}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SoftGold
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleThemeMode,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isSoftRoseLightTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "تبديل المظهر",
                                tint = BrandChampagneGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Status Pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, BrandChampagneGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Middleware 403 مفعل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandChampagneGold
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, EmeraldVerified.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldVerified,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تشفير bcrypt / PBKDF2",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldVerified
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.8.dp, BrandDustyRose.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "مخفي تماماً عن واجهة العملاء",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandDustyRose,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToPublicStore,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "معاينة واجهة العملاء",
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onSuperAdminSignOut,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorCrimson,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("super_admin_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "خروج من الإدارة",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Horizontal Section Selector Tabs inside Super Admin Panel
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AdminControlSection.entries) { section ->
                    val isSelected = activeSection == section
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeSection = section },
                        label = {
                            Text(
                                text = section.titleAr,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (section) {
                                    AdminControlSection.SERVICES -> Icons.Default.AddPhotoAlternate
                                    AdminControlSection.BOOKINGS -> Icons.Default.CalendarMonth
                                    AdminControlSection.WALLET -> Icons.Default.AccountBalanceWallet
                                    AdminControlSection.SECURITY_BLOCK -> Icons.Default.Block
                                    AdminControlSection.ADMIN_SETTINGS -> Icons.Default.Key
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandChampagneGold,
                            selectedLabelColor = ObsidianNight,
                            selectedLeadingIconColor = ObsidianNight
                        ),
                        modifier = Modifier.testTag("admin_section_tab_${section.name}")
                    )
                }
            }
        }

        // 3. Active Section Content
        when (activeSection) {
            AdminControlSection.SERVICES -> {
                // Section A: Add New Service / Post with Photos, Video & Price
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, BrandChampagneGold.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "إضافة خدمة أو منشور جديد بالكامل",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تحديد العنوان، المدينة، القسم، السعر ($)، الصور والفيديو مع إشعار فوري",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val activeImageKey = customGalleryUri ?: selectedVisualPreset
                            InteractiveVideoShowcaseCard(
                                videoTitle = videoTitleInput.ifBlank { "معاينة الفيديو الترويجي للخدمة" },
                                durationLabel = "01:45 4K",
                                cityName = "$selectedCity ($selectedCountry)",
                                priceUsd = priceUsdText.toIntOrNull() ?: 50,
                                imageUrl = activeImageKey
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandDustyRose,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_pick_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختيار صورة من المعرض", fontWeight = FontWeight.Bold)
                                }

                                if (customGalleryUri != null) {
                                    OutlinedButton(
                                        onClick = { customGalleryUri = null },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("استخدام القالب")
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(visualPresets, key = { it.first }) { (key, label) ->
                                    val selected = customGalleryUri == null && selectedVisualPreset == key
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            customGalleryUri = null
                                            selectedVisualPreset = key
                                        },
                                        label = { Text(label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandChampagneGold,
                                            selectedLabelColor = ObsidianNight
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = serviceTitle,
                                onValueChange = { serviceTitle = it },
                                label = { Text("اسم الخدمة أو عنوان المنشور") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_service_title_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = priceUsdText,
                                    onValueChange = { priceUsdText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("السعر بالدولار ($)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_service_price_input")
                                )

                                OutlinedTextField(
                                    value = videoTitleInput,
                                    onValueChange = { videoTitleInput = it },
                                    label = { Text("عنوان عرض الفيديو (4K)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("admin_video_title_input")
                                )
                            }

                            Text(
                                text = "اختر المدينة (سوريا / تركيا):",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(LayaliCatalog.cities, key = { it.id }) { city ->
                                    FilterChip(
                                        selected = selectedCity == city.nameAr,
                                        onClick = {
                                            selectedCity = city.nameAr
                                            selectedCountry = city.country
                                        },
                                        label = { Text("${city.nameAr} (${city.country})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandDustyRose,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LayaliCatalog.categories.filter { it != "الكل" }.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandChampagneGold,
                                            selectedLabelColor = ObsidianNight
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = serviceDescription,
                                onValueChange = { serviceDescription = it },
                                label = { Text("تفاصيل الخدمة والمنشور والأسعار") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_service_desc_input")
                            )

                            Button(
                                onClick = {
                                    val cleanPrice = (priceUsdText.toIntOrNull() ?: 50).coerceIn(5, 50000)
                                    val resolvedTitle = serviceTitle.ifBlank {
                                        "عرض $selectedCategory ملكي • $selectedCity"
                                    }
                                    val resolvedDesc = serviceDescription.ifBlank {
                                        "خدمة $selectedCategory راقية في $selectedCity مع إمكانية الحجز المباشر."
                                    }
                                    onPublishService(
                                        resolvedTitle,
                                        selectedCity,
                                        selectedCountry,
                                        selectedCategory,
                                        cleanPrice,
                                        resolvedDesc,
                                        customGalleryUri ?: selectedVisualPreset,
                                        videoTitleInput
                                    )
                                    serviceTitle = ""
                                    serviceDescription = ""
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandChampagneGold,
                                    contentColor = ObsidianNight
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("admin_publish_broadcast_button")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نشر الخدمة الآن وإرسال إشعار للجميع",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Section B: Edit & Delete Existing Published Services & Posts
                item {
                    Text(
                        text = "المنشورات والعروض الحالية — تعديل أو حذف (${publishedServices.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                items(publishedServices, key = { "admin_pub_${it.id}" }) { service ->
                    AdminPublishedServiceCard(
                        service = service,
                        onOpenFullEdit = { editingService = service },
                        onDelete = { onDeletePublishedService(service.id) },
                        onRebroadcast = {
                            onTriggerInstantNotification(
                                service.title,
                                service.cityName,
                                service.priceUsd.toInt(),
                                service.category
                            )
                        }
                    )
                }

                // Section C: Edit & Delete Catalog Venues
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (deletedCatalogCount > 0) {
                            OutlinedButton(
                                onClick = onRestoreDeletedCatalogVenues,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, EmeraldVerified)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = EmeraldVerified,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استعادة المحذوف ($deletedCatalogCount)", color = EmeraldVerified)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = "مراكز الدليل العام (${catalogVenues.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(catalogVenues, key = { "admin_cat_${it.id}" }) { venue ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { editingCatalogVenue = venue },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BrandChampagneGold),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = BrandChampagneGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل السعر", color = BrandChampagneGold)
                                }

                                IconButton(
                                    onClick = { onDeleteCatalogVenue(venue.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف المركز",
                                        tint = ErrorCrimson
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f).padding(start = 10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = venue.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${venue.cityName} (${venue.country}) • ${venue.category} • $${venue.startingPriceUsd}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandChampagneGold
                                )
                            }
                        }
                    }
                }
            }

            AdminControlSection.BOOKINGS -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, BrandChampagneGold.copy(alpha = 0.55f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "متابعة الحجوزات، قبولها، أو إلغاؤها (${bookingsList.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحكم كامل بجميع طلبات الحجز الواردة من العملاء في المدن السورية والتركية",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (bookingsList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد حجوزات معلقة حالياً",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ستظهر هنا جميع حجوزات العملاء فور إرسالها لقبولها أو إلغائها.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(bookingsList, key = { "admin_booking_${it.id}" }) { booking ->
                        val statusColor = when (booking.status) {
                            "confirmed" -> EmeraldVerified
                            "completed" -> BrandChampagneGold
                            else -> ErrorCrimson
                        }
                        val statusAr = when (booking.status) {
                            "confirmed" -> "مقبول ومؤكد"
                            "completed" -> "منفذ ومكتمل"
                            else -> "ملغى من الإدارة"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = statusColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, statusColor)
                                    ) {
                                        Text(
                                            text = statusAr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = "$${booking.totalPriceUsd}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = BrandChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = booking.venueName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${booking.cityName} (${booking.country}) • ${booking.serviceTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SoftGold
                                )
                                Text(
                                    text = "الموعد: ${booking.bookingDate} • الساعة ${booking.timeSlot} • العدد: ${booking.guestCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onAdminUpdateBookingStatus(booking.id, "confirmed") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldVerified,
                                            contentColor = ObsidianNight
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبول الحجز", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onAdminUpdateBookingStatus(booking.id, "cancelled") },
                                        border = BorderStroke(1.dp, ErrorCrimson),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = ErrorCrimson,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إلغاء الحجز", color = ErrorCrimson, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { onAdminDeleteBooking(booking.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف السجل",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminControlSection.WALLET -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, BrandChampagneGold.copy(alpha = 0.65f))
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
                                Text(
                                    text = "$$walletBalanceUsd",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = BrandChampagneGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "إدارة رصيد المحفظة وسجلات الدفع",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إضافة أو خصم رصيد المستخدمين ومراجعة العمليات المالية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = BrandChampagneGold.copy(alpha = 0.25f))

                            OutlinedTextField(
                                value = walletAdjustAmountText,
                                onValueChange = { walletAdjustAmountText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("المبلغ بالدولار ($)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_wallet_amount_input")
                            )

                            OutlinedTextField(
                                value = walletAdjustNote,
                                onValueChange = { walletAdjustNote = it },
                                label = { Text("بيان العملية (مثال: شحن VIP، مكافأة، تسوية حجز)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val amount = (walletAdjustAmountText.toIntOrNull() ?: 0).coerceIn(1, 100000)
                                        onAdminAdjustWalletBalance(amount, walletAdjustNote)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldVerified,
                                        contentColor = ObsidianNight
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("admin_credit_wallet_button")
                                ) {
                                    Text("إضافة شحن (+$$walletAdjustAmountText)", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val amount = (walletAdjustAmountText.toIntOrNull() ?: 0).coerceIn(1, 100000)
                                        onAdminAdjustWalletBalance(-amount, walletAdjustNote)
                                    },
                                    border = BorderStroke(1.dp, ErrorCrimson),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("admin_debit_wallet_button")
                                ) {
                                    Text("خصم رصيد (-$$walletAdjustAmountText)", color = ErrorCrimson, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "سجلات الدفع والشحن (${walletTransactions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                if (walletTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "لا توجد سجلات دفع أو شحن مسجلة حتى الآن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(walletTransactions, key = { "admin_tx_${it.id}" }) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (tx.isCredit) EmeraldVerified.copy(alpha = 0.45f) else ErrorCrimson.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (tx.isCredit) "+$${tx.amountUsd}" else "-$${tx.amountUsd}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (tx.isCredit) EmeraldVerified else ErrorCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tx.timestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminControlSection.SECURITY_BLOCK -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, ErrorCrimson.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نظام الحظر الإداري وحماية المنصة",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = blockIdentifierInput,
                                onValueChange = { blockIdentifierInput = it },
                                label = { Text("اسم المستخدم أو البريد أو المعرف المراد حظره") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_block_identifier_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = blockReasonInput,
                                    onValueChange = { blockReasonInput = it },
                                    label = { Text("سبب الحظر") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )

                                OutlinedTextField(
                                    value = blockCityScopeInput,
                                    onValueChange = { blockCityScopeInput = it },
                                    label = { Text("نطاق الحظر") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    if (blockIdentifierInput.trim().isNotEmpty()) {
                                        onBlockUser(
                                            blockIdentifierInput.trim(),
                                            blockReasonInput.trim(),
                                            blockCityScopeInput.trim()
                                        )
                                        blockIdentifierInput = ""
                                    }
                                },
                                enabled = blockIdentifierInput.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("admin_confirm_block_button")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تفعيل الحظر الفوري", fontWeight = FontWeight.Bold)
                            }

                            if (blockedUsers.isNotEmpty()) {
                                blockedUsers.forEach { blocked ->
                                    val isBlocked = blocked.status == "active"
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row {
                                                TextButton(
                                                    onClick = { onToggleBlockStatus(blocked.id, blocked.status) }
                                                ) {
                                                    Text(
                                                        text = if (isBlocked) "فك الحظر" else "إعادة الحظر",
                                                        color = if (isBlocked) EmeraldVerified else ErrorCrimson,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteBlockedRecord(blocked.id) }) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = blocked.blockedIdentifier,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${blocked.reason} • ${blocked.cityScope}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            AdminControlSection.ADMIN_SETTINGS -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, BrandChampagneGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "إعدادات أمان المسؤول الرئيسي (Super Admin)",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = BrandChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تغيير كلمة المرور وتحديث البريد الإلكتروني للمسؤول في أي وقت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Encrypted Hash & Credential Info Box
                            Surface(
                                color = ObsidianNight.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "البريد الإلكتروني المعتمد حالياً: ${credentialSnapshot.adminEmail}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldVerified,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "معيار التشفير: ${credentialSnapshot.hashAlgorithm}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SoftGold
                                    )
                                    Text(
                                        text = "بصمة التشفير (Hash): ${credentialSnapshot.passwordHashPreview}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandChampagneGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "آخر تحديث: ${credentialSnapshot.lastUpdatedLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = newAdminEmailInput,
                                onValueChange = {
                                    newAdminEmailInput = it
                                    credentialUpdateFeedback = null
                                },
                                label = { Text("البريد الإلكتروني للمسؤول الرئيسي") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_settings_email_input")
                            )

                            OutlinedTextField(
                                value = currentPasswordInput,
                                onValueChange = {
                                    currentPasswordInput = it
                                    credentialUpdateFeedback = null
                                },
                                label = { Text("كلمة المرور الحالية للمسؤول") },
                                singleLine = true,
                                visualTransformation = if (showPasswords) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPasswords = !showPasswords }) {
                                        Icon(
                                            imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = BrandChampagneGold
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_settings_current_password_input")
                            )

                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = {
                                    newPasswordInput = it
                                    credentialUpdateFeedback = null
                                },
                                label = { Text("كلمة المرور الجديدة (6 خانات على الأقل)") },
                                singleLine = true,
                                visualTransformation = if (showPasswords) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_settings_new_password_input")
                            )

                            OutlinedTextField(
                                value = confirmNewPasswordInput,
                                onValueChange = {
                                    confirmNewPasswordInput = it
                                    credentialUpdateFeedback = null
                                },
                                label = { Text("تأكيد كلمة المرور الجديدة") },
                                singleLine = true,
                                visualTransformation = if (showPasswords) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_settings_confirm_password_input")
                            )

                            credentialUpdateFeedback?.let { (isSuccess, message) ->
                                Surface(
                                    color = if (isSuccess) EmeraldVerified.copy(alpha = 0.16f) else ErrorCrimson.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSuccess) EmeraldVerified else ErrorCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSuccess) EmeraldVerified else ErrorCrimson,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (newPasswordInput != confirmNewPasswordInput) {
                                        credentialUpdateFeedback = false to "كلمة المرور الجديدة وتأكيدها غير متطابقين"
                                        return@Button
                                    }
                                    val result = SuperAdminSecurityManager.updateSuperAdminCredentials(
                                        context = context,
                                        sessionToken = adminSessionToken,
                                        currentPassword = currentPasswordInput,
                                        newEmail = newAdminEmailInput,
                                        newPassword = newPasswordInput
                                    )
                                    result.onSuccess { updatedSnapshot ->
                                        credentialSnapshot = updatedSnapshot
                                        currentPasswordInput = ""
                                        newPasswordInput = ""
                                        confirmNewPasswordInput = ""
                                        credentialUpdateFeedback = true to "تم تشفير وحفظ كلمة المرور والبريد الإلكتروني الجديد للمسؤول بنجاح!"
                                    }.onFailure { err ->
                                        credentialUpdateFeedback = false to (err.localizedMessage ?: "تعذر تحديث بيانات المسؤول")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandChampagneGold,
                                    contentColor = ObsidianNight
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("admin_save_credentials_button")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تشفير وحفظ بيانات اعتماد المسؤول الجديدة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Edit Dialog for Published Services & Posts
    editingService?.let { service ->
        var editTitle by remember(service.id) { mutableStateOf(service.title) }
        var editCity by remember(service.id) { mutableStateOf(service.cityName) }
        var editCountry by remember(service.id) { mutableStateOf(service.country) }
        var editCategory by remember(service.id) { mutableStateOf(service.category) }
        var editPrice by remember(service.id) { mutableStateOf(service.priceUsd.toString()) }
        var editDesc by remember(service.id) { mutableStateOf(service.description) }

        AlertDialog(
            onDismissRequest = { editingService = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل بيانات الخدمة والمنشور",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandChampagneGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان الخدمة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCity,
                            onValueChange = { editCity = it },
                            label = { Text("المدينة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it.filter { ch -> ch.isDigit() } },
                            label = { Text("السعر ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("القسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("تفاصيل العرض") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = editPrice.toIntOrNull() ?: service.priceUsd.toInt()
                        onUpdateServiceDetails(
                            service.id,
                            editTitle,
                            editCity,
                            editCountry,
                            editCategory,
                            parsedPrice,
                            editDesc
                        )
                        editingService = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandChampagneGold,
                        contentColor = ObsidianNight
                    )
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingService = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Price Edit Dialog for Catalog Venues
    editingCatalogVenue?.let { venue ->
        var newVenuePriceText by remember(venue.id) { mutableStateOf(venue.startingPriceUsd.toString()) }
        AlertDialog(
            onDismissRequest = { editingCatalogVenue = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل سعر المركز في الدليل",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandChampagneGold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "${venue.name} • ${venue.cityName}", color = SoftGold)
                    OutlinedTextField(
                        value = newVenuePriceText,
                        onValueChange = { newVenuePriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("السعر الجديد بالدولار ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newVenuePriceText.toIntOrNull() ?: venue.startingPriceUsd
                        onUpdateCatalogVenuePrice(venue.id, parsed)
                        editingCatalogVenue = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandChampagneGold,
                        contentColor = ObsidianNight
                    )
                ) {
                    Text("حفظ السعر", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCatalogVenue = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun Forbidden403RouteGuardScreen(
    errorTitle: String,
    errorMessage: String,
    onReturnToPublicHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .padding(24.dp)
            .testTag("forbidden_403_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = BrandDeepViolet
            ),
            border = BorderStroke(1.5.dp, ErrorCrimson)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GppBad,
                    contentDescription = "403 Forbidden",
                    tint = ErrorCrimson,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = errorTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ErrorCrimson,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReturnToPublicHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandChampagneGold,
                        contentColor = ObsidianNight
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("العودة إلى الصفحة الرئيسية", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminPublishedServiceCard(
    service: PublishedService,
    onOpenFullEdit: () -> Unit,
    onDelete: () -> Unit,
    onRebroadcast: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.4f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                SmartServiceMediaView(
                    imageUrl = service.imageUrl,
                    accentHex = 0xFFD6789BL,
                    secondaryHex = 0xFF47256BL,
                    category = service.category,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = BrandDustyRose,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${service.cityName} (${service.country}) • ${service.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = BrandChampagneGold,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "$${service.priceUsd}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ObsidianNight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenFullEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandChampagneGold,
                            contentColor = ObsidianNight
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل الخدمة", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRebroadcast,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandDustyRose)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = BrandDustyRose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إشعار", color = BrandDustyRose)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الخدمة",
                            tint = ErrorCrimson
                        )
                    }
                }
            }
        }
    }
}
