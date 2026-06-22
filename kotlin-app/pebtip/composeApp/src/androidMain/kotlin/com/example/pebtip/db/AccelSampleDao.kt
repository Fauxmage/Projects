package com.example.pebtip.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccelSampleDao {

    @Query("SELECT COUNT(*) FROM accel_samples")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM accel_samples ORDER BY id DESC LIMIT 20")
    fun observeRecent(): Flow<List<AccelSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<AccelSampleEntity>)

    @Query("SELECT DISTINCT batch_timestamp FROM accel_samples ORDER BY batch_timestamp ASC LIMIT :limit")
    suspend fun getOldestBatchTimestamps(limit: Int): List<Long>

    @Query("SELECT * FROM accel_samples WHERE batch_timestamp IN (:batchTimestamps) ORDER BY batch_timestamp ASC, id ASC")
    suspend fun getByBatchTimestamps(batchTimestamps: List<Long>): List<AccelSampleEntity>

    @Query("DELETE FROM accel_samples WHERE batch_timestamp IN (:batchTimestamps)")
    suspend fun deleteByBatchTimestamps(batchTimestamps: List<Long>)

    @Query("SELECT COUNT(*) FROM accel_samples")
    suspend fun countAll(): Int

    @Query("DELETE FROM accel_samples")
    suspend fun clearAll()
}
