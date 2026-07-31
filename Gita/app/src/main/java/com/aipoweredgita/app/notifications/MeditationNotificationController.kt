package com.aipoweredgita.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.R
import com.aipoweredgita.app.viewmodel.BreathingPhase

object MeditationNotificationController {
    const val CHANNEL_ID = "meditation_timer_channel"
    const val NOTIFICATION_ID = 1008

    const val ACTION_PAUSE = "com.aipoweredgita.app.MEDITATION_PAUSE"
    const val ACTION_RESUME = "com.aipoweredgita.app.MEDITATION_RESUME"
    const val ACTION_STOP = "com.aipoweredgita.app.MEDITATION_STOP"

    var onActionReceived: ((String) -> Unit)? = null

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Meditation Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active meditation timer and breathing controls"
                setSound(null, null)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(
        context: Context,
        phase: BreathingPhase,
        timerVal: Int,
        timeLeftSeconds: Int,
        isPaused: Boolean
    ) {
        createChannel(context)

        val minutes = timeLeftSeconds / 60
        val seconds = timeLeftSeconds % 60
        val timeFormatted = "%02d:%02d".format(minutes, seconds)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume PendingIntent
        val pauseResumeActionIntent = Intent(context, MeditationActionReceiver::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            pauseResumeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop PendingIntent
        val stopActionIntent = Intent(context, MeditationActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            stopActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPaused) "Meditation Paused 🧘" else "Meditation in Progress 🧘"
        val content = if (isPaused) {
            "Paused • $timeFormatted remaining"
        } else {
            "${phase.label} (${timerVal + 1}/${phase.seconds}s) • $timeFormatted remaining"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(!isPaused)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isPaused) {
            builder.addAction(R.mipmap.ic_launcher, "Resume ▶️", pauseResumePendingIntent)
        } else {
            builder.addAction(R.mipmap.ic_launcher, "Pause ⏸️", pauseResumePendingIntent)
        }
        builder.addAction(R.mipmap.ic_launcher, "Stop ⏹️", stopPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}

class MeditationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        MeditationNotificationController.onActionReceived?.invoke(action)
    }
}
