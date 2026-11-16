package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.CoralSoftAccent
import com.example.ui.theme.DarkBorderGray
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.ElevatedDarkSurface
import com.example.ui.theme.MutedLightText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.VioletBlueVip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val DecodedUriBitmapCache = LruCache<String, ImageBitmap>(24)

@Composable
fun WingedHeartLogo(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val centerOffset = Offset(w * 0.5f, h * 0.5f)
            val glowRadius = (w * 0.52f).coerceAtLeast(1f)

            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    CoralPinkPrimary.copy(alpha = 0.45f),
                    VioletBlueVip.copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = centerOffset,
                radius = glowRadius
            )

            val leftWing = Path().apply {
                moveTo(w * 0.35f, h * 0.55f)
                cubicTo(w * 0.12f, h * 0.45f, w * 0.08f, h * 0.25f, w * 0.16f, h * 0.18f)
                cubicTo(w * 0.24f, h * 0.28f, w * 0.30f, h * 0.36f, w * 0.40f, h * 0.44f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(w * 0.65f, h * 0.55f)
                cubicTo(w * 0.88f, h * 0.45f, w * 0.92f, h * 0.25f, w * 0.84f, h * 0.18f)
                cubicTo(w * 0.76f, h * 0.28f, w * 0.70f, h * 0.36f, w * 0.60f, h * 0.44f)
                close()
            }

            val wingBrush = Brush.linearGradient(
                colors = listOf(VioletBlueVip, CoralPinkPrimary)
            )

            val heartPath = Path().apply {
                moveTo(w * 0.5f, h * 0.82f)
                cubicTo(w * 0.22f, h * 0.62f, w * 0.24f, h * 0.32f, w * 0.39f, h * 0.32f)
                cubicTo(w * 0.45f, h * 0.32f, w * 0.48f, h * 0.37f, w * 0.5f, h * 0.42f)
                cubicTo(w * 0.52f, h * 0.37f, w * 0.55f, h * 0.32f, w * 0.61f, h * 0.32f)
                cubicTo(w * 0.76f, h * 0.32f, w * 0.78f, h * 0.62f, w * 0.5f, h * 0.82f)
                close()
            }
            val heartBrush = Brush.verticalGradient(
                colors = listOf(CoralSoftAccent, CoralPinkPrimary, VioletBlueVip)
            )
            val strokeStyle = Stroke(width = 2.2f)

            onDrawBehind {
                drawCircle(
                    brush = glowBrush,
                    radius = glowRadius,
                    center = centerOffset
                )
                drawPath(leftWing, brush = wingBrush)
                drawPath(rightWing, brush = wingBrush)
                drawPath(path = heartPath, brush = heartBrush)
                drawPath(path = heartPath, color = PureWhite.copy(alpha = 0.75f), style = strokeStyle)
            }
        }
    )
}

/**
 * High-performance media renderer that displays:
 * 1) Cached, downsampled admin/user-picked gallery photo if `imageUrl` is a content/file URI, OR
 * 2) Clean, zero-bitmap-allocation Modern Creator & VIP Club vector/gradient backdrop with category badge.
 */
