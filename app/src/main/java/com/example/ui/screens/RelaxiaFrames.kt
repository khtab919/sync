package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class RelaxiaFrame(val id: Int, val name: String, val detail: String, val accent: Color, val gold: Color)
internal val relaxiaFrames = listOf(
    RelaxiaFrame(0, "الناعم", "حلقة بترولية بسيطة", Color(0xFF3187A8), Color(0xFFDCECF2)),
    RelaxiaFrame(1, "وردة الياقوت", "ورود بارزة ولمعة ذهبية", Color(0xFF3187A8), Color(0xFFD9AD75)),
    RelaxiaFrame(2, "عقد اللؤلؤ", "حبات لؤلؤ وشريط حريري", Color(0xFF3187A8), Color(0xFFE8D1B2)),
    RelaxiaFrame(3, "نجمة الكريستال", "أطراف نجمية وأحجار لامعة", Color(0xFF3187A8), Color(0xFFDCECF2)),
    RelaxiaFrame(4, "تاج الورد", "تاج علوي وزهور مزدوجة", Color(0xFFDCECF2), Color(0xFFD7B06F)),
    RelaxiaFrame(5, "الفراشة", "جناحان ناعمان وحبات مضيئة", Color(0xFFDCECF2), Color(0xFFDFBB83)),
    RelaxiaFrame(6, "الأجنحة", "ريش فضي بعمق وردي", Color(0xFF3187A8), Color(0xFFD5CED8)),
    RelaxiaFrame(7, "قلادة الزهر", "قطرات متدلية وتفاصيل زهرية", Color(0xFF3187A8), Color(0xFFDAB77F)),
    RelaxiaFrame(8, "هالة الألماس", "أجنحة وأطراف كريستالية", Color(0xFF3187A8), Color(0xFFDCECF2)),
    RelaxiaFrame(9, "حديقة اللؤلؤ", "بتلات ولآلئ متشابكة", Color(0xFF3187A8), Color(0xFFD5B080)),
    RelaxiaFrame(10, "التاج الملكي", "تاج مجسّم وجناحان ذهبيان", Color(0xFF3187A8), Color(0xFFE6BD71))
)

