package com.example.speedsync

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class SpeedService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(0f))

        setupLocationUpdates()
        isRunning.value = true
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).setMinUpdateIntervalMillis(500L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val speedKmH = if (location.hasSpeed()) location.speed * 3.6f else 0f
                    currentSpeed.value = speedKmH
                    adjustVolume(speedKmH)
                    updateNotification(speedKmH)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun adjustVolume(speedKmH: Float) {
        val maxVolIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minVolIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else 0

        val minS = minSpeedThreshold
        val maxS = maxSpeedThreshold

        val ratio = when {
            speedKmH <= minS -> 0f
            speedKmH >= maxS -> 1f
            else -> (speedKmH - minS) / (maxS - minS)
        }

        val targetRatio = minVolRatio + ratio * (maxVolRatio - minVolRatio)
        val targetIndex = (minVolIndex + targetRatio * (maxVolIndex - minVolIndex)).roundToInt()

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SpeedSync Active Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(speedKmH: Float): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpeedSync Running")
            .setContentText("Current Speed: ${speedKmH.roundToInt()} km/h")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(speedKmH: Float) {
        val notification = createNotification(speedKmH)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "SpeedSyncChannel"
        const val NOTIFICATION_ID = 1001

        val currentSpeed = MutableStateFlow(0f)
        val isRunning = MutableStateFlow(false)

        var minSpeedThreshold = 15f
        var maxSpeedThreshold = 70f
        var minVolRatio = 0.3f
        var maxVolRatio = 1.0f
    }
}