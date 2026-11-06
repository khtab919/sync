package com.example.ui.screens

import android.widget.MediaController
import android.widget.VideoView
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp

@Composable internal fun ChatMediaViewer(url:String, video:Boolean, close:()->Unit) {
    var view by remember { mutableStateOf<VideoView?>(null) }
    var error by remember { mutableStateOf(false) }
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event -> if(event==androidx.lifecycle.Lifecycle.Event.ON_PAUSE)view?.pause() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit){onDispose{view?.stopPlayback()}}
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF171317)).safeDrawingPadding()) {
            if(video) AndroidView(modifier=Modifier.fillMaxWidth().fillMaxHeight(.85f).align(Alignment.Center),factory={context->VideoView(context).apply{
                view=this;setMediaController(MediaController(context).also{it.setAnchorView(this)})
                setOnPreparedListener{start()};setOnErrorListener{_,_,_->error=true;true};setVideoURI(Uri.parse(url))
            }}) else coil.compose.AsyncImage(url,"صورة المحادثة",Modifier.fillMaxSize().padding(16.dp),contentScale=ContentScale.Fit)
            if(error)Text("تعذّر تشغيل الفيديو. أغلقه وحاول مجددًا.",Modifier.align(Alignment.Center).padding(24.dp),color=Color.White)
            IconButton(close,Modifier.align(Alignment.TopEnd).padding(8.dp)){Icon(Icons.Outlined.Close,"إغلاق الوسائط",tint=Color.White)}
        }
    }
}
