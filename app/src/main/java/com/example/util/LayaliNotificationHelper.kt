package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlin.random.Random

object LayaliNotificationHelper {
    const val CHANNEL_ID = "layali_new_services_channel"
    private const val CHANNEL_NAME = "إشعارات الخدمات والعروض الجديدة"
    private const val CHANNEL_DESC = "تنبيهات فورية عند نشر أي خدمة مساج أو سهرة أو عرض جديد في جميع المدن"
    const val TELEGRAM_SUPPORT_USERNAME = "@RamaNUR"
    const val TELEGRAM_SUPPORT_URL = "https://t.me/RamaNUR"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendNewServiceBroadcastNotification(
        context: Context,
        title: String,
        cityName: String,
        priceUsd: Long,
        description: String,
        mediaType: String
    ) {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return

        val mediaBadge = when (mediaType) {
            "video" -> "🎥 مع عرض فيديو"
            "both" -> "📸🎥 صور وفيديو"
            else -> "📸 مع ألبوم صور"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(1000, 9999),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✨ خدمة جديدة في $cityName: $title")
            .setContentText("$mediaBadge • السعر: $$priceUsd • $description")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$description\nالمدينة: $cityName • السعر المعتمد: $$priceUsd ($mediaBadge)\nافتح التطبيق للحجز المباشر أو التواصل مع الدعم $TELEGRAM_SUPPORT_USERNAME"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Random.nextInt(2000, 99999), notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    fun openTelegramSupport(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_SUPPORT_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback handled in UI snackbar if browser is unavailable
        }
    }
}
