package com.example.pebtip

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.pebtip.service.PebbleBackgroundService
import com.example.pebtip.ui.api.TokenStorage

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startPebbleService()
        } else {
            Toast.makeText(
                this,
                "Bluetooth permissions required to find your Pebble",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        TokenStorage.init(this)

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val allAlreadyGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allAlreadyGranted) {
            startPebbleService()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }

        setContent {
            val app = LocalContext.current.applicationContext as PebtipApplication
            App(
                accelStorage = app.accelStorage,
                uploader = app.batchUploader,
                sensorRepository = app.sensorRepository,
            )
        }
    }

    private fun startPebbleService() {
        val intent = Intent(this, PebbleBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
