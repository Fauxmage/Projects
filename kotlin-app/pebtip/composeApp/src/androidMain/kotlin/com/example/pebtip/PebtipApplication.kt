package com.example.pebtip

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.pebtip.db.AccelDatabase
import com.example.pebtip.db.RoomAccelStorage
import com.example.pebtip.db.SafeAccelStorage
import com.example.pebtip.upload.BatchUploader
import com.example.pebtip.pebble.NoOpTokenProvider
import com.example.pebtip.pebble.NoOpTranscriptionProvider
import com.example.pebtip.pebble.NoOpWebServices
import com.example.pebtip.pebble.PebbleController
import io.rebble.libpebblecommon.connection.LibPebble3
import io.rebble.libpebblecommon.LibPebbleConfig
import io.rebble.libpebblecommon.connection.AppContext
import kotlinx.coroutines.flow.MutableStateFlow

class PebtipApplication : Application() {

    val sensorRepository = SensorRepository()
    val watchSessionManager by lazy { WatchSessionManager(sensorRepository) }
    private val database by lazy {
        AccelDatabase.create(this)
    }
    val accelStorage: AccelStorage by lazy {
        SafeAccelStorage(RoomAccelStorage(database.accelSampleDao()))
    }
    val batchUploader by lazy {
        BatchUploader(database.accelSampleDao())
    }

    override fun onCreate() {
        super.onCreate()
        val instance = LibPebble3.create(
            defaultConfig = LibPebbleConfig(),
            webServices = NoOpWebServices,
            appContext = AppContext(this),
            tokenProvider = NoOpTokenProvider,
            proxyTokenProvider = MutableStateFlow(null),
            transcriptionProvider = NoOpTranscriptionProvider
        )
        PebbleController.initialize(instance)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Pebble Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Pebble connection alive in the background"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pebtip_background"
    }
}