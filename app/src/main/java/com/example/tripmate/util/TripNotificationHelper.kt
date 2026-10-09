package com.example.tripmate.util

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
import com.example.tripmate.MainActivity
import com.example.tripmate.R

object TripNotificationHelper {

    const val CHANNEL_TRIP_UPDATES = "tripmate_trip_updates"
    const val CHANNEL_COLLABORATION = "tripmate_collaboration"

    /**
     * Creates standard notification channels for Android 8.0+ (Oreo, API 26+).
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val updatesChannel = NotificationChannel(
                CHANNEL_TRIP_UPDATES,
                "Trip Updates & Schedule",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts about itinerary departures, schedule adjustments, and weather warnings"
                enableLights(true)
                enableVibration(true)
            }

            val collabChannel = NotificationChannel(
                CHANNEL_COLLABORATION,
                "Collaboration & Voting",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when companions vote on activities or split trip expenses"
            }

            notificationManager.createNotificationChannels(listOf(updatesChannel, collabChannel))
        }
    }

    /**
     * Checks if notification permission is granted and channels are enabled.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Posts a notification to the Android system tray.
     */
    fun postNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_TRIP_UPDATES,
        notificationId: Int = 1001
    ) {
        initChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    /**
     * Cancels all notifications posted by TripMate.
     */
    fun cancelAll(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
            // Ignored
        }
    }
}
