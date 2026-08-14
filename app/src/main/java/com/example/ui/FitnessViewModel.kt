package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FitnessRecordEntity
import com.example.data.models.ActivityPreset
import com.example.data.models.FitnessMetrics
import com.example.data.models.HeartRatePoint
import com.example.data.repository.FitnessRepository
import com.example.health.HealthConnectManager
import com.example.service.FitnessGeneratorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FitnessRepository.getInstance(application)
    val healthConnectManager = HealthConnectManager(application)

    val metrics: StateFlow<FitnessMetrics> = repository.metrics
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FitnessMetrics()
        )

    val heartRateHistory: StateFlow<List<HeartRatePoint>> = repository.heartRateHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historyRecords: StateFlow<List<FitnessRecordEntity>> = repository.historyRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        checkHealthConnectPermissions()
    }

    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            val isSupported = healthConnectManager.isSupported()
            if (isSupported) {
                val hasPerms = healthConnectManager.hasAllPermissions()
                repository.setHealthConnectPermissionsGranted(hasPerms)
            } else {
                repository.updateMetrics {
                    it.copy(
                        healthConnectStatusMessage = healthConnectManager.getSdkStatusMessage()
                    )
                }
            }
        }
    }

    fun toggleGenerator(context: Context) {
        if (metrics.value.isRunning) {
            FitnessGeneratorService.stopService(context)
        } else {
            FitnessGeneratorService.startService(context)
        }
    }

    fun setActivityPreset(preset: ActivityPreset) {
        repository.setActivityPreset(preset)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        repository.setSpeedMultiplier(multiplier)
    }

    fun addManualSteps(steps: Int) {
        repository.addManualSteps(steps)
    }

    fun resetData() {
        repository.resetMetrics()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun toggleHealthConnectSync(enabled: Boolean) {
        repository.setHealthConnectSyncEnabled(enabled)
    }

    fun triggerManualSync(context: Context) {
        viewModelScope.launch {
            val current = metrics.value
            val success = healthConnectManager.writeFitnessBatch(
                stepsDelta = if (current.steps > 0) current.steps else 120,
                caloriesDelta = if (current.caloriesBurned > 0) current.caloriesBurned else 5.2,
                distanceMetersDelta = if (current.distanceMeters > 0) current.distanceMeters else 95.0,
                heartRateBpm = current.heartRateBpm,
                speedKmh = current.currentSpeedKmh,
                floorsDelta = if (current.floorsClimbed > 0) current.floorsClimbed else 1,
                intervalSeconds = 10
            )
            if (success) {
                repository.recordHealthConnectSyncSuccess(1)
            }
        }
    }

    fun generateCsvExport(): String {
        val current = metrics.value
        val history = historyRecords.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("Timestamp,Type,SessionName,Steps,CaloriesKcal,ActiveMinutes,AvgHeartRateBpm,DistanceKm\n")
        sb.append("${dateFormat.format(Date(current.lastUpdatedMillis))},CurrentLive,${current.currentPreset.displayName},${current.steps},${String.format(Locale.US, "%.2f", current.caloriesBurned)},${String.format(Locale.US, "%.2f", current.activeMinutes)},${current.heartRateBpm},${String.format(Locale.US, "%.3f", current.distanceKm)}\n")

        for (record in history) {
            sb.append("${dateFormat.format(Date(record.timestamp))},HistoryRecord,\"${record.sessionName}\",${record.totalSteps},${String.format(Locale.US, "%.2f", record.caloriesBurned)},${String.format(Locale.US, "%.2f", record.activeSeconds / 60.0)},${record.avgHeartRate},${String.format(Locale.US, "%.3f", record.distanceMeters / 1000.0)}\n")
        }
        return sb.toString()
    }

    fun generateJsonExport(): String {
        val current = metrics.value
        val history = historyRecords.value

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"Fit Data Generator\",\n")
        sb.append("  \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())}\",\n")
        sb.append("  \"currentMetrics\": {\n")
        sb.append("    \"steps\": ${current.steps},\n")
        sb.append("    \"caloriesKcal\": ${String.format(Locale.US, "%.2f", current.caloriesBurned)},\n")
        sb.append("    \"activeMinutes\": ${String.format(Locale.US, "%.2f", current.activeMinutes)},\n")
        sb.append("    \"heartRateBpm\": ${current.heartRateBpm},\n")
        sb.append("    \"distanceKm\": ${String.format(Locale.US, "%.3f", current.distanceKm)},\n")
        sb.append("    \"currentSpeedKmh\": ${String.format(Locale.US, "%.2f", current.currentSpeedKmh)},\n")
        sb.append("    \"preset\": \"${current.currentPreset.displayName}\"\n")
        sb.append("  },\n")
        sb.append("  \"historyRecords\": [\n")
        for ((idx, record) in history.withIndex()) {
            sb.append("    {\n")
            sb.append("      \"timestamp\": ${record.timestamp},\n")
            sb.append("      \"session\": \"${record.sessionName}\",\n")
            sb.append("      \"steps\": ${record.totalSteps},\n")
            sb.append("      \"caloriesKcal\": ${String.format(Locale.US, "%.2f", record.caloriesBurned)},\n")
            sb.append("      \"avgHeartRateBpm\": ${record.avgHeartRate},\n")
            sb.append("      \"distanceMeters\": ${String.format(Locale.US, "%.2f", record.distanceMeters)}\n")
            sb.append("    }${if (idx < history.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }
}
