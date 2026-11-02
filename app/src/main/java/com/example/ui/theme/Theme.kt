package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
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

private val LayaliDaylightColorScheme = lightColorScheme(
    primary = Color(0xFF176B77),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFF1),
    onPrimaryContainer = Color(0xFF173C45),
    secondary = Color(0xFF685591),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE6F6),
    onSecondaryContainer = Color(0xFF302844),
    tertiary = Color(0xFF46776F),
    onTertiary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF192330),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF192330),
    surfaceVariant = Color(0xFFEDF0F4),
    onSurfaceVariant = Color(0xFF526171),
    outline = Color(0xFFD2D9E2),
    error = Color(0xFFB3263E)
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
