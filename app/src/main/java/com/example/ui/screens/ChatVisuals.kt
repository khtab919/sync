package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class ConversationColors(val background: Color, val incoming: Color, val accent: Color, val soft: Color)
@Composable
internal fun conversationColors(): ConversationColors = if (MaterialTheme.colorScheme.background.luminance() < .5f)
    ConversationColors(Color(0xFF191517), Color(0xFF302B2D), Color(0xFF925562), Color(0xFF392D32))
else ConversationColors(Color(0xFFF8F5F4), Color(0xFFECE7E5), Color(0xFF754451), Color(0xFFEEE3E5))

@Composable
internal fun ChatAvatar(name:String, size:Int, url:String="") {
    Box(Modifier.size(size.dp).clip(CircleShape).background(conversationColors().soft).border(1.dp,conversationColors().accent.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center) {
        Text(name.trim().take(1),fontSize=(size*.32f).sp,color=MaterialTheme.colorScheme.onSurface)
        if(url.startsWith("https://"))coil.compose.AsyncImage(url,name,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable
internal fun ChatHeader(name: String, back: () -> Unit, call: () -> Unit, buy: () -> Unit, enabled: Boolean, handle: String = "", avatar: String = "", video: () -> Unit = call, verified: Boolean = false, priceCoins: Long = 0, online: Boolean = false, presenceKnown: Boolean = false) {
    val colors = conversationColors()
    Column(Modifier.background(colors.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatAvatar(name, 44, avatar)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (verified) Icon(Icons.Outlined.Verified, "حساب معتمد", Modifier.size(18.dp), tint = colors.accent)
                }
                Text(if(presenceKnown && online) "متصل الآن" else if(presenceKnown) "غير متصل" else if(handle.isNotBlank() && handle != name) "@$handle" else "محادثة خاصة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
            }
            FilledIconButton(video, enabled = enabled, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(14.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.soft, contentColor = MaterialTheme.colorScheme.onSurface)) { Icon(Icons.Outlined.Videocam, "اتصال فيديو") }
            FilledIconButton(call, enabled = enabled, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(14.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.soft, contentColor = MaterialTheme.colorScheme.onSurface)) { Icon(Icons.Outlined.Call, "بدء اتصال") }
            IconButton(back, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.ArrowForward, "رجوع", tint = MaterialTheme.colorScheme.onSurface) }
        }
        if(priceCoins > 0) Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(25.dp), color = colors.soft) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if(priceCoins > 0) Icons.Outlined.Lock else Icons.Outlined.ChatBubbleOutline, null, Modifier.size(24.dp), tint = colors.accent)
                Column(Modifier.weight(1f)) {
                    Text(if(priceCoins > 0) "رسائل $name مدفوعة" else "مساحتكما الخاصة", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(if(priceCoins > 0) "$priceCoins كوينز لكل رسالة · بتأكيدك قبل الإرسال" else "رسائل وصور وتسجيلات صوتية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(buy, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White), shape = RoundedCornerShape(20.dp)) { Text("شراء رصيد", fontSize = 12.sp) }
            }
        }
        else Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("محادثة خاصة",Modifier.weight(1f),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(buy,enabled=enabled,contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){Text("شراء رصيد",fontSize=12.sp,color=colors.accent)}
        }
    }
}

@Composable
internal fun ChatComposer(body: String, change: (String) -> Unit, busy: Boolean, photo: () -> Unit, record: () -> Unit, send: () -> Unit) {
    val colors = conversationColors()
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Keep a stable action position: empty drafts record; written drafts send.
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if(body.isBlank()) FilledIconButton(record, enabled = !busy, modifier = Modifier.fillMaxSize(), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = Color.White)) { Icon(Icons.Outlined.MicNone, "تسجيل رسالة صوتية", Modifier.size(27.dp)) }
            else FilledIconButton(send, enabled = !busy, modifier = Modifier.fillMaxSize(), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = Color.White)) { Icon(Icons.Outlined.Send, "إرسال الرسالة", Modifier.size(24.dp)) }
        }
        OutlinedTextField(body, change, placeholder = { Text("اكتب رسالة…", fontSize = 16.sp) }, minLines = 1, maxLines = 4, enabled = !busy, shape = RoundedCornerShape(28.dp), modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = colors.accent.copy(alpha = .35f), unfocusedContainerColor = colors.incoming, focusedContainerColor = colors.incoming))
        FilledIconButton(photo, enabled = !busy, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(18.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.soft, contentColor = MaterialTheme.colorScheme.onSurface)) { Icon(Icons.Outlined.Image, "إرسال صورة", Modifier.size(25.dp)) }
    }
}

