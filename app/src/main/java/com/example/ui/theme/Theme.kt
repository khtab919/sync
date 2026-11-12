package com.example.ui.theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Relaxia royal-blue identity, designed as one consistent day/night system.
val WisalInk = Color(0xFF173A78)
val WisalPaper = Color(0xFFF4F7FF)
val WisalSage = Color(0xFFDCE8FD)
val WisalMuted = Color(0xFF607492)
val RelaxiaGold = Color(0xFFC5A76B)
private val ReferenceLight = lightColorScheme(
 primary=Color(0xFF194C9F),onPrimary=Color.White,
 primaryContainer=Color(0xFFDCE8FD),onPrimaryContainer=Color(0xFF0D326C),
 secondary=Color(0xFF356DB6),onSecondary=Color.White,
 secondaryContainer=Color(0xFFE5EFFF),onSecondaryContainer=Color(0xFF183E78),
 tertiary=RelaxiaGold,onTertiary=Color(0xFF182945),
 tertiaryContainer=Color(0xFFF5EAD1),onTertiaryContainer=Color(0xFF59461F),
 background=WisalPaper,onBackground=Color(0xFF152947),
 surface=Color.White,onSurface=Color(0xFF152947),
 surfaceVariant=Color(0xFFE9F0FC),onSurfaceVariant=WisalMuted,
 outline=Color(0xFF7587A2),outlineVariant=Color(0xFFDAE3F2),
 error=Color(0xFFAE3E50),onError=Color.White,
 surfaceContainer=Color.White,surfaceContainerLow=Color(0xFFF9FBFF),surfaceContainerHigh=Color(0xFFE6EEFC)
)
private val ReferenceDark = darkColorScheme(
 primary=Color(0xFF9CC3FF),onPrimary=Color(0xFF0A224B),
 primaryContainer=Color(0xFF1A488F),onPrimaryContainer=Color(0xFFE6EFFF),
 secondary=Color(0xFFB1C9ED),onSecondary=Color(0xFF0A214B),
 secondaryContainer=Color(0xFF233D64),onSecondaryContainer=Color(0xFFE8F0FF),
 tertiary=RelaxiaGold,onTertiary=Color(0xFF1B2740),
 tertiaryContainer=Color(0xFF51452D),onTertiaryContainer=Color(0xFFF6E5BD),
 background=Color(0xFF080F23),onBackground=Color(0xFFF0F5FF),
 surface=Color(0xFF101F3D),onSurface=Color(0xFFF0F5FF),
 surfaceVariant=Color(0xFF1D3457),onSurfaceVariant=Color(0xFFB4C6E5),
 outline=Color(0xFF8298BC),outlineVariant=Color(0xFF324A6E),
 error=Color(0xFFF2A0AA),onError=Color(0xFF521C27),
 surfaceContainer=Color(0xFF132541),surfaceContainerLow=Color(0xFF0C1931),surfaceContainerHigh=Color(0xFF223C64)
)
val LayaliShapes=Shapes(extraSmall=RoundedCornerShape(16.dp),small=RoundedCornerShape(18.dp),medium=RoundedCornerShape(22.dp),large=RoundedCornerShape(26.dp),extraLarge=RoundedCornerShape(32.dp))
@Composable
fun MyApplicationTheme(isSoftRoseLightTheme:Boolean=true,darkTheme:Boolean=false,content: @Composable () -> Unit){
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){MaterialTheme(colorScheme=if(darkTheme)ReferenceDark else ReferenceLight,typography=Typography,shapes=LayaliShapes,content=content)}
}
