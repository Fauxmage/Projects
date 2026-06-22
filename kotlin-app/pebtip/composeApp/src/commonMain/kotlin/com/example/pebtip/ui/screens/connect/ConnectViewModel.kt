package com.example.pebtip.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pebtip.getLocalIpAddress
import com.example.pebtip.getPlatform
import com.example.pebtip.pebble.PebbleController
import io.rebble.libpebblecommon.connection.ConnectingPebbleDevice
import io.rebble.libpebblecommon.connection.ConnectedPebbleDevice
import io.rebble.libpebblecommon.connection.DisconnectingPebbleDevice
import io.rebble.libpebblecommon.connection.KnownPebbleDevice
import io.rebble.libpebblecommon.connection.PebbleConnectionEvent
import io.rebble.libpebblecommon.connection.PebbleDevice
import io.rebble.libpebblecommon.connection.bt.BluetoothState
import io.rebble.libpebblecommon.LibPebbleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class DeviceConnectionUiState {
    DISCOVERED,
    KNOWN,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
}

data class DeviceUiModel(
    val id: String,
    val name: String,
    val state: DeviceConnectionUiState,
    val details: String,
    val failure: String?,
    val canForget: Boolean = false,
    val devConnectionActive: Boolean,
    val devConnectionBusy: Boolean,
    val removeBusy: Boolean = false,
    val removeError: String? = null,
    val device: PebbleDevice,
)

data class ConnectUiState(
    val isInitialized: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isScanningClassic: Boolean = false,
    val supportsClassicScan: Boolean = false,
    val lanDevConnectionEnabled: Boolean = false,
    val supportsDevConnectionControls: Boolean = false,
    val devices: List<DeviceUiModel> = emptyList(),
    val lastEvent: String? = null,
    val bluetoothEvent: String? = null,
)