@Composable
fun SmartServiceMediaView(
    imageUrl: String,
    accentHex: Long,
    secondaryHex: Long,
    category: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCustomUri = imageUrl.startsWith("content://") || imageUrl.startsWith("file://")
    var decodedBitmap by remember(imageUrl) {
        mutableStateOf(if (isCustomUri) DecodedUriBitmapCache.get(imageUrl) else null)
    }

    LaunchedEffect(imageUrl) {
        if (isCustomUri && decodedBitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(imageUrl)
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }
                    var sampleSize = 1
                    val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                    while (maxDim / sampleSize > 720) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                DecodedUriBitmapCache.put(imageUrl, loaded)
                decodedBitmap = loaded
            }
        }
    }

    val isSpaCategory = remember(category) {
        category.contains("مساج") || category.contains("سبا") || category.contains("حمام")
    }

    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val baseGradient = Brush.linearGradient(
                colors = listOf(
                    CharcoalNight,
                    DarkCardSurface,
                    ElevatedDarkSurface
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val primaryGlow = Brush.radialGradient(
                colors = listOf(
                    (if (isSpaCategory) VioletBlueVip else CoralPinkPrimary).copy(alpha = 0.36f),
                    Color.Transparent
                ),
                center = Offset(w * 0.78f, h * 0.28f),
                radius = (w * 0.65f).coerceAtLeast(1f)
            )
            val secondaryGlow = Brush.radialGradient(
                colors = listOf(
                    (if (isSpaCategory) CoralPinkPrimary else VioletBlueVip).copy(alpha = 0.26f),
                    Color.Transparent
                ),
                center = Offset(w * 0.22f, h * 0.78f),
                radius = (w * 0.55f).coerceAtLeast(1f)
            )
            val bottomVignette = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    CharcoalNight.copy(alpha = 0.68f)
                ),
                startY = h * 0.35f,
                endY = h
            )
            onDrawBehind {
                drawRect(brush = baseGradient)
                drawCircle(
                    brush = primaryGlow,
                    radius = (w * 0.65f).coerceAtLeast(1f),
                    center = Offset(w * 0.78f, h * 0.28f)
                )
                drawCircle(
                    brush = secondaryGlow,
                    radius = (w * 0.55f).coerceAtLeast(1f),
                    center = Offset(w * 0.22f, h * 0.78f)
                )
                drawRect(brush = bottomVignette)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val bitmap = decodedBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = category,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CharcoalNight.copy(alpha = 0.08f),
                                CharcoalNight.copy(alpha = 0.62f)
                            )
                        )
                    )
            )
        } else {
            // Subtle centered VIP Club Emblem when no custom image is set
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.08f))
                        .border(1.dp, PureWhite.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpaCategory) Icons.Default.Spa else Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = if (isSpaCategory) VioletBlueVip else CoralPinkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BrandActionBadgesRow(
    onFavoritesClick: () -> Unit,
    onSupportChatClick: () -> Unit,
    onDirectCallClick: () -> Unit,
    onCitiesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Brand3DBadgeItem(
            icon = Icons.Default.Favorite,
            label = "المفضلة",
            accentColor = CoralPinkPrimary,
            onClick = onFavoritesClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.ChatBubble,
            label = "دعم @RamaNUR",
            accentColor = VioletBlueVip,
            onClick = onSupportChatClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.PhoneInTalk,
            label = "حجز فوري",
            accentColor = CoralPinkPrimary,
            onClick = onDirectCallClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.LocationOn,
            label = "جميع المدن",
            accentColor = VioletBlueVip,
            onClick = onCitiesClick
        )
    }
}

