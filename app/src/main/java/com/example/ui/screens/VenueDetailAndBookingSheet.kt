package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerReview
import com.example.data.model.ServicePackage
import com.example.data.model.Venue
import com.example.ui.components.BrandActionBadgesRow
import com.example.ui.components.FullScreenMediaViewerDialog
import com.example.ui.components.InteractiveVideoShowcaseCard
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.WarmCoral
import com.example.util.LayaliNotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VenueDetailScreen(
    venue: Venue,
    isFavorite: Boolean,
    userReviewsForVenue: List<CustomerReview>,
    defaultAuthorName: String,
    isSubmitting: Boolean,
    isSuperAdmin: Boolean = false,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenCheckout: (ServicePackage) -> Unit = {},
    onConfirmBooking: (
        serviceTitle: String,
        unitPriceUsd: Int,
        bookingDate: String,
        timeSlot: String,
        guestCount: Int,
        notes: String
    ) -> Unit,
    onSubmitReview: (
        authorName: String,
        serviceType: String,
        rating: Int,
        comment: String
    ) -> Unit,
    blockedAuthors: Set<String> = emptySet(),
    onBlockAuthor: (String) -> Unit = {},
    onUpdateVenuePrice: (Int) -> Unit = {},
    onUpdateVenuePhoto: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var selectedPackage by remember(venue.id, venue.startingPriceUsd) {
        mutableStateOf(venue.packages.firstOrNull())
    }
    var showBookingDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showMediaViewer by remember { mutableStateOf<Boolean?>(null) } // true = video, false = photos
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var newPriceInput by remember(venue.startingPriceUsd) {
        mutableStateOf(venue.startingPriceUsd.toString())
    }

    val presetPhotoKeys = remember(venue.visualPresetKey) {
        listOf(
            venue.visualPresetKey.ifBlank { "palace_qudud_hall" },
            "palace_qudud_hall",
            "card_qudud",
            "card_lounge",
            "card_spa",
            "card_vip_night"
        ).distinct()
    }
    val pagerState = rememberPagerState(pageCount = { presetPhotoKeys.size })

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 116.dp)
        ) {
            // Top Bar ("تفاصيل الخدمة") + Swipeable Image Carousel (Screen 3 of UI/UX Case Study)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Title Bar matching Screen 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("venue_detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (isFavorite) WarmCoral else BrandChampagneGold
                            )
                        }

                        Text(
                            text = "تفاصيل الخدمة",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("venue_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = BrandChampagneGold
                            )
                        }
                    }

                    // Swipeable Image Carousel (HorizontalPager) with Pagination Dots
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(250.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.2.dp, BrandChampagneGold.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            SmartServiceMediaView(
                                imageUrl = presetPhotoKeys[page],
                                accentHex = venue.accentHex,
                                secondaryHex = venue.secondaryHex,
                                category = venue.category,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showMediaViewer = false }
                            )
                        }

                        // Top-right 4K Video Badge inside Carousel
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { showMediaViewer = true },
                                color = BrandDustyRose.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandChampagneGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "فيديو 4K",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isSuperAdmin) {
                                Surface(
                                    onClick = { showEditPriceDialog = true },
                                    color = BrandChampagneGold,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = ObsidianNight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$${venue.startingPriceUsd}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ObsidianNight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Carousel Indicator Dots at the bottom center (• • ● •)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(ObsidianNight.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(presetPhotoKeys.size) { idx ->
                                val isCurrent = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 9.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) BrandChampagneGold else Color.White.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }
                    }

                    // Large Bold Service Title, Rating & Location below Carousel (Screen 3 hierarchy)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = RoyalGold,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = venue.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ObsidianNight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = EmeraldVerified.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EmeraldVerified)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = EmeraldVerified,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = venue.providerDisplayName.ifBlank { "مركز معتمد وموثوق" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldVerified,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = venue.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${venue.cityName} (${venue.country}) • ${venue.neighborhood}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SoftGold
                                )
                            }

                            Surface(
                                color = BrandChampagneGold,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$${venue.startingPriceUsd}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ObsidianNight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Rating, Opening Hours & Highlights
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${venue.ratingAverage}",
                                style = MaterialTheme.typography.titleLarge,
                                color = RoyalGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${venue.reviewCount + userReviewsForVenue.size} تقييم عميل موثق)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = venue.openingHours,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "تفاصيل ومزايا الخدمة",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        venue.badges.forEach { badge ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "تفاصيل الخدمة",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = venue.fullDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "• خدمة احترافية راقية وسريعة بأعلى معايير الخصوصية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGold
                        )
                        Text(
                            text = "• أجنحة ملكية خاصة ومجهزة بالكامل للسهرات والسبا",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGold
                        )
                        Text(
                            text = "• تأكيد فوري للحجز مع دعم مباشر عبر تلجرام @RamaNUR",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3D Brand Action Badges from Visual Identity Board
                    BrandActionBadgesRow(
                        onFavoritesClick = onToggleFavorite,
                        onSupportChatClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                        onDirectCallClick = {
                            val pkg = selectedPackage ?: venue.packages.firstOrNull()
                            if (pkg != null) onOpenCheckout(pkg) else showBookingDialog = true
                        },
                        onCitiesClick = onBack
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Video & Photo Showcase Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = BrandDustyRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عرض الفيديو والصور الحية للمركز",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(onClick = { showMediaViewer = false }) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح الألبوم الكامل", color = BrandChampagneGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    InteractiveVideoShowcaseCard(
                        videoTitle = venue.showcaseVideoTitle,
                        durationLabel = venue.showcaseDuration,
                        cityName = venue.cityName,
                        priceUsd = venue.startingPriceUsd,
                        imageUrl = venue.visualPresetKey
                    )

                    if (venue.galleryHighlights.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(venue.galleryHighlights) { index, photoCaption ->
                                val presetKey = presetPhotoKeys[index % presetPhotoKeys.size]
                                Card(
                                    modifier = Modifier
                                        .width(215.dp)
                                        .height(125.dp)
                                        .clickable { showMediaViewer = false },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.55f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SmartServiceMediaView(
                                            imageUrl = presetKey,
                                            accentHex = venue.accentHex,
                                            secondaryHex = venue.secondaryHex,
                                            category = venue.category,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .background(ObsidianNight.copy(alpha = 0.78f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = BrandChampagneGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = photoCaption,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Packages Section Header
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اختر الباقة أو الخدمة للحجز المباشر",
                                style = MaterialTheme.typography.titleLarge,
                                color = SoftGold
                            )
                            Text(
                                text = "يمكنك طلب جلسة مساج، حمام ملكي، أو حجز طاولة سهرة فنية مباشرة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSuperAdmin) {
                            OutlinedButton(
                                onClick = { showEditPriceDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandChampagneGold)
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
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Packages List
            items(venue.packages, key = { it.id }) { pkg ->
                val isSelected = selectedPackage?.id == pkg.id
                PackageCardItem(
                    pkg = pkg,
                    isSelected = isSelected,
                    onSelect = { selectedPackage = pkg },
                    onBookNowClick = {
                        selectedPackage = pkg
                        onOpenCheckout(pkg)
                    }
                )
            }

            // Verified Customer Reviews Section
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
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftGold
                        )
                        Text(
                            text = "تقييمات حقيقية من عملاء قاموا بالحجز والتجربة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showReviewDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier.testTag("add_venue_review_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أضف تقييمك",
                            color = RoyalGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // User's Cloud Reviews for this Venue
            val visibleUserReviews = userReviewsForVenue.filterNot {
                blockedAuthors.contains(it.authorName.trim())
            }
            items(visibleUserReviews, key = { "user_${it.id}" }) { review ->
                ReviewItemCard(
                    authorName = review.authorName,
                    cityBadge = review.cityName,
                    serviceTitle = review.serviceType,
                    rating = review.rating.toInt(),
                    comment = review.comment,
                    dateLabel = "تقييمك الموثق",
                    isCurrentUser = true,
                    onBlockAuthor = if (isSuperAdmin) ({ onBlockAuthor(review.authorName) }) else null
                )
            }

            // Curated Verified Reviews (filtered by blocked users)
            val visibleCuratedReviews = venue.curatedReviews.filterNot {
                blockedAuthors.contains(it.authorName.trim())
            }
            items(visibleCuratedReviews, key = { it.id }) { review ->
                ReviewItemCard(
                    authorName = review.authorName,
                    cityBadge = review.cityBadge,
                    serviceTitle = review.serviceTitle,
                    rating = review.rating,
                    comment = review.comment,
                    dateLabel = review.dateLabel,
                    isCurrentUser = false,
                    onBlockAuthor = if (isSuperAdmin) ({ onBlockAuthor(review.authorName) }) else null
                )
            }
        }

        // Sticky Bottom Direct Booking Bar (Screen 3 of UI/UX Case Study: Price per night/hour + wide gold CTA "بدء الحجز والطلب")
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "$${selectedPackage?.priceUsd ?: venue.startingPriceUsd} / ليلة",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedPackage?.title ?: venue.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val pkg = selectedPackage ?: venue.packages.firstOrNull()
                        if (pkg != null) {
                            onOpenCheckout(pkg)
                        } else {
                            showBookingDialog = true
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandChampagneGold,
                        contentColor = ObsidianNight
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                        .testTag("open_direct_booking_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بدء الحجز والطلب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Full-screen Media Viewer Dialog
    showMediaViewer?.let { isVideoInitial ->
        FullScreenMediaViewerDialog(
            title = venue.name,
            cityName = venue.cityName,
            category = venue.category,
            priceUsd = venue.startingPriceUsd,
            primaryImageUrl = venue.visualPresetKey,
            videoTitle = venue.showcaseVideoTitle,
            galleryCaptions = venue.galleryHighlights,
            initialTabIsVideo = isVideoInitial,
            isSuperAdmin = isSuperAdmin,
            onDismiss = { showMediaViewer = null },
            onBookNow = {
                showMediaViewer = null
                showBookingDialog = true
            },
            onEditPriceClick = {
                showMediaViewer = null
                showEditPriceDialog = true
            },
            onAddCustomPhoto = { uriStr ->
                onUpdateVenuePhoto(uriStr)
            }
        )
    }

    // Quick Price Edit Dialog
    if (showEditPriceDialog) {
        AlertDialog(
            onDismissRequest = { showEditPriceDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تعديل السعر المعتمد",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandChampagneGold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${venue.name} • ${venue.cityName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftGold
                    )
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { newPriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("السعر الجديد بالدولار ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newPriceInput.toIntOrNull() ?: venue.startingPriceUsd
                        onUpdateVenuePrice(parsed)
                        showEditPriceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandDustyRose,
                        contentColor = Color.White
                    )
                ) {
                    Text("حفظ السعر", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPriceDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBookingDialog) {
        val pkg = selectedPackage ?: venue.packages.first()
        DirectBookingDialog(
            venue = venue,
            selectedPackage = pkg,
            isSubmitting = isSubmitting,
            onDismiss = { showBookingDialog = false },
            onConfirm = { date, slot, guests, notes ->
                onConfirmBooking(pkg.title, pkg.priceUsd, date, slot, guests, notes)
                showBookingDialog = false
            }
        )
    }

    if (showReviewDialog) {
        AddVerifiedReviewDialog(
            venue = venue,
            defaultAuthorName = defaultAuthorName,
            isSubmitting = isSubmitting,
            onDismiss = { showReviewDialog = false },
            onSubmit = { author, service, rating, comment ->
                onSubmitReview(author, service, rating, comment)
                showReviewDialog = false
            }
        )
    }
}

@Composable
private fun PackageCardItem(
    pkg: ServicePackage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onBookNowClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 0.8.dp,
            color = if (isSelected) RoyalGold else RoyalGold.copy(alpha = 0.22f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (pkg.isPopular) {
                        Surface(
                            color = WarmCoral.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "الأكثر طلباً",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmCoral,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = pkg.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المدة: ${pkg.durationLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftGold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${pkg.priceUsd}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RoyalGold
                    )
                    Text(
                        text = "شامل الضيافة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pkg.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onBookNowClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) RoyalGold else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isSelected) ObsidianNight else Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "طلب وحجز هذه الباقة",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItemCard(
    authorName: String,
    cityBadge: String,
    serviceTitle: String,
    rating: Int,
    comment: String,
    dateLabel: String,
    isCurrentUser: Boolean,
    onBlockAuthor: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        ),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isCurrentUser) EmeraldVerified else RoyalGold.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RoyalGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = authorName.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyalGold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "عميل موثق",
                                tint = EmeraldVerified,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "$cityBadge • $serviceTitle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(rating.coerceIn(1, 5)) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (onBlockAuthor != null && !isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onBlockAuthor,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "حظر المستخدم",
                                    tint = ErrorCrimson.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = comment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectBookingDialog(
    venue: Venue,
    selectedPackage: ServicePackage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (date: String, timeSlot: String, guestCount: Int, notes: String) -> Unit
) {
    val dateOptions = remember {
        listOf(
            "اليوم - المساء",
            "غداً - المساء",
            "الخميس القادم",
            "الجمعة القادمة",
            "السبت القادم"
        )
    }
    var selectedDate by remember { mutableStateOf(dateOptions.first()) }
    var selectedTimeSlot by remember {
        mutableStateOf(venue.availableTimeSlots.firstOrNull() ?: "08:00 مساءً")
    }
    var guestCount by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "تأكيد الحجز المباشر",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RoyalGold
                )
                Text(
                    text = "${venue.name} • ${venue.cityName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = selectedPackage.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سعر الفرد/الباقة: $${selectedPackage.priceUsd} • ${selectedPackage.durationLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalGold
                        )
                    }
                }

                Text(
                    text = "اختر يوم الحجز:",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftGold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dateOptions.forEach { dateOpt ->
                        FilterChip(
                            selected = selectedDate == dateOpt,
                            onClick = { selectedDate = dateOpt },
                            label = { Text(dateOpt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = ObsidianNight
                            )
                        )
                    }
                }

                Text(
                    text = "اختر الوقت المتاح:",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftGold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    venue.availableTimeSlots.forEach { slot ->
                        FilterChip(
                            selected = selectedTimeSlot == slot,
                            onClick = { selectedTimeSlot = slot },
                            label = { Text(slot) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = ObsidianNight
                            )
                        )
                    }
                }

                // Guest Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عدد الأشخاص:",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftGold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (guestCount > 1) guestCount-- }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "إنقاص", tint = RoyalGold)
                        }
                        Text(
                            text = "$guestCount",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (guestCount < 20) guestCount++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = RoyalGold)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات خاصة (نوع المساج، موقع الطاولة، مناسبة خاصة...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_notes_input"),
                    maxLines = 3
                )

                Surface(
                    color = RoyalGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الإجمالي المؤكد:",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftGold
                        )
                        Text(
                            text = "$${selectedPackage.priceUsd * guestCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RoyalGold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDate, selectedTimeSlot, guestCount, notes)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight
                ),
                modifier = Modifier.testTag("confirm_direct_booking_button")
            ) {
                Text(
                    text = if (isSubmitting) "جارٍ التأكيد..." else "تأكيد الحجز الآن",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddVerifiedReviewDialog(
    venue: Venue,
    defaultAuthorName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (authorName: String, serviceType: String, rating: Int, comment: String) -> Unit
) {
    var authorName by remember { mutableStateOf(defaultAuthorName.ifBlank { "عميل موثق" }) }
    var selectedService by remember {
        mutableStateOf(venue.packages.firstOrNull()?.title ?: venue.category)
    }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "إضافة تقييم عميل موثق",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RoyalGold
                )
                Text(
                    text = "${venue.name} • ${venue.cityName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("الاسم الظاهر في التقييم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "الخدمة التي قمت بتجربتها:",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftGold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    venue.packages.forEach { pkg ->
                        FilterChip(
                            selected = selectedService == pkg.title,
                            onClick = { selectedService = pkg.title },
                            label = { Text(pkg.title, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = ObsidianNight
                            )
                        )
                    }
                }

                Text(
                    text = "التقييم بالنجوم ($rating / 5):",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star نجوم",
                                tint = if (star <= rating) RoyalGold else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("اكتب تجربتك بالتفصيل (جودة المساج، الأجواء، الخدمة...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input"),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        authorName.ifBlank { "عميل موثق" },
                        selectedService,
                        rating,
                        comment
                    )
                },
                enabled = !isSubmitting && comment.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight
                ),
                modifier = Modifier.testTag("submit_review_button")
            ) {
                Text(
                    text = if (isSubmitting) "جارٍ النشر..." else "نشر التقييم الموثق",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
