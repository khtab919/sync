package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val WisalInk = Color(0xFF153E53)
val WisalPaper = Color(0xFFF4F8FA)
val WisalSage = Color(0xFFDCECF2)
val WisalMuted = Color(0xFF788792)
val RelaxiaGold = Color(0xFFD7AD72)
private val ReferenceLight = lightColorScheme(
    primary=Color(0xFF277C9D),onPrimary=Color.White,
    primaryContainer=WisalSage,onPrimaryContainer=WisalInk,
    secondary=WisalInk,onSecondary=Color.White,
    secondaryContainer=Color(0xFFE5EEF3),onSecondaryContainer=WisalInk,
    tertiary=RelaxiaGold,onTertiary=WisalInk,
    tertiaryContainer=Color(0xFFF5E9D8),onTertiaryContainer=WisalInk,
    background=WisalPaper,onBackground=Color(0xFF202D36),
    surface=Color.White,onSurface=Color(0xFF202D36),
    surfaceVariant=Color(0xFFE9F0F4),onSurfaceVariant=WisalMuted,
    outline=Color(0xFF788792),outlineVariant=Color(0xFFD7E2E8),
    error=Color(0xFFAD4652),onError=Color.White,
    surfaceContainer=Color.White,surfaceContainerLow=Color(0xFFF0F5F8),surfaceContainerHigh=Color(0xFFE9F0F4)
)
private val ReferenceDark = darkColorScheme(
    primary=Color(0xFF3187A8),onPrimary=Color(0xFF101820),
    primaryContainer=Color(0xFF214C61),onPrimaryContainer=Color(0xFFE4F2F8),
    secondary=Color(0xFFA3B1BA),onSecondary=Color(0xFF101820),
    secondaryContainer=Color(0xFF263E4D),onSecondaryContainer=Color(0xFFF4F7F9),
    tertiary=RelaxiaGold,onTertiary=Color(0xFF101820),
    tertiaryContainer=Color(0xFF493F30),onTertiaryContainer=Color(0xFFF3DEC0),
    background=Color(0xFF101820),onBackground=Color(0xFFF4F7F9),
    surface=Color(0xFF1C2933),onSurface=Color(0xFFF4F7F9),
    surfaceVariant=Color(0xFF273B48),onSurfaceVariant=Color(0xFFA3B1BA),
    outline=Color(0xFF70848F),outlineVariant=Color(0xFF344A58),
    error=Color(0xFFE7A2A8),onError=Color(0xFF40191D),
    surfaceContainer=Color(0xFF1C2933),surfaceContainerLow=Color(0xFF18242D),surfaceContainerHigh=Color(0xFF273B48)
)
val LayaliShapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp))
@Composable
fun MyApplicationTheme(isSoftRoseLightTheme:Boolean=true,darkTheme:Boolean=false,content: @Composable () -> Unit){
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){MaterialTheme(colorScheme=if(darkTheme)ReferenceDark else ReferenceLight,typography=Typography,shapes=LayaliShapes,content=content)}
}
