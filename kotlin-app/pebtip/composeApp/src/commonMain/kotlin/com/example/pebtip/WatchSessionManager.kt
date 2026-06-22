package com.example.pebtip

class WatchSessionManager(
    private val repository: SensorRepository,
) {
    fun processIncomingData(sample: AccelSample) {
        repository.updateAccel(sample.x.toInt(), sample.y.toInt(), sample.z.toInt())
    }
}