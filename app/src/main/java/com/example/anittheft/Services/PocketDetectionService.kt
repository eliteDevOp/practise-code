
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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.anittheft.R

class PocketDetectionService : LifecycleService(), SensorEventListener {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private lateinit var powerManager: PowerManager
    private var screenOnReceiver: BroadcastReceiver? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "PocketDetectionChannel"
        private const val NOTIFICATION_ID = 1
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        registerScreenOnOffReceiver()

        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        unregisterScreenOnOffReceiver()
        stopAudio()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val distance = event.values[0]
                    if (distance < (proximitySensor?.maximumRange ?: 0f)) {
                        stopAudio()
                    } else {
                        if (isScreenOn()) {
                            playAudio()
                        }
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun playAudio() {
        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm)

            mediaPlayer?.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.start()
            }

            mediaPlayer?.start()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, PocketDetectionService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Pocket Detection Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Pocket Detection Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun registerScreenOnOffReceiver() {
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            if (isScreenOn()) {
                                playAudio()
                            }
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            stopAudio()
                        }
                    }
                }
            }
        }

        registerReceiver(screenOnReceiver, intentFilter)
    }

    private fun unregisterScreenOnOffReceiver() {
        screenOnReceiver?.let { unregisterReceiver(it) }
    }

    private fun isScreenOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }
}