@Composable
internal fun RelaxiaAvatar(name: String, frame: Int, size: Int, imageUrl: String = "") {
    val style = relaxiaFrames[frame.coerceIn(0, 10)]
    val padding = if (frame == 0) (size * .075f).dp else (size * .205f).dp
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(padding).shadow(5.dp, CircleShape, clip = false)
            .background(Brush.radialGradient(listOf(Color.White, Color(0xFFDCECF2), style.accent.copy(alpha = .35f))), CircleShape)
            .padding(3.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            Text(name.trim().take(1).ifBlank { "R" }, color = Color(0xFF153E53), fontSize = (size * .29f).sp, fontWeight = FontWeight.SemiBold)
            if (imageUrl.startsWith("https://")) AsyncImage(imageUrl, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Canvas(Modifier.fillMaxSize()) { drawRelaxiaFrame(style, size >= 70) }
    }
}

private fun DrawScope.drawRelaxiaFrame(frame: RelaxiaFrame, detailed: Boolean) {
    val w = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val r = w * if (frame.id == 0) .45f else .326f
    val depth = (w * .025f).coerceAtLeast(1f)
    drawCircle(Color(0xFF153E53).copy(alpha = .20f), r + depth * 1.8f, c + Offset(0f, depth * 1.7f), style = Stroke(depth * 1.5f))
    drawCircle(Brush.linearGradient(listOf(Color.White, frame.gold, frame.accent, Color(0xFF277C9D), frame.gold, Color.White)), r, c, style = Stroke(depth * 2f))
    drawCircle(Color.White.copy(alpha = .9f), r - depth, c, style = Stroke((w * .007f).coerceAtLeast(.6f)))
    drawCircle(frame.accent.copy(alpha = .7f), r + depth, c, style = Stroke((w * .006f).coerceAtLeast(.5f)))
    if (frame.id == 0) return
    val count = if (detailed) 20 else 10
    for (i in 0 until count) {
        val angle = 2 * PI * i / count
        val p = c + Offset(cos(angle).toFloat() * (r + depth * 1.4f), sin(angle).toFloat() * (r + depth * 1.4f))
        when (frame.id) {
            2 -> pearl(p, w * .028f, frame.gold)
            3 -> diamond(p, w * .046f, w * .063f, frame.accent, angle.toFloat() - PI.toFloat() / 2)
            8 -> diamond(p, w * .028f, w * .045f, frame.accent, angle.toFloat() - PI.toFloat() / 2)
            9 -> if (i % 2 == 0) flower(p, w * .049f, frame.accent, frame.gold) else diamond(p, w * .018f, w * .035f, frame.gold, angle.toFloat())
            1, 4, 7 -> if (i % 3 == 0) flower(p, w * .045f, frame.accent, frame.gold) else pearl(p, w * .014f, frame.gold)
            else -> pearl(p, w * .017f, if (i % 2 == 0) frame.accent else frame.gold)
        }
    }
    if (frame.id in listOf(6, 8, 10)) {
        for (side in listOf(-1f, 1f)) {
            for (i in 0..5) {
                val y = c.y + r * .65f - i * r * .25f
                val x = c.x + side * (r + w * .025f)
                val feather = Path().apply {
                    moveTo(x, y)
                    cubicTo(x + side * w * .14f, y - w * .01f, x + side * w * .16f, y - w * .14f, x + side * w * (.11f + i * .008f), y - w * .15f)
                    cubicTo(x + side * w * .065f, y - w * .08f, x, y - w * .08f, x, y)
                    close()
                }
                drawPath(feather, Brush.linearGradient(listOf(Color.White, frame.gold, frame.accent, Color(0xFF277C9D))))
                drawPath(feather, Color.White.copy(alpha = .8f), style = Stroke(w * .004f))
            }
        }
    }
    if (frame.id == 5) {
        val p = Offset(c.x, c.y - r - w * .03f)
        for (side in listOf(-1f, 1f)) {
            val wing = Path().apply {
                moveTo(p.x, p.y + w * .04f)
                cubicTo(p.x + side * w * .23f, p.y - w * .19f, p.x + side * w * .24f, p.y + w * .10f, p.x, p.y + w * .04f)
                cubicTo(p.x + side * w * .17f, p.y + w * .17f, p.x + side * w * .12f, p.y - w * .03f, p.x, p.y + w * .04f)
                close()
            }
            drawPath(wing, Brush.linearGradient(listOf(Color.White, frame.accent, frame.gold, frame.accent)))
            drawPath(wing, frame.gold, style = Stroke(w * .008f))
        }
        pearl(p + Offset(0f, w * .03f), w * .022f, frame.gold)
    }
    if (frame.id in listOf(4, 10)) {
        val y = c.y - r - w * .01f
        val crown = Path().apply {
            moveTo(c.x - w * .11f, y)
            lineTo(c.x - w * .15f, y - w * .13f)
            lineTo(c.x - w * .055f, y - w * .065f)
            lineTo(c.x, y - w * .15f)
            lineTo(c.x + w * .055f, y - w * .065f)
            lineTo(c.x + w * .15f, y - w * .13f)
            lineTo(c.x + w * .11f, y)
            close()
        }
        drawPath(crown, Brush.verticalGradient(listOf(Color.White, frame.gold, frame.accent, Color(0xFF82502C))))
        drawPath(crown, Color.White.copy(alpha = .7f), style = Stroke(w * .008f))
        diamond(Offset(c.x, y - w * .07f), w * .023f, w * .035f, frame.accent)
    }
    if (frame.id == 7 || frame.id == 9) {
        for (side in listOf(-1f, 1f)) {
            val p = c + Offset(side * r * .78f, r * .8f)
            drawLine(frame.gold, p, p + Offset(0f, w * .12f), strokeWidth = w * .009f)
            diamond(p + Offset(0f, w * .135f), w * .025f, w * .05f, frame.accent)
        }
    }
    // Satin base with bevels, sized to remain readable on small avatars.
    val base = Offset(c.x - w * .16f, c.y + r - w * .022f)
    drawRoundRect(Brush.verticalGradient(listOf(frame.gold, Color(0xFF277C9D), frame.accent)), base, Size(w * .32f, w * .076f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .025f))
    drawLine(Color.White.copy(alpha = .8f), base + Offset(w * .025f, w * .008f), base + Offset(w * .29f, w * .008f), w * .005f)
    if (detailed) {
        val sparkle = c + Offset(-r * .68f, -r * .7f)
        drawLine(Color.White, sparkle - Offset(w * .025f, 0f), sparkle + Offset(w * .025f, 0f), w * .007f)
        drawLine(Color.White, sparkle - Offset(0f, w * .025f), sparkle + Offset(0f, w * .025f), w * .007f)
    }
}

private fun DrawScope.pearl(p: Offset, radius: Float, color: Color) {
    drawCircle(Color(0xFF153E53).copy(alpha = .28f), radius * 1.05f, p + Offset(0f, radius * .3f))
    drawCircle(Brush.radialGradient(listOf(Color.White, color, Color(0xFF277C9D)), p - Offset(radius * .3f, radius * .4f), radius * 1.9f), radius, p)
    drawCircle(Color.White.copy(alpha = .9f), radius * .2f, p - Offset(radius * .32f, radius * .35f))
}
private fun DrawScope.diamond(p: Offset, rx: Float, ry: Float, color: Color, angle: Float = 0f) {
    if (angle != 0f) {
        rotate(angle * 180f / PI.toFloat(), p) { diamond(p, rx, ry, color) }
        return
    }
    val path = Path().apply {
        moveTo(p.x, p.y - ry); lineTo(p.x + rx, p.y); lineTo(p.x, p.y + ry); lineTo(p.x - rx, p.y); close()
    }
    drawPath(path, Brush.linearGradient(listOf(Color.White, color, Color(0xFF277C9D), color), p - Offset(rx, ry), p + Offset(rx, ry)))
    drawPath(path, Color.White.copy(alpha = .65f), style = Stroke((rx * .13f).coerceAtLeast(.4f)))
    drawLine(Color.White.copy(alpha = .8f), p - Offset(rx * .5f, 0f), p - Offset(0f, ry * .5f), strokeWidth = (rx * .14f).coerceAtLeast(.5f))
}
private fun DrawScope.flower(p: Offset, radius: Float, color: Color, gold: Color) {
    for (i in 0..4) {
        val a = i * PI * 2 / 5
        pearl(p + Offset(cos(a).toFloat() * radius * .53f, sin(a).toFloat() * radius * .53f), radius * .51f, color)
    }
    pearl(p, radius * .31f, gold)
}

@Composable
internal fun RelaxiaFramePicker(current: Int, name: String, imageUrl: String, onChoose: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("إطارات بعمق ولمعة", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
        Text("عشرة تصاميم؛ اختر إطارك ليظهر على ملفك وفي المحادثات.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        ReferenceRow(androidx.compose.material.icons.Icons.Outlined.RadioButtonUnchecked, "الإطار الناعم", if (current == 0) "إطارك الحالي" else "العودة إلى الحلقة البسيطة") { onChoose(0) }
        for (pair in relaxiaFrames.drop(1).chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (frame in pair) {
                    Box(Modifier.weight(1f)) {
                        ReferenceCard(onClick = { onChoose(frame.id) }) {
                            Column(Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 220.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                RelaxiaAvatar(name, frame.id, 128, imageUrl)
                                Text(frame.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                Text(if (current == frame.id) "إطارك الحالي ✓" else frame.detail, fontSize = 11.sp, color = if (current == frame.id) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
