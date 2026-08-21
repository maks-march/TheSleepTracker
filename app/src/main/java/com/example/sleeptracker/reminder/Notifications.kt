package com.example.sleeptracker.reminder

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
import com.example.sleeptracker.MainActivity
import com.example.sleeptracker.R

/** Каналы и показ уведомлений. */
object Notifications {

    /** Вечернее — со звуком, его легко пропустить. */
    const val CHANNEL_BEDTIME = "bedtime"

    /** Утреннее — намеренно беззвучное, чтобы не будить. */
    const val CHANNEL_MORNING = "morning"

    private const val ID_BEDTIME = 1
    private const val ID_MORNING = 2

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val bedtime = NotificationChannel(
            CHANNEL_BEDTIME,
            context.getString(R.string.channel_bedtime),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_bedtime_desc)
        }

        // IMPORTANCE_LOW: без звука и без всплывающего баннера
        val morning = NotificationChannel(
            CHANNEL_MORNING,
            context.getString(R.string.channel_morning),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_morning_desc)
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannels(listOf(bedtime, morning))
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showBedtime(context: Context) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BEDTIME)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_bedtime_title))
            .setContentText(context.getString(R.string.notif_bedtime_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_BEDTIME, notification)
    }

    fun showMorning(context: Context) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_morning_title))
            .setContentText(context.getString(R.string.notif_morning_text))
            // тихое уведомление: без звука, вибрации и всплытия
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_MORNING, notification)
    }
}
