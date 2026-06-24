package com.eyecareguard.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class EyeRestService : Service() {

    private var timer: CountDownTimer? = null
    private lateinit var prefs: SharedPreferences

    companion object {
        const val CHANNEL_ID = "eye_rest_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.eyecareguard.app.ACTION_STOP_REST"
        const val INTERVAL_MS = 20L * 60 * 1000
        const val PREFS_NAME = "eyecare_prefs"
        const val KEY_SOUND = "notify_sound"
        const val KEY_VIBRATE = "notify_vibrate"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTimerAndService()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildOngoingNotification())
        startTimer()
        return START_STICKY
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(INTERVAL_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                sendRestReminder()
                startTimer()
            }
        }.start()
    }

    private fun sendRestReminder() {
        val useSound = prefs.getBoolean(KEY_SOUND, true)
        val useVibrate = prefs.getBoolean(KEY_VIBRATE, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (useSound) NotificationManager.IMPORTANCE_HIGH
                            else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, "พักสายตา 20-20-20", importance)
            channel.enableVibration(useVibrate)
            if (!useSound) channel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, RestReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ถึงเวลาพักสายตาแล้ว 👀")
            .setContentText("มองไกล 20 ฟุต เป็นเวลา 20 วินาที")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (useSound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun buildOngoingNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "พักสายตา 20-20-20",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, EyeRestService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("แจ้งเตือนพักสายตา 20-20-20 เปิดอยู่")
            .setContentText("จะแจ้งเตือนทุก 20 นาที")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ปิด", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun stopTimerAndService() {
        timer?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
