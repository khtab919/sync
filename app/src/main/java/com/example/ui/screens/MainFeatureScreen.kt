package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.catalog.LayaliCatalog
import com.example.data.model.Booking
import com.example.data.model.CityInfo
import com.example.data.model.CustomerReview
import com.example.data.model.FavoriteVenue
import com.example.data.model.PublishedService
import com.example.data.model.ServicePackage
import com.example.data.model.UserProfile
import com.example.data.model.Venue
import com.example.data.repository.LayaliRepository
import com.example.ui.auth.signOut
import com.example.ui.components.BrandActionBadgesRow
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.FullScreenMediaViewerDialog
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.WingedHeartLogo
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.BrandRoyalPlum
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.VioletBlueVip
import com.example.ui.theme.WarmCoral
import com.example.ui.viewmodel.LayaliViewModel
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.UiState
import com.example.util.LayaliNotificationHelper
import com.google.firebase.firestore.FirebaseFirestore

enum class MainTab(val titleAr: String, val tag: String) {
    EXPLORE("الرئيسية", "tab_explore"),
    REELS("الريلز", "tab_reels"),
    CHAT("الدردشة", "tab_chat"),
    WALLET("شراء رصيد", "tab_wallet"),
    BOOKINGS("الحجوزات", "tab_bookings"),
    FAVORITES("المفضلة", "tab_favorites"),
    PROFILE("الحساب", "tab_profile"),
    REVIEWS("التقييمات", "tab_reviews"),
    ADMIN("الإدارة", "tab_admin")
}

private data class MediaViewerPayload(
    val title: String,
    val cityName: String,
    val category: String,
    val priceUsd: Int,
    val primaryImageUrl: String,
    val videoTitle: String,
    val galleryCaptions: List<String>,
    val initialTabIsVideo: Boolean,
    val onBookNow: () -> Unit,
    val onEditPrice: () -> Unit,
    val onAddPhoto: (String) -> Unit
)

private data class PriceEditPayload(
    val title: String,
    val subtitle: String,
    val currentPriceUsd: Int,
    val onConfirmNewPrice: (Int) -> Unit
)

