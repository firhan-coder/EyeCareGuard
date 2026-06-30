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
    private var minuteTimer: CountDownTimer? = null
    private lateinit var prefs: SharedPreferences
    private var minutesLeft = 20

    companion object {
        const val CHANNEL_ID_ONGOING = "eye_rest_channel"
        const val CHANNEL_ID_ALERT_SOUND = "eye_rest_alert_sound"
        const val CHANNEL_ID_ALERT_SILENT = "eye_rest_alert_silent"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.eyecareguard.app.ACTION_STOP_REST"
        const val ACTION_TICK = "com.eyecareguard.app.ACTION_REST_TICK"
        const val EXTRA_MILLIS_LEFT = "extra_millis_left"
        const val INTERVAL_MS = 20L * 60 * 1000
        const val PREFS_NAME = "eyecare_prefs"
        const val KEY_SOUND = "notify_sound"
        const val KEY_VIBRATE = "notify_vibrate"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createAlertChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTimerAndService()
            return START_NOT_STICKY
        }
        minutesLeft = 20
        startForeground(NOTIFICATION_ID, buildOngoingNotification(minutesLeft))
        startTimer()
        startMinuteUpdater()
        return START_STICKY
    }

    private fun createAlertChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // ลบ channel เก่าทิ้งก่อน เผื่อมีของเดิมค้างอยู่ที่ตั้งค่าผิด
            manager.deleteNotificationChannel(CHANNEL_ID_ALERT_SOUND)
            manager.deleteNotificationChannel(CHANNEL_ID_ALERT_SILENT)

            val soundChannel = NotificationChannel(
                CHANNEL_ID_ALERT_SOUND, "พักสายตา (มีเสียง)", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(soundChannel)

            val silentChannel = NotificationChannel(
                CHANNEL_ID_ALERT_SILENT, "พักสายตา (ไม่มีเสียง)", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(silentChannel)

            val ongoingChannel = NotificationChannel(
                CHANNEL_ID_ONGOING, "พักสายตา 20-20-20 (สถานะ)", NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(ongoingChannel)
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(INTERVAL_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                sendRestReminder()
                minutesLeft = 20
                startTimer()
                startMinuteUpdater()
            }
        }.start()
    }

    private fun startMinuteUpdater() {
        minuteTimer?.cancel()
        minutesLeft = 20
        minuteTimer = object : CountDownTimer(INTERVAL_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                minutesLeft = Math.ceil(millisUntilFinished / 60000.0).toInt()
                sendTickBroadcast(millisUntilFinished)
                if (millisUntilFinished % 60000 < 1000) {
                    updateOngoingNotification()
                }
            }
            override fun onFinish() {
                minutesLeft = 0
                sendTickBroadcast(0)
            }
        }.start()
    }

    private fun sendTickBroadcast(millisLeft: Long) {
        val intent = Intent(ACTION_TICK).apply {
            putExtra(EXTRA_MILLIS_LEFT, millisLeft)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun updateOngoingNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildOngoingNotification(minutesLeft))
    }

    private fun sendRestReminder() {
        val useSound = prefs.getBoolean(KEY_SOUND, true)
        val useVibrate = prefs.getBoolean(KEY_VIBRATE, true)

        val channelId = if (useSound || useVibrate) CHANNEL_ID_ALERT_SOUND else CHANNEL_ID_ALERT_SILENT

        val openIntent = Intent(this, RestReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ถึงเวลาพักสายตาแล้ว 👀")
            .setContentText("มองไกล 20 ฟุต เป็นเวลา 20 วินาที")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (useSound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        if (useVibrate) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, builder.build())
    }

    private fun buildOngoingNotification(minutes: Int): Notification {
        val stopIntent = Intent(this, EyeRestService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_ONGOING)
            .setContentTitle("แจ้งเตือนพักสายตา 20-20-20")
            .setContentText("พักสายตาใน $minutes นาที")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ปิด", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun stopTimerAndService() {
        timer?.cancel()
        minuteTimer?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        minuteTimer?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
