package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONObject

private val LiveRoyal = Color(0xFF11356D)
private val LiveRoyalDeep = Color(0xFF0C2447)
private val LiveGold = Color(0xFFE5B84B)

/** Home discovery contains server-reported, currently active streams only.
 * Tapping a card opens that exact broadcast's authenticated admission workflow,
 * not a new stream and not a random creator profile.
 */
@Composable
internal fun RelaxiaActiveHomeStreams(
    rooms: List<JSONObject>,
    loading: Boolean,
    unavailable: Boolean,
    onOpenRoom: (JSONObject) -> Unit,
    onBrowse: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(color = Color(0xFFDCE8FA), shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Rounded.Sensors, "البث الآن",
                    Modifier.padding(9.dp), tint = LiveRoyal)
            }
            Column(Modifier.weight(1f)) {
                Text("مباشر الآن", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(if (rooms.isNotEmpty()) "${rooms.size} بث متاح للمشاهدة" else "اكتشف البثوث النشطة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBrowse) { Text("عرض الكل") }
        }

        if (rooms.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rooms.take(12).forEach { room ->
                    val audio = room.optString("mode") == "audio"
                    val name = room.optString("owner_name").ifBlank { "صاحب البث" }
                    val title = room.optString("title").ifBlank { if (audio) "غرفة صوتية" else "بث مباشر" }
                    val avatar = room.optString("avatar_url")
                    Surface(
                        modifier = Modifier.width(174.dp).height(176.dp)
                            .clickable(onClick = { onOpenRoom(room) }),
                        shape = RoundedCornerShape(23.dp),
                        color = LiveRoyalDeep,
                        shadowElevation = 7.dp
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color(0xFF1F5193), LiveRoyalDeep))
                            )
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFD63F55)
                                    ) {
                                        Text("● مباشر",
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 10.sp, color = Color.White,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        if(audio) Icons.Rounded.HeadsetMic else Icons.Rounded.LiveTv,
                                        null, Modifier.size(21.dp), tint = LiveGold
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF385E91),
                                        modifier = Modifier.size(49.dp)
                                    ) {
                                        if (avatar.startsWith("https://")) {
                                            AsyncImage(model = avatar, contentDescription = "صورة $name",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Person, null, tint = Color.White)
                                            }
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            color = Color.White, fontSize = 13.sp)
                                        Text(if(audio) "غرفة صوتية" else "بث فيديو",
                                            fontSize = 11.sp, color = Color(0xFFC7DBF3))
                                    }
                                }
                                Text(title, color = Color.White,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Visibility, null,
                                        Modifier.size(14.dp), tint = Color(0xFFC7DBF3))
                                    Spacer(Modifier.width(4.dp))
                                    Text(room.optInt("viewers", 0).toString(),
                                        fontSize = 11.sp, color = Color.White)
                                    Spacer(Modifier.weight(1f))
                                    Text(if(room.optBoolean("private")) "مدفوع" else "انضم الآن",
                                        fontSize = 11.sp, color = LiveGold,
                                        fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.PlayArrow, null,
                                        Modifier.size(15.dp), tint = LiveGold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = LiveRoyal)
        } else if (unavailable) {
            Text("تعذّر تحديث قائمة البثوث الآن. اضغط عرض الكل للمحاولة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Compact active host rail. All entries are returned by /api/live; no fake online users. */
@Composable
internal fun RelaxiaLiveHostsBar(
    rooms:List<JSONObject>,onOpenRoom:(JSONObject)->Unit,onBrowse:()->Unit
){
    if(rooms.isEmpty())return
    Column(Modifier.fillMaxWidth().padding(top=8.dp,bottom=4.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp),
            verticalAlignment=Alignment.CenterVertically){
            Text("المباشر الآن",Modifier.weight(1f),fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium,color=Color(0xFF1E3A8A))
            TextButton(onClick=onBrowse){Text("عرض الكل")}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            rooms.take(15).forEach{room->
                val user=room.optString("owner_name")
                Column(Modifier.width(72.dp).clickable{onOpenRoom(room)},
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Box(Modifier.size(64.dp),contentAlignment=Alignment.Center){
                        Surface(Modifier.size(64.dp),shape=CircleShape,
                            color=Color(0xFF38BDF8)){
                            Box(Modifier.padding(3.dp).background(Color(0xFF1E3A8A),CircleShape)
                                .padding(3.dp),contentAlignment=Alignment.Center){
                                val avatar=room.optString("avatar_url")
                                if(avatar.startsWith("https://"))
                                    AsyncImage(model=avatar,contentDescription="مباشر $user",
                                        modifier=Modifier.fillMaxSize().clip(CircleShape))
                                else Icon(Icons.Rounded.Person,null,tint=Color.White)
                            }
                        }
                        Surface(Modifier.align(Alignment.BottomCenter).offset(y=4.dp),
                            color=Color(0xFFC72B55),shape=RoundedCornerShape(7.dp)){
                            Text("LIVE",Modifier.padding(horizontal=6.dp,vertical=2.dp),
                                fontSize=9.sp,color=Color.White,fontWeight=FontWeight.Bold)
                        }
                    }
                    Text(user,maxLines=1,overflow=TextOverflow.Ellipsis,
                        fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/** A real in-feed stream card after every fifth regular post.
 * Audio visualization uses actual approved-seat count from the backend.
 * For video, there is no publicly authorized inline SFU preview endpoint yet:
 * show the host art, not a fabricated or unconsented remote camera stream.
 */
@Composable
internal fun RelaxiaInFeedLiveCard(room:JSONObject,onOpenRoom:()->Unit){
    val audio=room.optString("mode")=="audio"
    val owner=room.optString("owner_name").ifBlank{"مضيف"}
    val occupants=room.optInt("speaker_count",1).coerceAtLeast(1)
    ElevatedCard(onClick=onOpenRoom,
        modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=5.dp),
        shape=RoundedCornerShape(16.dp),
        elevation=CardDefaults.elevatedCardElevation(defaultElevation=5.dp)){
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(
            Color(0xFF1D4E91),Color(0xFF10274C)))).padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Surface(color=Color(0xFFD93658),shape=RoundedCornerShape(8.dp)){
                    Text("● LIVE",Modifier.padding(horizontal=9.dp,vertical=4.dp),
                        color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.width(9.dp))
                Icon(Icons.Rounded.Visibility,null,Modifier.size(15.dp),tint=Color(0xFFD5EAFD))
                Text(" ${room.optInt("viewers",0)}",color=Color.White,fontSize=12.sp)
                Spacer(Modifier.weight(1f))
                Text(if(audio)"غرفة صوتية"else"بث فيديو مباشر",color=Color(0xFFDAEAFD),
                    style=MaterialTheme.typography.labelMedium)
            }
            Box(Modifier.fillMaxWidth().height(100.dp),contentAlignment=Alignment.Center){
                if(audio){
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        verticalAlignment=Alignment.CenterVertically){
                        repeat(8){i->
                            Box(Modifier.size(if(i<occupants)27.dp else 22.dp)
                                .background(if(i<occupants)Color(0xFF38BDF8)
                                    else Color(0xFF5478A3),CircleShape),
                                contentAlignment=Alignment.Center){
                                Icon(Icons.Rounded.HeadsetMic,null,Modifier.size(13.dp),
                                    tint=Color.White)
                            }
                        }
                    }
                    Text("${occupants.coerceAtMost(8)} / 8 مقاعد مشغولة",
                        Modifier.align(Alignment.BottomCenter),
                        color=Color(0xFFDBE8F7),fontSize=11.sp)
                }else{
                    val avatar=room.optString("avatar_url")
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(15.dp)){
                        if(avatar.startsWith("https://")){
                            AsyncImage(model=avatar,contentDescription=null,
                                modifier=Modifier.size(72.dp).clip(CircleShape))
                        }else Icon(Icons.Rounded.LiveTv,null,Modifier.size(58.dp),
                            tint=Color(0xFF38BDF8))
                        Column{
                            Text("بث مباشر",color=Color.White,
                                style=MaterialTheme.typography.titleLarge)
                            Text("اضغط للمشاهدة بالصوت والصورة",color=Color(0xFFD0E2FA),
                                fontSize=12.sp)
                        }
                    }
                }
            }
            HorizontalDivider(color=Color.White.copy(alpha=.24f))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)){
                val image=room.optString("avatar_url")
                if(image.startsWith("https://"))
                    AsyncImage(model=image,contentDescription=null,
                        modifier=Modifier.size(38.dp).clip(CircleShape))
                else Icon(Icons.Rounded.Person,null,Modifier.size(38.dp),
                    tint=Color(0xFFD4EAFD))
                Column(Modifier.weight(1f)){
                    Text(owner,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    Text(room.optString("title"),color=Color(0xFFE0EDFA),
                        fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Surface(color=Color(0xFF38BDF8),shape=RoundedCornerShape(50)){
                    Text("انضم للبث",Modifier.padding(horizontal=13.dp,vertical=10.dp),
                        color=Color(0xFF0F2544),fontSize=12.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
