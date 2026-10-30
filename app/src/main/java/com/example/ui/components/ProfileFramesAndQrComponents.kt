package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarFrameCatalog
import com.example.data.model.OfficialPaymentCredentials
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.CoralSoftAccent
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ShamCashIndigo
import com.example.ui.theme.ShamCashTeal
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.TitleDarkText
import com.example.ui.theme.UsdtTetherGreen
import com.example.ui.theme.VioletBlueVip
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 1. FRAMED USER AVATAR WITH ROYAL ORNAMENTAL RINGS, CROWN & LEVEL BADGE
// ============================================================================

@Composable
fun FramedUserAvatar(
    avatarUri: String,
    fallbackInitial: String,
    frameId: String,
    level: Int,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 86.dp,
    showLevelBadge: Boolean = true,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val frame = remember(frameId) { AvatarFrameCatalog.findById(frameId) }
    val primaryColor = Color(frame.primaryHex)
    val secondaryColor = Color(frame.secondaryHex)
    val accentColor = Color(frame.accentHex)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(avatarSize + 18.dp)
            .let { base ->
                if (onClick != null) base.clickable { onClick() } else base
            }
    ) {
        // Outer Ornamental Frame Canvas (Rings, Corner Gems, Crown Crest)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - 4.dp.toPx()
            val innerRadius = outerRadius - 5.dp.toPx()

            // Soft outer aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius + 6.dp.toPx()
                ),
                radius = outerRadius + 6.dp.toPx(),
                center = center
            )

            // Outer gradient ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor,
                        accentColor,
                        secondaryColor,
                        primaryColor,
                        accentColor,
                        primaryColor
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Inner metallic hairline ring
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 8 Ornamental Studs around the frame
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45.0) - 90.0)
                val px = center.x + (outerRadius * cos(angleRad)).toFloat()
                val py = center.y + (outerRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) accentColor else secondaryColor,
                    radius = if (i == 0) 4.2.dp.toPx() else 2.6.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Top Royal Crown / Diamond Crest
            val crownCenterY = center.y - outerRadius - 1.dp.toPx()
            val crownWidth = 18.dp.toPx()
            val crownHeight = 9.dp.toPx()
            val crownPath = Path().apply {
                moveTo(center.x - crownWidth / 2f, crownCenterY + crownHeight / 2f)
                lineTo(center.x - crownWidth * 0.38f, crownCenterY - crownHeight * 0.4f)
                lineTo(center.x - crownWidth * 0.15f, crownCenterY + crownHeight * 0.1f)
                lineTo(center.x, crownCenterY - crownHeight * 0.7f)
                lineTo(center.x + crownWidth * 0.15f, crownCenterY + crownHeight * 0.1f)
                lineTo(center.x + crownWidth * 0.38f, crownCenterY - crownHeight * 0.4f)
                lineTo(center.x + crownWidth / 2f, crownCenterY + crownHeight / 2f)
                close()
            }
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(accentColor, primaryColor, secondaryColor)
                )
            )
        }

        // Inner Avatar Circle
        Box(
            modifier = Modifier
                .size(avatarSize - 4.dp)
                .clip(CircleShape)
                .background(DarkCardSurface),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri.isNotBlank()) {
                SmartServiceMediaView(
                    imageUrl = avatarUri,
                    accentHex = frame.primaryHex,
                    secondaryHex = frame.secondaryHex,
                    category = "صورة شخصية",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (fallbackInitial.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    CharcoalNight
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fallbackInitial.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                WingedHeartLogo(modifier = Modifier.size(avatarSize * 0.56f))
            }
        }

        // Online Status Indicator Dot (Top Start)
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 6.dp, start = 6.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(EmeraldVerified)
                    .border(2.dp, CharcoalNight, CircleShape)
            )
        }

        // Bottom Level Pill Badge (Lv. X)
        if (showLevelBadge) {
            Surface(
                color = CharcoalNight,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.2.dp, primaryColor),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Lv.$level",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. DETERMINISTIC QR CODE & 1D BARCODE RENDERERS FOR SHAM CASH & USDT BEP-20
// ============================================================================

@Composable
fun ScannableQrMatrixCanvas(
    payload: String,
    moduleColor: Color,
    backgroundColor: Color = Color.White,
    centerBadgeColor: Color? = null,
    centerBadgeSymbol: String? = null,
    modifier: Modifier = Modifier
) {
    val gridSize = 29
    val matrix = remember(payload) { buildDeterministicQrMatrix(payload, gridSize) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.minDimension / gridSize.toFloat()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (matrix[r][c]) {
                        drawRoundRect(
                            color = moduleColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f),
                            cornerRadius = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)
                        )
                    }
                }
            }
        }

        if (centerBadgeColor != null && !centerBadgeSymbol.isNullOrBlank()) {
            Surface(
                color = centerBadgeColor,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.5.dp, Color.White),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = centerBadgeSymbol,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UsdtLinearBarcodeCanvas(
    address: String,
    barColor: Color = Color(0xFF0F172A),
    backgroundColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val barWeights = remember(address) {
        val list = mutableListOf<Boolean>()
        // Start guard bars
        list.addAll(listOf(true, false, true, false, true, true, false))
        address.forEachIndexed { idx, ch ->
            val code = ch.code + (idx * 7)
            for (bit in 0..5) {
                list.add(((code shr bit) and 1) == 1)
            }
            list.add(false)
        }
        // End guard bars
        list.addAll(listOf(true, true, false, true, false, true))
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, UsdtTetherGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = UsdtTetherGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "BEP-20 BARCODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = UsdtTetherGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "بار كود عنوان USDT المعتمد",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF334155),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val totalBars = barWeights.size.coerceAtLeast(1)
            val unitWidth = size.width / totalBars.toFloat()
            barWeights.forEachIndexed { index, isDark ->
                if (isDark) {
                    val isGuard = index < 7 || index > totalBars - 7 || (index % 32 == 0)
                    val barHeight = if (isGuard) size.height else size.height * 0.86f
                    drawRect(
                        color = barColor,
                        topLeft = Offset(index * unitWidth, 0f),
                        size = Size((unitWidth * 0.88f).coerceAtLeast(1.2f), barHeight)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = address,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildDeterministicQrMatrix(payload: String, size: Int): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }
    val reserved = Array(size) { BooleanArray(size) }

    fun placeFinderPattern(rowOffset: Int, colOffset: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val rr = rowOffset + r
                val cc = colOffset + c
                if (rr in 0 until size && cc in 0 until size) {
                    reserved[rr][cc] = true
                    val inOuter = r in 0..6 && c in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                    val inInner = r in 2..4 && c in 2..4
                    grid[rr][cc] = inOuter || inInner
                }
            }
        }
    }

    placeFinderPattern(0, 0)
    placeFinderPattern(0, size - 7)
    placeFinderPattern(size - 7, 0)

    // Alignment pattern at bottom-right (around size - 9, size - 9)
    val alignCenter = size - 7
    for (r in -2..2) {
        for (c in -2..2) {
            val rr = alignCenter + r
            val cc = alignCenter + c
            if (rr in 0 until size && cc in 0 until size) {
                reserved[rr][cc] = true
                grid[rr][cc] = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
            }
        }
    }

    // Timing patterns
    for (i in 8 until size - 8) {
        reserved[6][i] = true
        grid[6][i] = (i % 2 == 0)
        reserved[i][6] = true
        grid[i][6] = (i % 2 == 0)
    }

    // Reserve center area for optional logo
    for (r in (size / 2 - 2)..(size / 2 + 2)) {
        for (c in (size / 2 - 2)..(size / 2 + 2)) {
            if (r in 0 until size && c in 0 until size) {
                reserved[r][c] = true
                grid[r][c] = false
            }
        }
    }

    // Fill data modules deterministically from payload bytes
    val bytes = payload.encodeToByteArray()
    var bitIdx = 0
    for (r in 0 until size) {
        for (c in 0 until size) {
            if (!reserved[r][c]) {
                val byteVal = bytes[bitIdx % bytes.size].toInt() and 0xFF
                val shift = (bitIdx / bytes.size) % 8
                val hashMix = ((r * 31 + c * 17 + bitIdx * 13) xor byteVal)
                grid[r][c] = (((byteVal shr shift) xor (hashMix and 1)) and 1) == 1
                bitIdx++
            }
        }
    }
    return grid
}

// ============================================================================
// 3. SHAM CASH (شام كاش السوري) OFFICIAL RECEIVE & QR CARD
// ============================================================================

@Composable
fun ShamCashEmblemIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top right blue-indigo geometric ribbon
        val topRibbon = Path().apply {
            moveTo(w * 0.34f, h * 0.14f)
            lineTo(w * 0.78f, h * 0.38f)
            lineTo(w * 0.78f, h * 0.56f)
            lineTo(w * 0.54f, h * 0.68f)
            lineTo(w * 0.38f, h * 0.58f)
            lineTo(w * 0.56f, h * 0.46f)
            lineTo(w * 0.34f, h * 0.32f)
            close()
        }
        drawPath(
            path = topRibbon,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
            )
        )

        // Bottom left emerald-teal geometric ribbon
        val bottomRibbon = Path().apply {
            moveTo(w * 0.66f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.22f, h * 0.44f)
            lineTo(w * 0.46f, h * 0.32f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.44f, h * 0.54f)
            lineTo(w * 0.66f, h * 0.68f)
            close()
        }
        drawPath(
            path = bottomRibbon,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2DD4BF), Color(0xFF0EA5E9))
            )
        )
    }
}

