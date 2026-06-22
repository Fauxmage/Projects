package com.example.pebtip.ui.screens.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pebtip.AccelRecord
import com.example.pebtip.AccelStorage
import com.example.pebtip.SensorRepository
import com.example.pebtip.upload.StoredDataUploader
import com.example.pebtip.upload.UploadAllResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SAMPLES_PER_BATCH = 25

class DataViewModel(
    repository: SensorRepository,
    private val storage: AccelStorage,
    private val uploader: StoredDataUploader,
) : ViewModel() {

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus = _uploadStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val batchCount = repository.batchCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sampleCount = repository.sampleCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val storedCount = storage.totalCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val storedBatchCount = storedCount
        .map { samples -> samples / SAMPLES_PER_BATCH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val storedSampleCount = storedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentSamples = storage.recentSamples
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<AccelRecord>())

    val latestBatteryLevel = storage.recentSamples
        .map { samples -> samples.firstOrNull()?.batteryLevel }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun uploadAllStoredData() {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                val result: UploadAllResult = uploader.uploadAllStoredData()
                _uploadStatus.value =
                    if (result.failedRequests == 0) {
                        "Uploaded ${result.uploadedBatches} batches (${result.uploadedSamples} samples)."
                    } else {
                        "Partial upload: ${result.uploadedBatches} batches uploaded; ${result.failedRequests} request(s) failed."
                    }
            } catch (e: Exception) {
                _uploadStatus.value = "Upload failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isUploading.value = false
            }
        }
    }
}
