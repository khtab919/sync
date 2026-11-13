package com.example.ui.screens

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R

internal val milestoneNames = listOf("الفضي", "البرونزي", "التاج الفضي", "التاج الذهبي", "البلاتيني", "الماسي", "النخبة", "التنين", "الملكي", "القمة")
// Measured opening center and radius in each atlas cell, independent of its ornaments.
internal data class FrameOpening(val x: Float, val y: Float, val radius: Float)
internal val frameOpenings = listOf(
    FrameOpening(.5488f, .4829f, .3149f), FrameOpening(.5341f, .4888f, .3054f),
    FrameOpening(.4996f, .5180f, .2887f), FrameOpening(.4531f, .4960f, .2789f),
    FrameOpening(.4393f, .5042f, .2920f), FrameOpening(.5358f, .4702f, .2760f),
    FrameOpening(.5284f, .4652f, .2690f), FrameOpening(.4773f, .4833f, .2772f),
    FrameOpening(.4539f, .4785f, .2580f), FrameOpening(.4342f, .4766f, .2601f)
)
// Tight per-cell ornament bounds exclude fragments of neighbouring atlas frames.
private val frameBounds = listOf(
    floatArrayOf(.1212f,.0455f,.9798f,.9141f), floatArrayOf(.0907f,.0455f,.9824f,.8965f),
    floatArrayOf(.0657f,.0101f,.9343f,.9066f), floatArrayOf(0f,.0051f,.9144f,.9066f),
    floatArrayOf(0f,.0303f,.8967f,.9217f), floatArrayOf(.0429f,0f,1f,.9194f),
    floatArrayOf(.0882f,.0025f,.9798f,.9144f), floatArrayOf(.0505f,0f,.9141f,.9169f),
    floatArrayOf(0f,0f,.9169f,.9395f), floatArrayOf(0f,0f,.8892f,.9320f)
)
private object FrameAtlas { var image: ImageBitmap? = null }

@Composable
internal fun PlainAvatar(name: String, size: Int, imageUrl: String, modifier: Modifier = Modifier) {
    Box(modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).ifBlank { "R" }, color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = (size * .32f).sp, fontWeight = FontWeight.Medium)
        if (imageUrl.startsWith("https://")) AsyncImage(imageUrl, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun MilestoneAvatar(name: String, frame: Int, size: Int, imageUrl: String) {
    val context = LocalContext.current
    val index = ((frame - 110) / 10).coerceIn(0, 9)
    val atlas = remember { synchronized(FrameAtlas) {
        FrameAtlas.image ?: BitmapFactory.decodeResource(context.resources, R.drawable.relaxia_frame_atlas).asImageBitmap().also { FrameAtlas.image = it }
    } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    val motionAllowed = remember(context, visible) { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    var shimmer = .4f
    if (index >= 4 && visible && motionAllowed && size >= 48) {
        val transition = rememberInfiniteTransition(label = "frame-light")
        val value by transition.animateFloat(.15f, .9f, infiniteRepeatable(tween(2400 - index * 100, easing = LinearEasing), RepeatMode.Reverse), label = "gem-light")
        shimmer = value
    }
    val opening = frameOpenings[index]
    // Normalize the opening, not the ornament bounds: every face has the same
    // center and diameter. A small overlap sits beneath the inner metal rim.
    val frameScale = .23f / opening.radius
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        PlainAvatar(name, (size * .49f).toInt(), imageUrl,
            Modifier.offset(y = (size * .03f).dp))
        Canvas(Modifier.fillMaxSize()) {
            val x = atlas.width * (index % 5) / 5
            val y = atlas.height * (index / 5) / 2
            val right = atlas.width * (index % 5 + 1) / 5
            val bottom = atlas.height * (index / 5 + 1) / 2
            val w = this.size.width
            val left = (.5f - opening.x * frameScale) * w
            val top = (.53f - opening.y * frameScale) * w
            val extent = (w * frameScale).toInt()
            val bounds = frameBounds[index]
            clipRect(left + bounds[0]*extent, top + bounds[1]*extent,
                left + bounds[2]*extent, top + bounds[3]*extent) {
                drawImage(atlas, srcOffset = IntOffset(x, y), srcSize = IntSize(right - x, bottom - y),
                    dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(extent, extent))
            }
            if (index >= 4 && visible && motionAllowed && size >= 48) {
                val w = this.size.width
                for (point in listOf(Offset(.5f,.13f), Offset(.17f,.6f), Offset(.83f,.62f)).take(if(index >= 7)3 else 1)) {
                    val p = Offset(left + point.x*w*frameScale, top + point.y*w*frameScale)
                    drawCircle(Color.White.copy(alpha=shimmer*.7f), w*.013f, p)
                    drawLine(Color.White.copy(alpha=shimmer), p-Offset(w*.025f,0f), p+Offset(w*.025f,0f), w*.006f)
                    drawLine(Color.White.copy(alpha=shimmer), p-Offset(0f,w*.025f), p+Offset(0f,w*.025f), w*.006f)
                }
            }
        }
    }
}

@Composable
internal fun RelaxiaLevelBadge(level: Int) {
    val tierColor = when(level.coerceIn(1,100)) {
        in 1..10 -> Color(0xFF30A66B)
        in 11..25 -> Color(0xFF3187DA)
        in 26..40 -> Color(0xFF9666D9)
        in 41..60 -> Color(0xFFD3A64F)
        in 61..80 -> Color(0xFFB8CDD9)
        else -> Color(0xFFE1A5D0)
    }
    Surface(
        color=tierColor.copy(alpha=.16f),
        shape=RoundedCornerShape(18.dp),
        border=BorderStroke(1.dp,tierColor.copy(alpha=.55f))
    ) {
        Row(
            Modifier.padding(horizontal=11.dp,vertical=6.dp),
            horizontalArrangement=Arrangement.spacedBy(7.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(tierColor,CircleShape))
            Text("LV ${level.coerceIn(1,100)}",color=tierColor,
                style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
        }
    }
}
