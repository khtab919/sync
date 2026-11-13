package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/** The real production home hub. All actions invoke existing server-backed routes. */
@Composable
internal fun RelaxiaHomeDashboard(
    profile: JSONObject,
    balanceCents: Long?,
    query: String,
    onSearch: (String) -> Unit,
    onProfile: () -> Unit,
    onWallet: () -> Unit,
    onLive: () -> Unit,
    onAudio: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onProfile,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReferenceAvatar(
                        profile.optString("display_name"),
                        profile.optInt("selected_frame"),
                        40,
                        profile.optString("avatar_url")
                    )
                    Column {
                        Text("أهلًا بك", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            profile.optString("display_name").ifBlank { "حسابي" },
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(onClick = onWallet, shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Rounded.AccountBalanceWallet, "الرصيد",
                        Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("الرصيد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(if (balanceCents == null) "—" else usd(balanceCents),
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            placeholder = { Text("ابحث عن حساب، ريلز أو خدمة", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotBlank()) IconButton(onClick = { onSearch("") }) {
                    Icon(Icons.Rounded.Close, "مسح البحث")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        // Reels and store are already in bottom navigation. Only live entry points
        // remain here, avoiding the four oversized duplicate dashboard cards.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeDestinationCard("بث مباشر", Icons.Rounded.Sensors,
                Modifier.weight(1f), onLive)
            HomeDestinationCard("غرف صوتية", Icons.Rounded.Mic,
                Modifier.weight(1f), onAudio)
        }
    }
}

/** Compact destination row: use the home screen for the real feed, not giant shortcuts. */
@Composable
private fun HomeDestinationCard(
    title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 57.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(title, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
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
