package com.example.anittheft.Services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import android.app.Service
import com.example.anittheft.R

class ChargingStatusService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var chargingStateReceiver: BroadcastReceiver? = null
    private var screenOnReceiver: BroadcastReceiver? = null
    private lateinit var powerManager: PowerManager

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        registerChargingStateReceiver()
        registerScreenOnOffReceiver()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterChargingStateReceiver()
        unregisterScreenOnOffReceiver()
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun registerChargingStateReceiver() {
        val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)

        chargingStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    when (intent.action) {
                        Intent.ACTION_POWER_CONNECTED -> {
                            // Device is connected to power (charging)
                            stopAlarm()
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            // Device is disconnected from power (not charging)
                            // Check if the screen is on before playing the alarm
                            if (isScreenOn()) {
                                playAlarm()
                            }
                        }
                    }
                }
            }
        }

        registerReceiver(chargingStateReceiver, filter)
    }

    private fun unregisterChargingStateReceiver() {
        chargingStateReceiver?.let { unregisterReceiver(it) }
    }

    private fun registerScreenOnOffReceiver() {
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            if (!isCharging()) {
                                playAlarm()
                            }
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            stopAlarm()
                        }
                    }
                }
            }
        }

        registerReceiver(screenOnReceiver, filter)
    }

    private fun unregisterScreenOnOffReceiver() {
        screenOnReceiver?.let { unregisterReceiver(it) }
    }

    private fun playAlarm() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)

        mediaPlayer?.setOnCompletionListener { mediaPlayer ->
            mediaPlayer.start()
        }

        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun isScreenOn(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    private fun isCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val notificationIntent = Intent(this, ChargingStatusService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Charging Status Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Charging Status Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ChargingStatusChannel"
        private const val NOTIFICATION_ID = 1
    }
}
