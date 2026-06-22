package com.example.pebtip
import kotlinx.coroutines.flow.SharedFlow

data class AccelSample(val x: Short, val y: Short, val z: Short)

data class AccelRecord(
    val id: Long = 0,
    val x: Short,
    val y: Short,
    val z: Short,
    val batch_timestamp: Long,
    val batteryLevel: Int = 0,
)

interface PebbleReceiver {
    val accelData: SharedFlow<AccelSample>
}