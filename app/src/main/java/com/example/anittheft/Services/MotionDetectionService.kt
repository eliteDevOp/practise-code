package com.example.anittheft.Services


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.anittheft.R
import kotlin.math.sqrt

class MotionDetectionService : LifecycleService(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var powerManager: PowerManager
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false


    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (accelerometer != null) {
            sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager!!.unregisterListener(this)
        stopMedia()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val acceleration = calculateAcceleration(event.values)
            Log.d("MotionDetectionService", "Acceleration: $acceleration")

            if (acceleration > SHAKE_THRESHOLD && isScreenOn() && !isPlaying) {
                Log.d("MotionDetectionService", "Shake detected!")
                playAlarm()
            }
        }
    }

    private fun calculateAcceleration(values: FloatArray): Float {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        return sqrt(x * x + y * y + z * z)
    }


    private fun playAlarm() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        isPlaying = true

        mediaPlayer!!.setOnCompletionListener { obj: MediaPlayer ->
            obj.release()
            isPlaying = false
        }
    }

    private fun stopMedia() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
            isPlaying = false
        }
    }


    private fun isScreenOn(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val notificationIntent = Intent(
            this,
            MotionDetectionService::class.java
        )
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Motion Detection Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Motion Detection Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD = 16.0f
        private const val NOTIFICATION_CHANNEL_ID = "MotionDetectionChannel"
        private const val NOTIFICATION_ID = 2
    }
}
