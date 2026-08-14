package com.example.data.models

enum class HeartRateZone(val displayName: String, val minBpm: Int, val maxBpm: Int) {
    RESTING("Resting", 40, 90),
    WARM_UP("Warm-up", 90, 115),
    FAT_BURN("Fat Burn", 115, 135),
    AEROBIC("Cardio / Aerobic", 135, 155),
    ANAEROBIC("Anaerobic", 155, 175),
    PEAK("Peak Performance", 175, 220);

    companion object {
        fun fromBpm(bpm: Int): HeartRateZone {
            return values().firstOrNull { bpm in it.minBpm..it.maxBpm } ?: RESTING
        }
    }
}

enum class ActivityPreset(
    val displayName: String,
    val description: String,
    val baseCadenceSpm: Int,      // Steps per minute
    val baseHeartRateBpm: Int,    // Heart rate BPM
    val baseSpeedKmh: Double,     // Speed in km/h
    val metValue: Double          // MET (Metabolic Equivalent)
) {
    DAILY_ROUTINE("Realistic Daily Life", "Mimics standard healthy person (walking, desk, errands)", 90, 88, 3.8, 3.0),
    CASUAL_WALK("Casual Walk", "Relaxed park stroll or office walking", 85, 92, 4.2, 3.5),
    BRISK_WALK("Brisk Fitness Walk", "Paced active walking for cardio", 115, 110, 5.8, 4.8),
    JOGGING("Outdoor Jogging", "Steady aerobic endurance jog", 145, 138, 8.5, 7.5),
    RUNNING("Paced Running", "Vigorous running routine", 170, 158, 11.2, 11.0),
    HIIT("HIIT & Sprints", "High-intensity interval training bursts", 195, 172, 14.0, 13.5),
    RESTING("Rest & Recovery", "Sitting, desk work, resting BMR", 0, 68, 0.0, 1.2)
}

data class PersonProfile(
    val ageYears: Int = 28,
    val weightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val restingHeartRateBpm: Int = 65,
    val maxHeartRateBpm: Int = 192
)

data class FitnessMetrics(
    val isRunning: Boolean = false,
    val currentPreset: ActivityPreset = ActivityPreset.DAILY_ROUTINE,
    val speedMultiplier: Float = 1.0f,
    val steps: Int = 0,
    val caloriesBurned: Double = 0.0,
    val activeSeconds: Long = 0,
    val heartRateBpm: Int = 72,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val currentCadenceSpm: Int = 0,
    val floorsClimbed: Int = 0,
    val minHeartRate: Int = 72,
    val maxHeartRate: Int = 72,
    val personProfile: PersonProfile = PersonProfile(),
    val isHealthConnectSyncEnabled: Boolean = true,
    val healthConnectSyncedCount: Int = 0,
    val healthConnectLastSyncMillis: Long = 0L,
    val healthConnectHasPermissions: Boolean = false,
    val healthConnectStatusMessage: String = "Ready to sync with OS layer",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val activeMinutes: Double
        get() = activeSeconds / 60.0

    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val paceMinPerKm: Double
        get() = if (currentSpeedKmh > 0.5) 60.0 / currentSpeedKmh else 0.0

    val heartRateZone: HeartRateZone
        get() = HeartRateZone.fromBpm(heartRateBpm)
}

data class HeartRatePoint(
    val timestamp: Long,
    val bpm: Int
)
