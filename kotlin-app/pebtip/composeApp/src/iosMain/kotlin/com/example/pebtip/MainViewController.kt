package com.example.pebtip

import androidx.compose.ui.window.ComposeUIViewController
import io.rebble.libpebblecommon.connection.*
import io.rebble.libpebblecommon.LibPebbleConfig
import io.rebble.libpebblecommon.BleConfig
import com.example.pebtip.pebble.*
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIViewController


fun MainViewController(): UIViewController {
    if (PebbleController.libPebble.value == null) {
        val instance = LibPebble3.create(
            defaultConfig = LibPebbleConfig(bleConfig = BleConfig(reversedPPoG = true)),
            webServices = NoOpWebServices,
            appContext = AppContext(),
            tokenProvider = NoOpTokenProvider,
            proxyTokenProvider = MutableStateFlow(null),
            transcriptionProvider = NoOpTranscriptionProvider
        )
        PebbleController.initialize(instance)
    }
    return ComposeUIViewController { App() }
}
