package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_REBOUND_ALERTS = "lunointel_rebound_alerts"
    const val CHANNEL_EMERGENCY_TRIGGER = "lunointel_emergency_triggers"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_REBOUND_ALERTS,
                "Peluang Lantunan Gred A",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Amaran peluang beli Gred A yang melepasi 6 semakan dan had skor MYT."
            }

            val triggerChannel = NotificationChannel(
                CHANNEL_EMERGENCY_TRIGGER,
                "Pencetus Kecemasan Radar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pintas jadual segera apabila koin jatuh >4% dalam 1 jam atau RSI 1H < 32."
            }

            nm.createNotificationChannel(alertChannel)
            nm.createNotificationChannel(triggerChannel)
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

    fun sendRadarAlert(
        context: Context,
        pair: String,
        title: String,
        body: String,
        isEmergency: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_pair", pair)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            pair.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isEmergency) CHANNEL_EMERGENCY_TRIGGER else CHANNEL_REBOUND_ALERTS
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(pair.hashCode(), notification)
        }
    }
}