@Composable
internal fun ChatDayLabel(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = conversationColors().soft) { Text(label, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun ChatBubble(message: JSONObject, mine: Boolean, play: (String) -> Unit, join: (String) -> Unit, retry: (String) -> Unit = {}, remove: (String) -> Unit = {}, peerName: String = "", peerAvatar: String = "", unlock: () -> Unit = {}) {
    val colors = conversationColors()
    val outgoing = Color(0xFF754451)
    val bubbleColor by animateColorAsState(if(mine)outgoing else colors.incoming,animationSpec=androidx.compose.animation.core.tween(150),label="messageColor")
    var showMedia by remember { mutableStateOf(false) }
    if(showMedia) ChatMediaViewer(message.optString("attachment_url"),message.optString("attachment_mime").startsWith("video/")){showMedia=false}
    val time = runCatching { Instant.parse(message.optString("created_at")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if(mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
            if(!mine && peerName.isNotBlank()) { ChatAvatar(peerName, 28, peerAvatar); Spacer(Modifier.width(6.dp)) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(Modifier.widthIn(max = 300.dp).weight(1f, fill = false), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if(mine) 22.dp else 5.dp, bottomEnd = if(mine) 5.dp else 22.dp), shadowElevation = 0.dp, color = bubbleColor, contentColor = if(mine) Color.White else MaterialTheme.colorScheme.onSurface) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if(message.optString("body").isNotBlank() && message.optString("call_id").isBlank()) SelectionContainer { Text(message.optString("body"), fontSize = 16.sp, lineHeight = 23.sp) }
                        if(message.optBoolean("media_locked")) Surface(onClick=unlock,shape=RoundedCornerShape(16.dp),color=colors.soft) {
                            Column(Modifier.widthIn(min=180.dp).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Lock,"مرفق مدفوع",tint=MaterialTheme.colorScheme.onSurface)
                                Text(if(message.optString("attachment_mime").startsWith("video/"))"فيديو خاص"else"صورة خاصة",color=MaterialTheme.colorScheme.onSurface)
                                Text("فتح مقابل ${message.optLong("media_price_cents")} كوينز",color=colors.accent,fontSize=13.sp)
                            }
                        }
                        val media = message.optString("attachment_url")
                        if(media.isNotBlank() && message.optString("attachment_mime").startsWith("image/")) coil.compose.AsyncImage(model=coil.request.ImageRequest.Builder(LocalContext.current).data(media).memoryCacheKey(media.substringBefore('?')).diskCachePolicy(coil.request.CachePolicy.DISABLED).build(),contentDescription="صورة في المحادثة",modifier=Modifier.widthIn(max=250.dp).fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp)).clickable{showMedia=true},contentScale=androidx.compose.ui.layout.ContentScale.Crop)
                        if(media.isNotBlank() && message.optString("attachment_mime").startsWith("video/")) Surface(onClick={showMedia=true},shape=RoundedCornerShape(14.dp),color=LocalContentColor.current.copy(alpha=.1f)) {
                            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Outlined.PlayCircleOutline,"فتح فيديو المحادثة",Modifier.size(32.dp));Text("مقطع فيديو")}
                        }
                        if(media.isNotBlank() && message.optString("attachment_mime").startsWith("audio/")) VoiceMessage(media,message.optString("client_key").ifBlank{"message-${message.optInt("id")}"})
                        if(message.optBoolean("failed")&&message.optString("failure_reason").isNotBlank())Text(message.optString("failure_reason"),fontSize=12.sp)
                        if(message.optBoolean("failed")) Row { TextButton({retry(message.optString("client_key"))},colors=ButtonDefaults.textButtonColors(contentColor=if(mine)Color.White else MaterialTheme.colorScheme.primary)){Text("إعادة المحاولة")};TextButton({remove(message.optString("client_key"))},colors=ButtonDefaults.textButtonColors(contentColor=if(mine)Color.White else MaterialTheme.colorScheme.primary)){Text("حذف")}}
                        if(message.optString("call_id").isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(if(message.optString("call_mode") == "video") Icons.Outlined.Videocam else Icons.Outlined.Call, null, Modifier.size(22.dp))
                                Column {
                                    Text(if(message.optString("call_mode") == "video") "مكالمة فيديو" else "مكالمة صوتية", fontWeight = FontWeight.Medium, fontSize=14.sp)
                                    Text(when(message.optString("call_status")) { "missed" -> "لم يتم الرد"; "rejected" -> "تم رفض الاتصال"; "ringing" -> "جارٍ الاتصال"; "accepted" -> "مكالمة جارية"; else -> "انتهت المكالمة" }, fontSize = 12.sp)
                                }
                            }
                            if(message.optString("call_status") in listOf("ringing", "accepted")) TextButton(onClick = { join(message.optString("call_id")) }, colors = ButtonDefaults.textButtonColors(contentColor = if(mine) Color.White else colors.accent)) { Text("فتح المكالمة") }
                        }
                        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(time, fontSize = 11.sp, color = if(mine) Color.White.copy(alpha=.65f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            if(mine) Icon(when {message.optBoolean("failed")->Icons.Outlined.ErrorOutline;message.optBoolean("pending")->Icons.Outlined.Schedule;!message.isNull("read_at")->Icons.Outlined.DoneAll;!message.isNull("delivered_at")->Icons.Outlined.DoneAll;else->Icons.Outlined.Check},when{message.optBoolean("failed")->"تعذّر الإرسال";message.optBoolean("pending")->"بانتظار الإرسال";!message.isNull("read_at")->"قرأها المستلم";!message.isNull("delivered_at")->"وصلت إلى جهاز المستلم";else->"تم إرسال الرسالة"},Modifier.size(14.dp),tint=if(!message.isNull("read_at"))Color(0xFFAED0D7)else Color.White.copy(alpha=.7f))
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
