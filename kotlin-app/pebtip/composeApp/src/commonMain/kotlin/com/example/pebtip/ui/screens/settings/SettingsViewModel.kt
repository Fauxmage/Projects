package com.example.pebtip.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pebtip.pebble.PebbleController
import io.rebble.libpebblecommon.LibPebbleConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SettingsUiState(
    val lanDevConnectionEnabled: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel{
    private val scope = CoroutineScope(Dispatchers.Main)
    val uiState: StateFlow<SettingsUiState> = PebbleController.libPebble
        .flatMapLatest { pebbleApi ->
            pebbleApi?.config?.map { config ->
                SettingsUiState(lanDevConnectionEnabled = config.watchConfig.lanDevConnection)
            } ?: flowOf(SettingsUiState())
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setLanDevConnection(enabled: Boolean) {
        val pebbleApi = PebbleController.libPebble.value ?: return
        val current = pebbleApi.config.value
        pebbleApi.updateConfig(
            current.copy(watchConfig = current.watchConfig.copy(lanDevConnection = enabled))
        )
    }
}
