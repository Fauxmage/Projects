package com.example.pebtip.upload

data class UploadAllResult(
    val uploadedBatches: Int,
    val uploadedSamples: Int,
    val failedRequests: Int,
)

interface StoredDataUploader {
    suspend fun uploadAllStoredData(): UploadAllResult
}
