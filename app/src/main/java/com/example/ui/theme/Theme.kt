package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LayaliDarkColors = darkColorScheme(
    primary = DeepTealPrimary,
    onPrimary = Color(0xFF0D2423),
    primaryContainer = ElevatedNavySurface,
    onPrimaryContainer = SoftWhiteText,
    secondary = RoyalAmethystVip,
    onSecondary = RoyalNavyBackground,
    secondaryContainer = RoyalNavyCard,
    onSecondaryContainer = SoftWhiteText,
    tertiary = DeepTealBright,
    onTertiary = RoyalNavyBackground,
    background = RoyalNavyBackground,
    onBackground = SoftWhiteText,
    surface = RoyalNavyCard,
    onSurface = SoftWhiteText,
    surfaceVariant = ElevatedNavySurface,
    onSurfaceVariant = SlateSecondaryText,
    outline = DarkBorderGray,
    error = ErrorCrimson
)

private val LayaliLightColors = lightColorScheme(
    primary = Color(0xFF397572),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEDEB),
    onPrimaryContainer = Color(0xFF174643),
    secondary = Color(0xFF5C6F7E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDF1),
    onSecondaryContainer = Color(0xFF263746),
    tertiary = Color(0xFF477D7A),
    onTertiary = Color.White,
    background = SnowGraySurface,
    onBackground = Color(0xFF19232D),
    surface = LightCardSurface,
    onSurface = Color(0xFF19232D),
    surfaceVariant = Color(0xFFEDF1F4),
    onSurfaceVariant = Color(0xFF52616F),
    outline = LightBorderGray,
    error = Color(0xFFB7414A)
)

val LayaliShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun MyApplicationTheme(
    isSoftRoseLightTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (isSoftRoseLightTheme) LayaliLightColors else LayaliDarkColors,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