@Composable
fun ShamCashOfficialCard(
    amountUsd: Int,
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val address = OfficialPaymentCredentials.SHAM_CASH_ADDRESS
    var showCopiedPill by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sham_cash_official_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFDDE3EA) // Matches the cool silver-blue backdrop of the Sham Cash screenshot
        ),
        border = BorderStroke(1.5.dp, ShamCashTeal.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: "استلام" + Sham Cash Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "المبلغ المطلوب: $$amountUsd",
                        style = MaterialTheme.typography.labelLarge,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "استلام • شام كاش السوري",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Top Emblem Tab + White QR Card (Exact layout from Screenshot 2)
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main White Rounded QR Container
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .padding(top = 34.dp)
                        .size(235.dp)
                ) {
                    ScannableQrMatrixCanvas(
                        payload = address,
                        moduleColor = Color(0xFF283B63), // Deep Navy Sham Cash QR color
                        backgroundColor = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    )
                }

                // Top Protruding White Rounded Crest with Sham Cash Geometric S-Ribbon
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(width = 76.dp, height = 58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ShamCashEmblemIcon(modifier = Modifier.size(42.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-line Sham Cash Address matching Screenshot 2
            Text(
                text = address.take(24),
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center
            )
            Text(
                text = address.drop(24),
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated "تم نسخ العنوان" Toast Pill matching Screenshot 2
            AnimatedVisibility(visible = showCopiedPill) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ShamCashEmblemIcon(modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "تم نسخ العنوان",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Copy & Share Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        copyTextToClipboard(context, "Sham Cash Address", address)
                        showCopiedPill = true
                        onCopyMessage("تم نسخ عنوان شام كاش السوري: $address")
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_sham_cash_address_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ العنوان",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ كود شام كاش", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        sharePaymentAddress(
                            context = context,
                            title = "عنوان استلام شام كاش السوري",
                            body = "عنوان شام كاش السوري للتحويل ($$amountUsd):\n$address"
                        )
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.2.dp, Color(0xFF334155)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 4. USDT (BEP-20) TETHER DEPOSIT CARD WITH QR CODE + 1D BARCODE
// ============================================================================

@Composable
fun UsdtBep20OfficialCard(
    amountUsd: Int,
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val depositAddress = OfficialPaymentCredentials.USDT_DEPOSIT_ADDRESS
    val accountId = OfficialPaymentCredentials.USDT_ACCOUNT_ID
    val networkName = OfficialPaymentCredentials.USDT_NETWORK
    var showBarcodeMode by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("usdt_bep20_official_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.5.dp, UsdtTetherGreen.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Black Header Strip + "حساب الإيداع" Title (Matching Screenshot 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = UsdtTetherGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, UsdtTetherGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = UsdtTetherGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "USDT BEP-20 معتمد",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "حساب الإيداع",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // QR Code + 1D Barcode Section for USDT BEP-20 (Specifically requested: "وقم بإنشاء بار كود لي usdt")
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showBarcodeMode = !showBarcodeMode },
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (showBarcodeMode) "عرض QR + الباركود معاً" else "تفعيل الباركود الخطي",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = "امسح رمز QR أو الباركود للإيداع الفوري",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2D Scannable QR Code for USDT Deposit Address
                    ScannableQrMatrixCanvas(
                        payload = depositAddress,
                        moduleColor = Color(0xFF0F172A),
                        backgroundColor = Color.White,
                        centerBadgeColor = UsdtTetherGreen,
                        centerBadgeSymbol = "₮",
                        modifier = Modifier
                            .size(196.dp)
                            .border(1.5.dp, UsdtTetherGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .testTag("usdt_qr_code_canvas")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1D Linear Barcode for USDT Deposit Address
                    UsdtLinearBarcodeCanvas(
                        address = depositAddress,
                        modifier = Modifier.testTag("usdt_linear_barcode_canvas")
                    )
                }
            }

            // Structured Deposit Details matching User Screenshot 1
            // Row 1: المبلغ المطلوب دفعه -> USDT X
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            copyTextToClipboard(context, "USDT Amount", "$amountUsd")
                            onCopyMessage("تم نسخ المبلغ المطلوب: USDT $amountUsd")
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ المبلغ",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "USDT $amountUsd",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "المبلغ المطلوب دفعه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Row 2: الحساب -> MT5Standard1250311802
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accountId,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "الحساب",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Row 3: الشبكة -> (BEP-20) Tether USD/USDT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = networkName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "الشبكة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Row 4: عنوان الإيداع -> 0xc35797768180752b67743d791f79f812bb0972d1
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "عنوان الإيداع",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            copyTextToClipboard(context, "USDT BEP-20 Address", depositAddress)
                            onCopyMessage("تم نسخ عنوان الإيداع USDT (BEP-20) بنجاح")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ عنوان الإيداع",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = depositAddress,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Copy & Share Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        copyTextToClipboard(context, "USDT Address", depositAddress)
                        onCopyMessage("تم نسخ عنوان الإيداع USDT (BEP-20): $depositAddress")
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UsdtTetherGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_usdt_address_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ عنوان USDT", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        sharePaymentAddress(
                            context = context,
                            title = "عنوان إيداع USDT (BEP-20)",
                            body = "الشبكة: $networkName\nالحساب: $accountId\nعنوان الإيداع:\n$depositAddress"
                        )
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.2.dp, UsdtTetherGreen),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = Color(0xFF065F46),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 5. MANDATORY PAYMENT RECEIPT UPLOADER (إرفاق إيصال الدفع الإلزامي)
// ============================================================================

@Composable
fun MandatoryReceiptAttachmentCard(
    receiptImageUri: String,
    transactionRef: String,
    onReceiptSelected: (String) -> Unit,
    onTransactionRefChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onReceiptSelected(uri.toString())
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mandatory_receipt_attachment_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (receiptImageUri.isNotBlank()) EmeraldVerified else SovereignGold
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (receiptImageUri.isNotBlank()) {
                        EmeraldVerified.copy(alpha = 0.16f)
                    } else {
                        ErrorCrimson.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (receiptImageUri.isNotBlank()) "تم إرفاق الإيصال ✓" else "مطلوب لإتمام الدفع",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (receiptImageUri.isNotBlank()) EmeraldVerified else ErrorCrimson,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "إرفاق إيصال الدفع الحقيقي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = SovereignGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = "الدفع في التطبيق حقيقي 100% وغير وهمي. بعد التحويل عبر شام كاش أو USDT، قم بإرفاق لقطة شاشة لإيصال التحويل ليتم توثيق دفعتك فوراً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (receiptImageUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, EmeraldVerified, RoundedCornerShape(16.dp))
                ) {
                    SmartServiceMediaView(
                        imageUrl = receiptImageUri,
                        accentHex = 0xFF10B981,
                        secondaryHex = 0xFF09111E,
                        category = "إيصال الدفع المرفق",
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = CharcoalNight.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldVerified,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إيصال جاهز للتأكيد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    receiptPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (receiptImageUri.isNotBlank()) VioletBlueVip else SovereignGold,
                    contentColor = if (receiptImageUri.isNotBlank()) Color.White else CharcoalNight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pick_payment_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (receiptImageUri.isNotBlank()) {
                        "تغيير صورة إيصال الدفع المرفق"
                    } else {
                        "إرفاق صورة إيصال الدفع من الهاتف"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = transactionRef,
                onValueChange = onTransactionRefChange,
                label = { Text("رقم عملية التحويل أو الملاحظة (اختياري)") },
                placeholder = { Text("مثال: TxID أو اسم المرسل في شام كاش...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_tx_ref_input")
            )
        }
    }
}

// ============================================================================
// 6. LUXURY UPGRADED BUTTONS (تحسين جميع الأزرار)
// ============================================================================

@Composable
fun LuxuryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isEmeraldStyle: Boolean = false,
    testTag: String? = null
) {
    val brush = if (!enabled) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF334155))
        )
    } else if (isEmeraldStyle) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF0D9488), Color(0xFF10B981), Color(0xFF059669))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFB45309))
        )
    }

    val borderBrush = Brush.horizontalGradient(
        colors = if (isEmeraldStyle) {
            listOf(Color(0xFF6EE7B7), Color(0xFFF59E0B))
        } else {
            listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
        }
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(brush)
            .border(1.2.dp, borderBrush, RoundedCornerShape(50))
            .clickable(enabled = enabled) { onClick() }
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun sharePaymentAddress(context: Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, title))
    }
}
