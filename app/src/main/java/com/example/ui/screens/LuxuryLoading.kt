package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
internal fun LuxuryLoadingStatus(label: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LuxurySkeletonCards() {
    val motion = rememberInfiniteTransition(label = "loading-shimmer")
    val light by motion.animateFloat(.30f, .68f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "shimmer-opacity")
    val base = MaterialTheme.colorScheme.primaryContainer
    val shimmer = Brush.horizontalGradient(listOf(base.copy(alpha = light), MaterialTheme.colorScheme.surfaceVariant, base.copy(alpha = light)))
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) {
            ReferenceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(20.dp)).background(shimmer))
                    Box(Modifier.fillMaxWidth(.7f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(shimmer))
                    Box(Modifier.fillMaxWidth(.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
                }
            }
        }
    }
}
