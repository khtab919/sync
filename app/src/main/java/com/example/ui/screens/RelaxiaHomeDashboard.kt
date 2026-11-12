package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/**
 * Compact, fully navigable home hub. Every destination uses the existing
 * app route callbacks; balance and identity are never mocked.
 */
@Composable
internal fun RelaxiaHomeDashboard(
    profile: JSONObject,
    balanceCents: Long?,
    query: String,
    onSearch: (String) -> Unit,
    onProfile: () -> Unit,
    onWallet: () -> Unit,
    onLive: () -> Unit,
    onAudio: () -> Unit,
    onReels: () -> Unit,
    onStore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // One unified header instead of two disconnected floating boxes.
        Box(
            modifier = Modifier.fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF43283B), Color(0xFF271C2A), Color(0xFF52283C))
                    )
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(17.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        ReferenceAvatar(
                            profile.optString("display_name"),
                            profile.optInt("selected_frame"),
                            44,
                            profile.optString("avatar_url")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "مرحبًا بك",
                                color = Color(0xFFE5C9D2),
                                fontSize = 11.sp
                            )
                            Text(
                                profile.optString("display_name").ifBlank { "حسابي" },
                                color = Color(0xFFFFF9F5),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Surface(
                    onClick = onWallet,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = .10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "المحفظة",
                            modifier = Modifier.size(19.dp),
                            tint = Color(0xFFE8C38D)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("الرصيد", color = Color(0xFFE5C9D2), fontSize = 10.sp)
                            Text(
                                if (balanceCents == null) "—" else usd(balanceCents),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            placeholder = {
                Text("ابحث عن حساب أو ريلز أو خدمة", fontSize = 13.sp, maxLines = 1)
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(22.dp))
            },
            trailingIcon = {
                if (query.isNotBlank()) IconButton(onClick = { onSearch("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "مسح البحث")
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(57.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeFeatureCard(
                title = "البث المباشر",
                subtitle = "شاهد وتفاعل",
                icon = Icons.Rounded.Sensors,
                modifier = Modifier.weight(1f),
                onClick = onLive
            )
            HomeFeatureCard(
                title = "الغرف الصوتية",
                subtitle = "مساحات صوتية",
                icon = Icons.Rounded.Mic,
                modifier = Modifier.weight(1f),
                onClick = onAudio
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickDestination("الريلز", Icons.Rounded.SmartDisplay, Modifier.weight(1f), onReels)
            HomeQuickDestination("المتجر والحجز", Icons.Rounded.Storefront, Modifier.weight(1f), onStore)
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    ReferenceCard(modifier = modifier.height(111.dp), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(35.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HomeQuickDestination(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(57.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .60f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun RelaxiaProfileShortcut(
    title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit
) {
    ReferenceCard(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, fontSize = 11.sp)
        }
    }
}
