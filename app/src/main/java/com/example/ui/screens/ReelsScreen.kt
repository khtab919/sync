package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PublishedService
import com.example.data.model.Venue
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.RoyalNavyBackground
import com.example.ui.theme.RoyalNavyCard
import com.example.ui.theme.SoftWhiteText

/** Kept as the price-editor contract. Stats are deliberately not synthesized. */
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
    // One reel = one real approved video. No synthetic venue slides or counters.
    val reels = remember(publishedServices) {
        publishedServices.filter { it.status == "active" &&
            it.videoUrl.isNotBlank() && it.category != "منشور مدفوع" }
    }
    val pageIndex = remember(reels, initialReelId) {
        reels.indexOfFirst { it.id == initialReelId || "reel_pub_${it.id}" == initialReelId }
            .coerceAtLeast(0)
    }
    val pager = rememberPagerState(initialPage = pageIndex, pageCount = { reels.size.coerceAtLeast(1) })
    var muted by remember { mutableStateOf(false) }
    LaunchedEffect(initialReelId, reels.size) {
        val requested = reels.indexOfFirst { it.id == initialReelId || "reel_pub_${it.id}" == initialReelId }
        if (requested >= 0 && requested != pager.currentPage) pager.scrollToPage(requested)
    }

    Box(modifier = Modifier.fillMaxSize().background(RoyalNavyBackground).testTag("reels_screen_root")) {
        if (reels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد فيديوهات ريلز معتمدة حاليًا", color = SoftWhiteText)
            }
        } else {
            VerticalPager(
                state = pager,
                beyondViewportPageCount = 0,
                key = { reels[it].id },
                modifier = Modifier.fillMaxSize().testTag("reels_vertical_pager")
            ) { index ->
                val service = reels[index]
                Box(Modifier.fillMaxSize()) {
                    ApprovedReelPlayer(
                        url = service.videoUrl,
                        isPlaying = pager.currentPage == index,
                        muted = muted
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(RoyalNavyBackground.copy(alpha = 0.86f))
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(service.title, color = SoftWhiteText, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${service.cityName} • ${service.category}",
                            color = SoftWhiteText.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onBookPublished(service) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("reel_request_booking_${service.id}")
                            ) {
                                Text("طلب حجز • ${service.priceUsd}$")
                            }
                            IconButton(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, service.videoUrl)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(send, "مشاركة الفيديو"))
                                    } catch (e: Exception) {
                                        onShowMessage("تعذر فتح مشاركة الفيديو")
                                    }
                                },
                                modifier = Modifier.testTag("reel_share_${service.id}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة الفيديو", tint = SoftWhiteText)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الريلز", color = SoftWhiteText, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { muted = !muted }, modifier = Modifier.testTag("reel_toggle_sound_button")) {
                Icon(
                    if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (muted) "تشغيل الصوت" else "كتم الصوت",
                    tint = SoftWhiteText
                )
            }
        }
    }
}

@Composable
private fun ApprovedReelPlayer(url: String, isPlaying: Boolean, muted: Boolean) {
    var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var view by remember(url) { mutableStateOf<VideoView?>(null) }
    var hasError by remember(url) { mutableStateOf(false) }
    DisposableEffect(url) {
        onDispose {
            view?.stopPlayback()
            player = null
            view = null
        }
    }
    LaunchedEffect(muted, player) {
        player?.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    view = this
                    setVideoURI(Uri.parse(url))
                    setOnPreparedListener { media ->
                        player = media
                        media.isLooping = true
                        media.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                        if (isPlaying) start()
                    }
                    setOnErrorListener { _, _, _ ->
                        hasError = true
                        true
                    }
                }
            },
            update = { video ->
                if (isPlaying && !hasError) video.start() else video.pause()
            },
            modifier = Modifier.fillMaxSize().testTag("actual_reel_video")
        )
        if (hasError) Text("تعذّر تشغيل الفيديو", color = Color.White)
        if (!isPlaying && !hasError) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

/** Compact home strip, containing only videos published in the actual database. */
@Composable
fun HomeReelsPreviewStrip(
    venues: List<Venue>,
    publishedServices: List<PublishedService>,
    onOpenReelItem: (String) -> Unit,
    onOpenAllReels: () -> Unit
) {
    val reels = remember(publishedServices) {
        publishedServices.filter { it.status == "active" &&
            it.videoUrl.isNotBlank() && it.category != "منشور مدفوع" }
    }
    if (reels.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("الريلز", style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.TextButton(onClick = onOpenAllReels) { Text("عرض الكل") }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(reels.take(8), key = { it.id }) { reel ->
                Surface(
                    onClick = { onOpenReelItem(reel.id) },
                    color = RoyalNavyCard,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(112.dp).height(155.dp)
                        .testTag("home_reel_${reel.id}")
                ) {
                    Box(Modifier.fillMaxSize()) {
                        SmartServiceMediaView(
                            imageUrl = reel.imageUrl,
                            category = reel.category,
                            accentHex = 0xFF45788CL,
                            secondaryHex = 0xFF182D3EL,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            reel.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(RoyalNavyBackground.copy(alpha = 0.85f))
                                .padding(9.dp)
                        )
                    }
                }
            }
        }
    }
}
