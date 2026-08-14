package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.models.ActivityPreset
import com.example.data.repository.FitnessRepository
import com.example.health.HealthConnectManager
import com.example.sensor.FitnessSensorBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

class FitnessGeneratorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var generatorJob: Job? = null
    private lateinit var repository: FitnessRepository
    private lateinit var healthConnectManager: HealthConnectManager

    private var fractionalStepsAcc = 0.0
    private var fractionalFloorsAcc = 0.0
    private var sineAngle = 0.0
    private var tickCount = 0L

    // Accumulators for Health Connect delta batching (every 5 seconds)
    private var healthConnectStepsAcc = 0
    private var healthConnectCaloriesAcc = 0.0
    private var healthConnectDistanceAcc = 0.0
    private var healthConnectFloorsAcc = 0

    override fun onCreate() {
        super.onCreate()
        repository = FitnessRepository.getInstance(applicationContext)
        healthConnectManager = HealthConnectManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startGeneration()
            ACTION_STOP -> stopGeneration()
            ACTION_MANUAL_SYNC_HEALTH_CONNECT -> {
                serviceScope.launch {
                    performManualHealthConnectSync()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun performManualHealthConnectSync() {
        val current = repository.metrics.value
        val steps = current.steps
        val calories = current.caloriesBurned
        val distance = current.distanceMeters
        val hr = current.heartRateBpm
        val speed = current.currentSpeedKmh
        val floors = current.floorsClimbed

        val success = healthConnectManager.writeFitnessBatch(
            stepsDelta = if (steps > 0) steps else 50,
            caloriesDelta = if (calories > 0) calories else 2.5,
            distanceMetersDelta = if (distance > 0) distance else 40.0,
            heartRateBpm = hr,
            speedKmh = speed,
            floorsDelta = if (floors > 0) floors else 1,
            intervalSeconds = 10
        )
        if (success) {
            repository.recordHealthConnectSyncSuccess(1)
        }
    }

    private fun startGeneration() {
        if (generatorJob?.isActive == true) return

        repository.updateMetrics { it.copy(isRunning = true) }

        val notification = buildNotification("Generating fitness metrics for OS...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("FitnessGeneratorService", "Could not start as foreground notification service: ${e.message}")
        }

        generatorJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                tickCount++

                val currentMetrics = repository.metrics.value
                val preset = currentMetrics.currentPreset
                val speedMult = currentMetrics.speedMultiplier
                val profile = currentMetrics.personProfile

                // Handle Daily Routine realistic variation phases (walk -> brisk -> short rest -> walk)
                val effectivePreset = if (preset == ActivityPreset.DAILY_ROUTINE) {
                    val phaseCycle = (tickCount / 25) % 4
                    when (phaseCycle) {
                        0L -> ActivityPreset.CASUAL_WALK
                        1L -> ActivityPreset.BRISK_WALK
                        2L -> ActivityPreset.CASUAL_WALK
                        else -> ActivityPreset.RESTING
                    }
                } else preset

                // Cadence & Steps calculation
                val cadenceSpm = (effectivePreset.baseCadenceSpm * speedMult + Random.nextInt(-3, 4)).toInt().coerceAtLeast(0)
                val stepsPerSec = cadenceSpm / 60.0
                fractionalStepsAcc += stepsPerSec

                val newStepsIncrement = fractionalStepsAcc.toInt()
                if (newStepsIncrement > 0) {
                    fractionalStepsAcc -= newStepsIncrement
                }

                // Stride & Distance calculation (~0.76m per stride with speed variation)
                val strideMeters = 0.76 * (0.85 + 0.15 * speedMult)
                val addedDistance = newStepsIncrement * strideMeters

                // MET-based Calories calculation: (MET * 3.5 * weightKg / 200) / 60 per second
                val met = effectivePreset.metValue * speedMult
                val caloriesPerSec = (met * 3.5 * profile.weightKg / 200.0) / 60.0

                // Stairs / Floors climbed (1 floor per ~18 steps when walking/running)
                if (effectivePreset != ActivityPreset.RESTING && newStepsIncrement > 0) {
                    fractionalFloorsAcc += (newStepsIncrement / 18.0) * (0.15 * speedMult)
                }
                val newFloorsIncrement = fractionalFloorsAcc.toInt()
                if (newFloorsIncrement > 0) {
                    fractionalFloorsAcc -= newFloorsIncrement
                }

                // Heart rate simulation (Healthy human dynamics with smooth variation)
                sineAngle += 0.12
                val sineVariation = sin(sineAngle) * 3.5
                val noise = Random.nextDouble(-1.5, 1.5)
                val targetBpm = (effectivePreset.baseHeartRateBpm * (0.88 + 0.12 * speedMult)) + sineVariation + noise

                val currentBpm = currentMetrics.heartRateBpm
                val smoothedBpm = if (currentBpm == 0) targetBpm.toInt() else {
                    val diff = targetBpm - currentBpm
                    (currentBpm + diff * 0.18).toInt().coerceIn(52, 190)
                }

                // Speed calculation
                val speedKmh = if (effectivePreset == ActivityPreset.RESTING) 0.0 else {
                    (effectivePreset.baseSpeedKmh * speedMult + Random.nextDouble(-0.2, 0.2)).coerceAtLeast(0.0)
                }

                val updatedSteps = currentMetrics.steps + newStepsIncrement
                val updatedCalories = currentMetrics.caloriesBurned + caloriesPerSec
                val updatedDistance = currentMetrics.distanceMeters + addedDistance
                val updatedActiveSeconds = currentMetrics.activeSeconds + (if (effectivePreset != ActivityPreset.RESTING || newStepsIncrement > 0) 1 else 0)
                val updatedFloors = currentMetrics.floorsClimbed + newFloorsIncrement

                val minHr = if (currentMetrics.minHeartRate == 0) smoothedBpm else minOf(currentMetrics.minHeartRate, smoothedBpm)
                val maxHr = maxOf(currentMetrics.maxHeartRate, smoothedBpm)

                repository.updateMetrics {
                    it.copy(
                        steps = updatedSteps,
                        caloriesBurned = updatedCalories,
                        distanceMeters = updatedDistance,
                        activeSeconds = updatedActiveSeconds,
                        floorsClimbed = updatedFloors,
                        heartRateBpm = smoothedBpm,
                        currentSpeedKmh = speedKmh,
                        currentCadenceSpm = if (newStepsIncrement > 0) cadenceSpm else 0,
                        minHeartRate = minHr,
                        maxHeartRate = maxHr,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                }

                repository.addHeartRatePoint(smoothedBpm)

                // 1. Dispatch Android Standard Broadcast Intents for testers, OS layer & companion apps
                FitnessSensorBroadcastManager.broadcastSensorData(
                    context = applicationContext,
                    totalSteps = updatedSteps,
                    stepDelta = newStepsIncrement,
                    heartRateBpm = smoothedBpm,
                    caloriesKcal = updatedCalories,
                    distanceMeters = updatedDistance,
                    speedKmh = speedKmh,
                    cadenceSpm = if (newStepsIncrement > 0) cadenceSpm else 0,
                    floorsClimbed = updatedFloors
                )

                // 2. Accumulate for Health Connect delta batching
                healthConnectStepsAcc += newStepsIncrement
                healthConnectCaloriesAcc += caloriesPerSec
                healthConnectDistanceAcc += addedDistance
                healthConnectFloorsAcc += newFloorsIncrement

                // 3. Periodic Live Health Connect Sync (Every 5 seconds)
                if (tickCount % 5L == 0L && currentMetrics.isHealthConnectSyncEnabled) {
                    if (healthConnectStepsAcc > 0 || healthConnectCaloriesAcc > 0.1 || healthConnectDistanceAcc > 1.0 || smoothedBpm > 0) {
                        val batchSteps = healthConnectStepsAcc
                        val batchCals = healthConnectCaloriesAcc
                        val batchDist = healthConnectDistanceAcc
                        val batchFloors = healthConnectFloorsAcc

                        serviceScope.launch {
                            val success = healthConnectManager.writeFitnessBatch(
                                stepsDelta = batchSteps,
                                caloriesDelta = batchCals,
                                distanceMetersDelta = batchDist,
                                heartRateBpm = smoothedBpm,
                                speedKmh = speedKmh,
                                floorsDelta = batchFloors,
                                intervalSeconds = 5
                            )
                            if (success) {
                                repository.recordHealthConnectSyncSuccess(1)
                            }
                        }

                        // Reset accumulators
                        healthConnectStepsAcc = 0
                        healthConnectCaloriesAcc = 0.0
                        healthConnectDistanceAcc = 0.0
                        healthConnectFloorsAcc = 0
                    }
                }

                // Periodic auto-snapshot to Room DB every 60 seconds
                if (tickCount % 60L == 0L) {
                    repository.saveSessionRecord()
                }

                // Update Foreground Notification every 2 seconds
                if (tickCount % 2L == 0L) {
                    val statusText = String.format(
                        Locale.getDefault(),
                        "%,d steps • %d BPM • %.1f kcal • %.2f km",
                        updatedSteps, smoothedBpm, updatedCalories, updatedDistance / 1000.0
                    )
                    updateNotification(statusText)
                }
            }
        }
    }

    private fun stopGeneration() {
        generatorJob?.cancel()
        generatorJob = null

        repository.updateMetrics { it.copy(isRunning = false, currentCadenceSpm = 0) }

        serviceScope.launch {
            repository.saveSessionRecord()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fitness Data Generator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background health and fitness metric generation status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FitnessGeneratorService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fit Data Generator (Generating Data)")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_media_pause, "Stop Generator", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopGeneration()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "fitness_generator_channel"
        const val NOTIFICATION_ID = 8801

        const val ACTION_START = "com.example.action.START_GENERATOR"
        const val ACTION_STOP = "com.example.action.STOP_GENERATOR"
        const val ACTION_MANUAL_SYNC_HEALTH_CONNECT = "com.example.action.MANUAL_SYNC_HEALTH_CONNECT"

        fun triggerManualSync(context: Context) {
            val intent = Intent(context, FitnessGeneratorService::class.java).apply {
                action = ACTION_MANUAL_SYNC_HEALTH_CONNECT
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("FitnessGeneratorService", "Failed to trigger manual sync: ${e.message}")
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, FitnessGeneratorService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                try {
                    context.startService(intent)
                } catch (ex: Exception) {
                    android.util.Log.e("FitnessGeneratorService", "Failed to start service: ${ex.message}")
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FitnessGeneratorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
