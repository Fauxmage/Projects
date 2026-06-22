@file:OptIn(
    kotlin.uuid.ExperimentalUuidApi::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)

package com.example.pebtip.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pebtip.AccelRecord
import com.example.pebtip.AccelSample
import com.example.pebtip.PebtipApplication
import com.example.pebtip.pebble.PebbleController
import io.rebble.libpebblecommon.connection.ConnectedPebbleDevice
import io.rebble.libpebblecommon.connection.CustomDataLoggingSink
import io.rebble.libpebblecommon.packets.AppMessage
import io.rebble.libpebblecommon.services.appmessage.AppMessageData
import io.rebble.libpebblecommon.services.appmessage.AppMessageResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.uuid.Uuid

class PebbleBackgroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        val app = application as PebtipApplication
        val accelSink = CustomDataLoggingSink { event ->
            if (event.appUuid != DATALOG_APP_UUID && event.appUuid != APPMSG_APP_UUID) return@CustomDataLoggingSink
            if (event.tag !in ACCEL_TAGS) return@CustomDataLoggingSink
            if (event.data.size % DATALOG_PACKET_SIZE_64_WITH_CTR != 0) return@CustomDataLoggingSink

            val buf = ByteBuffer.wrap(event.data).order(ByteOrder.LITTLE_ENDIAN)
            while (buf.remaining() >= DATALOG_PACKET_SIZE_64_WITH_CTR) {
                val batteryLevel = buf.get().toInt() and 0xFF
                val batchTimestamp = buf.long
                val records = ArrayList<AccelRecord>(SAMPLES_PER_BATCH)
                repeat(SAMPLES_PER_BATCH) {
                    val x = buf.short
                    val y = buf.short
                    val z = buf.short
                    app.watchSessionManager.processIncomingData(AccelSample(x, y, z))
                    records.add(
                        AccelRecord(
                            x = x,
                            y = y,
                            z = z,
                            batch_timestamp = batchTimestamp,
                            batteryLevel = batteryLevel,
                        ),
                    )
                }
                app.sensorRepository.recordBatch(records.size)
                withContext(Dispatchers.IO) {
                    app.accelStorage.insertAll(records)
                }
            }
        }

        serviceScope.launch {
            PebbleController.libPebble.collectLatest { libPebble ->
                if (libPebble == null) return@collectLatest
                libPebble.setDataSink(accelSink)
                try {
                    awaitCancellation()
                } finally {
                    libPebble.setDataSink(null)
                }
            }
        }

        serviceScope.launch {
            PebbleController.libPebble
                .filterNotNull()
                .flatMapLatest { libPebble ->
                    libPebble.watches.flatMapLatest { devices ->
                        val appMessageFlows = devices
                            .filterIsInstance<ConnectedPebbleDevice>()
                            .map { device -> deviceAppMessageFlow(device) }

                        if (appMessageFlows.isEmpty()) {
                            emptyFlow()
                        } else {
                            merge(*appMessageFlows.toTypedArray())
                        }
                    }
                }
                .collect { (device, msg) ->
                    handleAppMessage(device, msg, app)
                }
        }
    }

private suspend fun handleAppMessage(
        device: ConnectedPebbleDevice,
        msg: AppMessageData,
        app: PebtipApplication,
    ) {
        val bTstampBytes = (msg.data[KEY_B_TSTAMP] as? UByteArray)?.toByteArray()
        val accelDataUBytes = msg.data[KEY_SAMPLES] as? UByteArray

        if (bTstampBytes == null || accelDataUBytes == null) {
            device.sendAppMessageResult(AppMessageResult.NACK(msg.transactionId))
            return
        }

        val accelData = accelDataUBytes.toByteArray()
        if (accelData.size < SAMPLE_SIZE || accelData.size % SAMPLE_SIZE != 0) {
            device.sendAppMessageResult(AppMessageResult.NACK(msg.transactionId))
            return
        }

        val batchTimestamp = ByteBuffer.wrap(bTstampBytes).order(ByteOrder.LITTLE_ENDIAN).long
        val batteryLevel = (msg.data[KEY_BATRY_LVL] as? ULong)?.toInt() ?: 0

        val buf = ByteBuffer.wrap(accelData).order(ByteOrder.LITTLE_ENDIAN)
        val records = ArrayList<AccelRecord>(accelData.size / SAMPLE_SIZE)
        repeat(accelData.size / SAMPLE_SIZE) {
            val x = buf.short
            val y = buf.short
            val z = buf.short
            app.watchSessionManager.processIncomingData(AccelSample(x, y, z))
            records.add(
                AccelRecord(
                    x = x,
                    y = y,
                    z = z,
                    batch_timestamp = batchTimestamp,
                    batteryLevel = batteryLevel,
                ),
            )
        }

        if (records.isEmpty()) {
            device.sendAppMessageResult(AppMessageResult.NACK(msg.transactionId))
            return
        }

        app.sensorRepository.recordBatch(records.size)
        app.accelStorage.insertAll(records)
        device.sendAppMessageResult(AppMessageResult.ACK(msg.transactionId))
    }

    private fun deviceAppMessageFlow(
        device: ConnectedPebbleDevice,
    ): Flow<Pair<ConnectedPebbleDevice, AppMessageData>> =
        device.inboundMessages
            .filterIsInstance<AppMessage.AppMessagePush>()
            .filter { it.uuid.get() == APPMSG_APP_UUID }
            .map { packet ->
                val dataMap = HashMap<Int, Any>()
                for (tuple in packet.dictionary) {
                    dataMap[tuple.key.get().toInt()] = tuple.getTypedData()
                }
                Pair(
                    device,
                    AppMessageData(
                        transactionId = packet.transactionId.get(),
                        uuid = packet.uuid.get(),
                        data = dataMap,
                    ),
                )
            }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithType()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithType() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, PebtipApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Pebtip")
            .setContentText("Receiving accelerometer data via DataLogging/AppMessage")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID   = 1
        private const val BATCH_BATTERY_LVL = 1
        private const val SAMPLES_PER_BATCH = 25
        private const val SAMPLE_SIZE       = 6
        private const val BATCH_NUM         = 4
        private const val DATALOG_TIMESTAMP_SIZE_32 = 4
        private const val DATALOG_TIMESTAMP_SIZE_64 = 8
        private const val DATALOG_PACKET_SIZE_32 = DATALOG_TIMESTAMP_SIZE_32 + SAMPLES_PER_BATCH * SAMPLE_SIZE
        private const val DATALOG_PACKET_SIZE_64 = DATALOG_TIMESTAMP_SIZE_64 + SAMPLES_PER_BATCH * SAMPLE_SIZE
        private const val DATALOG_PACKET_SIZE_64_WITH_CTR = DATALOG_TIMESTAMP_SIZE_64 + SAMPLES_PER_BATCH * SAMPLE_SIZE + BATCH_BATTERY_LVL
        private val DATALOG_APP_UUID = Uuid.parse("35221a7e-8e56-421f-a5b2-3d87e515b1df")
        private val APPMSG_APP_UUID  = Uuid.parse("35c16ba3-a941-4bbd-a7fe-f0ed34c06ef1")
        private val ACCEL_TAGS: Set<UInt> = setOf(0x0091u, 0x0092u)

        private const val KEY_B_TSTAMP     = 10000
        private const val KEY_BATRY_LVL    = 10002
        private const val KEY_SAMPLE_COUNT = 10003
        private const val KEY_SAMPLES      = 10004
    }
}
