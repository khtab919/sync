package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LuxuryNightlifeColorScheme = darkColorScheme(
    primary = Color(0xFFB4C7DB),              // #0D9488 - الأزرار والتفاعل
    onPrimary = Color(0xFF16263A),              // #F8FAFC - النص الأساسي
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
    outlineVariant = Color(0xFF2B3B4E),
    outline = DarkBorderGray,               // #1E2E48 - حدود هادئة غير متكررة
    error = ErrorCrimson
)

private val CalmDayColorScheme = lightColorScheme(
    primary = Color(0xFF203A57), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EAF0), onPrimaryContainer = Color(0xFF1D3046),
    secondary = Color(0xFF8A6E43), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E9DE), onSecondaryContainer = Color(0xFF51422C),
    tertiary = Color(0xFF526D87),
    background = Color(0xFFF3F3F6), onBackground = Color(0xFF1B2636),
    surface = Color(0xFFFCFCFD), onSurface = Color(0xFF1B2636),
    surfaceVariant = Color(0xFFE9ECF1), onSurfaceVariant = Color(0xFF586474),
    outline = Color(0xFFABB3BF), outlineVariant = Color(0xFFDFE3E9),
    error = Color(0xFFA3424D)
)

val LayaliShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun MyApplicationTheme(
    isSoftRoseLightTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (isSoftRoseLightTheme) CalmDayColorScheme else LuxuryNightlifeColorScheme,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