private data class BaseConnectSnapshot(
    val isScanning: Boolean,
    val isScanningClassic: Boolean,
    val bluetoothState: BluetoothState,
    val config: LibPebbleConfig,
    val devices: List<PebbleDevice>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModel : ViewModel() {
    private val supportsDevConnectionControls = getPlatform().name.startsWith("Android")
    val supportsClassicScan = getPlatform().name.startsWith("Android")
    val localIpAddress: String = getLocalIpAddress()
    private val lastEvent = MutableStateFlow<String?>(null)
    private val devConnectionBusyIds = MutableStateFlow<Set<String>>(emptySet())
    private val removeBusyIds = MutableStateFlow<Set<String>>(emptySet())
    private val removeErrors = MutableStateFlow<Map<String, String>>(emptyMap())

    val uiState: StateFlow<ConnectUiState> = PebbleController.libPebble
        .flatMapLatest { pebbleApi ->
            if (pebbleApi == null) {
                flowOf(ConnectUiState())
            } else {
                val devConnectionStates = pebbleApi.watches.flatMapLatest { devices ->
                    val connectedDevices = devices.filterIsInstance<ConnectedPebbleDevice>()
                    if (connectedDevices.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        combine(connectedDevices.map { it.devConnectionActive }) { states ->
                            connectedDevices.mapIndexed { index, device ->
                                device.identifier.asString to states[index]
                            }.toMap()
                        }
                    }
                }

                val baseSnapshot = combine(
                    pebbleApi.isScanningBle,
                    pebbleApi.isScanningClassic,
                    pebbleApi.bluetoothEnabled,
                    pebbleApi.config,
                    pebbleApi.watches,
                ) { isScanningBle, isScanningClassic, bluetoothState, config, devices ->
                    BaseConnectSnapshot(
                        isScanning = isScanningBle,
                        isScanningClassic = isScanningClassic,
                        bluetoothState = bluetoothState,
                        config = config,
                        devices = devices,
                    )
                }

                val withDevState = combine(
                    baseSnapshot,
                    devConnectionStates,
                    devConnectionBusyIds,
                ) { snapshot, devActiveMap, devBusyIds ->
                    Triple(snapshot, devActiveMap, devBusyIds)
                }

                val withRemoveBusy = combine(withDevState, removeBusyIds) { devState, removeBusy ->
                    devState to removeBusy
                }

                val withRemoveErrors = combine(withRemoveBusy, removeErrors) { stateWithBusy, errors ->
                    stateWithBusy to errors
                }

                combine(withRemoveErrors, lastEvent) { stateWithErrors, event ->
                    val stateWithBusy = stateWithErrors.first
                    val errors = stateWithErrors.second
                    val devState = stateWithBusy.first
                    val removeBusy = stateWithBusy.second
                    val snapshot = devState.first
                    val devActiveMap = devState.second
                    val devBusyIds = devState.third

                    ConnectUiState(
                        isInitialized = true,
                        bluetoothEnabled = snapshot.bluetoothState == BluetoothState.Enabled,
                        isScanning = snapshot.isScanning || snapshot.isScanningClassic,
                        isScanningClassic = snapshot.isScanningClassic,
                        supportsClassicScan = supportsClassicScan,
                        lanDevConnectionEnabled = snapshot.config.watchConfig.lanDevConnection,
                        supportsDevConnectionControls = supportsDevConnectionControls,
                        devices = snapshot.devices
                            .map { device ->
                                device.toUiModel(
                                    devConnectionActive = devActiveMap[device.identifier.asString] == true,
                                    devConnectionBusy = devBusyIds.contains(device.identifier.asString),
                                    removeBusy = removeBusy.contains(device.identifier.asString),
                                    removeError = errors[device.identifier.asString],
                                )
                            }
                            .sortedBy { it.state.sortOrder() },
                        lastEvent = event,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectUiState())


    init {
        viewModelScope.launch {
            PebbleController.libPebble
                .flatMapLatest { pebbleApi ->
                    pebbleApi?.connectionEvents ?: flowOf()
                }
                .collect { event ->
                    val message = when (event) {
                        is PebbleConnectionEvent.PebbleConnectedEvent -> {
                            "Connected: ${event.device.displayName()}"
                        }

                        is PebbleConnectionEvent.PebbleDisconnectedEvent -> {
                            "Disconnected: ${event.identifier.asString}"
                        }
                    }
                    lastEvent.value = message
                }
        }
    }

    fun startScan() {
        val pebbleApi = PebbleController.libPebble.value
        pebbleApi?.startBleScan()
        if (supportsClassicScan) {
            pebbleApi?.startClassicScan()
        }
    }

    fun startClassicScan() {
        PebbleController.libPebble.value?.startClassicScan()
    }

    fun stopScan() {
        PebbleController.libPebble.value?.stopBleScan()
        PebbleController.libPebble.value?.stopClassicScan()
    }

    fun stopClassicScan() {
        PebbleController.libPebble.value?.stopClassicScan()
    }

    fun requestRemoveDevice(device: DeviceUiModel) {
        requestRemoveDevice(device.device)
    }

    fun requestRemoveDevice(device: PebbleDevice) {
        val knownDevice = device as? KnownPebbleDevice ?: return
        launchRemoveAction(knownDevice.identifier.asString) {
            knownDevice.forget()
            lastEvent.value = "Removed: ${knownDevice.displayName()}"
        }
    }

    // Compatibility wrapper for existing UI call sites.
    fun forgetDevice(device: PebbleDevice) {
        requestRemoveDevice(device)
    }

    fun clearRemoveError(deviceId: String) {
        removeErrors.update { errors -> errors - deviceId }
    }

    fun onDevicePrimaryAction(device: PebbleDevice) {
        when (device) {
            is ConnectedPebbleDevice -> device.disconnect()
            is ConnectingPebbleDevice -> device.disconnect()
            else -> device.connect()
        }
    }

    fun onStartDevConnection(device: DeviceUiModel) {
        val connectedDevice = device.device as? ConnectedPebbleDevice ?: return
        launchDevConnectionAction(connectedDevice.identifier.asString) {
            connectedDevice.startDevConnection()
        }
    }

    fun onStopDevConnection(device: DeviceUiModel) {
        val connectedDevice = device.device as? ConnectedPebbleDevice ?: return
        launchDevConnectionAction(connectedDevice.identifier.asString) {
            connectedDevice.stopDevConnection()
        }
    }

    private fun launchDevConnectionAction(identifier: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            devConnectionBusyIds.update { it + identifier }
            try {
                action()
            } catch (t: Throwable) {
                lastEvent.value = "Dev connection error: ${t.message ?: "unknown"}"
            } finally {
                devConnectionBusyIds.update { it - identifier }
            }
        }
    }

    private fun launchRemoveAction(identifier: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            removeBusyIds.update { it + identifier }
            removeErrors.update { it - identifier }
            try {
                action()
            } catch (t: Throwable) {
                val message = t.message ?: "unknown"
                removeErrors.update { it + (identifier to message) }
                lastEvent.value = "Remove error: $message"
            } finally {
                removeBusyIds.update { it - identifier }
            }
        }
    }

    private fun PebbleDevice.toUiModel(
        devConnectionActive: Boolean,
        devConnectionBusy: Boolean,
        removeBusy: Boolean,
        removeError: String?,
    ): DeviceUiModel {
        val uiState = when (this) {
            is ConnectedPebbleDevice -> DeviceConnectionUiState.CONNECTED
            is ConnectingPebbleDevice -> DeviceConnectionUiState.CONNECTING
            is DisconnectingPebbleDevice -> DeviceConnectionUiState.DISCONNECTING
            is KnownPebbleDevice -> DeviceConnectionUiState.KNOWN
            else -> DeviceConnectionUiState.DISCOVERED
        }

        val details = when (this) {
            is ConnectedPebbleDevice -> {
                val platform = watchInfo.platform.name
                val battery = batteryLevel?.let { " | Battery $it%" } ?: ""
                "$platform$battery"
            }

            is ConnectingPebbleDevice -> {
                if (negotiating) "Negotiating..." else "Connecting..."
            }

            is KnownPebbleDevice -> "Known watch (${watchType.name})"
            else -> identifier.asString
        }

        val failureText = connectionFailureInfo?.let { "${it.reason} (${it.times})" }
        return DeviceUiModel(
            id = identifier.asString,
            name = displayName().ifBlank { name.ifBlank { "Unknown Pebble" } },
            state = uiState,
            details = details,
            failure = failureText,
            canForget = this is KnownPebbleDevice,
            devConnectionActive = devConnectionActive,
            devConnectionBusy = devConnectionBusy,
            removeBusy = removeBusy,
            removeError = removeError,
            device = this,
        )
    }

    private fun DeviceConnectionUiState.sortOrder(): Int = when (this) {
        DeviceConnectionUiState.CONNECTED -> 0
        DeviceConnectionUiState.CONNECTING -> 1
        DeviceConnectionUiState.DISCONNECTING -> 2
        DeviceConnectionUiState.KNOWN -> 3
        DeviceConnectionUiState.DISCOVERED -> 4
    }
}