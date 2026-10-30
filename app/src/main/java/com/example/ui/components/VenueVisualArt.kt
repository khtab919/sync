package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.BrandRoyalPlum
import com.example.ui.theme.DeepRoyalViolet
import com.example.ui.theme.DustyRose
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IvoryCream
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SoftRosePink

@Composable
fun WingedHeartLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient Rose-Gold & Violet Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DustyRose.copy(alpha = 0.55f),
                    RoyalGold.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.52f
            ),
            radius = w * 0.52f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Golden Wings
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

        val goldBrush = Brush.linearGradient(
            colors = listOf(SoftGold, RoyalGold, DustyRose)
        )
        drawPath(leftWing, brush = goldBrush)
        drawPath(rightWing, brush = goldBrush)

        // Central Rose-Violet Heart
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.22f, h * 0.62f, w * 0.24f, h * 0.32f, w * 0.39f, h * 0.32f)
            cubicTo(w * 0.45f, h * 0.32f, w * 0.48f, h * 0.37f, w * 0.5f, h * 0.42f)
            cubicTo(w * 0.52f, h * 0.37f, w * 0.55f, h * 0.32f, w * 0.61f, h * 0.32f)
            cubicTo(w * 0.76f, h * 0.32f, w * 0.78f, h * 0.62f, w * 0.5f, h * 0.82f)
            close()
        }
        drawPath(
            path = heartPath,
            brush = Brush.verticalGradient(
                colors = listOf(SoftRosePink, DustyRose, DeepRoyalViolet)
            )
        )
        drawPath(
            path = heartPath,
            color = SoftGold,
            style = Stroke(width = 2.5f)
        )
    }
}

