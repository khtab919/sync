package com.example.platform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Static vector geometry: no bitmap allocations or perpetual animation in lists.
@Composable
internal fun RoyalAvatar(name: String, frame: String, compact: Boolean = false) {
    val metal = when (frame) { "imperial" -> Color(0xFFD5B46D); "sapphire" -> Color(0xFF8EB9FF); else -> Color(0xFFCED8E8) }
    val gem = when (frame) { "imperial" -> Color(0xFF6B39AE); "sapphire" -> Color(0xFF2459B0); else -> Color(0xFF59677E) }
    val diameter = if (compact) 60.dp else 132.dp
    Box(Modifier.size(diameter).padding(8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center; val r = size.minDimension * .39f
            drawCircle(Brush.radialGradient(listOf(metal.copy(alpha=.18f),Color.Transparent)),r*1.3f,c)
            drawCircle(Color(0xFF060911),r+4,c)
            val sheen = Brush.sweepGradient(listOf(gem,metal,Color.White,metal,gem,metal,Color.White,gem),c)
            drawCircle(sheen,r,c,style=Stroke(r*.12f))
            drawCircle(metal.copy(alpha=.8f),r*.89f,c,style=Stroke(r*.018f))
            drawCircle(Color(0xFF151B29),r*.85f,c)
            if (!compact) {
                for(i in 0 until 32) {
                    val angle=i*Math.PI/16; val p=Offset(c.x+cos(angle).toFloat()*r,c.y+sin(angle).toFloat()*r)
                    drawCircle(if(i%4==0) Color.White else metal,r*(if(i%4==0).027f else .012f),p)
                }
                // Paired laurel leaves, cut from the same metal as the bezel.
                for(side in listOf(-1,1)) for(i in 0..5) {
                    val y=c.y+r*.65f-i*r*.23f; val x=c.x+side*(r*1.03f+(3-i)*r*.018f)
                    val leaf=Path().apply { moveTo(x,y); quadraticTo(x+side*r*.25f,y-r*.22f,x+side*r*.05f,y-r*.31f); quadraticTo(x-side*r*.12f,y-r*.13f,x,y) }
                    drawPath(leaf,sheen)
                }
                val crown=Path().apply {
                    moveTo(c.x-r*.38f,c.y-r*.9f); lineTo(c.x-r*.46f,c.y-r*1.28f)
                    lineTo(c.x-r*.16f,c.y-r*1.10f); lineTo(c.x,c.y-r*1.46f)
                    lineTo(c.x+r*.16f,c.y-r*1.10f); lineTo(c.x+r*.46f,c.y-r*1.28f)
                    lineTo(c.x+r*.38f,c.y-r*.9f); close()
                }
                drawPath(crown,sheen)
                drawCircle(gem,r*.07f,Offset(c.x,c.y-r*1.15f))
            }
        }
        Text(name.take(1).ifBlank { "ل" },fontSize=if(compact)20.sp else 36.sp,color=metal,fontWeight=FontWeight.Bold)
    }
}
