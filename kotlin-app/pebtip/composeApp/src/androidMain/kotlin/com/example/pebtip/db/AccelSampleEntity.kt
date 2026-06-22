package com.example.pebtip.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pebtip.AccelRecord

@Entity(tableName = "accel_samples")
data class AccelSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val x: Short,
    val y: Short,
    val z: Short,
    val batch_timestamp: Long,
    val batteryLevel: Int = 0,
)

fun AccelSampleEntity.toRecord() = AccelRecord(
    id = id,
    x = x,
    y = y,
    z = z,
    batch_timestamp = batch_timestamp,
    batteryLevel = batteryLevel,
)

fun AccelRecord.toEntity() = AccelSampleEntity(
    id = id,
    x = x,
    y = y,
    z = z,
    batch_timestamp = batch_timestamp,
    batteryLevel = batteryLevel,
)