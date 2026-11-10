package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow

object NotificationRoutes {
    val pending = MutableStateFlow<Map<String,String>?>(null)
    fun accept(intent: Intent?) {
        val uri=intent?.data
        if(uri?.scheme=="relaxia" && uri.host in listOf("reel","profile","live")){
            pending.value=mapOf("category" to (if(uri.host=="live")"live_link"else uri.host.orEmpty()),"target" to uri.lastPathSegment.orEmpty())
            return
        }
        val category=intent?.getStringExtra("category") ?: return
        if(category !in channels.keys)return
        pending.value=listOf("category","target","account_id","event_id").associateWith { intent.getStringExtra(it).orEmpty() }
    }
    val channels=mapOf("messages" to "الرسائل", "calls" to "المكالمات", "live" to "البث المباشر", "bookings" to "الحجوزات", "wallet" to "المحفظة")
    fun setup(context:Context) {
        if(Build.VERSION.SDK_INT>=26){
            val manager=context.getSystemService(NotificationManager::class.java)
            channels.forEach { (id,label) -> manager.createNotificationChannel(NotificationChannel(id,label,if(id in listOf("messages","calls")) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)) }
        }
    }
    fun account(context:Context,id:Int){
        val prefs=context.getSharedPreferences("relaxia_push",Context.MODE_PRIVATE)
        if(prefs.getInt("account_id",0)!=id) context.getSystemService(NotificationManager::class.java).cancelAll()
        prefs.edit().putInt("account_id",id).apply()
    }
}

class RelaxiaMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {
        // Registration is authenticated and retried on the next foreground session.
        getSharedPreferences("relaxia_push",MODE_PRIVATE).edit().putString("token",token).apply()
    }
    override fun onMessageReceived(message:RemoteMessage) {
        val data=message.data
        val prefs=getSharedPreferences("relaxia_push",MODE_PRIVATE)
        if(prefs.getInt("account_id",0)<=0 || data["account_id"]!=prefs.getInt("account_id",0).toString())return
        val channel=data["category"]?.takeIf{it in NotificationRoutes.channels} ?: return
        NotificationRoutes.setup(this)
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val intent=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        data.forEach{(key,value)->intent.putExtra(key,value)}
        val id=(data["event_id"] ?: message.messageId ?: "relaxia").hashCode()
        val pending=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alert=NotificationCompat.Builder(this,channel).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data["title"] ?: "Relaxia").setContentText("اضغط لعرض التفاصيل")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pending)
            .setCategory(if(channel=="calls")NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setTimeoutAfter(if(channel=="calls")45000L else 86400000L).build()
        getSystemService(NotificationManager::class.java).notify(id,alert)
    }
}
