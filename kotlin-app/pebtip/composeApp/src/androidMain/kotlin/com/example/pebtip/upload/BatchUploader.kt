package com.example.pebtip.upload

import android.util.Log
import com.example.pebtip.db.AccelSampleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BatchUploader"
private const val POST_URL = "https://www.ubr004.xyz/accel"
private const val MAX_BATCHES_PER_REQUEST = 300

data class SamplePayload(val x: Int, val y: Int, val z: Int)

data class BatchPayload(
    val samples: List<SamplePayload>,
    val batchTimestamp: Long,
    val batteryLevel: Int,
)

class BatchUploader(
    private val accelSampleDao: AccelSampleDao,
) : StoredDataUploader {

    private val flushInProgress = AtomicBoolean(false)

    override suspend fun uploadAllStoredData(): UploadAllResult {
        if (!flushInProgress.compareAndSet(false, true)) {
            return UploadAllResult(uploadedBatches = 0, uploadedSamples = 0, failedRequests = 1)
        }

        try {
            return withContext(Dispatchers.IO) {
                uploadAllPages()
            }
        } finally {
            flushInProgress.set(false)
        }
    }

    private suspend fun uploadAllPages(): UploadAllResult {
        var uploadedBatches = 0
        var uploadedSamples = 0
        var failedRequests = 0

        while (true) {
            val batchTimestamps = accelSampleDao.getOldestBatchTimestamps(MAX_BATCHES_PER_REQUEST)
            if (batchTimestamps.isEmpty()) {
                return UploadAllResult(uploadedBatches, uploadedSamples, failedRequests)
            }

            val rows = accelSampleDao.getByBatchTimestamps(batchTimestamps)
            val grouped = rows.groupBy { it.batch_timestamp }.toSortedMap()
            val payloads = grouped.map { (_, samples) ->
                val first = samples.first()
                BatchPayload(
                    samples = samples.map { SamplePayload(it.x.toInt(), it.y.toInt(), it.z.toInt()) },
                    batchTimestamp = first.batch_timestamp,
                    batteryLevel = first.batteryLevel,
                )
            }

            if (payloads.isEmpty()) {
                accelSampleDao.deleteByBatchTimestamps(batchTimestamps)
                continue
            }

            val body = buildRequestBody(payloads)
            if (post(body)) {
                accelSampleDao.deleteByBatchTimestamps(batchTimestamps)
                uploadedBatches += payloads.size
                uploadedSamples += payloads.sumOf { it.samples.size }
            } else {
                failedRequests += 1
                return UploadAllResult(uploadedBatches, uploadedSamples, failedRequests)
            }
        }
    }

    private fun batchToJson(batch: BatchPayload): JSONObject {
        val samplesArray = JSONArray()
        for (s in batch.samples) {
            samplesArray.put(
                JSONObject().apply {
                    put("x", s.x)
                    put("y", s.y)
                    put("z", s.z)
                },
            )
        }

        return JSONObject().apply {
            put("samples", samplesArray)
            put("tstamp", batch.batchTimestamp)
            put("battery_level", batch.batteryLevel)
        }
    }

    private fun buildRequestBody(batches: List<BatchPayload>): String {
        val batchArray = JSONArray()
        batches.forEach { batchArray.put(batchToJson(it)) }
        return JSONObject().put("batches", batchArray).toString()
    }

    private fun post(body: String): Boolean {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(POST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code in 200..299) {
                Log.d(TAG, "Uploaded ${body.length} bytes — HTTP $code")
                return true
            } else {
                Log.w(TAG, "Upload failed — HTTP $code")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload error: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        return false
    }
}