@Composable
fun MainFeatureScreen(
    currentUserId: String,
    userDisplayName: String,
    isGoogleSignedIn: Boolean = false,
    userEmail: String? = null,
    adminSessionToken: String? = null,
    isSuperAdmin: Boolean = false,
    onRequestGoogleSignIn: () -> Unit = {},
    onSuperAdminSignOut: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val viewModel: LayaliViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LayaliViewModel(LayaliRepository(db), currentUserId, app.applicationContext)
            }
        }
    )

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val filteredCities by viewModel.filteredCities.collectAsStateWithLifecycle()
    val filteredVenues by viewModel.filteredVenues.collectAsStateWithLifecycle()
    val selectedVenue by viewModel.selectedVenue.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val userReviewsState by viewModel.userReviewsState.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val publishedServicesState by viewModel.publishedServicesState.collectAsStateWithLifecycle()
    val blockedUsersState by viewModel.blockedUsersState.collectAsStateWithLifecycle()
    val incomingBroadcastAlert by viewModel.incomingBroadcastAlert.collectAsStateWithLifecycle()
    val isSoftRoseLightTheme by viewModel.isSoftRoseLightTheme.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val walletBalanceUsd by viewModel.walletBalanceUsd.collectAsStateWithLifecycle()
    val creatorAvailableUsd by viewModel.creatorAvailableUsd.collectAsStateWithLifecycle()
    val creatorReservedUsd by viewModel.creatorReservedUsd.collectAsStateWithLifecycle()
    val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
    val paymentReceipts by viewModel.paymentReceipts.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val lastInfrastructureSyncLabel by viewModel.lastInfrastructureSyncLabel.collectAsStateWithLifecycle()
    val chatMembers by viewModel.chatMembers.collectAsStateWithLifecycle()
    val unlockedChatMemberIds by viewModel.unlockedChatMemberIds.collectAsStateWithLifecycle()
    val activeChatMemberId by viewModel.activeChatMemberId.collectAsStateWithLifecycle()
    val chatMessagesByMember by viewModel.chatMessagesByMember.collectAsStateWithLifecycle()
    val deletedCatalogVenueIds by viewModel.deletedCatalogVenueIds.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(if (isSuperAdmin) MainTab.ADMIN else MainTab.EXPLORE) }
    var selectedInitialReelId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickOrderVenue by remember { mutableStateOf<Venue?>(null) }
    var activeCheckoutOrder by remember { mutableStateOf<Pair<Venue, ServicePackage>?>(null) }
    var activeMediaViewer by remember { mutableStateOf<MediaViewerPayload?>(null) }
    var activePriceEditor by remember { mutableStateOf<PriceEditPayload?>(null) }

    // Synchronize Super Admin session token, user profile, & perform automatic Role-Based Routing to Control Panel
    LaunchedEffect(userDisplayName, userEmail, adminSessionToken, isSuperAdmin) {
        viewModel.setSuperAdminSessionToken(adminSessionToken)
        viewModel.syncAuthenticatedAccount(
            authenticatedDisplayName = userDisplayName,
            isSuperAdmin = isSuperAdmin,
            emailOverride = userEmail
        )
        if (isSuperAdmin) {
            currentTab = MainTab.ADMIN
        } else if (currentTab == MainTab.ADMIN) {
            currentTab = MainTab.EXPLORE
        }
    }

    // Runtime permission launcher for Android 13+ POST_NOTIFICATIONS
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        LayaliNotificationHelper.ensureChannelCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LayaliNotificationHelper.hasNotificationPermission(context)
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Trigger Android system broadcast notification whenever a new service/offer is published
    LaunchedEffect(incomingBroadcastAlert) {
        incomingBroadcastAlert?.let { alert ->
            LayaliNotificationHelper.sendNewServiceBroadcastNotification(
                context = context,
                title = alert.title,
                cityName = alert.cityName,
                priceUsd = alert.priceUsd,
                description = alert.description,
                mediaType = alert.mediaType
            )
            viewModel.consumeBroadcastAlert()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val favoriteIds = remember(favoritesState) {
        when (val st = favoritesState) {
            is UiState.Success -> st.data.map { it.venueId }.toSet()
            else -> emptySet()
        }
    }

    val bookingsList = remember(bookingsState) {
        when (val st = bookingsState) {
            is UiState.Success -> st.data
            else -> emptyList()
        }
    }

    val userReviewsList = remember(userReviewsState) {
        when (val st = userReviewsState) {
            is UiState.Success -> st.data
            else -> emptyList()
        }
    }

    val publishedServicesList = remember(publishedServicesState) {
        when (val st = publishedServicesState) {
            is UiState.Success -> st.data
            else -> emptyList()
        }
    }

    val blockedUsersList = remember(blockedUsersState) {
        when (val st = blockedUsersState) {
            is UiState.Success -> st.data
            else -> emptyList()
        }
    }

    val blockedAuthorsSet = remember(blockedUsersList) {
        blockedUsersList.filter { it.status == "active" }.map { it.blockedIdentifier.trim() }.toSet()
    }

    val visibleVenues = remember(filteredVenues, blockedAuthorsSet) {
        filteredVenues.filterNot { v ->
            blockedAuthorsSet.contains(v.name.trim()) ||
                blockedAuthorsSet.contains(v.providerDisplayName.trim())
        }
    }

    val helperBookPublishedService: (PublishedService) -> Unit = { pub ->
        val pkg = ServicePackage(
            id = "pub_${pub.id}",
            title = pub.title,
            durationLabel = "حجز مباشر VIP",
            priceUsd = pub.priceUsd.toInt(),
            description = pub.description,
            isPopular = true
        )
        val updatedVenue = Venue(
            id = pub.id,
            name = pub.title,
            cityName = pub.cityName,
            country = pub.country,
            neighborhood = "",
            category = pub.category,
            shortDescription = pub.description,
            fullDescription = pub.description,
            ratingAverage = 0.0,
            reviewCount = 0,
            startingPriceUsd = pub.priceUsd.toInt(),
            openingHours = "يُحدّد حسب الطلب",
            badges = emptyList(),
            accentHex = 0xFF0D9488L,
            secondaryHex = 0xFF09111EL,
            packages = listOf(pkg),
            curatedReviews = emptyList(),
            availableTimeSlots = emptyList(),
            providerDisplayName = pub.title,
            providerUsername = "",
            providerLevel = 1,
            providerFrameId = "emerald_crown",
            providerChatPriceUsd = 0,
            isOnlineNow = false,
            visualPresetKey = pub.imageUrl,
            showcaseVideoTitle = "",
            showcaseDuration = "",
            galleryHighlights = emptyList()
        )
        activeCheckoutOrder = updatedVenue to pkg
    }

    val helperOpenPublishedMediaViewer: (PublishedService, Boolean) -> Unit = { pub, initialVideo ->
        activeMediaViewer = MediaViewerPayload(
            title = pub.title,
            cityName = "${pub.cityName} • ${pub.country}",
            category = pub.category,
            priceUsd = pub.priceUsd.toInt(),
            primaryImageUrl = pub.imageUrl,
            videoTitle = pub.videoUrl.ifBlank { "عرض فيديو 4K للخدمة في ${pub.cityName}" },
            galleryCaptions = listOf(
                "الجناح الملكي الرئيسي • ${pub.cityName}",
                "النسخة الفاتحة والناعمة VIP",
                "تفاصيل الضيافة والشموع العطرية",
                "صالة السهرات والموسيقى الحية"
            ),
            initialTabIsVideo = initialVideo,
            onBookNow = { helperBookPublishedService(pub) },
            onEditPrice = {
                activePriceEditor = PriceEditPayload(
                    title = pub.title,
                    subtitle = "${pub.cityName} • ${pub.category}",
                    currentPriceUsd = pub.priceUsd.toInt(),
                    onConfirmNewPrice = { newPrice ->
                        viewModel.updatePublishedPrice(pub.id, newPrice)
                    }
                )
            },
            onAddPhoto = { uriStr ->
                viewModel.addCustomProfilePhoto(uriStr)
            }
        )
    }

    val helperOpenVenueMediaViewer: (Venue, Boolean) -> Unit = { venue, initialVideo ->
        activeMediaViewer = MediaViewerPayload(
            title = venue.name,
            cityName = "${venue.cityName} • ${venue.country}",
            category = venue.category,
            priceUsd = venue.startingPriceUsd,
            primaryImageUrl = venue.visualPresetKey,
            videoTitle = venue.showcaseVideoTitle,
            galleryCaptions = venue.galleryHighlights,
            initialTabIsVideo = initialVideo,
            onBookNow = {
                val pkg = venue.packages.firstOrNull() ?: ServicePackage(
                    id = "default_${venue.id}",
                    title = venue.name,
                    durationLabel = "حجز مباشر VIP",
                    priceUsd = venue.startingPriceUsd,
                    description = venue.shortDescription,
                    isPopular = true
                )
                activeCheckoutOrder = venue to pkg
            },
            onEditPrice = {
                activePriceEditor = PriceEditPayload(
                    title = venue.name,
                    subtitle = "${venue.cityName} • ${venue.category}",
                    currentPriceUsd = venue.startingPriceUsd,
                    onConfirmNewPrice = { newPrice ->
                        viewModel.updateVenueStartingPrice(venue.id, newPrice)
                    }
                )
            },
            onAddPhoto = { uriStr ->
                viewModel.updateVenuePhoto(venue.id, uriStr)
            }
        )
    }

    val bottomNavigationTabs = remember {
        listOf(
            MainTab.EXPLORE,
            MainTab.REELS,
            MainTab.WALLET,
            MainTab.CHAT,
            MainTab.PROFILE
        )
    }

    MyApplicationTheme(isSoftRoseLightTheme = isSoftRoseLightTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (
                        selectedVenue == null &&
                        activeCheckoutOrder == null &&
                        currentTab != MainTab.CHAT &&
                        currentTab != MainTab.REELS &&
                        currentTab != MainTab.PROFILE
                    ) {
                        Surface(
                            onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                            color = VioletBlueVip,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("telegram_support_fab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "دعم تلجرام @RamaNUR",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "الدعم",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen && selectedVenue == null && activeCheckoutOrder == null) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            bottomNavigationTabs.forEach { tab ->
                                val selected = currentTab == tab
                                val isWalletCenterTab = tab == MainTab.WALLET
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        if (isWalletCenterTab) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(VioletBlueVip),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = "شراء رصيد",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = when (tab) {
                                                    MainTab.EXPLORE -> Icons.Default.Home
                                                    MainTab.REELS -> Icons.Default.PlayCircleFilled
                                                    MainTab.CHAT -> Icons.Default.Chat
                                                    MainTab.BOOKINGS -> Icons.Default.CalendarMonth
                                                    MainTab.WALLET -> Icons.Default.AccountBalanceWallet
                                                    MainTab.FAVORITES -> Icons.Default.FavoriteBorder
                                                    MainTab.PROFILE -> Icons.Default.Person
                                                    MainTab.REVIEWS -> Icons.Default.Verified
                                                    MainTab.ADMIN -> Icons.Default.AdminPanelSettings
                                                },
                                                contentDescription = tab.titleAr,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isWalletCenterTab) "المحفظة" else tab.titleAr,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VioletBlueVip,
                                        selectedTextColor = VioletBlueVip,
                                        indicatorColor = if (isWalletCenterTab) Color.Transparent else VioletBlueVip.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen && selectedVenue == null && activeCheckoutOrder == null) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val railTabs = remember(isSuperAdmin) {
                                MainTab.entries.filter { isSuperAdmin || it != MainTab.ADMIN }
                            }
                            railTabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                MainTab.EXPLORE -> Icons.Default.Home
                                                MainTab.REELS -> Icons.Default.PlayCircleFilled
                                                MainTab.CHAT -> Icons.Default.Chat
                                                MainTab.BOOKINGS -> Icons.Default.CalendarMonth
                                                MainTab.WALLET -> Icons.Default.AccountBalanceWallet
                                                MainTab.FAVORITES -> Icons.Default.FavoriteBorder
                                                MainTab.PROFILE -> Icons.Default.Person
                                                MainTab.REVIEWS -> Icons.Default.Verified
                                                MainTab.ADMIN -> Icons.Default.AdminPanelSettings
                                            },
                                            contentDescription = tab.titleAr
                                        )
                                    },
                                    label = { Text(tab.titleAr) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val checkoutTarget = activeCheckoutOrder
                        val activeVenue = selectedVenue
                        if (checkoutTarget != null) {
                            // Screen 5: Checkout & Real Payment Verification Steps Screen
                            CheckoutScreen(
                                venue = checkoutTarget.first,
                                selectedPackage = checkoutTarget.second,
                                walletBalanceUsd = walletBalanceUsd,
                                isSubmitting = isSubmitting,
                                onBack = { activeCheckoutOrder = null },
                                onOpenWalletTopUp = {
                                    activeCheckoutOrder = null
                                    viewModel.closeVenueDetail()
                                    currentTab = MainTab.WALLET
                                },
                                onConfirmCheckout = { date, slot, guests, notes, paymentMethod, receiptUri, txRef ->
                                    // The backend debits and confirms atomically; never pre-debit locally.
                                    viewModel.createDirectBooking(
                                        venue = checkoutTarget.first,
                                        serviceTitle = "${checkoutTarget.second.title} • ${paymentMethod.titleAr}",
                                        unitPriceUsd = checkoutTarget.second.priceUsd,
                                        bookingDate = date,
                                        timeSlot = slot,
                                        guestCount = guests,
                                        notes = notes,
                                        paymentMethodTitle = paymentMethod.titleAr,
                                        receiptImageUri = receiptUri,
                                        transactionReference = txRef,
                                        onSuccess = {
                                            activeCheckoutOrder = null
                                            viewModel.closeVenueDetail()
                                            currentTab = MainTab.BOOKINGS
                                        }
                                    )
                                },
                                onShowMessage = viewModel::showInfoMessage
                            )
                        } else if (activeVenue != null) {
                            // Screen 3: Modern Creator & VIP Club Details Screen with Choose a Plan Sheet
                            VenueDetailScreen(
                                venue = activeVenue,
                                liveReviews = userReviewsList.filter { it.venueId == activeVenue.id },
                                isFavorite = favoriteIds.contains(activeVenue.id),
                                isSubmitting = isSubmitting,
                                isAdminUnlocked = isSuperAdmin,
                                walletBalanceUsd = walletBalanceUsd,
                                onBack = { viewModel.closeVenueDetail() },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(
                                        activeVenue,
                                        favoriteIds.contains(activeVenue.id)
                                    )
                                },
                                onProceedToCheckout = { pkg ->
                                    activeCheckoutOrder = activeVenue to pkg
                                },
                                onSubmitReview = { author, service, rating, comment ->
                                    viewModel.submitCustomerReview(
                                        authorName = author,
                                        venue = activeVenue,
                                        serviceType = service,
                                        rating = rating,
                                        comment = comment
                                    )
                                },
                                onDeleteReview = viewModel::deleteCustomerReview,
                                onUpdateStartingPrice = { newPrice ->
                                    viewModel.updateVenueStartingPrice(activeVenue.id, newPrice)
                                },
                                onUpdatePhoto = { newPhotoUri ->
                                    viewModel.updateVenuePhoto(activeVenue.id, newPhotoUri)
                                },
                                onOpenWalletTopUp = {
                                    viewModel.closeVenueDetail()
                                    currentTab = MainTab.WALLET
                                }
                            )
                        } else {
                            when (currentTab) {
                                MainTab.EXPLORE -> ExploreTabContent(
                                    userProfile = userProfile,
                                    userDisplayName = userProfile.displayName.ifBlank { userDisplayName },
                                    userAvatarUri = userProfile.avatarUri,
                                    walletBalanceUsd = walletBalanceUsd,
                                    selectedCountry = selectedCountry,
                                    selectedCity = selectedCity,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    sortOption = sortOption,
                                    cities = filteredCities,
                                    venues = visibleVenues,
                                    publishedServices = publishedServicesList.filter { it.authorId == viewModel.currentUserId },
                                    favoriteIds = favoriteIds,
                                    isSoftRoseLightTheme = isSoftRoseLightTheme,
                                    isSuperAdmin = isSuperAdmin,
                                    onToggleThemeMode = viewModel::toggleThemeMode,
                                    onOpenLoginScreen = onRequestGoogleSignIn,
                                    onOpenWalletTab = { currentTab = MainTab.WALLET },
                                    onOpenChatTab = { currentTab = MainTab.CHAT },
                                    onOpenBookingsTab = { currentTab = MainTab.BOOKINGS },
                                    onOpenProfileTab = { currentTab = MainTab.PROFILE },
                                    onOpenAdminPanel = { currentTab = MainTab.ADMIN },
                                    onOpenReviewsTab = { currentTab = MainTab.REVIEWS },
                                    onOpenReelsTab = { initialId ->
                                        selectedInitialReelId = initialId
                                        currentTab = MainTab.REELS
                                    },
                                    onSelectCountry = viewModel::selectCountry,
                                    onSelectCity = viewModel::selectCity,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onSelectSort = viewModel::selectSortOption,
                                    onOpenVenue = viewModel::openVenueDetail,
                                    onQuickBookVenue = { venue ->
                                        val pkg = venue.packages.firstOrNull() ?: ServicePackage(
                                            id = "default_${venue.id}",
                                            title = venue.name,
                                            durationLabel = "حجز مباشر VIP",
                                            priceUsd = venue.startingPriceUsd,
                                            description = venue.shortDescription,
                                            isPopular = true
                                        )
                                        activeCheckoutOrder = venue to pkg
                                    },
                                    onBookPublishedService = helperBookPublishedService,
                                    onOpenPublishedMedia = helperOpenPublishedMediaViewer,
                                    onEditPublishedPrice = { pub ->
                                        activePriceEditor = PriceEditPayload(
                                            title = pub.title,
                                            subtitle = "${pub.cityName} • ${pub.category}",
                                            currentPriceUsd = pub.priceUsd.toInt(),
                                            onConfirmNewPrice = { newPrice ->
                                                viewModel.updatePublishedPrice(pub.id, newPrice)
                                            }
                                        )
                                    },
                                    onOpenVenueMedia = helperOpenVenueMediaViewer,
                                    onEditVenuePrice = { venue ->
                                        activePriceEditor = PriceEditPayload(
                                            title = venue.name,
                                            subtitle = "${venue.cityName} • ${venue.category}",
                                            currentPriceUsd = venue.startingPriceUsd,
                                            onConfirmNewPrice = { newPrice ->
                                                viewModel.updateVenueStartingPrice(venue.id, newPrice)
                                            }
                                        )
                                    },
                                    onBlockVenueProvider = { venue ->
                                        viewModel.blockUserAccount(
                                            identifier = venue.providerDisplayName.ifBlank { venue.name },
                                            reason = "حظر مباشر من صفحة العرض",
                                            cityScope = venue.cityName
                                        )
                                    },
                                    onToggleFavorite = { venue ->
                                        viewModel.toggleFavorite(venue, favoriteIds.contains(venue.id))
                                    }
                                )

                                MainTab.REELS -> ReelsScreen(
                                    venues = visibleVenues,
                                    publishedServices = publishedServicesList,
                                    favoriteVenueIds = favoriteIds,
                                    initialReelId = selectedInitialReelId,
                                    isSuperAdmin = isSuperAdmin,
                                    onBookVenue = { venue ->
                                        val pkg = venue.packages.firstOrNull() ?: ServicePackage(
                                            id = "default_${venue.id}",
                                            title = venue.name,
                                            durationLabel = "حجز مباشر VIP",
                                            priceUsd = venue.startingPriceUsd,
                                            description = venue.shortDescription,
                                            isPopular = true
                                        )
                                        activeCheckoutOrder = venue to pkg
                                    },
                                    onBookPublished = helperBookPublishedService,
                                    onOpenVenueDetails = viewModel::openVenueDetail,
                                    onOpenPublishedMedia = { pub -> helperOpenPublishedMediaViewer(pub, true) },
                                    onToggleFavoriteVenue = { venue ->
                                        viewModel.toggleFavorite(venue, favoriteIds.contains(venue.id))
                                    },
                                    onEditReelPrice = { reel ->
                                        val pub = reel.sourcePublished
                                        val ven = reel.sourceVenue
                                        if (pub != null) {
                                            activePriceEditor = PriceEditPayload(
                                                title = pub.title,
                                                subtitle = "${pub.cityName} • ${pub.category}",
                                                currentPriceUsd = pub.priceUsd.toInt(),
                                                onConfirmNewPrice = { newPrice ->
                                                    viewModel.updatePublishedPrice(pub.id, newPrice)
                                                }
                                            )
                                        } else if (ven != null) {
                                            activePriceEditor = PriceEditPayload(
                                                title = ven.name,
                                                subtitle = "${ven.cityName} • ${ven.category}",
                                                currentPriceUsd = ven.startingPriceUsd,
                                                onConfirmNewPrice = { newPrice ->
                                                    viewModel.updateVenueStartingPrice(ven.id, newPrice)
                                                }
                                            )
                                        }
                                    },
                                    onOpenAdminPublishReel = { currentTab = MainTab.ADMIN },
                                    onOpenCreatorChat = { currentTab = MainTab.CHAT },
                                    onShowMessage = viewModel::showInfoMessage
                                )

                                MainTab.CHAT -> ChatScreen(
                                    userProfile = userProfile,
                                    walletBalanceUsd = walletBalanceUsd,
                                    chatMembers = chatMembers,
                                    unlockedMemberIds = unlockedChatMemberIds,
                                    activeConversationMemberId = activeChatMemberId,
                                    messagesByMember = chatMessagesByMember,
                                    onSelectConversationMember = viewModel::selectActiveChatMember,
                                    onUnlockChatWithWallet = viewModel::unlockChatWithWallet,
                                    onUnlockChatWithReceipt = viewModel::unlockChatWithReceipt,
                                    onSendChatMessage = viewModel::sendChatMessage,
                                    onUpdateMyChatPrice = viewModel::updateChatPricing,
                                    onOpenWalletTab = { currentTab = MainTab.WALLET },
                                    onShowMessage = viewModel::showInfoMessage
                                )

                                MainTab.WALLET -> WalletScreen(
                                    walletBalanceUsd = walletBalanceUsd,
                                    creatorAvailableUsd = creatorAvailableUsd,
                                    creatorReservedUsd = creatorReservedUsd,
                                    onRequestCreatorWithdrawal = viewModel::submitCreatorWithdrawal,
                                    transactions = walletTransactions,
                                    paymentReceipts = paymentReceipts,
                                    onBack = { currentTab = MainTab.EXPLORE },
                                    onSubmitRealDepositReceipt = viewModel::submitRealDepositReceipt,
                                    onTopUpConfirm = { amount, bonus, pkgTitle ->
                                        viewModel.topUpWallet(amount, bonus, pkgTitle)
                                    },
                                    onShowMessage = viewModel::showInfoMessage
                                )

                                MainTab.PROFILE -> ProfileScreen(
                                    userProfile = userProfile,
                                    walletBalanceUsd = walletBalanceUsd,
                                    walletTransactions = walletTransactions,
                                    isGoogleSignedIn = isGoogleSignedIn,
                                    userEmail = userEmail,
                                    publishedServices = publishedServicesList,
                                    bookingsCount = bookingsList.size,
                                    reviewsCount = userReviewsList.size,
                                    favoritesCount = favoriteIds.size,
                                    blockedUsers = blockedUsersList,
                                    isSoftRoseLightTheme = isSoftRoseLightTheme,
                                    onToggleThemeMode = viewModel::toggleThemeMode,
                                    onUpdateProfile = viewModel::updateUserProfile,
                                    onUpdateUsername = viewModel::updateUsername,
                                    onSelectAvatarFrame = viewModel::selectAvatarFrame,
                                    onUpdateChatPricing = viewModel::updateChatPricing,
                                    onGainProfileXp = viewModel::gainProfileXp,
                                    onUpdateAvatar = viewModel::updateProfileAvatar,
                                    onAddShowcasePhoto = { uriStr ->
                                        viewModel.addCustomProfilePhoto(uriStr)
                                        viewModel.publishNewService(
                                            title = "${userProfile.displayName.substringBefore("•").trim()} • عرض خاص جديد",
                                            cityName = userProfile.cityName.substringBefore("•").trim().ifBlank { "دمشق" },
                                            country = "سوريا",
                                            category = "سبا ومساج",
                                            description = "صورة عرض جديدة مضافة من الملف الشخصي مع إمكانية الحجز المباشر.",
                                            priceUsd = 80,
                                            imageUrl = uriStr,
                                            videoUrl = "عرض فيديو 4K مرفق",
                                            mediaType = "both"
                                        )
                                    },
                                    onOpenMediaViewer = helperOpenPublishedMediaViewer,
                                    onEditServicePrice = { pub ->
                                        activePriceEditor = PriceEditPayload(
                                            title = pub.title,
                                            subtitle = "${pub.cityName} • ${pub.category}",
                                            currentPriceUsd = pub.priceUsd.toInt(),
                                            onConfirmNewPrice = { newPrice ->
                                                viewModel.updatePublishedPrice(pub.id, newPrice)
                                            }
                                        )
                                    },
                                    onOpenAdminTab = { currentTab = MainTab.ADMIN },
                                    onOpenBookingsTab = { currentTab = MainTab.BOOKINGS },
                                    onOpenFavoritesTab = { currentTab = MainTab.FAVORITES },
                                    onOpenExploreTab = { currentTab = MainTab.EXPLORE },
                                    onOpenChatTab = { currentTab = MainTab.CHAT },
                                    onOpenWalletTab = { currentTab = MainTab.WALLET },
                                    onQuickBlockUser = { identifier, reason ->
                                        viewModel.blockUserAccount(
                                            identifier = identifier,
                                            reason = reason,
                                            cityScope = "جميع المدن السورية والتركية"
                                        )
                                    },
                                    onToggleBlockStatus = viewModel::toggleBlockUserStatus,
                                    onTriggerBroadcastNotification = {
                                        LayaliNotificationHelper.sendNewServiceBroadcastNotification(
                                            context = context,
                                            title = "عرض ملكي جديد من ${userProfile.displayName}",
                                            cityName = userProfile.cityName,
                                            priceUsd = 75L,
                                            description = "جلسة مساج ملكي وسهرة VIP متاحة الآن للحجز المباشر!",
                                            mediaType = "both"
                                        )
                                        viewModel.showInfoMessage("تم إرسال إشعار أندرويد الفوري إلى جميع المستخدمين!")
                                    },
                                    onOpenGoogleSignIn = onRequestGoogleSignIn,
                                    onSignOut = {
                                        viewModel.clearAuthenticatedAccount()
                                        if (isSuperAdmin) {
                                            onSuperAdminSignOut()
                                        }
                                        signOut(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = onSignedOut,
                                            scope = coroutineScope
                                        )
                                    },
                                    onPurchaseContentWithWallet = { amountUsd, itemTitle ->
                                        viewModel.showInfoMessage("فتح المحتوى المدفوع يتطلب تأكيد عملية الشراء من الخادم.")
                                        false
                                    },
                                    onShowMessage = viewModel::showInfoMessage,
                                    onPublishPaidPost = viewModel::publishCreatorPaidPost,
                                    isSuperAdmin = isSuperAdmin,
                                    savedAccounts = savedAccounts,
                                    lastInfrastructureSyncLabel = lastInfrastructureSyncLabel,
                                    onSaveInfrastructureNow = {
                                        viewModel.saveInfrastructureToDatabase(
                                            emailOverride = userEmail,
                                            showSnackbar = true
                                        )
                                    },
                                    onChangeAccountPassword = { currentPass, newPass, requireCurrent ->
                                        viewModel.changeAccountPassword(
                                            currentPassword = currentPass,
                                            newPassword = newPass,
                                            requireCurrentPassword = requireCurrent
                                        )
                                    }
                                )

                                MainTab.BOOKINGS -> BookingsTabContent(
                                    bookingsState = bookingsState,
                                    onUpdateStatus = viewModel::updateBookingStatus,
                                    onReschedule = viewModel::rescheduleBooking,
                                    onDeleteBooking = viewModel::deleteBooking,
                                    onExploreClick = { currentTab = MainTab.EXPLORE }
                                )

                                MainTab.REVIEWS -> VerifiedReviewsTabContent(
                                    userReviewsState = userReviewsState,
                                    defaultAuthorName = userProfile.displayName.ifBlank { userDisplayName },
                                    isSubmitting = isSubmitting,
                                    blockedAuthors = blockedAuthorsSet,
                                    isSuperAdmin = isSuperAdmin,
                                    onBlockAuthor = { authorToBlock ->
                                        viewModel.blockUserAccount(
                                            identifier = authorToBlock,
                                            reason = "مخالفة معايير التقييم الموثق",
                                            cityScope = "جميع المدن السورية والتركية"
                                        )
                                    },
                                    onSubmitReviewForVenue = { venue, author, service, rating, comment ->
                                        viewModel.submitCustomerReview(
                                            authorName = author,
                                            venue = venue,
                                            serviceType = service,
                                            rating = rating,
                                            comment = comment
                                        )
                                    },
                                    onDeleteUserReview = viewModel::deleteCustomerReview,
                                    onOpenVenue = viewModel::openVenueDetail
                                )

                                MainTab.FAVORITES -> FavoritesTabContent(
                                    favoritesState = favoritesState,
                                    onOpenVenue = { venueId ->
                                        filteredVenues.find { it.id == venueId }?.let {
                                            viewModel.openVenueDetail(it)
                                        }
                                    },
                                    onRemoveFavorite = { fav ->
                                        filteredVenues.find { it.id == fav.venueId }?.let {
                                            viewModel.toggleFavorite(it, true)
                                        }
                                    },
                                    onExploreClick = { currentTab = MainTab.EXPLORE }
                                )

                                MainTab.ADMIN -> AdminManagementScreen(
                                    adminSessionToken = adminSessionToken,
                                    publishedServicesState = publishedServicesState,
                                    catalogVenues = visibleVenues,
                                    deletedCatalogCount = deletedCatalogVenueIds.size,
                                    bookingsState = bookingsState,
                                    walletBalanceUsd = walletBalanceUsd,
                                    walletTransactions = walletTransactions,
                                    blockedUsersState = blockedUsersState,
                                    isSoftRoseLightTheme = isSoftRoseLightTheme,
                                    isSubmitting = isSubmitting,
                                    onToggleThemeMode = viewModel::toggleThemeMode,
                                    onPublishService = { title, city, country, cat, price, desc, imgUri, vidUrl ->
                                        val mediaType = if (vidUrl.isNotBlank()) "both" else "image"
                                        viewModel.publishNewService(
                                            title = title,
                                            cityName = city,
                                            country = country,
                                            category = cat,
                                            description = desc,
                                            priceUsd = price,
                                            imageUrl = imgUri,
                                            videoUrl = vidUrl,
                                            mediaType = mediaType,
                                            onPublished = { published ->
                                                LayaliNotificationHelper.sendNewServiceBroadcastNotification(
                                                    context = context,
                                                    title = published.title,
                                                    cityName = published.cityName,
                                                    priceUsd = published.priceUsd,
                                                    description = published.description,
                                                    mediaType = published.mediaType
                                                )
                                            }
                                        )
                                    },
                                    onUpdateServicePrice = viewModel::updatePublishedPrice,
                                    onUpdateServiceDetails = viewModel::updatePublishedServiceDetails,
                                    onDeletePublishedService = viewModel::deletePublishedService,
                                    onUpdateCatalogVenuePrice = viewModel::updateVenueStartingPrice,
                                    onDeleteCatalogVenue = viewModel::deleteCatalogVenue,
                                    onRestoreDeletedCatalogVenues = viewModel::restoreDeletedCatalogVenues,
                                    onAdminUpdateBookingStatus = viewModel::adminUpdateBookingStatus,
                                    onAdminDeleteBooking = viewModel::deleteBooking,
                                    onAdminAdjustWalletBalance = viewModel::adminAdjustWalletBalance,
                                    onBlockUser = viewModel::blockUserAccount,
                                    onToggleBlockStatus = viewModel::toggleBlockUserStatus,
                                    onDeleteBlockedRecord = viewModel::deleteBlockedRecord,
                                    onTriggerInstantNotification = { title, city, price, cat ->
                                        LayaliNotificationHelper.sendNewServiceBroadcastNotification(
                                            context = context,
                                            title = title,
                                            cityName = city,
                                            priceUsd = price.toLong(),
                                            description = "عرض $cat جديد متاح الآن للحجز المباشر",
                                            mediaType = "both"
                                        )
                                        viewModel.showInfoMessage("تم إرسال إشعار أندرويد الفوري إلى شريط الإشعارات!")
                                    },
                                    onNavigateToPublicStore = { currentTab = MainTab.EXPLORE },
                                    onSuperAdminSignOut = {
                                        viewModel.clearAuthenticatedAccount()
                                        onSuperAdminSignOut()
                                        currentTab = MainTab.EXPLORE
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Media Viewer Modal (Video + Photo Album + Upload + Edit Price + Book)
        activeMediaViewer?.let { payload ->
            FullScreenMediaViewerDialog(
                title = payload.title,
                cityName = payload.cityName,
                category = payload.category,
                priceUsd = payload.priceUsd,
                primaryImageUrl = payload.primaryImageUrl,
                videoTitle = payload.videoTitle,
                galleryCaptions = payload.galleryCaptions,
                initialTabIsVideo = payload.initialTabIsVideo,
                onDismiss = { activeMediaViewer = null },
                onBookNow = payload.onBookNow,
                onEditPriceClick = payload.onEditPrice,
                onAddCustomPhoto = payload.onAddPhoto,
                isSuperAdmin = isSuperAdmin
            )
        }

        // Quick Price Edit Dialog
        activePriceEditor?.let { pricePayload ->
            var priceText by remember(pricePayload) {
                mutableStateOf(pricePayload.currentPriceUsd.toString())
            }
            AlertDialog(
                onDismissRequest = { activePriceEditor = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "وضع وتعديل السعر ($)",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandChampagneGold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = pricePayload.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = pricePayload.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandDustyRose
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("السعر الجديد بالدولار ($)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("global_quick_price_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = priceText.toIntOrNull() ?: pricePayload.currentPriceUsd
                            pricePayload.onConfirmNewPrice(parsed)
                            activePriceEditor = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandDustyRose,
                            contentColor = Color.White
                        )
                    ) {
                        Text("حفظ السعر المعتمد", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activePriceEditor = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Choose a Plan Bottom Sheet for Quick Orders
        quickOrderVenue?.let { venue ->
            ChoosePlanBookingBottomSheet(
                venue = venue,
                walletBalanceUsd = walletBalanceUsd,
                onDismiss = { quickOrderVenue = null },
                onConfirmPlanBooking = { chosenPkg, _ ->
                    quickOrderVenue = null
                    activeCheckoutOrder = venue to chosenPkg
                },
                onOpenWalletTopUp = {
                    quickOrderVenue = null
                    currentTab = MainTab.WALLET
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreTabContent(
    userProfile: UserProfile,
    userDisplayName: String,
    userAvatarUri: String,
    walletBalanceUsd: Int,
    selectedCountry: String,
    selectedCity: String?,
    selectedCategory: String,
    searchQuery: String,
    sortOption: SortOption,
    cities: List<CityInfo>,
    venues: List<Venue>,
    publishedServices: List<PublishedService>,
    favoriteIds: Set<String>,
    isSoftRoseLightTheme: Boolean,
    isSuperAdmin: Boolean,
    onToggleThemeMode: () -> Unit,
    onOpenLoginScreen: () -> Unit,
    onOpenWalletTab: () -> Unit,
    onOpenChatTab: () -> Unit,
    onOpenBookingsTab: () -> Unit,
    onOpenProfileTab: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenReviewsTab: () -> Unit,
    onOpenReelsTab: (String?) -> Unit,
    onSelectCountry: (String) -> Unit,
    onSelectCity: (String?) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onOpenVenue: (Venue) -> Unit,
    onQuickBookVenue: (Venue) -> Unit,
    onBookPublishedService: (PublishedService) -> Unit,
    onOpenPublishedMedia: (PublishedService, Boolean) -> Unit,
    onEditPublishedPrice: (PublishedService) -> Unit,
    onOpenVenueMedia: (Venue, Boolean) -> Unit,
    onEditVenuePrice: (Venue) -> Unit,
    onBlockVenueProvider: (Venue) -> Unit,
    onToggleFavorite: (Venue) -> Unit
) {
    val context = LocalContext.current
    var showFiltersRow by rememberSaveable { mutableStateOf(false) }
    var visibleVenuesCount by rememberSaveable(selectedCountry, selectedCity, selectedCategory, searchQuery) {
        mutableIntStateOf(6)
    }
    val displayedVenues = remember(venues, visibleVenuesCount) {
        venues.take(visibleVenuesCount)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Clean, Minimal Top Header Bar
        item(key = "explore_top_header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            LayaliNotificationHelper.sendNewServiceBroadcastNotification(
                                context = context,
                                title = "عرض خاص VIP",
                                cityName = selectedCity ?: "دمشق وإسطنبول",
                                priceUsd = 50L,
                                description = "احجز سهرتك الآن",
                                mediaType = "both"
                            )
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("home_notification_bell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "الإشعارات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Compact Wallet Balance Pill
                    Surface(
                        onClick = onOpenWalletTab,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("top_wallet_balance_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SovereignGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$$walletBalanceUsd",
                                fontSize = 11.sp,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenBookingsTab,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("top_bookings_shortcut_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "حجوزاتي",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right Profile & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ليالي • Layali",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = SovereignGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "LEVEL ${userProfile.level}",
                                fontSize = 9.sp,
                                color = SovereignGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clickable { onOpenProfileTab() }
                            .testTag("open_profile_top_button")
                    ) {
                        FramedUserAvatar(
                            avatarUri = userAvatarUri,
                            fallbackInitial = userProfile.displayName.take(1).ifBlank { "L" },
                            frameId = userProfile.selectedFrameId,
                            level = userProfile.level,
                            avatarSize = 38.dp,
                            showLevelBadge = false
                        )
                    }
                }
            }
        }

        // 2. Compact Search & Filter Bar
        item(key = "explore_search_bar", contentType = "search") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "ابحث عن سهرة أو مدينة...",
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "مسح البحث",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_venues_input")
                )

                Surface(
                    onClick = { showFiltersRow = !showFiltersRow },
                    color = if (showFiltersRow) CoralPinkPrimary else MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("toggle_filters_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "الفلاتر",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. Compact Horizontal Category Pills
        item(key = "explore_categories_row", contentType = "categories") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LayaliCatalog.categories, key = { it }) { category ->
                        val isSelected = selectedCategory == category
                        LuxuryCategoryCard(
                            category = category,
                            isSelected = isSelected,
                            onClick = { onSelectCategory(category) }
                        )
                    }
                }
            }
        }

        // 3b. Horizontal 4K Reels Preview Strip for Web & App
        item(key = "explore_reels_strip", contentType = "reels_strip") {
            HomeReelsPreviewStrip(
                venues = venues,
                publishedServices = publishedServices,
                onOpenReelItem = { reelId -> onOpenReelsTab(reelId) },
                onOpenAllReels = { onOpenReelsTab(null) }
            )
        }

        // 4. Collapsible Country & City Filter Strip
        if (showFiltersRow) {
            item(key = "explore_city_filters", contentType = "city_filters") {
                Column(modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LayaliCatalog.countries.forEach { country ->
                                FilterChip(
                                    selected = selectedCountry == country,
                                    onClick = { onSelectCountry(country) },
                                    label = { Text(country) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandChampagneGold,
                                        selectedLabelColor = ObsidianNight
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "المدن السورية والتركية",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            CityChipCard(
                                cityName = "جميع المدن",
                                country = "${cities.size} مدينة",
                                isSelected = selectedCity == null,
                                onClick = { onSelectCity(null) }
                            )
                        }
                        items(cities, key = { it.id }) { city ->
                            CityChipCard(
                                cityName = city.nameAr,
                                country = city.country,
                                isSelected = selectedCity == city.nameAr,
                                onClick = { onSelectCity(city.nameAr) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Section Header: "الخدمات" + Sort Pills + Admin/Reviews Quick Access
        item(key = "explore_sort_header", contentType = "sort_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SortOption.entries.forEach { option ->
                            val active = sortOption == option
                            Surface(
                                color = if (active) CoralPinkPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    0.7.dp,
                                    if (active) CoralPinkPrimary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.clickable { onSelectSort(option) }
                            ) {
                                Text(
                                    text = option.labelAr,
                                    fontSize = 11.sp,
                                    color = if (active) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "السهرات (${venues.size})",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. 2-Column Featured Services Grid matching Screen 2 of the UI/UX Case Study
        if (venues.isNotEmpty()) {
            val gridVenues = venues.take(4)
            val gridRows = gridVenues.chunked(2)
            items(gridRows.size, key = { "grid_row_$it" }) { rowIndex ->
                val pair = gridRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { venue ->
                        CompactServiceGridCard(
                            venue = venue,
                            isFavorite = favoriteIds.contains(venue.id),
                            onOpenDetails = { onOpenVenue(venue) },
                            onBookNow = { onQuickBookVenue(venue) },
                            onToggleFavorite = { onToggleFavorite(venue) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 7. Live Published Services & Offers Showcase (Zero Admin UI for Customers)
        if (publishedServices.isNotEmpty()) {
            item(key = "explore_published_offers", contentType = "published_offers") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSuperAdmin) {
                            Surface(
                                onClick = onOpenAdminPanel,
                                color = BrandDustyRose.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandChampagneGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = BrandChampagneGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "لوحة التحكم (${publishedServices.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "عروض VIP المنشورة (صور وفيديو 4K)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BrandDustyRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(publishedServices, key = { "pub_explore_${it.id}" }) { pub ->
                            PublishedServiceOfferCard(
                                service = pub,
                                isSuperAdmin = isSuperAdmin,
                                onBookOffer = { onBookPublishedService(pub) },
                                onOpenVideo = { onOpenPublishedMedia(pub, true) },
                                onOpenPhotos = { onOpenPublishedMedia(pub, false) },
                                onEditPrice = { onEditPublishedPrice(pub) }
                            )
                        }
                    }
                }
            }
        }

        // 8. Full Detailed Service Cards List for All Syrian & Turkish Cities
        if (venues.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = RoyalGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد مراكز مطابقة للفلتر الحالي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            onSelectCountry("الكل")
                            onSelectCity(null)
                            onSelectCategory("الكل")
                            onSearchChange("")
                        }
                    ) {
                        Text("إعادة ضبط جميع الفلاتر", color = RoyalGold)
                    }
                }
            }
        } else {
            item(key = "explore_all_venues_header", contentType = "venues_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onOpenReviewsTab) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BrandChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جميع التقييمات الموثوقة", color = BrandChampagneGold)
                    }
                    Text(
                        text = "جميع المراكز والسهرات المتاحة",
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(
                items = displayedVenues,
                key = { it.id },
                contentType = { "venue_card" }
            ) { venue ->
                VenueCard(
                    venue = venue,
                    isFavorite = favoriteIds.contains(venue.id),
                    isSuperAdmin = isSuperAdmin,
                    onClick = { onOpenVenue(venue) },
                    onQuickBook = { onQuickBookVenue(venue) },
                    onOpenVideo = { onOpenVenueMedia(venue, true) },
                    onOpenPhotos = { onOpenVenueMedia(venue, false) },
                    onEditPrice = { onEditVenuePrice(venue) },
                    onBlockProvider = { onBlockVenueProvider(venue) },
                    onToggleFavorite = { onToggleFavorite(venue) }
                )
            }

            if (venues.size > visibleVenuesCount) {
                item(key = "explore_load_more_venues", contentType = "load_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = { visibleVenuesCount += 8 },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.2.dp, BrandChampagneGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("load_more_venues_button")
                        ) {
                            Text(
                                text = "عرض المزيد من المراكز والسهرات (${venues.size - visibleVenuesCount} متبقية)",
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxuryCategoryCard(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) {
            CoralPinkPrimary // Deep Teal #0D9488
        } else {
            MaterialTheme.colorScheme.surface // Royal Navy Card #111D30
        },
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isSelected) CoralPinkPrimary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .height(34.dp)
            .testTag("category_card_$category")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = category,
                fontSize = 12.sp,
                color = PureWhite,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactServiceGridCard(
    venue: Venue,
    isFavorite: Boolean,
    onOpenDetails: () -> Unit,
    onBookNow: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVip = venue.startingPriceUsd >= 90 || venue.badges.any { it.contains("VIP", ignoreCase = true) }

    Card(
        modifier = modifier
            .clickable { onOpenDetails() }
            .testTag("grid_venue_card_${venue.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                SmartServiceMediaView(
                    imageUrl = venue.visualPresetKey,
                    accentHex = venue.accentHex,
                    secondaryHex = venue.secondaryHex,
                    category = venue.category,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CharcoalNight.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CharcoalNight.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isFavorite) WarmCoral else PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (isVip) {
                    Surface(
                        color = CharcoalNight.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(0.7.dp, RoyalGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "VIP",
                            fontSize = 9.sp,
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${venue.ratingAverage}",
                            fontSize = 11.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "$${venue.startingPriceUsd}",
                        fontSize = 13.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = venue.name,
                    fontSize = 13.sp,
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${venue.cityName} • ${venue.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onBookNow,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPinkPrimary,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text(
                        text = "حجز",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CityChipCard(
    cityName: String,
    country: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) CoralPinkPrimary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CoralPinkPrimary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• $country",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) PureWhite.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VenueCard(
    venue: Venue,
    isFavorite: Boolean,
    isSuperAdmin: Boolean,
    onClick: () -> Unit,
    onQuickBook: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenPhotos: () -> Unit,
    onEditPrice: () -> Unit,
    onBlockProvider: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isVip = venue.startingPriceUsd >= 90 || venue.badges.any { it.contains("VIP", ignoreCase = true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("venue_card_${venue.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            // 1. Clean Nightlife Photo (186.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
            ) {
                SmartServiceMediaView(
                    imageUrl = venue.visualPresetKey,
                    accentHex = venue.accentHex,
                    secondaryHex = venue.secondaryHex,
                    category = venue.category,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CharcoalNight.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    CharcoalNight.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CharcoalNight.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CoralPinkPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = venue.cityName,
                                    fontSize = 11.sp,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isVip) {
                            Surface(
                                color = CharcoalNight.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.7.dp, RoyalGold.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = "VIP",
                                    fontSize = 9.sp,
                                    color = RoyalGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onOpenVideo,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CharcoalNight.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "فيديو السهرة",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CharcoalNight.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "حفظ في المفضلة",
                                tint = if (isFavorite) WarmCoral else PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 2. Compact, Harmonious Info & Action Row
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${venue.startingPriceUsd}",
                            fontSize = 15.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${venue.ratingAverage}",
                            fontSize = 11.sp,
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = venue.name,
                        fontSize = 14.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${venue.neighborhood} • ${venue.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                if (isSuperAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = onEditPrice,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل السعر",
                                    tint = RoyalGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "السعر ($${venue.startingPriceUsd})",
                                    fontSize = 10.sp,
                                    color = PureWhite
                                )
                            }
                        }

                        Surface(
                            onClick = onBlockProvider,
                            color = ErrorCrimson.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "حظر",
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "حظر",
                                    fontSize = 10.sp,
                                    color = ErrorCrimson
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(0.38f)
                            .height(36.dp)
                    ) {
                        Text(
                            text = "التفاصيل",
                            fontSize = 11.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onQuickBook,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPinkPrimary,
                            contentColor = PureWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(0.62f)
                            .height(36.dp)
                            .testTag("quick_book_${venue.id}")
                    ) {
                        Text(
                            text = "احجز الآن",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishedServiceOfferCard(
    service: PublishedService,
    isSuperAdmin: Boolean,
    onBookOffer: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenPhotos: () -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(296.dp)
            .clickable { onOpenPhotos() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            ) {
                SmartServiceMediaView(
                    imageUrl = service.imageUrl,
                    accentHex = 0xFF0D9488L,
                    secondaryHex = 0xFF09111EL,
                    category = service.category,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = CharcoalNight.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${service.cityName} • ${service.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // VIP Gold Badge
                Surface(
                    color = CharcoalNight.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, RoyalGold.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "VIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                // Price Badge
                Surface(
                    onClick = { if (isSuperAdmin) onEditPrice() else onBookOffer() },
                    color = CharcoalNight.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSuperAdmin) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل السعر",
                                tint = RoyalGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "$${service.priceUsd}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenVideo,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.42f)
                    ) {
                        Text(
                            text = "الفيديو",
                            color = PureWhite,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onBookOffer,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPinkPrimary, // Deep Teal #0D9488
                            contentColor = PureWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.58f)
                    ) {
                        Text(
                            text = "احجز الآن • $${service.priceUsd}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingsTabContent(
    bookingsState: UiState<List<Booking>>,
    onUpdateStatus: (bookingId: String, newStatus: String) -> Unit,
    onReschedule: (bookingId: String, newDate: String, newSlot: String, newGuests: Int, newNotes: String) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onExploreClick: () -> Unit
) {
    var editingBooking by remember { mutableStateOf<Booking?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "حجوزاتي المباشرة",
                style = MaterialTheme.typography.headlineMedium,
                color = RoyalGold
            )
            Text(
                text = "إدارة حجوزات المساج والسبا والسهرات الفنية في سوريا وتركيا",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (bookingsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoyalGold)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bookingsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is UiState.Success -> {
                val list = bookingsState.data
                if (list.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد حجوزات مباشرة حتى الآن",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اختر مدينتك في سوريا أو تركيا واحجز جلسة مساج ملكي أو طاولة سهرة فنية مباشرة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onExploreClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianNight
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("استكشف المدن والخدمات الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { booking ->
                            BookingItemCard(
                                booking = booking,
                                onEditClick = { editingBooking = booking },
                                onMarkCompleted = { onUpdateStatus(booking.id, "completed") },
                                onCancelBooking = { onUpdateStatus(booking.id, "cancelled") },
                                onDeleteClick = { onDeleteBooking(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    editingBooking?.let { booking ->
        RescheduleBookingDialog(
            booking = booking,
            onDismiss = { editingBooking = null },
            onConfirm = { newDate, newSlot, newGuests, newNotes ->
                onReschedule(booking.id, newDate, newSlot, newGuests, newNotes)
                editingBooking = null
            }
        )
    }
}

@Composable
private fun BookingItemCard(
    booking: Booking,
    onEditClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onCancelBooking: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusColor = when (booking.status) {
        "confirmed" -> EmeraldVerified
        "completed" -> RoyalGold
        else -> ErrorCrimson
    }
    val statusText = when (booking.status) {
        "confirmed" -> "حجز مؤكد"
        "completed" -> "مكتمل"
        else -> "ملغى"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
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
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "$${booking.totalPriceUsd}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RoyalGold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = booking.venueName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${booking.cityName} (${booking.country}) • ${booking.category}",
                style = MaterialTheme.typography.labelLarge,
                color = SoftGold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "الباقة: ${booking.serviceTitle}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "الموعد: ${booking.bookingDate} • الساعة ${booking.timeSlot} • ${booking.guestCount} أشخاص",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظاتك: ${booking.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (booking.status == "confirmed") {
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoyalGold)
                    ) {
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل الموعد", color = RoyalGold)
                    }

                    Button(
                        onClick = onMarkCompleted,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldVerified,
                            contentColor = ObsidianNight
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تمت التجربة", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onCancelBooking) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = ErrorCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إلغاء", color = ErrorCrimson)
                    }
                } else {
                    TextButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف من السجل", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescheduleBookingDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (newDate: String, newSlot: String, newGuests: Int, newNotes: String) -> Unit
) {
    val dateOptions = listOf("اليوم - المساء", "غداً - المساء", "الخميس القادم", "الجمعة القادمة", "السبت القادم")
    var selectedDate by remember { mutableStateOf(booking.bookingDate) }
    var selectedSlot by remember { mutableStateOf(booking.timeSlot) }
    var guestCount by remember { mutableIntStateOf(booking.guestCount.toInt()) }
    var notes by remember { mutableStateOf(booking.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "تعديل موعد الحجز المباشر",
                style = MaterialTheme.typography.titleLarge,
                color = RoyalGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = booking.venueName, color = SoftGold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dateOptions.forEach { d ->
                        FilterChip(
                            selected = selectedDate == d,
                            onClick = { selectedDate = d },
                            label = { Text(d) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LayaliCatalog.defaultTimeSlots.forEach { slot ->
                        FilterChip(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            label = { Text(slot) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تحديث الملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, selectedSlot, guestCount, notes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight
                )
            ) {
                Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun VerifiedReviewsTabContent(
    userReviewsState: UiState<List<CustomerReview>>,
    defaultAuthorName: String,
    isSubmitting: Boolean,
    blockedAuthors: Set<String>,
    isSuperAdmin: Boolean,
    onBlockAuthor: (String) -> Unit,
    onSubmitReviewForVenue: (Venue, String, String, Int, String) -> Unit,
    onDeleteUserReview: (String) -> Unit,
    onOpenVenue: (Venue) -> Unit
) {
    var selectedCountryFilter by remember { mutableStateOf("الكل") }
    var venueToReview by remember { mutableStateOf<Venue?>(null) }

    val userReviews = when (userReviewsState) {
        is UiState.Success -> userReviewsState.data.filterNot {
            blockedAuthors.contains(it.authorName.trim())
        }
        else -> emptyList()
    }

    val allCuratedWithVenue = remember(selectedCountryFilter, blockedAuthors) {
        LayaliCatalog.venues
            .filter { selectedCountryFilter == "الكل" || it.country == selectedCountryFilter }
            .flatMap { venue ->
                venue.curatedReviews
                    .filterNot { blockedAuthors.contains(it.authorName.trim()) }
                    .map { review -> venue to review }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تقييمات العملاء الموثوقة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RoyalGold
                    )
                    Text(
                        text = "تجارب حقيقية للمساج والسهرات في جميع المدن السورية والتركية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { venueToReview = LayaliCatalog.venues.firstOrNull() },
                    enabled = LayaliCatalog.venues.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianNight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("open_add_review_modal_button")
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة تقييم", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayaliCatalog.countries.forEach { country ->
                    FilterChip(
                        selected = selectedCountryFilter == country,
                        onClick = { selectedCountryFilter = country },
                        label = { Text(country) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalGold,
                            selectedLabelColor = ObsidianNight
                        )
                    )
                }
            }
        }

        if (userReviews.isNotEmpty()) {
            item {
                Text(
                    text = "تقييماتي المنشورة (${userReviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldVerified,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(userReviews, key = { "my_${it.id}" }) { rev ->
                Box {
                    ReviewItemCard(
                        authorName = rev.authorName,
                        cityBadge = "${rev.venueName} • ${rev.cityName}",
                        serviceTitle = rev.serviceType,
                        rating = rev.rating.toInt(),
                        comment = rev.comment,
                        dateLabel = "تقييمك الموثق",
                        isCurrentUser = true
                    )
                    IconButton(
                        onClick = { onDeleteUserReview(rev.id) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 28.dp, bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف التقييم",
                            tint = ErrorCrimson
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "أحدث تقييمات العملاء في المدن",
                style = MaterialTheme.typography.titleMedium,
                color = SoftGold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        items(allCuratedWithVenue, key = { it.second.id }) { (venue, review) ->
            Column(modifier = Modifier.clickable { onOpenVenue(venue) }) {
                ReviewItemCard(
                    authorName = review.authorName,
                    cityBadge = "${venue.name} • ${review.cityBadge}",
                    serviceTitle = review.serviceTitle,
                    rating = review.rating,
                    comment = review.comment,
                    dateLabel = review.dateLabel,
                    isCurrentUser = false,
                    onBlockAuthor = if (isSuperAdmin) {
                        { onBlockAuthor(review.authorName) }
                    } else {
                        null
                    }
                )
            }
        }
    }

    venueToReview?.let { currentSelectedVenue ->
        VenueSelectorReviewDialog(
            initialVenue = currentSelectedVenue,
            defaultAuthorName = defaultAuthorName,
            isSubmitting = isSubmitting,
            onDismiss = { venueToReview = null },
            onSubmit = { chosenVenue, author, service, rating, comment ->
                onSubmitReviewForVenue(chosenVenue, author, service, rating, comment)
                venueToReview = null
            }
        )
    }
}

@Composable
private fun VenueSelectorReviewDialog(
    initialVenue: Venue,
    defaultAuthorName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Venue, String, String, Int, String) -> Unit
) {
    var chosenVenue by remember { mutableStateOf(initialVenue) }
    var authorName by remember { mutableStateOf(defaultAuthorName.ifBlank { "عميل موثق" }) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("نشر تقييم عميل موثق", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختر المركز أو السهرة:", style = MaterialTheme.typography.labelLarge, color = SoftGold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LayaliCatalog.venues, key = { it.id }) { v ->
                        FilterChip(
                            selected = chosenVenue.id == v.id,
                            onClick = { chosenVenue = v },
                            label = { Text("${v.cityName}: ${v.name}", maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = ObsidianNight
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("اسمك") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "$star",
                                tint = if (star <= rating) RoyalGold else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("اكتب رأيك وتجربتك (3 أحرف على الأقل)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        chosenVenue,
                        authorName.ifBlank { "عميل موثق" },
                        chosenVenue.packages.firstOrNull()?.title ?: chosenVenue.category,
                        rating,
                        comment
                    )
                },
                enabled = !isSubmitting && comment.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight
                )
            ) {
                Text("نشر التقييم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun FavoritesTabContent(
    favoritesState: UiState<List<FavoriteVenue>>,
    onOpenVenue: (String) -> Unit,
    onRemoveFavorite: (FavoriteVenue) -> Unit,
    onExploreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "قائمة المفضلة",
                style = MaterialTheme.typography.headlineMedium,
                color = RoyalGold
            )
            Text(
                text = "مراكز المساج والسهرات المحفوظة للوصول السريع",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (favoritesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoyalGold)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(favoritesState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val favs = favoritesState.data
                if (favs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "قائمة المفضلة فارغة",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على أيقونة القلب في أي مركز مساج أو سهرة لحفظه هنا.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onExploreClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianNight
                            )
                        ) {
                            Text("تصفح المراكز والسهرات", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favs, key = { it.venueId }) { fav ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenVenue(fav.venueId) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fav.venueName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${fav.cityName} • ${fav.category}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { onRemoveFavorite(fav) }) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "إزالة من المفضلة",
                                            tint = CoralPinkPrimary
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

@Composable
private fun ReviewItemCard(
    authorName: String,
    cityBadge: String,
    serviceTitle: String,
    rating: Int,
    comment: String,
    dateLabel: String,
    isCurrentUser: Boolean,
    onBlockAuthor: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentUser) VioletBlueVip else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = VioletBlueVip.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletBlueVip,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (onBlockAuthor != null) {
                        IconButton(
                            onClick = onBlockAuthor,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "حظر المستخدم",
                                tint = ErrorCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = VioletBlueVip,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$cityBadge • $serviceTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                repeat(rating.coerceIn(1, 5)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CoralPinkPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

