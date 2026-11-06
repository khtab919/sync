package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ChatHeader(name: String, back: () -> Unit, call: () -> Unit, buy: () -> Unit, enabled: Boolean) {
    Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(back) { Icon(Icons.Outlined.ArrowForward, "رجوع") }
                RelaxiaAvatar(name, 0, 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1); Text("المحادثة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                IconButton(call, enabled = enabled) { Icon(Icons.Outlined.Call, "بدء اتصال") }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("مساحتك للتواصل", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                TextButton(buy, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)) { Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("شراء رصيد", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
internal fun ChatComposer(body: String, change: (String) -> Unit, busy: Boolean, photo: () -> Unit, record: () -> Unit, send: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledIconButton(send, enabled = !busy && body.isNotBlank(), modifier = Modifier.size(50.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Outlined.Send, "إرسال الرسالة", Modifier.size(23.dp)) }
        OutlinedTextField(body, change, placeholder = { Text("رسالة…", fontSize = 16.sp) }, minLines = 1, maxLines = 4, enabled = !busy, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f),
            trailingIcon = { IconButton(photo, enabled = !busy) { Icon(Icons.Outlined.Image, "إرسال صورة", Modifier.size(23.dp)) } },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .2f), unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface))
        IconButton(record, enabled = !busy, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.MicNone, "تسجيل رسالة صوتية", Modifier.size(25.dp)) }
    }
}

@Composable
internal fun ChatBubble(message: JSONObject, mine: Boolean, play: (String) -> Unit, join: (String) -> Unit) {
    val outgoing = Color(0xFF65364F)
    val time = runCatching { Instant.parse(message.optString("created_at")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if(mine) Arrangement.End else Arrangement.Start) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(Modifier.widthIn(max = 310.dp), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if(mine) 22.dp else 5.dp, bottomEnd = if(mine) 5.dp else 22.dp), shadowElevation = 1.dp, color = if(mine) outgoing else MaterialTheme.colorScheme.surface, contentColor = if(mine) Color.White else MaterialTheme.colorScheme.onSurface) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if(message.optString("body").isNotBlank()) SelectionContainer { Text(message.optString("body"), fontSize = 17.sp, lineHeight = 26.sp) }
                        val media = message.optString("attachment_url")
                        if(media.isNotBlank() && message.optString("attachment_mime").startsWith("image/")) ReferenceImage(media, Modifier.width(250.dp).height(220.dp))
                        if(media.isNotBlank() && message.optString("attachment_mime").startsWith("audio/")) TextButton(onClick = { play(media) }, colors = ButtonDefaults.textButtonColors(contentColor = if(mine) Color.White else MaterialTheme.colorScheme.primary)) { Icon(Icons.Outlined.PlayCircleOutline, null, Modifier.size(32.dp)); Spacer(Modifier.width(8.dp)); Text("رسالة صوتية") }
                        if(message.optString("call_id").isNotBlank() && message.optString("call_status") in listOf("ringing", "accepted")) TextButton(onClick = { join(message.optString("call_id")) }, colors = ButtonDefaults.textButtonColors(contentColor = if(mine) Color.White else MaterialTheme.colorScheme.primary)) { Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(6.dp)); Text("فتح المكالمة") }
                        if(message.optString("call_id").isNotBlank() && message.optString("call_status") !in listOf("ringing", "accepted")) Text(when(message.optString("call_status")) { "missed" -> "لم يتم الرد"; "rejected" -> "تم رفض الاتصال"; else -> "انتهت المكالمة" }, fontSize = 12.sp)
                        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(time, fontSize = 10.sp, color = if(mine) Color.White.copy(alpha=.65f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            if(mine) Icon(Icons.Outlined.Check, "تم إرسال الرسالة", Modifier.size(13.dp), tint = Color.White.copy(alpha=.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConversationTile(person: JSONObject, open: () -> Unit) {
    val time = runCatching { Instant.parse(person.optString("created_at")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("")
    Surface(onClick = open, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(23.dp), shadowElevation = 1.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            RelaxiaAvatar(person.optString("display_name"), person.optInt("selected_frame"), 54, person.optString("avatar_url"))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(person.optString("display_name"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(person.optString("last_message").ifBlank { "مرفق في المحادثة" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Outlined.ChevronLeft, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
