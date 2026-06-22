package com.example.pebtip.pebble

import io.rebble.libpebblecommon.connection.LibPebble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PebbleController {
    private val _libPebble = MutableStateFlow<LibPebble?>(null)
    val libPebble: StateFlow<LibPebble?> = _libPebble.asStateFlow()

    fun initialize(instance: LibPebble) {
        if (_libPebble.value == null) {
            _libPebble.value = instance
            instance.init()
        }
    }
}