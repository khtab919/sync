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

private val DarkCreatorVipScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = CharcoalNight,
    primaryContainer = DarkCardSurface,
    onPrimaryContainer = Color.White,
    secondary = VioletBlueVip,
    onSecondary = Color.White,
    secondaryContainer = ElevatedDarkSurface,
    onSecondaryContainer = Color.White,
    tertiary = ShamCashTeal,
    onTertiary = Color.White,
    background = CharcoalNight,
    onBackground = Color.White,
    surface = DarkCardSurface,
    onSurface = Color.White,
    surfaceVariant = ElevatedDarkSurface,
    onSurfaceVariant = MutedLightText,
    outline = DarkBorderGray,
    error = ErrorCrimson
)

private val LightCreatorVipScheme = lightColorScheme(
    primary = CoralPinkPrimary,
    onPrimary = Color.White,
    primaryContainer = SnowGraySurface,
    onPrimaryContainer = TitleDarkText,
    secondary = VioletBlueVip,
    onSecondary = Color.White,
    secondaryContainer = SnowGraySurface,
    onSecondaryContainer = TitleDarkText,
    tertiary = CharcoalButton,
    onTertiary = Color.White,
    background = PureWhite,
    onBackground = TitleDarkText,
    surface = LightCardSurface,
    onSurface = TitleDarkText,
    surfaceVariant = SnowGraySurface,
    onSurfaceVariant = SubtitleGrayText,
    outline = LightBorderGray,
    error = ErrorCrimson
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
    val scheme = if (isSoftRoseLightTheme) LightCreatorVipScheme else DarkCreatorVipScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
