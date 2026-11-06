package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val WisalInk = Color(0xFF202623)
val WisalPaper = Color(0xFFF7F8F4)
val WisalSage = Color(0xFFDCE5D9)
val WisalMuted = Color(0xFF788078)
private val ReferenceLight = lightColorScheme(
    primary = WisalInk, onPrimary = Color.White,
    primaryContainer = WisalSage, onPrimaryContainer = WisalInk,
    secondary = WisalSage, onSecondary = WisalInk,
    secondaryContainer = WisalSage, onSecondaryContainer = WisalInk,
    background = WisalPaper, onBackground = WisalInk,
    surface = Color.White, onSurface = WisalInk,
    surfaceVariant = WisalSage.copy(alpha = .45f), onSurfaceVariant = WisalMuted,
    outline = Color(0xFFDDE1D9), outlineVariant = Color(0xFFEAEEE7),
    error = Color(0xFF9C5F56)
)
private val ReferenceDark = darkColorScheme(
    primary = WisalSage, onPrimary = WisalInk,
    primaryContainer = Color(0xFF333D36), onPrimaryContainer = WisalSage,
    secondary = Color(0xFFB9C6B6), onSecondary = WisalInk,
    secondaryContainer = Color(0xFF333D36), onSecondaryContainer = WisalSage,
    background = Color(0xFF161C18), onBackground = Color(0xFFF0F3EC),
    surface = Color(0xFF222A24), onSurface = Color(0xFFF0F3EC),
    surfaceVariant = Color(0xFF333D36), onSurfaceVariant = Color(0xFFB5BDB2),
    outline = Color(0xFF485247), outlineVariant = Color(0xFF333D36),
    error = Color(0xFFE2AAA2)
)
val LayaliShapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp))

@Composable
fun MyApplicationTheme(isSoftRoseLightTheme: Boolean = true, darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = if (darkTheme) ReferenceDark else ReferenceLight, typography = Typography, shapes = LayaliShapes, content = content)
    }
}
