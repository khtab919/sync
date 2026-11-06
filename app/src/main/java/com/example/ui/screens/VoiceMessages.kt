package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal class VoicePlayback(private val context:Context) {
    var url by mutableStateOf("");private set
    var playing by mutableStateOf(false);private set
    var loading by mutableStateOf(false);private set
    var position by mutableIntStateOf(0);private set
    var duration by mutableIntStateOf(0);private set
    var error by mutableStateOf("");private set
    private var player:MediaPlayer?=null
    fun identity(source:String):String=source.substringBefore('?')
    fun toggle(source:String,messageKey:String=""){
        val key=messageKey.ifBlank{identity(source)}
        if(key==url&&loading)return
        if(key==url&&player!=null&&!loading){if(playing){player?.pause();playing=false}else{player?.start();playing=true};return}
        close();url=key;loading=true;error=""
        runCatching { MediaPlayer().also{p->player=p;p.setDataSource(context,Uri.parse(source));p.setOnPreparedListener{duration=it.duration;loading=false;playing=true;it.start()};p.setOnCompletionListener{playing=false;position=0;it.seekTo(0)};p.setOnErrorListener{mp,_,_->mp.release();player=null;loading=false;playing=false;error="تعذّر التشغيل. اضغط لإعادة المحاولة.";true};p.prepareAsync()} }.onFailure{close();url=key;error="تعذّر تحميل التسجيل."}
    }
    fun tick(){if(playing)position=runCatching{player?.currentPosition?:0}.getOrDefault(0)}
    fun seek(value:Int){player?.seekTo(value);position=value}
    fun pause(){runCatching{player?.pause()};playing=false}
    fun close(){runCatching{player?.release()};player=null;playing=false;loading=false;position=0;duration=0}
}
internal val LocalVoicePlayback=staticCompositionLocalOf<VoicePlayback?>{null}

@Composable
internal fun VoiceMessage(url:String,messageKey:String="") {
    val context=LocalContext.current
    val own=remember{VoicePlayback(context)}
    val shared=LocalVoicePlayback.current
    val controller=shared?:own
    val identity=messageKey.ifBlank{controller.identity(url)}
    val active=controller.url==identity
    DisposableEffect(Unit){onDispose{own.close()}}
    LaunchedEffect(controller,controller.playing){if(shared==null)while(controller.playing){controller.tick();delay(200)}}
    Column(Modifier.widthIn(min=200.dp,max=270.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton({controller.toggle(url,identity)}){if(active&&controller.loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Icon(if(active&&controller.playing)Icons.Outlined.PauseCircleOutline else Icons.Outlined.PlayCircleOutline,"تشغيل أو إيقاف الرسالة الصوتية",Modifier.size(32.dp))}
            Column(Modifier.weight(1f)){Text("رسالة صوتية",style=MaterialTheme.typography.labelMedium);Slider(value=if(active&&controller.duration>0)controller.position.toFloat()/controller.duration else 0f,onValueChange={controller.seek((it*controller.duration).toInt())},enabled=active&&controller.duration>0,colors=SliderDefaults.colors(thumbColor=LocalContentColor.current,activeTrackColor=LocalContentColor.current,inactiveTrackColor=LocalContentColor.current.copy(alpha=.25f)),modifier=Modifier.height(28.dp))}
            val value=if(active)if(controller.playing)controller.position else controller.duration else 0
            Text(if(active)"${value/60000}:${((value/1000)%60).toString().padStart(2,'0')}"else"صوت",style=MaterialTheme.typography.labelSmall)
        }
        if(active&&controller.error.isNotBlank())Text(controller.error,style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun RecordingIndicator(seconds:Int,amplitude:Float){
    val color=MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Icon(Icons.Outlined.Mic,"جارٍ تسجيل الصوت",tint=color)
        Text("${seconds/60}:${(seconds%60).toString().padStart(2,'0')}")
        Canvas(Modifier.weight(1f).height(30.dp)){
            val center=size.height/2
            for(i in 0..23){val x=(i+.5f)*size.width/24;val scale=if(i%3==0)1f else if(i%2==0).7f else .4f;val h=2.dp.toPx()+amplitude.coerceIn(0f,1f)*center*scale;drawLine(color,Offset(x,center-h),Offset(x,center+h),strokeWidth=3.dp.toPx(),cap=StrokeCap.Round)}
        }
        Text("60ث كحد أقصى",style=MaterialTheme.typography.labelSmall)
    }
}
