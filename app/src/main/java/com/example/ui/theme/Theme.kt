package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LuxuryNightlifeColorScheme = darkColorScheme(
    primary = DeepTealPrimary,              // #0D9488 - الأزرار والتفاعل
    onPrimary = SoftWhiteText,              // #F8FAFC - النص الأساسي
    primaryContainer = RoyalNavyCard,       // #111D30 - البطاقات
    onPrimaryContainer = SoftWhiteText,
    secondary = MutedLuxuryGold,            // #D6AD62 - الذهبي للفخامة والعضويات فقط
    onSecondary = RoyalNavyBackground,
    secondaryContainer = ElevatedNavySurface,
    onSecondaryContainer = SoftWhiteText,
    tertiary = DeepTealBright,
    onTertiary = RoyalNavyBackground,
    background = RoyalNavyBackground,       // #09111E - الخلفية الرئيسية
    onBackground = SoftWhiteText,           // #F8FAFC - النص الأساسي
    surface = RoyalNavyCard,                // #111D30 - البطاقات
    onSurface = SoftWhiteText,              // #F8FAFC - النص الأساسي
    surfaceVariant = ElevatedNavySurface,   // #17263F - الأسطح الفرعية
    onSurfaceVariant = SlateSecondaryText,  // #94A3B8 - النص الثانوي
    outline = DarkBorderGray,               // #1E2E48 - حدود هادئة غير متكررة
    error = ErrorCrimson
)

// Restrained daylight palette, with independent surface and content contrast.
private val LayaliDaylightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF126878),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD9EDF0),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF103E48),
    secondary = androidx.compose.ui.graphics.Color(0xFF536A79),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE4EBF0),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF243B49),
    tertiary = androidx.compose.ui.graphics.Color(0xFF336B82),
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFFF5F7F8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF17232D),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF17232D),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEDF1F3),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF53616B),
    outline = androidx.compose.ui.graphics.Color(0xFFD7DEE3),
    error = androidx.compose.ui.graphics.Color(0xFFB42336)
)

val LayaliShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun MyApplicationTheme(
    isSoftRoseLightTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (isSoftRoseLightTheme) LayaliDaylightColorScheme else LuxuryNightlifeColorScheme,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
