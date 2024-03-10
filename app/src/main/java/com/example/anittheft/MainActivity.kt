package com.example.anittheft

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.anittheft.Services.ChargingStatusService
import com.example.anittheft.Services.MotionDetectionService
import com.example.anittheft.Services.PocketDetectionService
import com.example.anittheft.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pocketServiceIntent: Intent
    private lateinit var batteryServiceIntent: Intent
    private lateinit var motionServiceIntent: Intent
    private var isPocketServiceRunning = false
    private var isBatteryServiceRunning = false
    private var isMotionServiceRunning = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pocketServiceIntent = Intent(this, PocketDetectionService::class.java)
        batteryServiceIntent = Intent(this, ChargingStatusService::class.java)
        motionServiceIntent = Intent(this, MotionDetectionService::class.java)

        binding.startService.setOnClickListener {
            if (!isPocketServiceRunning) {
                // Start the PocketDetectionService
                startForegroundService(pocketServiceIntent)
                isPocketServiceRunning = true

                // Change button text
                binding.startService.text = getString(R.string.stop_service)
            } else {
                // Stop the PocketDetectionService
                stopService(pocketServiceIntent)
                isPocketServiceRunning = false

                // Change button text
                binding.startService.text = getString(R.string.start_service)
            }
        }

        binding.startServiceBattery.setOnClickListener {
            if (!isBatteryServiceRunning) {
                startService(batteryServiceIntent)
                isBatteryServiceRunning = true

                binding.startServiceBattery.text = getString(R.string.stop_service)
            } else {
                stopService(batteryServiceIntent)
                isBatteryServiceRunning = false

                binding.startServiceBattery.text = getString(R.string.start_service_battery)
            }
        }

        binding.detetctMotionService.setOnClickListener {
            if (!isMotionServiceRunning) {
                startService(motionServiceIntent)
                isMotionServiceRunning = true

                binding.detetctMotionService.text = getString(R.string.stop_service)
            } else {
                stopService(motionServiceIntent)
                isMotionServiceRunning = false

                binding.detetctMotionService.text = getString(R.string.detect_motion)
            }
        }

    }
}
