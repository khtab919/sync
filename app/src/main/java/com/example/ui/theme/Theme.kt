package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val WisalInk = Color(0xFF30242A)
val WisalPaper = Color(0xFFF8F5F4)
val WisalSage = Color(0xFFDFCDD2)
val WisalMuted = Color(0xFF806771)
private val ReferenceLight = lightColorScheme(
    primary = Color(0xFF754451), onPrimary = Color.White,
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
    primaryContainer = Color(0xFF3B3035), onPrimaryContainer = WisalSage,
    secondary = Color(0xFFC5A4AE), onSecondary = WisalInk,
    secondaryContainer = Color(0xFF3B3035), onSecondaryContainer = WisalSage,
    background = Color(0xFF191517), onBackground = Color(0xFFF4EBED),
    surface = Color(0xFF292326), onSurface = Color(0xFFF4EBED),
    surfaceVariant = Color(0xFF3B3035), onSurfaceVariant = Color(0xFFC8B8BE),
    outline = Color(0xFF6B4A5F), outlineVariant = Color(0xFF3B3035),
    error = Color(0xFFE2AAA2)
)
val LayaliShapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp))

@Composable
fun MyApplicationTheme(isSoftRoseLightTheme: Boolean = true, darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = if (darkTheme) ReferenceDark else ReferenceLight, typography = Typography, shapes = LayaliShapes, content = content)
    }
}