/**
 * Smart media renderer that displays:
 * 1) Real user-picked gallery photo if `imageUrl` is a content/file URI, OR
 * 2) Distinct high-resolution visual quadrants from the uploaded Brand Identity board (`R.drawable.img_brand_identity`)
 *    combined with custom luxury lighting overlays so every profile, venue, and offer has real visual richness.
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
    val decodedBitmap: ImageBitmap? = remember(imageUrl) {
        if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(imageUrl))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        } else {
            null
        }
    }

    val specificDrawableRes = remember(imageUrl, category, accentHex) {
        when {
            imageUrl.contains("palace_hall") -> R.drawable.img_palace_hall
            imageUrl.contains("card_qudud") -> R.drawable.img_card_qudud
            imageUrl.contains("card_lounge") -> R.drawable.img_card_lounge
            imageUrl.contains("card_spa") -> R.drawable.img_card_spa
            imageUrl.contains("card_vip_night") -> R.drawable.img_card_vip_night
            imageUrl.contains("case_study") -> R.drawable.img_ui_case_study
            category.contains("مساج") || category.contains("سبا") -> R.drawable.img_card_spa
            category.contains("زيارات") -> R.drawable.img_card_vip_night
            category.contains("حمام") -> R.drawable.img_card_spa
            accentHex == 0xFFE5B84BL -> R.drawable.img_palace_hall
            accentHex == 0xFFE87A5DL -> R.drawable.img_card_lounge
            else -> null
        }
    }

    val cropAlignment = remember(imageUrl, category, accentHex) {
        when {
            imageUrl.contains("light") || imageUrl.contains("gold") -> BiasAlignment(-0.65f, 0.75f)
            imageUrl.contains("collateral") || category.contains("حمام") -> BiasAlignment(0.75f, -0.7f)
            imageUrl.contains("billboard") || category.contains("لاونج") -> BiasAlignment(0.75f, 0.75f)
            else -> BiasAlignment(-0.7f, -0.65f)
        }
    }

    Box(modifier = modifier) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = category,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (specificDrawableRes != null) {
            Image(
                painter = painterResource(id = specificDrawableRes),
                contentDescription = category,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VenueHeaderArt(
                accentHex = accentHex,
                secondaryHex = secondaryHex,
                category = category,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(id = R.drawable.img_brand_identity),
                contentDescription = category,
                contentScale = ContentScale.Crop,
                alignment = cropAlignment,
                alpha = 0.82f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle luxury gradient overlay for text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandDeepViolet.copy(alpha = 0.10f),
                            BrandDeepViolet.copy(alpha = 0.72f)
                        )
                    )
                )
        )
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
            gradientColors = listOf(Color(0xFF9333EA), DustyRose),
            onClick = onFavoritesClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.ChatBubble,
            label = "دعم @RamaNUR",
            gradientColors = listOf(DeepRoyalViolet, RoyalGold),
            onClick = onSupportChatClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.PhoneInTalk,
            label = "حجز فوري",
            gradientColors = listOf(DustyRose, RoyalGold),
            onClick = onDirectCallClick
        )
        Brand3DBadgeItem(
            icon = Icons.Default.LocationOn,
            label = "جميع المدن",
            gradientColors = listOf(RoyalGold, DeepRoyalViolet),
            onClick = onCitiesClick
        )
    }
}

@Composable
private fun Brand3DBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    gradientColors: List<Color>,
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
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(gradientColors))
                .border(1.dp, SoftGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = IvoryCream,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
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
    val infiniteTransition = rememberInfiniteTransition(label = "video_progress")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scrubber"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianNight,
        border = BorderStroke(1.2.dp, RoyalGold.copy(alpha = 0.6f))
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
                    accentHex = 0xFFD6789B,
                    secondaryHex = 0xFF2C1644,
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
                        color = DustyRose,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPlaying) "مشغل الفيديو المباشر • 4K HD" else "عرض فيديو متوقف • $durationLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = RoyalGold,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$$priceUsd • $cityName",
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianNight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Center Play/Pause Button
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DustyRose, RoyalGold)
                            )
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل الفيديو",
                        tint = ObsidianNight,
                        modifier = Modifier.size(34.dp)
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
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (isPlaying) progress else 0.35f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = RoyalGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen interactive modal for viewing a service/profile's Photos & Video showcase,
 * uploading additional photos from the device gallery, editing the price, or booking immediately.
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
            "brand_light_showcase" to (galleryCaptions.getOrNull(1) ?: "النسخة الفاتحة والناعمة VIP"),
            "brand_collateral" to (galleryCaptions.getOrNull(2) ?: "تفاصيل الضيافة والشموع العطرية"),
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
            shape = RoundedCornerShape(28.dp),
            color = BrandDeepViolet,
            border = BorderStroke(1.5.dp, BrandChampagneGold)
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            WingedHeartLogo(modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$cityName • $category • السعر: $$priceUsd",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandChampagneGold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BrandRoyalPlum)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
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
                            label = { Text("🎥 مشغل الفيديو 4K") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandDustyRose,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = !isVideoMode,
                            onClick = { isVideoMode = false },
                            label = { Text("📸 ألبوم الصور (${allPhotoItems.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandChampagneGold,
                                selectedLabelColor = ObsidianNight
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
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandChampagneGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = BrandChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة صورة", color = BrandChampagneGold, style = MaterialTheme.typography.labelMedium)
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
                            color = BrandRoyalPlum.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "تفاصيل عرض الفيديو والخدمة:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrandChampagneGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "معاينة حية للأجنحة الخاصة وتجهيزات المساج الملكي وصالة السهرات في $cityName.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        val activePhoto = allPhotoItems.getOrElse(selectedPhotoIndex) { allPhotoItems.first() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.5.dp, BrandChampagneGold, RoundedCornerShape(22.dp))
                        ) {
                            SmartServiceMediaView(
                                imageUrl = activePhoto.first,
                                accentHex = 0xFFD6789B,
                                secondaryHex = 0xFF47256B,
                                category = category,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = BrandDeepViolet.copy(alpha = 0.85f),
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
                                        tint = BrandChampagneGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedPhotoIndex + 1}/${allPhotoItems.size} • ${activePhoto.second}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
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
                                        .width(110.dp)
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (selected) 2.dp else 0.8.dp,
                                            color = if (selected) BrandChampagneGold else Color.White.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedPhotoIndex = idx }
                                ) {
                                    SmartServiceMediaView(
                                        imageUrl = item.first,
                                        accentHex = 0xFFD6789B,
                                        secondaryHex = 0xFF241338,
                                        category = category,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar: Direct Booking (Price Edit visible only to Super Admin)
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
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, BrandChampagneGold),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = BrandChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تعديل السعر ($$priceUsd)",
                                color = BrandChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onBookNow()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandDustyRose,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("media_viewer_book_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حجز مباشر الآن",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
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
                    Color(0xFF130A1E),
                    Color(0xFF2B1442),
                    Color(0xFF51215E),
                    Color(0xFF1C0E2B)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DustyRose.copy(alpha = 0.48f),
                    RoyalGold.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(w * 0.25f, h * 0.35f),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.25f, h * 0.35f)
        )

        val archPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.70f)
            quadraticTo(w * 0.18f, h * 0.46f, w * 0.35f, h * 0.68f)
            quadraticTo(w * 0.55f, h * 0.40f, w * 0.75f, h * 0.66f)
            quadraticTo(w * 0.88f, h * 0.48f, w, h * 0.64f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = archPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF28123D).copy(alpha = 0.65f),
                    Color(0xFF12091D).copy(alpha = 0.95f)
                )
            )
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
    val accentColor = Color(accentHex)
    val secondaryColor = Color(secondaryHex)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    secondaryColor,
                    Color(0xFF1A0F2B),
                    DustyRose.copy(alpha = 0.35f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.45f),
                    DustyRose.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(w * 0.75f, h * 0.35f),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(w * 0.75f, h * 0.35f)
        )

        if (category.contains("مساج") || category.contains("حمام")) {
            val cx = w * 0.25f
            val cy = h * 0.52f
            for (i in 1..3) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.22f / i),
                    radius = (h * 0.22f) * i,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5f)
                )
            }
        } else {
            val wavePath = Path().apply {
                moveTo(0f, h * 0.75f)
                cubicTo(
                    w * 0.25f, h * 0.45f,
                    w * 0.55f, h * 0.85f,
                    w, h * 0.50f
                )
            }
            drawPath(
                path = wavePath,
                color = accentColor.copy(alpha = 0.35f),
                style = Stroke(width = 3f)
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF120B1D).copy(alpha = 0.90f)
                ),
                startY = h * 0.32f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.32f),
            size = Size(w, h * 0.68f)
        )
    }
}
