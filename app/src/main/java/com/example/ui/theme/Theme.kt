package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val WisalInk = Color(0xFF301827)
val WisalPaper = Color(0xFFFFF5F8)
val WisalSage = Color(0xFFF3D9E5)
val WisalMuted = Color(0xFF957786)
private val ReferenceLight = lightColorScheme(
    primary = Color(0xFFA84D76), onPrimary = Color.White,
    primaryContainer = WisalSage, onPrimaryContainer = WisalInk,
    secondary = WisalSage, onSecondary = WisalInk,
    secondaryContainer = WisalSage, onSecondaryContainer = WisalInk,
    background = WisalPaper, onBackground = WisalInk,
    surface = Color.White, onSurface = WisalInk,
    surfaceVariant = WisalSage.copy(alpha = .45f), onSurfaceVariant = WisalMuted,
    outline = Color(0xFFEAD6DF), outlineVariant = Color(0xFFF2E3EA),
    error = Color(0xFFAC3D64)
)
private val ReferenceDark = darkColorScheme(
    primary = WisalSage, onPrimary = WisalInk,
    primaryContainer = Color(0xFF482A3D), onPrimaryContainer = WisalSage,
    secondary = Color(0xFFE7ABCA), onSecondary = WisalInk,
    secondaryContainer = Color(0xFF482A3D), onSecondaryContainer = WisalSage,
    background = Color(0xFF20141D), onBackground = Color(0xFFFCEEF5),
    surface = Color(0xFF30202B), onSurface = Color(0xFFFCEEF5),
    surfaceVariant = Color(0xFF482A3D), onSurfaceVariant = Color(0xFFCDB0C0),
    outline = Color(0xFF6B4A5F), outlineVariant = Color(0xFF482A3D),
    error = Color(0xFFE2AAA2)
)
val LayaliShapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp))

@Composable
fun MyApplicationTheme(isSoftRoseLightTheme: Boolean = true, darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = if (darkTheme) ReferenceDark else ReferenceLight, typography = Typography, shapes = LayaliShapes, content = content)
    }
}
