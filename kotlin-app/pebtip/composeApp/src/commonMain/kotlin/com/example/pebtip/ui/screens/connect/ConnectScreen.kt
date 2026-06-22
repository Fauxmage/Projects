package com.example.pebtip.ui.screens.connect

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel = remember { ConnectViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold() {
        paddingValues -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.isInitialized) {
                Text(
                    text = "Pebble service is still initializing...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ){
                Icon(
                    when {
                        !state.bluetoothEnabled -> Lucide.BluetoothOff
                        state.bluetoothEnabled -> Lucide.Bluetooth
                        else -> Lucide.CircleQuestionMark
                    },
                    contentDescription = "Bluetooth status",
                    tint = if(state.bluetoothEnabled) Color.hsl(210f, 1f, 0.5f) else Color.Gray,
                    modifier = Modifier
                        .size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.bluetoothEnabled && state.isInitialized,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    if (state.isScanning) "Stop Scanning" else "Scan All",
                     color = MaterialTheme.colorScheme.outline 
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isScanning) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching for device(s)...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.devices.isEmpty() && !state.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No device found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.devices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            supportsDevConnectionControls = state.supportsDevConnectionControls,
                            localIpAddress = viewModel.localIpAddress,
                            onPrimaryActionClick = { viewModel.onDevicePrimaryAction(device.device) },
                            onRemoveDeviceClick = { viewModel.requestRemoveDevice(device) },
                            onClearRemoveErrorClick = { viewModel.clearRemoveError(device.id) },
                            onStartDevConnectionClick = { viewModel.onStartDevConnection(device) },
                            onStopDevConnectionClick = { viewModel.onStopDevConnection(device) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceUiModel,
    supportsDevConnectionControls: Boolean,
    localIpAddress: String,
    onPrimaryActionClick: () -> Unit,
    onRemoveDeviceClick: () -> Unit,
    onClearRemoveErrorClick: () -> Unit,
    onStartDevConnectionClick: () -> Unit,
    onStopDevConnectionClick: () -> Unit,
) {
    val isConnected = device.state == DeviceConnectionUiState.CONNECTED
    val showOverflowActions = isConnected || device.state == DeviceConnectionUiState.KNOWN
    var menuExpanded by remember(device.id) { mutableStateOf(false) }
    var showForgetDialog by remember(device.id) { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (!showOverflowActions) {
                    it.clickable { onPrimaryActionClick() }
                } else {
                    it
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.state.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.state == DeviceConnectionUiState.CONNECTED) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (device.failure != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Last failure: ${device.failure}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (device.removeBusy) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Removing device...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (device.removeError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Remove failed: ${device.removeError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onClearRemoveErrorClick) {
                        Text("Dismiss")
                    }
                }
                if (supportsDevConnectionControls && device.state == DeviceConnectionUiState.CONNECTED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (device.devConnectionActive) {
                            "Dev connection active"
                        } else {
                            "Dev connection inactive"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (device.devConnectionActive) {
                        Text(
                            text = "IP: $localIpAddress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (showOverflowActions) {
                    Box {
                        FilledTonalButton(
                            onClick = { menuExpanded = true },
                            enabled = !device.removeBusy,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                                disabledContainerColor = MaterialTheme.colorScheme.primary,
                                disabledContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.background)
                        ) {
                            Icon(
                                imageVector = Lucide.Ellipsis,
                                contentDescription = "Pebble Settings",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(device.state.primaryActionLabel()) },
                                onClick = {
                                    menuExpanded = false
                                    onPrimaryActionClick()
                                },
                                enabled = device.state != DeviceConnectionUiState.DISCONNECTING,
                            )
                            if (device.canForget) {
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(0.5.dp, MaterialTheme.colorScheme.background)
                                ){
                                    DropdownMenuItem(
                                        text = { Text("Forget Device") },
                                        onClick = {
                                            menuExpanded = false
                                            showForgetDialog = true
                                        },
                                        enabled = !device.removeBusy,
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.onBackground,
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onPrimaryActionClick,
                        enabled = device.state != DeviceConnectionUiState.DISCONNECTING,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.background)
                    ) {
                        Text(device.state.primaryActionLabel())
                    }
                }
                if (supportsDevConnectionControls && device.state == DeviceConnectionUiState.CONNECTED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (device.devConnectionActive) {
                                onStopDevConnectionClick()
                            } else {
                                onStartDevConnectionClick()
                            }
                        },
                        enabled = !device.devConnectionBusy,
                    ) {
                        Text(
                            text = if (device.devConnectionActive){
                                "Stop Dev"
                            } else {
                                "Start Dev"
                            },
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

    if (showForgetDialog && device.canForget) {
        AlertDialog(
            onDismissRequest = { showForgetDialog = false },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Forget device?") },
            text = { Text("This removes the watch from known devices. You will need to pair again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgetDialog = false
                        onRemoveDeviceClick()
                    },
                    enabled = !device.removeBusy,
                ) {
                    Text("Forget", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                }
            },
        )
    }
}

private fun DeviceConnectionUiState.label(): String = when (this) {
    DeviceConnectionUiState.DISCOVERED -> "Discovered"
    DeviceConnectionUiState.KNOWN -> "Known"
    DeviceConnectionUiState.CONNECTING -> "Connecting"
    DeviceConnectionUiState.CONNECTED -> "Connected"
    DeviceConnectionUiState.DISCONNECTING -> "Disconnecting"
}

private fun DeviceConnectionUiState.primaryActionLabel(): String = when (this) {
    DeviceConnectionUiState.CONNECTED,
    DeviceConnectionUiState.CONNECTING,
    DeviceConnectionUiState.DISCONNECTING -> "Disconnect"
    DeviceConnectionUiState.DISCOVERED,
    DeviceConnectionUiState.KNOWN -> "Connect"
}

