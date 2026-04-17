package com.aipoweredgita.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.R

object YogaLevelUpNotificationManager {
    
    private const val CHANNEL_ID = "yoga_level_up_channel"
    private const val NOTIFICATION_ID = 3001
    private const val LEVEL_DECREASE_NOTIFICATION_ID = 3002
    
    fun showLevelUpNotification(context: Context, newLevel: Int) {
        createNotificationChannel(context)
        
        val (levelName, message) = when (newLevel) {
            1 -> Pair("Bhakti Yoga", "🎉 You've unlocked Bhakti Yoga! Continue your journey with devotion.")
            2 -> Pair("Jnana Yoga", "🎉 You've unlocked Jnana Yoga! Wisdom guides your path now.")
            3 -> Pair("Moksha", "🎉 You've achieved Moksha! The ultimate liberation is yours!")
            else -> Pair("Level Up", "🎉 Congratulations on your progress!")
        }
        
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle("Level Up! $levelName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    fun showLevelDecreaseNotification(context: Context, oldLevel: Int, newLevel: Int, daysInactive: Int) {
        createNotificationChannel(context)
        
        val oldLevelName = when (oldLevel) {
            1 -> "Bhakti Yoga"
            2 -> "Jnana Yoga"
            3 -> "Moksha"
            else -> "Karma Yoga"
        }
        
        val newLevelName = when (newLevel) {
            0 -> "Karma Yoga"
            1 -> "Bhakti Yoga"
            2 -> "Jnana Yoga"
            else -> "Unknown"
        }
        
        val message = "📉 Due to $daysInactive days of inactivity, your level decreased from $oldLevelName to $newLevelName. Don't worry - your progress is waiting for you! Come back and continue your journey."
        
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle("Level Decreased - We Miss You!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(LEVEL_DECREASE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Yoga Level Up"
            val descriptionText = "Notifications for yoga level achievements"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
