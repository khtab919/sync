package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val WisalInk = Color(0xFF61263B)
val WisalPaper = Color(0xFFFAF6F2)
val WisalSage = Color(0xFFF0DEE3)
val WisalMuted = Color(0xFF77656D)
val RelaxiaGold = Color(0xFFD7AD72)
private val ReferenceLight = lightColorScheme(
    primary=Color(0xFF792D45),onPrimary=Color.White,
    primaryContainer=WisalSage,onPrimaryContainer=WisalInk,
    secondary=WisalInk,onSecondary=Color.White,
    secondaryContainer=Color(0xFFF3E7E8),onSecondaryContainer=WisalInk,
    tertiary=RelaxiaGold,onTertiary=WisalInk,
    tertiaryContainer=Color(0xFFF5E9D8),onTertiaryContainer=WisalInk,
    background=WisalPaper,onBackground=Color(0xFF30232A),
    surface=Color.White,onSurface=Color(0xFF30232A),
    surfaceVariant=Color(0xFFF2E7E8),onSurfaceVariant=WisalMuted,
    outline=Color(0xFF806C75),outlineVariant=Color(0xFFE4D5D9),
    error=Color(0xFFAD4652),onError=Color.White,
    surfaceContainer=Color.White,surfaceContainerLow=Color(0xFFFBF6F2),surfaceContainerHigh=Color(0xFFF2E9E7)
)
private val ReferenceDark = darkColorScheme(
    primary=Color(0xFFE9A8BA),onPrimary=Color(0xFF1B1418),
    primaryContainer=Color(0xFF542938),onPrimaryContainer=Color(0xFFFFE6EE),
    secondary=Color(0xFFC4AEB7),onSecondary=Color(0xFF1B1418),
    secondaryContainer=Color(0xFF3D2C35),onSecondaryContainer=Color(0xFFFFF9F3),
    tertiary=RelaxiaGold,onTertiary=Color(0xFF1B1418),
    tertiaryContainer=Color(0xFF493F30),onTertiaryContainer=Color(0xFFF3DEC0),
    background=Color(0xFF1B1418),onBackground=Color(0xFFFFF9F3),
    surface=Color(0xFF281E24),onSurface=Color(0xFFFFF9F3),
    surfaceVariant=Color(0xFF3C2C35),onSurfaceVariant=Color(0xFFC4AEB7),
    outline=Color(0xFFAD909D),outlineVariant=Color(0xFF503D46),
    error=Color(0xFFE7A2A8),onError=Color(0xFF40191D),
    surfaceContainer=Color(0xFF281E24),surfaceContainerLow=Color(0xFF23191F),surfaceContainerHigh=Color(0xFF3C2C35)
)
val LayaliShapes=Shapes(extraSmall=RoundedCornerShape(16.dp),small=RoundedCornerShape(18.dp),medium=RoundedCornerShape(22.dp),large=RoundedCornerShape(26.dp),extraLarge=RoundedCornerShape(32.dp))
@Composable
fun MyApplicationTheme(isSoftRoseLightTheme:Boolean=true,darkTheme:Boolean=false,content: @Composable () -> Unit){
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){MaterialTheme(colorScheme=if(darkTheme)ReferenceDark else ReferenceLight,typography=Typography,shapes=LayaliShapes,content=content)}
}
