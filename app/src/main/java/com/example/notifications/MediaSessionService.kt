package com.example.notifications

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/** Keeps an explicitly started session visible; it never starts recording itself. */
class MediaSessionService:Service(){
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("active_session","الجلسات الصوتية النشطة",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=NotificationCompat.Builder(this,"active_session").setSmallIcon(R.drawable.ic_notification).setContentTitle("جلسة Relaxia نشطة").setContentText("اضغط للعودة إلى الجلسة وإنهائها").setOngoing(true).setContentIntent(open).build()
        val type=if(Build.VERSION.SDK_INT>=30 && intent?.getBooleanExtra("microphone",false)==true) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else if(Build.VERSION.SDK_INT>=29)ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this,701,notification,type)
        return START_NOT_STICKY
    }
    companion object {
        fun start(context:Context,microphone:Boolean){ContextCompat.startForegroundService(context,Intent(context,MediaSessionService::class.java).putExtra("microphone",microphone))}
        fun stop(context:Context){context.stopService(Intent(context,MediaSessionService::class.java))}
    }
}
