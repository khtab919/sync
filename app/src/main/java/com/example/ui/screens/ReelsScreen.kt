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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.DeepTealPrimary
import com.example.ui.theme.MutedLuxuryGold
import com.example.ui.theme.RoyalNavyBackground
import com.example.ui.theme.RoyalNavyCard
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.SoftWhiteText
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay

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
                subtitle = pub.cityName,
                cityName = pub.cityName,
                country = pub.country,
                category = pub.category,
                description = pub.description,
                priceUsd = pub.priceUsd.toInt(),
                rating = 4.9,
                reviewsCount = 140 + (idx * 27),
                baseLikes = 620 + (idx * 115),
                imageUrl = pub.imageUrl,
                audioTrackTitle = pub.cityName,
                durationLabel = "00:45",
                badges = listOf("VIP", pub.category),
                accentHex = 0xFF0D9488L,
                secondaryHex = 0xFF09111EL,
                sourcePublished = pub
            )
        }
        val venueReels = venues.mapIndexed { idx, venue ->
            ReelItemUiModel(
                id = "reel_venue_${venue.id}",
                title = venue.name,
                subtitle = venue.cityName,
                cityName = venue.cityName,
                country = venue.country,
                category = venue.category,
                description = venue.shortDescription,
                priceUsd = venue.startingPriceUsd,
                rating = venue.ratingAverage,
                reviewsCount = venue.reviewCount,
                baseLikes = 480 + (idx * 95),
                imageUrl = venue.visualPresetKey,
                audioTrackTitle = venue.cityName,
                durationLabel = venue.showcaseDuration,
                badges = venue.badges.take(2),
                accentHex = venue.accentHex,
                secondaryHex = venue.secondaryHex,
                sourceVenue = venue
            )
        }
        pubReels + venueReels
    }

    val initialPageIndex = remember(allReels, initialReelId) {
        if (initialReelId == null) 0
        else allReels.indexOfFirst {
            it.id == initialReelId ||
                it.sourceVenue?.id == initialReelId ||
                it.sourcePublished?.id == initialReelId
        }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { allReels.size.coerceAtLeast(1) }
    )

    LaunchedEffect(initialReelId, allReels.size) {
        if (initialReelId != null && allReels.isNotEmpty()) {
            val targetIdx = allReels.indexOfFirst {
                it.id == initialReelId ||
                    it.sourceVenue?.id == initialReelId ||
                    it.sourcePublished?.id == initialReelId
            }
            if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalNavyBackground)
            .testTag("reels_screen_root")
    ) {
        if (allReels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "لا توجد مقاطع ريلز حالياً",
                    fontSize = 14.sp,
                    color = SlateSecondaryText
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                key = { allReels[it].id },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { pageIndex ->
                val reel = allReels[pageIndex]
                val isLiked = likedReelIds[reel.id] == true
                val extraCommentsCount = customCommentsByReel[reel.id]?.size ?: 0

                SingleReelFullPage(
                    reel = reel,
                    reelIndex = pageIndex + 1,
                    isPlaying = isPlaying && pagerState.currentPage == pageIndex,
                    isLiked = isLiked,
                    extraCommentsCount = extraCommentsCount,
                    isSuperAdmin = isSuperAdmin,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onDoubleTapLike = { likedReelIds[reel.id] = true },
                    onToggleLike = {
                        likedReelIds[reel.id] = !(likedReelIds[reel.id] ?: false)
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
                    onOpenCreatorChat = onOpenCreatorChat,
                    onEditPrice = { onEditReelPrice(reel) }
                )
            }
        }

        // Ultra-Clean Minimal Top Bar (Only Mute icon + subtle title, zero clutter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(RoyalNavyBackground.copy(alpha = 0.45f))
                        .testTag("reel_toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "الصوت",
                        tint = SoftWhiteText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isSuperAdmin) {
                    IconButton(
                        onClick = onOpenAdminPublishReel,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DeepTealPrimary.copy(alpha = 0.85f))
                            .testTag("admin_add_reel_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "نشر ريلز",
                            tint = SoftWhiteText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Reels",
                fontSize = 14.sp,
                color = SoftWhiteText.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Compact Comments Dialog
    activeCommentReel?.let { reel ->
        var newCommentText by remember { mutableStateOf("") }
        val customList = customCommentsByReel[reel.id] ?: emptyList()

        AlertDialog(
            onDismissRequest = { activeCommentReel = null },
            containerColor = RoyalNavyCard,
            title = {
                Text(
                    text = "التعليقات (${reel.reviewsCount + customList.size})",
                    fontSize = 15.sp,
                    color = SoftWhiteText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sampleComments = listOf(
                        "أجواء راقية جداً في ${reel.cityName} ✨",
                        "تجربة مميزة وحجز سريع."
                    )
                    (customList + sampleComments).take(4).forEach { comment ->
                        Surface(
                            color = RoyalNavyBackground.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = comment,
                                fontSize = 12.sp,
                                color = SoftWhiteText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("أضف تعليقاً...", fontSize = 12.sp) },
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
                            customCommentsByReel[reel.id] = listOf(newCommentText.trim()) + customList
                            newCommentText = ""
                            onShowMessage("تم إضافة تعليقك")
                        } else {
                            activeCommentReel = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepTealPrimary,
                        contentColor = SoftWhiteText
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("إرسال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeCommentReel = null }) {
                    Text("إغلاق", fontSize = 12.sp, color = SlateSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun SingleReelFullPage(
    reel: ReelItemUiModel,
    reelIndex: Int,
    isPlaying: Boolean,
    isLiked: Boolean,
    extraCommentsCount: Int,
    isSuperAdmin: Boolean,
    onTogglePlay: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShareReel: () -> Unit,
    onBookFromReel: () -> Unit,
    onOpenCreatorChat: () -> Unit,
    onEditPrice: () -> Unit
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.2f) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750)
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
                    onTap = { onTogglePlay() }
                )
            }
    ) {
        // 1. Full-Screen Video / Visual Media Canvas
        SmartServiceMediaView(
            imageUrl = reel.imageUrl,
            accentHex = reel.accentHex,
            secondaryHex = reel.secondaryHex,
            category = reel.category,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle Bottom Gradient Scrim (Clean, light, unobtrusive)
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val vignetteBrush = Brush.verticalGradient(
                        colors = listOf(
                            RoyalNavyBackground.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Transparent,
                            RoyalNavyBackground.copy(alpha = 0.55f),
                            RoyalNavyBackground.copy(alpha = 0.88f)
                        )
                    )
                    onDrawBehind {
                        drawRect(brush = vignetteBrush)
                    }
                }
        )

        // 3. Minimal Center Play Icon when paused
        if (!isPlaying) {
            Surface(
                color = RoyalNavyBackground.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = SoftWhiteText,
                        modifier = Modifier.size(30.dp)
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
                modifier = Modifier.size(84.dp)
            )
        }

        // 4. Clean 3-Icon Side Rail: ONLY Like, Comment, Share (Small & Uniform)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReelActionCircleButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                iconTint = if (isLiked) WarmCoral else SoftWhiteText,
                label = formatCompactMetric(reel.baseLikes + (if (isLiked) 1 else 0)),
                onClick = onToggleLike,
                testTag = "reel_like_button_${reel.id}"
            )

            ReelActionCircleButton(
                icon = Icons.Default.ChatBubbleOutline,
                iconTint = SoftWhiteText,
                label = formatCompactMetric(reel.reviewsCount + extraCommentsCount),
                onClick = onOpenComments,
                testTag = "reel_comments_button_${reel.id}"
            )

            ReelActionCircleButton(
                icon = Icons.Default.Share,
                iconTint = SoftWhiteText,
                label = "مشاركة",
                onClick = onShareReel,
                testTag = "reel_share_button_${reel.id}"
            )

            if (isSuperAdmin) {
                ReelActionCircleButton(
                    icon = Icons.Default.Edit,
                    iconTint = MutedLuxuryGold,
                    label = "$${reel.priceUsd}",
                    onClick = onEditPrice,
                    testTag = "reel_admin_edit_price_${reel.id}"
                )
            }
        }

        // 5. Minimal Bottom Creator Identity Only (65% less text — No bulky paragraphs or multi-buttons)
        val creatorLevel = 12 + ((reelIndex * 3) % 18)
        val creatorDisplayName = reel.sourceVenue?.providerDisplayName?.ifBlank { reel.title }
            ?: reel.title

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 68.dp, end = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek Compact Booking Pill
                Surface(
                    onClick = onBookFromReel,
                    color = DeepTealPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("reel_book_now_button_${reel.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "حجز • $${reel.priceUsd}",
                            fontSize = 11.sp,
                            color = SoftWhiteText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Creator Name + Golden LEVEL Badge + Small Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onOpenCreatorChat() }
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Golden LEVEL badge instead of cluttered icons
                            Surface(
                                color = MutedLuxuryGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.7.dp, MutedLuxuryGold.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = "LV.$creatorLevel",
                                    fontSize = 9.sp,
                                    color = MutedLuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }

                            Text(
                                text = creatorDisplayName,
                                fontSize = 13.sp,
                                color = SoftWhiteText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = reel.cityName,
                            fontSize = 11.sp,
                            color = SlateSecondaryText,
                            maxLines = 1
                        )
                    }

                    FramedUserAvatar(
                        avatarUri = reel.imageUrl,
                        fallbackInitial = creatorDisplayName.take(1),
                        frameId = "amber_sovereign",
                        level = creatorLevel,
                        avatarSize = 34.dp,
                        showLevelBadge = false,
                        isOnline = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hairline Progress Bar
            LinearProgressIndicator(
                progress = { if (isPlaying) progress else 0.45f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .clip(RoundedCornerShape(50)),
                color = DeepTealPrimary,
                trackColor = SoftWhiteText.copy(alpha = 0.16f)
            )
        }
    }
}

@Composable
private fun ReelActionCircleButton(
    icon: ImageVector,
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
            color = RoyalNavyBackground.copy(alpha = 0.48f),
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = SoftWhiteText.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun formatCompactMetric(count: Int): String {
    return if (count >= 1000) {
        String.format("%.1fK", count / 1000.0)
    } else {
        count.toString()
    }
}

/**
 * Clean, compact Horizontal Reels preview strip displayed on the Home/Explore screen.
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
            .padding(top = 4.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onOpenAllReels,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("home_open_all_reels_button")
            ) {
                Text(
                    text = "عرض الكل",
                    fontSize = 12.sp,
                    color = DeepTealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "الريلز",
                fontSize = 15.sp,
                color = SoftWhiteText,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(publishedServices.take(4), key = { "home_reel_pub_${it.id}" }) { pub ->
                HomeReelStoryCard(
                    title = pub.title,
                    imageUrl = pub.imageUrl,
                    category = pub.category,
                    accentHex = 0xFF0D9488L,
                    secondaryHex = 0xFF09111EL,
                    onClick = { onOpenReelItem(pub.id) }
                )
            }
            items(venues.take(6), key = { "home_reel_venue_${it.id}" }) { venue ->
                HomeReelStoryCard(
                    title = venue.name,
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

private val ReelStoryVignetteBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        RoyalNavyBackground.copy(alpha = 0.88f)
    )
)

@Composable
private fun HomeReelStoryCard(
    title: String,
    imageUrl: String,
    category: String,
    accentHex: Long,
    secondaryHex: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = RoyalNavyCard,
        modifier = Modifier
            .width(102.dp)
            .height(146.dp)
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

            Text(
                text = title,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = SoftWhiteText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}

private fun shareOrCopyReelLink(
    context: Context,
    reel: ReelItemUiModel,
    onCopiedMessage: (String) -> Unit
) {
    val shareText = buildString {
        append("${reel.title} (${reel.cityName})\n")
        append(SHARED_WEB_APP_URL)
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
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure {
        onCopiedMessage("تم نسخ الرابط إلى الحافظة")
    }
}
