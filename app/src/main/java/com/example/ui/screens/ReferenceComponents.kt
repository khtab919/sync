package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import org.json.JSONObject
import java.util.Locale

internal fun JSONObject.rows(): List<JSONObject> {
    val rows = optJSONArray("items") ?: return emptyList()
    return (0 until rows.length()).map { rows.getJSONObject(it) }
}
internal fun usd(cents: Long): String = String.format(Locale.US, "$%.2f", cents / 100.0)

@Composable
internal fun ReferenceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    if (onClick != null) Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = colors, elevation = elevation, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface), content = content)
    else Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = colors, elevation = elevation, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface), content = content)
}

@Composable
internal fun ReferenceAvatar(name: String, frame: Int = 0, size: Int = 52) {
    val palette = when (frame) {
        1 -> listOf(Color(0xFFE8D8AC), Color(0xFF8A744B), Color(0xFFD8C18F))
        2 -> listOf(Color(0xFFD8E1E3), Color(0xFF69868C), Color(0xFFC8DCDF))
        else -> listOf(Color(0xFFB9C6B6), Color(0xFF59705D))
    }
    Box(Modifier.size(size.dp).background(Brush.linearGradient(palette), CircleShape).padding(if (frame == 0) 2.dp else 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background, CircleShape).padding(3.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(WisalSage, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.trim().take(1).ifBlank { "L" }, color = WisalInk, fontSize = (size / 3).sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (frame > 0) Icon(if (frame == 2) Icons.Outlined.Diamond else Icons.Outlined.WorkspacePremium, null, Modifier.align(Alignment.BottomCenter).offset(y = 9.dp).size(18.dp).background(palette.first(), CircleShape), tint = WisalInk)
    }
}

@Composable
internal fun ReferenceImage(url: String, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Image, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f))
        if (url.startsWith("https://")) AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun ReferenceEmpty(icon: ImageVector, title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 38.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(76.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun ReferenceRow(icon: ImageVector, title: String, subtitle: String = "", onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(23.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ReferencePrimary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)) { Text(text, fontWeight = FontWeight.SemiBold) }
}
