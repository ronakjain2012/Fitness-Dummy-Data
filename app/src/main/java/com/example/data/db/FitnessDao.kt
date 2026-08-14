package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {
    @Query("SELECT * FROM fitness_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<FitnessRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FitnessRecordEntity)

    @Query("DELETE FROM fitness_records")
    suspend fun clearAllRecords()
}
