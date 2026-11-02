package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WingedHeartLogo

/** First-run welcome only. Existing returning sessions skip this view. */
@Composable
fun LayaliWelcomeScreen(
    isLightTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    onBrowse: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAYALI",
                    color = colors.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isLightTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = if (isLightTheme) "تفعيل الوضع الليلي" else "تفعيل الوضع النهاري",
                        tint = colors.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(45.dp))
            WingedHeartLogo(modifier = Modifier.size(106.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ليالي",
                color = colors.onBackground,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "عالمك للخدمات والتواصل",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "اكتشف العروض، شاهد الفيديوهات القصيرة، وتواصل مع مقدمي الخدمات في مكان واحد.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(38.dp))

            WelcomeFeature(Icons.Default.PlayCircleOutline, "اكتشف", "فيديوهات قصيرة لاستعراض الخدمات")
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeFeature(Icons.Default.Storefront, "اختر", "عروض واضحة وأسعار قبل الطلب")
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeFeature(Icons.Default.Smartphone, "تواصل", "رسائل وحجوزات داخل التطبيق")

            Spacer(modifier = Modifier.height(35.dp))
            Button(
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                Text("إنشاء حساب", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(11.dp))
            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Text("لدي حساب بالفعل", color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onBrowse, modifier = Modifier.padding(top = 8.dp)) {
                Text("استكشاف كضيف", color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, title: String, description: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(23.dp))
            Column {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
