package com.example.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Velocity
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private fun getClient(): HealthConnectClient? {
        return try {
            if (isSupported()) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HealthConnectClient: ${e.message}")
            null
        }
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getWritePermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    fun isSupported(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            availability == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    fun getSdkStatusMessage(): String {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> "Health Connect Active (Ready to sync to Google Fit / OS store)"
                HealthConnectClient.SDK_UNAVAILABLE -> "Health Connect OS Service not available on this emulator/ROM"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect Update Required from Play Store"
                else -> "Health Connect Ready"
            }
        } catch (e: Exception) {
            "Health Connect Ready"
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            permissions.all { it in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}")
            false
        }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = getClient() ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun writeFitnessBatch(
        stepsDelta: Int,
        caloriesDelta: Double,
        distanceMetersDelta: Double,
        heartRateBpm: Int,
        speedKmh: Double,
        floorsDelta: Int,
        intervalSeconds: Long = 5
    ): Boolean {
        val client = getClient() ?: run {
            Log.w(TAG, "Cannot write to Health Connect: client unavailable or OS SDK status is not available.")
            return false
        }
        return try {
            val now = Instant.now()
            val startTime = now.minus(intervalSeconds.coerceAtLeast(1), ChronoUnit.SECONDS)
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)

            val records = mutableListOf<androidx.health.connect.client.records.Record>()

            if (stepsDelta > 0) {
                records.add(
                    StepsRecord(
                        count = stepsDelta.toLong(),
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset
                    )
                )
            }

            if (heartRateBpm in 40..220) {
                records.add(
                    HeartRateRecord(
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset,
                        samples = listOf(
                            HeartRateRecord.Sample(
                                time = now,
                                beatsPerMinute = heartRateBpm.toLong()
                            )
                        )
                    )
                )
            }

            if (caloriesDelta > 0.01) {
                records.add(
                    TotalCaloriesBurnedRecord(
                        energy = Energy.kilocalories(caloriesDelta),
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset
                    )
                )
                records.add(
                    ActiveCaloriesBurnedRecord(
                        energy = Energy.kilocalories(caloriesDelta),
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset
                    )
                )
            }

            if (distanceMetersDelta > 0.1) {
                records.add(
                    DistanceRecord(
                        distance = Length.meters(distanceMetersDelta),
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset
                    )
                )
            }

            if (speedKmh > 0.1) {
                val speedMps = speedKmh / 3.6
                records.add(
                    SpeedRecord(
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset,
                        samples = listOf(
                            SpeedRecord.Sample(
                                time = now,
                                speed = Velocity.metersPerSecond(speedMps)
                            )
                        )
                    )
                )
            }

            if (floorsDelta > 0) {
                records.add(
                    ElevationGainedRecord(
                        elevation = Length.meters(floorsDelta * 3.0),
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset
                    )
                )
            }

            if (records.isNotEmpty()) {
                client.insertRecords(records)
                Log.d(TAG, "Successfully synced ${records.size} fitness records to Health Connect OS store.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing records to Health Connect: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "HealthConnectManager"
    }
}
