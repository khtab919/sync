package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublishedService
import com.example.data.model.Venue
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.VioletBlueVip
import com.example.ui.theme.WarmCoral
import com.example.util.LayaliNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ReelItemUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val cityName: String,
    val country: String,
    val category: String,
    val description: String,
    val priceUsd: Int,
    val rating: Double,
    val reviewsCount: Int,
    val baseLikes: Int,
    val imageUrl: String,
    val audioTrackTitle: String,
    val durationLabel: String,
    val badges: List<String>,
    val accentHex: Long,
    val secondaryHex: Long,
    val sourceVenue: Venue? = null,
    val sourcePublished: PublishedService? = null
)

private const val SHARED_WEB_APP_URL =
    "https://ais-pre-sjqe77b3fvxrkycxo3sijk-206066300891.europe-west2.run.app"

@Composable
fun ReelsScreen(
    venues: List<Venue>,
    publishedServices: List<PublishedService>,
    favoriteVenueIds: Set<String>,
    initialReelId: String? = null,
    isSuperAdmin: Boolean = false,
    onBookVenue: (Venue) -> Unit,
    onBookPublished: (PublishedService) -> Unit,
    onOpenVenueDetails: (Venue) -> Unit,
    onOpenPublishedMedia: (PublishedService) -> Unit,
    onToggleFavoriteVenue: (Venue) -> Unit,
    onEditReelPrice: (ReelItemUiModel) -> Unit,
    onOpenAdminPublishReel: () -> Unit,
    onOpenCreatorChat: () -> Unit = {},
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("الكل") }
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var activeCommentReel by remember { mutableStateOf<ReelItemUiModel?>(null) }

    val likedReelIds = remember { mutableStateMapOf<String, Boolean>() }
    val customCommentsByReel = remember { mutableStateMapOf<String, List<String>>() }

    val allReels = remember(venues, publishedServices) {
        val pubReels = publishedServices.mapIndexed { idx, pub ->
            ReelItemUiModel(
                id = "reel_pub_${pub.id}",
                title = pub.title,
                subtitle = pub.videoUrl.ifBlank { "ريلز 4K مباشر • ${pub.cityName}" },
                cityName = pub.cityName,
                country = pub.country,
                category = pub.category,
                description = pub.description,
                priceUsd = pub.priceUsd.toInt(),
                rating = 4.9,
                reviewsCount = 140 + (idx * 27),
                baseLikes = 620 + (idx * 115),
                imageUrl = pub.imageUrl,
                audioTrackTitle = "موسيقى وأجواء ملكية حية • ${pub.cityName}",
                durationLabel = "00:45 4K",
                badges = listOf("ريلز موثق", pub.category, "حجز فوري VIP"),
                accentHex = 0xFFD6789BL,
                secondaryHex = 0xFF47256BL,
                sourcePublished = pub
            )
        }
        val venueReels = venues.mapIndexed { idx, venue ->
            ReelItemUiModel(
                id = "reel_venue_${venue.id}",
                title = venue.name,
                subtitle = venue.showcaseVideoTitle,
                cityName = venue.cityName,
                country = venue.country,
                category = venue.category,
                description = venue.shortDescription,
                priceUsd = venue.startingPriceUsd,
                rating = venue.ratingAverage,
                reviewsCount = venue.reviewCount,
                baseLikes = 480 + (idx * 95),
                imageUrl = venue.visualPresetKey,
                audioTrackTitle = "أجواء ${venue.cityName} الملكية • ${venue.category}",
                durationLabel = venue.showcaseDuration,
                badges = venue.badges.take(3),
                accentHex = venue.accentHex,
                secondaryHex = venue.secondaryHex,
                sourceVenue = venue
            )
        }
        pubReels + venueReels
    }

    val filteredReels = remember(allReels, selectedFilter) {
        when (selectedFilter) {
            "الكل" -> allReels
            "سوريا", "تركيا" -> allReels.filter { it.country == selectedFilter }
            else -> allReels.filter { it.category == selectedFilter }
        }.ifEmpty { allReels }
    }

    val initialPageIndex = remember(filteredReels, initialReelId) {
        if (initialReelId == null) 0
        else filteredReels.indexOfFirst {
            it.id == initialReelId ||
                it.sourceVenue?.id == initialReelId ||
                it.sourcePublished?.id == initialReelId
        }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { filteredReels.size.coerceAtLeast(1) }
    )

    LaunchedEffect(initialReelId, filteredReels.size) {
        if (initialReelId != null && filteredReels.isNotEmpty()) {
            val targetIdx = filteredReels.indexOfFirst {
                it.id == initialReelId ||
                    it.sourceVenue?.id == initialReelId ||
                    it.sourcePublished?.id == initialReelId
            }
            if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    val filterOptions = remember {
        listOf("الكل", "سهرات فنية", "سبا ومساج", "زيارات خارجية", "سوريا", "تركيا")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .testTag("reels_screen_root")
    ) {
        if (filteredReels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "لا توجد مقاطع ريلز متاحة حالياً",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandChampagneGold
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                key = { filteredReels[it].id },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { pageIndex ->
                val reel = filteredReels[pageIndex]
                val isLiked = likedReelIds[reel.id] == true
                val isFavorite = reel.sourceVenue?.let { favoriteVenueIds.contains(it.id) } ?: isLiked
                val extraCommentsCount = customCommentsByReel[reel.id]?.size ?: 0

                SingleReelFullPage(
                    reel = reel,
                    reelIndex = pageIndex + 1,
                    totalReels = filteredReels.size,
                    isPlaying = isPlaying && pagerState.currentPage == pageIndex,
                    isMuted = isMuted,
                    isLiked = isLiked,
                    isFavorite = isFavorite,
                    extraCommentsCount = extraCommentsCount,
                    isSuperAdmin = isSuperAdmin,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onDoubleTapLike = {
                        likedReelIds[reel.id] = true
                    },
                    onToggleLike = {
                        val next = !(likedReelIds[reel.id] ?: false)
                        likedReelIds[reel.id] = next
                    },
                    onToggleFavorite = {
                        val venue = reel.sourceVenue
                        if (venue != null) {
                            onToggleFavoriteVenue(venue)
                        } else {
                            val next = !(likedReelIds[reel.id] ?: false)
                            likedReelIds[reel.id] = next
                            onShowMessage(if (next) "تم حفظ الريلز في المفضلة" else "تمت الإزالة من المفضلة")
                        }
                    },
                    onOpenComments = { activeCommentReel = reel },
                    onShareReel = {
                        shareOrCopyReelLink(
                            context = context,
                            reel = reel,
                            onCopiedMessage = onShowMessage
                        )
                    },
                    onBookFromReel = {
                        reel.sourceVenue?.let(onBookVenue)
                        reel.sourcePublished?.let(onBookPublished)
                    },
                    onOpenDetailsFromReel = {
                        reel.sourceVenue?.let(onOpenVenueDetails)
                        reel.sourcePublished?.let(onOpenPublishedMedia)
                    },
                    onOpenCreatorChat = onOpenCreatorChat,
                    onEditPrice = { onEditReelPrice(reel) }
                )
            }
        }

        // Top Floating Reels Header & Category Filter Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianNight.copy(alpha = 0.88f),
                            ObsidianNight.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 12.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sound Mute/Unmute Pill
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ObsidianNight.copy(alpha = 0.72f))
                            .border(1.dp, BrandChampagneGold.copy(alpha = 0.5f), CircleShape)
                            .testTag("reel_toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "الصوت",
                            tint = BrandChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Web Link Share Pill
                    Surface(
                        onClick = {
                            copyWebLinkToClipboard(context, onShowMessage)
                        },
                        color = BrandDeepViolet.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BrandChampagneGold),
                        modifier = Modifier.testTag("reels_share_web_link_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "نسخ رابط الويب",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isSuperAdmin) {
                        Surface(
                            onClick = onOpenAdminPublishReel,
                            color = BrandDustyRose,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, BrandChampagneGold),
                            modifier = Modifier.testTag("admin_add_reel_top_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "نشر ريلز",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right Title: "ريلز 4K"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = BrandDustyRose,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "LIVE 4K",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "ريلز السهرات والسبا",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = BrandChampagneGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val selected = selectedFilter == filter
                    Surface(
                        onClick = {
                            selectedFilter = filter
                            coroutineScope.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                        color = if (selected) BrandChampagneGold else ObsidianNight.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (selected) BrandChampagneGold else BrandChampagneGold.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ObsidianNight else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Comments / Quick Review Modal for the Active Reel
    activeCommentReel?.let { reel ->
        var newCommentText by remember { mutableStateOf("") }
        val customList = customCommentsByReel[reel.id] ?: emptyList()

        AlertDialog(
            onDismissRequest = { activeCommentReel = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BrandChampagneGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "★ ${reel.rating}",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandChampagneGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "تعليقات وتقييمات الريلز",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${reel.title} (${reel.cityName})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    // Curated + User Comments
                    val sampleComments = listOf(
                        "أجواء راقية جداً وخدمة VIP ممتازة في ${reel.cityName} ✨",
                        "حجزت مباشرة من الريلز وكان الاستقبال في قمة الفخامة والخصوصية."
                    )
                    (customList + sampleComments).take(5).forEach { comment ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, BrandChampagneGold.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("أضف تعليقك على هذا الريلز...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reel_comment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            val updated = listOf(newCommentText.trim()) + customList
                            customCommentsByReel[reel.id] = updated
                            newCommentText = ""
                            onShowMessage("تم نشر تعليقك على الريلز بنجاح!")
                        } else {
                            activeCommentReel = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandChampagneGold,
                        contentColor = ObsidianNight
                    )
                ) {
                    Text("إرسال التعليق", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeCommentReel = null }) {
                    Text("إغلاق", color = BrandDustyRose)
                }
            }
        )
    }
}

@Composable
private fun SingleReelFullPage(
    reel: ReelItemUiModel,
    reelIndex: Int,
    totalReels: Int,
    isPlaying: Boolean,
    isMuted: Boolean,
    isLiked: Boolean,
    isFavorite: Boolean,
    extraCommentsCount: Int,
    isSuperAdmin: Boolean,
    onTogglePlay: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    onShareReel: () -> Unit,
    onBookFromReel: () -> Unit,
    onOpenDetailsFromReel: () -> Unit,
    onOpenCreatorChat: () -> Unit,
    onEditPrice: () -> Unit
) {
    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(850)
            showHeartBurst = false
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(300L)
            progress = if (progress >= 0.98f) 0.08f else progress + 0.025f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTapLike()
                        showHeartBurst = true
                    },
                    onTap = {
                        onTogglePlay()
                    }
                )
            }
    ) {
        // 1. Full-Screen Visual Media Background
        SmartServiceMediaView(
            imageUrl = reel.imageUrl,
            accentHex = reel.accentHex,
            secondaryHex = reel.secondaryHex,
            category = reel.category,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Cached Vignette & Luxury Ambient Overlay (zero per-frame allocations during swipe)
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val glowRadius = (w * 0.65f).coerceAtLeast(1f)
                    val glowCenter = Offset(w * 0.5f, h * 0.42f)
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            BrandChampagneGold.copy(alpha = 0.12f),
                            BrandDustyRose.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    )
                    val vignetteBrush = Brush.verticalGradient(
                        colors = listOf(
                            ObsidianNight.copy(alpha = 0.55f),
                            Color.Transparent,
                            ObsidianNight.copy(alpha = 0.68f),
                            ObsidianNight.copy(alpha = 0.95f)
                        )
                    )
                    onDrawBehind {
                        drawCircle(brush = glowBrush, radius = glowRadius, center = glowCenter)
                        drawRect(brush = vignetteBrush)
                    }
                }
        )

        // 4. Center Play/Pause Indicator or Double-Tap Heart Burst
        if (!isPlaying) {
            Surface(
                color = ObsidianNight.copy(alpha = 0.72f),
                shape = CircleShape,
                border = BorderStroke(2.dp, BrandChampagneGold),
                modifier = Modifier
                    .size(78.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "تشغيل",
                        tint = BrandChampagneGold,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = WarmCoral,
                modifier = Modifier.size(112.dp)
            )
        }

        // 5. Left/Start Vertical Social & Action Rail (Like, Comment, Favorite, Share, Support)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Like Button
            ReelActionCircleButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                iconTint = if (isLiked) WarmCoral else Color.White,
                label = "${reel.baseLikes + (if (isLiked) 1 else 0)}",
                onClick = onToggleLike,
                testTag = "reel_like_button_${reel.id}"
            )

            // Comments / Verified Reviews Button
            ReelActionCircleButton(
                icon = Icons.Default.ChatBubbleOutline,
                iconTint = BrandChampagneGold,
                label = "${reel.reviewsCount + extraCommentsCount}",
                onClick = onOpenComments,
                testTag = "reel_comments_button_${reel.id}"
            )

            // Save / Favorite Button
            ReelActionCircleButton(
                icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                iconTint = if (isFavorite) BrandChampagneGold else Color.White,
                label = if (isFavorite) "محفوظ" else "حفظ",
                onClick = onToggleFavorite,
                testTag = "reel_favorite_button_${reel.id}"
            )

            // Share Web & App Link Button
            ReelActionCircleButton(
                icon = Icons.Default.Share,
                iconTint = Color.White,
                label = "مشاركة",
                onClick = onShareReel,
                testTag = "reel_share_button_${reel.id}"
            )

            // Telegram Support @RamaNUR Quick Button
            ReelActionCircleButton(
                icon = Icons.Default.SupportAgent,
                iconTint = BrandDustyRose,
                label = "@RamaNUR",
                onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                testTag = "reel_telegram_button_${reel.id}"
            )

            if (isSuperAdmin) {
                ReelActionCircleButton(
                    icon = Icons.Default.Edit,
                    iconTint = BrandChampagneGold,
                    label = "$${reel.priceUsd}",
                    onClick = onEditPrice,
                    testTag = "reel_admin_edit_price_${reel.id}"
                )
            }
        }

        // 6. Bottom Right/Center Info Overlay & Direct Booking Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 82.dp, end = 16.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Reel Counter + City + Rating Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = ObsidianNight.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, BrandChampagneGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$reelIndex / $totalReels • ${reel.durationLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = EmeraldVerified.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = ObsidianNight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "متاح الآن",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianNight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = BrandDeepViolet.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BrandChampagneGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BrandChampagneGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${reel.cityName} • ${reel.country}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Creator Framed Avatar + @username + Level Badge Strip
            val creatorFrames = listOf("emerald_crown", "amber_sovereign", "sapphire_neon", "amethyst_vip", "crimson_flame", "obsidian_elite")
            val creatorFrameId = creatorFrames[(reelIndex - 1) % creatorFrames.size]
            val creatorLevel = 16 + ((reelIndex * 3) % 20)
            val creatorUsername = reel.sourceVenue?.providerUsername ?: "@layali_${reel.cityName.hashCode().let { kotlin.math.abs(it) % 99 }}"

            Surface(
                onClick = onOpenCreatorChat,
                color = ObsidianNight.copy(alpha = 0.82f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = VioletBlueVip,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "دردشة VIP ($15)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = creatorUsername,
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "المستوى الملكي Lv.$creatorLevel • متصل الآن",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified
                            )
                        }

                        FramedUserAvatar(
                            avatarUri = reel.imageUrl,
                            fallbackInitial = reel.title.take(1),
                            frameId = creatorFrameId,
                            level = creatorLevel,
                            avatarSize = 42.dp,
                            showLevelBadge = true,
                            isOnline = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reel Title
            Text(
                text = reel.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = reel.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Track & Badges Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = BrandChampagneGold,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isMuted) "الصوت مكتوم • ${reel.audioTrackTitle}" else reel.audioTrackTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandChampagneGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price + Direct Booking CTA Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenDetailsFromReel,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.2.dp, BrandChampagneGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(0.42f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "التفاصيل والصور",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandChampagneGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                LuxuryGradientButton(
                    text = "احجز الآن • $${reel.priceUsd}",
                    onClick = onBookFromReel,
                    modifier = Modifier.weight(0.58f),
                    testTag = "reel_book_now_button_${reel.id}"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated 4K Video Progress Bar
            LinearProgressIndicator(
                progress = { if (isPlaying) progress else 0.45f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50)),
                color = BrandChampagneGold,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun ReelActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Surface(
            color = ObsidianNight.copy(alpha = 0.72f),
            shape = CircleShape,
            border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f)),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Horizontal Reels preview strip displayed on the Home/Explore screen so users on Web & Android
 * can see live 4K Reels stories and jump straight into the full-screen vertical Reels player.
 */
@Composable
fun HomeReelsPreviewStrip(
    venues: List<Venue>,
    publishedServices: List<PublishedService>,
    onOpenReelItem: (String) -> Unit,
    onOpenAllReels: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onOpenAllReels,
                modifier = Modifier.testTag("home_open_all_reels_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = BrandChampagneGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "مشاهدة كل الريلز 4K",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandChampagneGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = BrandDustyRose,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "جديد • REELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ريلز السهرات والسبا",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(publishedServices.take(4), key = { "home_reel_pub_${it.id}" }) { pub ->
                HomeReelStoryCard(
                    title = pub.title,
                    cityName = pub.cityName,
                    priceUsd = pub.priceUsd.toInt(),
                    imageUrl = pub.imageUrl,
                    category = pub.category,
                    accentHex = 0xFFD6789BL,
                    secondaryHex = 0xFF47256BL,
                    onClick = { onOpenReelItem(pub.id) }
                )
            }
            items(venues.take(6), key = { "home_reel_venue_${it.id}" }) { venue ->
                HomeReelStoryCard(
                    title = venue.name,
                    cityName = venue.cityName,
                    priceUsd = venue.startingPriceUsd,
                    imageUrl = venue.visualPresetKey,
                    category = venue.category,
                    accentHex = venue.accentHex,
                    secondaryHex = venue.secondaryHex,
                    onClick = { onOpenReelItem(venue.id) }
                )
            }
        }
    }
}

private val ReelStoryBorderBrush = Brush.linearGradient(
    colors = listOf(BrandChampagneGold, BrandDustyRose, BrandChampagneGold)
)

private val ReelStoryVignetteBrush = Brush.verticalGradient(
    colors = listOf(
        ObsidianNight.copy(alpha = 0.35f),
        Color.Transparent,
        ObsidianNight.copy(alpha = 0.90f)
    )
)

@Composable
private fun HomeReelStoryCard(
    title: String,
    cityName: String,
    priceUsd: Int,
    imageUrl: String,
    category: String,
    accentHex: Long,
    secondaryHex: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 2.dp,
            brush = ReelStoryBorderBrush
        ),
        modifier = Modifier
            .width(122.dp)
            .height(178.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SmartServiceMediaView(
                imageUrl = imageUrl,
                accentHex = accentHex,
                secondaryHex = secondaryHex,
                category = category,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReelStoryVignetteBrush)
            )

            // Top Play 4K Badge
            Surface(
                color = ObsidianNight.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(0.8.dp, BrandChampagneGold),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = BrandChampagneGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ريلز",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Title + City + Price
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = BrandChampagneGold,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$cityName • $$priceUsd",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianNight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun copyWebLinkToClipboard(context: Context, onMessage: (String) -> Unit) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("رابط موقع وتطبيق ليالي", SHARED_WEB_APP_URL))
        onMessage("تم نسخ رابط الويب الرسمي: $SHARED_WEB_APP_URL")
    }
}

private fun shareOrCopyReelLink(
    context: Context,
    reel: ReelItemUiModel,
    onCopiedMessage: (String) -> Unit
) {
    val shareText = buildString {
        append("شاهد ريلز: ${reel.title} (${reel.cityName} - ${reel.country})\n")
        append("السعر: $${reel.priceUsd} • حجز مباشر VIP\n")
        append("الرابط المباشر للويب والتطبيق:\n$SHARED_WEB_APP_URL")
    }
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("رابط ريلز", shareText))
    }
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الريلز والرابط").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure {
        onCopiedMessage("تم نسخ رابط الريلز والويب إلى الحافظة!")
    }
}
