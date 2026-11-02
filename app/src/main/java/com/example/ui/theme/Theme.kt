package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LuxuryNightlifeColorScheme = darkColorScheme(
    primary = DeepTealPrimary,              // #0D9488 - الأزرار والتفاعل
    onPrimary = RoyalNavyBackground,              // #F8FAFC - النص الأساسي
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

val LayaliShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    isSoftRoseLightTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = LuxuryNightlifeColorScheme,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