@Composable
private fun Brand3DBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun InteractiveVideoShowcaseCard(
    videoTitle: String,
    durationLabel: String,
    cityName: String,
    priceUsd: Int,
    imageUrl: String = "brand_dark_showcase",
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.18f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(350L)
            progress = if (progress >= 0.96f) 0.12f else progress + 0.03f
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CharcoalNight,
        border = BorderStroke(1.dp, DarkBorderGray)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clickable { isPlaying = !isPlaying }
            ) {
                SmartServiceMediaView(
                    imageUrl = imageUrl,
                    accentHex = 0xFFFF3B6B,
                    secondaryHex = 0xFF121214,
                    category = "فيديو",
                    modifier = Modifier.fillMaxSize()
                )

                // Top Live Video Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CoralPinkPrimary,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPlaying) "بث 4K مباشر" else "متوقف • $durationLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        color = VioletBlueVip,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$$priceUsd • $cityName",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Center Play/Pause Button
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(CoralPinkPrimary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل الفيديو",
                        tint = PureWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Bottom Video Title & Progress
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = PureWhite.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = VioletBlueVip,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (isPlaying) progress else 0.35f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = CoralPinkPrimary,
                        trackColor = PureWhite.copy(alpha = 0.22f)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen interactive modal for viewing a service/profile's Photos & Video showcase,
 * uploading additional photos from the device gallery (Super Admin), editing the price, or booking immediately.
 */
@Composable
fun FullScreenMediaViewerDialog(
    title: String,
    cityName: String,
    category: String,
    priceUsd: Int,
    primaryImageUrl: String,
    videoTitle: String,
    galleryCaptions: List<String>,
    initialTabIsVideo: Boolean = true,
    isSuperAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onBookNow: () -> Unit,
    onEditPriceClick: () -> Unit,
    onAddCustomPhoto: (String) -> Unit = {}
) {
    var isVideoMode by remember { mutableStateOf(initialTabIsVideo) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    var customGalleryUris by remember { mutableStateOf(listOf<String>()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriStr = uri.toString()
            customGalleryUris = listOf(uriStr) + customGalleryUris
            selectedPhotoIndex = 0
            isVideoMode = false
            onAddCustomPhoto(uriStr)
        }
    }

    val allPhotoItems = remember(primaryImageUrl, customGalleryUris, galleryCaptions) {
        val basePresets = listOf(
            primaryImageUrl.ifBlank { "brand_dark_showcase" } to (galleryCaptions.getOrNull(0) ?: "الجناح الملكي الرئيسي"),
            "brand_light_showcase" to (galleryCaptions.getOrNull(1) ?: "أجواء الضيافة الخاصة VIP"),
            "brand_collateral" to (galleryCaptions.getOrNull(2) ?: "تفاصيل العناية والشموع العطرية"),
            "brand_billboard" to (galleryCaptions.getOrNull(3) ?: "صالة السهرات والموسيقى الحية")
        )
        val customMapped = customGalleryUris.mapIndexed { idx, uri ->
            uri to "صورة مرفقة من المعرض #${idx + 1}"
        }
        customMapped + basePresets
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            color = CharcoalNight,
            border = BorderStroke(1.dp, DarkBorderGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkCardSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = PureWhite
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = VioletBlueVip,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$cityName • $category • $$priceUsd",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedLightText
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            WingedHeartLogo(modifier = Modifier.size(38.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Switcher: Video Player vs Photo Album
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isVideoMode,
                            onClick = { isVideoMode = true },
                            label = { Text("مشغل الفيديو 4K", maxLines = 1) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralPinkPrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = !isVideoMode,
                            onClick = { isVideoMode = false },
                            label = { Text("ألبوم الصور (${allPhotoItems.size})", maxLines = 1) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletBlueVip,
                                selectedLabelColor = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSuperAdmin) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, VioletBlueVip),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = VioletBlueVip,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إضافة صورة",
                                    color = PureWhite,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Center Media Stage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isVideoMode) {
                        InteractiveVideoShowcaseCard(
                            videoTitle = videoTitle,
                            durationLabel = "01:45 4K HD",
                            cityName = cityName,
                            priceUsd = priceUsd,
                            imageUrl = primaryImageUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = DarkCardSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DarkBorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "تفاصيل العرض والخدمة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "معاينة حية للأجنحة الخاصة وتجهيزات السبا والسهرات في $cityName.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedLightText
                                )
                            }
                        }
                    } else {
                        val activePhoto = allPhotoItems.getOrElse(selectedPhotoIndex) { allPhotoItems.first() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, DarkBorderGray, RoundedCornerShape(20.dp))
                        ) {
                            SmartServiceMediaView(
                                imageUrl = activePhoto.first,
                                accentHex = 0xFFFF3B6B,
                                secondaryHex = 0xFF1C1C1F,
                                category = category,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = CharcoalNight.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = VioletBlueVip,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedPhotoIndex + 1}/${allPhotoItems.size} • ${activePhoto.second}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(allPhotoItems) { idx, item ->
                                val selected = idx == selectedPhotoIndex
                                Box(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) VioletBlueVip else DarkBorderGray,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { selectedPhotoIndex = idx }
                                ) {
                                    SmartServiceMediaView(
                                        imageUrl = item.first,
                                        accentHex = 0xFFFF3B6B,
                                        secondaryHex = 0xFF1C1C1F,
                                        category = category,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar: Capsule CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSuperAdmin) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditPriceClick()
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VioletBlueVip),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = VioletBlueVip,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تعديل ($$priceUsd)",
                                color = PureWhite,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onBookNow()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPinkPrimary,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("media_viewer_book_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حجز مباشر الآن • $$priceUsd",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroBannerCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    CharcoalNight,
                    DarkCardSurface,
                    ElevatedDarkSurface
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CoralPinkPrimary.copy(alpha = 0.35f),
                    VioletBlueVip.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(w * 0.25f, h * 0.35f),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.25f, h * 0.35f)
        )
    }
}

@Composable
fun VenueHeaderArt(
    accentHex: Long,
    secondaryHex: Long,
    category: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    CharcoalNight,
                    DarkCardSurface
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VioletBlueVip.copy(alpha = 0.32f),
                    CoralPinkPrimary.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(w * 0.75f, h * 0.35f),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.75f, h * 0.35f)
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    CharcoalNight.copy(alpha = 0.85f)
                ),
                startY = h * 0.32f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.32f),
            size = Size(w, h * 0.68f)
        )
    }
}
