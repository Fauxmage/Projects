package com.example.pebtip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorRepository {

    private val _accelData = MutableStateFlow(Triple(0, 0, 0))
    val accelData = _accelData.asStateFlow()

    private val _batchCount = MutableStateFlow(0)
    val batchCount = _batchCount.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount = _sampleCount.asStateFlow()

    fun updateAccel(x: Int, y: Int, z: Int) {
        _accelData.value = Triple(x, y, z)
    }

    fun recordBatch(numSamples: Int) {
        _batchCount.value += 1
        _sampleCount.value += numSamples
    }
}
