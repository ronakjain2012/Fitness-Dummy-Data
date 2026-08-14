package com.example.sensor

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object FitnessSensorBroadcastManager {

    // Action constants for Global and Local broadcasts
    const val ACTION_FITNESS_UPDATE = "com.example.fitness.ACTION_FITNESS_DATA_UPDATE"
    const val ACTION_STEP_TICK = "com.example.action.STEP_COUNTER_TICK"
    const val ACTION_HEART_RATE_TICK = "com.example.action.HEART_RATE_TICK"
    const val ACTION_FITNESS_RECORD_UPDATE = "com.google.android.gms.fitness.RECORD_UPDATE"
    const val ACTION_FITNESS_METRICS_SYNC = "com.example.fitness.ACTION_METRICS_SYNC"

    // Extra keys
    const val EXTRA_TOTAL_STEPS = "extra_total_steps"
    const val EXTRA_STEP_DELTA = "extra_step_delta"
    const val EXTRA_HEART_RATE_BPM = "extra_heart_rate_bpm"
    const val EXTRA_CALORIES_KCAL = "extra_calories_kcal"
    const val EXTRA_DISTANCE_METERS = "extra_distance_meters"
    const val EXTRA_SPEED_KMH = "extra_speed_kmh"
    const val EXTRA_CADENCE_SPM = "extra_cadence_spm"
    const val EXTRA_FLOORS_CLIMBED = "extra_floors_climbed"
    const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"

    private const val TAG = "FitnessBroadcast"

    fun broadcastSensorData(
        context: Context,
        totalSteps: Int,
        stepDelta: Int,
        heartRateBpm: Int,
        caloriesKcal: Double,
        distanceMeters: Double,
        speedKmh: Double,
        cadenceSpm: Int,
        floorsClimbed: Int
    ) {
        val now = System.currentTimeMillis()

        // 1. Comprehensive Intent
        val updateIntent = Intent(ACTION_FITNESS_UPDATE).apply {
            putExtra(EXTRA_TOTAL_STEPS, totalSteps)
            putExtra(EXTRA_STEP_DELTA, stepDelta)
            putExtra(EXTRA_HEART_RATE_BPM, heartRateBpm)
            putExtra(EXTRA_CALORIES_KCAL, caloriesKcal)
            putExtra(EXTRA_DISTANCE_METERS, distanceMeters)
            putExtra(EXTRA_SPEED_KMH, speedKmh)
            putExtra(EXTRA_CADENCE_SPM, cadenceSpm)
            putExtra(EXTRA_FLOORS_CLIMBED, floorsClimbed)
            putExtra(EXTRA_TIMESTAMP_MILLIS, now)
            // also support short keys for convenience in testing scripts
            putExtra("steps", totalSteps)
            putExtra("step_delta", stepDelta)
            putExtra("bpm", heartRateBpm)
            putExtra("calories", caloriesKcal)
            putExtra("distance", distanceMeters)
            putExtra("speed", speedKmh)
            putExtra("cadence", cadenceSpm)
            putExtra("floors", floorsClimbed)
            putExtra("timestamp", now)
        }

        // --- LOCAL BROADCAST MANAGER (in-app listeners & test harnesses) ---
        try {
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
            Log.v(TAG, "Sent LocalBroadcastManager update: steps=$totalSteps (+$stepDelta), HR=$heartRateBpm bpm")
        } catch (e: Exception) {
            Log.e(TAG, "LocalBroadcastManager send failed: ${e.message}")
        }

        // --- SYSTEM GLOBAL BROADCASTS (external tester tools, ADB broadcast receivers) ---
        try {
            // Global fitness intent
            val globalIntent = Intent(updateIntent).apply {
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(globalIntent)

            // Step tick broadcast
            if (stepDelta > 0) {
                val stepIntent = Intent(ACTION_STEP_TICK).apply {
                    putExtra("steps", totalSteps)
                    putExtra("step_delta", stepDelta)
                    putExtra("cadence_spm", cadenceSpm)
                    putExtra("timestamp", now)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(stepIntent)
                LocalBroadcastManager.getInstance(context).sendBroadcast(stepIntent)
            }

            // Heart rate tick broadcast
            if (heartRateBpm > 0) {
                val hrIntent = Intent(ACTION_HEART_RATE_TICK).apply {
                    putExtra("bpm", heartRateBpm)
                    putExtra("timestamp", now)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(hrIntent)
                LocalBroadcastManager.getInstance(context).sendBroadcast(hrIntent)
            }

            // GMS / Google Fit companion broadcast format
            val gmsIntent = Intent(ACTION_FITNESS_RECORD_UPDATE).apply {
                putExtra("data_type", "type_step_count_delta")
                putExtra("steps", totalSteps)
                putExtra("bpm", heartRateBpm)
                putExtra("calories", caloriesKcal)
                putExtra("timestamp", now)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(gmsIntent)
            LocalBroadcastManager.getInstance(context).sendBroadcast(gmsIntent)

        } catch (e: Exception) {
            Log.e(TAG, "System broadcast failed: ${e.message}")
        }
    }
}
