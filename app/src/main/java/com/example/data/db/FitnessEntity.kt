package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fitness_records")
data class FitnessRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionName: String,
    val totalSteps: Int,
    val caloriesBurned: Double,
    val activeSeconds: Long,
    val avgHeartRate: Int,
    val distanceMeters: Double,
    val floorsClimbed: Int = 0
)
