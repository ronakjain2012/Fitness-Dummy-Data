package com.example.data.repository

import android.content.Context
import com.example.data.db.FitnessDatabase
import com.example.data.db.FitnessRecordEntity
import com.example.data.models.ActivityPreset
import com.example.data.models.FitnessMetrics
import com.example.data.models.HeartRatePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FitnessRepository private constructor(context: Context) {

    private val db = FitnessDatabase.getInstance(context.applicationContext)
    private val dao = db.fitnessDao()

    private val _metrics = MutableStateFlow(FitnessMetrics())
    val metrics: StateFlow<FitnessMetrics> = _metrics.asStateFlow()

    private val _heartRateHistory = MutableStateFlow<List<HeartRatePoint>>(emptyList())
    val heartRateHistory: StateFlow<List<HeartRatePoint>> = _heartRateHistory.asStateFlow()

    val historyRecords: Flow<List<FitnessRecordEntity>> = dao.getAllRecords()

    fun updateMetrics(transform: (FitnessMetrics) -> FitnessMetrics) {
        _metrics.update { transform(it) }
    }

    fun addHeartRatePoint(bpm: Int) {
        val now = System.currentTimeMillis()
        val point = HeartRatePoint(now, bpm)
        _heartRateHistory.update { currentList ->
            val updated = currentList + point
            if (updated.size > 60) updated.takeLast(60) else updated
        }
    }

    fun setActivityPreset(preset: ActivityPreset) {
        _metrics.update { it.copy(currentPreset = preset) }
    }

    fun setSpeedMultiplier(multiplier: Float) {
        _metrics.update { it.copy(speedMultiplier = multiplier) }
    }

    fun addManualSteps(stepsToAdd: Int) {
        _metrics.update { current ->
            val newSteps = current.steps + stepsToAdd
            val addedCalories = stepsToAdd * 0.045 * current.speedMultiplier
            val addedDistance = stepsToAdd * 0.76 // ~0.76m average stride
            val addedFloors = (stepsToAdd / 180).coerceAtLeast(0) // ~180 steps per floor
            val newActiveSecs = current.activeSeconds + ((stepsToAdd / 100.0) * 60).toLong()

            current.copy(
                steps = newSteps,
                caloriesBurned = current.caloriesBurned + addedCalories,
                distanceMeters = current.distanceMeters + addedDistance,
                floorsClimbed = current.floorsClimbed + addedFloors,
                activeSeconds = newActiveSecs
            )
        }
    }

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        _metrics.update { it.copy(isHealthConnectSyncEnabled = enabled) }
    }

    fun setHealthConnectPermissionsGranted(granted: Boolean) {
        _metrics.update {
            it.copy(
                healthConnectHasPermissions = granted,
                healthConnectStatusMessage = if (granted) "Permissions granted • Sync active" else "Permissions required"
            )
        }
    }

    fun recordHealthConnectSyncSuccess(recordsWritten: Int) {
        _metrics.update {
            it.copy(
                healthConnectSyncedCount = it.healthConnectSyncedCount + recordsWritten,
                healthConnectLastSyncMillis = System.currentTimeMillis(),
                healthConnectStatusMessage = "Last synced ${recordsWritten} records to OS at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
            )
        }
    }

    fun resetMetrics() {
        _metrics.value = FitnessMetrics()
        _heartRateHistory.value = emptyList()
    }

    suspend fun saveSessionRecord() {
        val current = _metrics.value
        if (current.steps > 0 || current.activeSeconds > 0) {
            val record = FitnessRecordEntity(
                timestamp = System.currentTimeMillis(),
                sessionName = "${current.currentPreset.displayName} (${String.format("%.1fx", current.speedMultiplier)})",
                totalSteps = current.steps,
                caloriesBurned = current.caloriesBurned,
                activeSeconds = current.activeSeconds,
                avgHeartRate = current.heartRateBpm,
                distanceMeters = current.distanceMeters,
                floorsClimbed = current.floorsClimbed
            )
            dao.insertRecord(record)
        }
    }

    suspend fun clearHistory() {
        dao.clearAllRecords()
    }

    companion object {
        @Volatile
        private var INSTANCE: FitnessRepository? = null

        fun getInstance(context: Context): FitnessRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FitnessRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
