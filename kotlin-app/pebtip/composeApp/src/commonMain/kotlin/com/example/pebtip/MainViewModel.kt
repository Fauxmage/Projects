package com.example.pebtip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn

class MainViewModel(repository: SensorRepository) : ViewModel() {

    @OptIn(FlowPreview::class)
    val accel = repository.accelData
        .sample(1_000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))
}
