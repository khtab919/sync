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

private val DarkRoyalBrandScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianNight,
    primaryContainer = ElevatedPlumCard,
    onPrimaryContainer = SoftGold,
    secondary = DustyRose,
    onSecondary = ObsidianNight,
    secondaryContainer = DeepRoyalViolet,
    onSecondaryContainer = IvoryCream,
    tertiary = EmeraldVerified,
    onTertiary = ObsidianNight,
    background = ObsidianNight,
    onBackground = IvoryCream,
    surface = VelvetPlum,
    onSurface = IvoryCream,
    surfaceVariant = ElevatedPlumCard,
    onSurfaceVariant = MutedLavender,
    outline = DeepWineOutline,
    error = ErrorCrimson
)

private val LightSoftRoseBrandScheme = lightColorScheme(
    primary = DustyRose,
    onPrimary = Color.White,
    primaryContainer = SoftRoseCard,
    onPrimaryContainer = DeepPlumText,
    secondary = DeepRoyalViolet,
    onSecondary = Color.White,
    secondaryContainer = SoftRoseSurface,
    onSecondaryContainer = DeepPlumText,
    tertiary = EmeraldVerified,
    onTertiary = Color.White,
    background = SoftBlushBackground,
    onBackground = DeepPlumText,
    surface = SoftRoseSurface,
    onSurface = DeepPlumText,
    surfaceVariant = SoftRoseCard,
    onSurfaceVariant = MutedPlumText,
    outline = SoftRoseOutline,
    error = ErrorCrimson
)

val LayaliShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    isSoftRoseLightTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (isSoftRoseLightTheme) LightSoftRoseBrandScheme else DarkRoyalBrandScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = LayaliShapes,
            content = content
        )
    }
}
