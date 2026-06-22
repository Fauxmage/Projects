package com.example.pebtip

import androidx.compose.runtime.*
import com.example.pebtip.ui.screens.login.LogIn
import com.example.pebtip.ui.screens.login.LoginViewModel
import com.example.pebtip.ui.screens.data.DataViewModel
import com.example.pebtip.ui.theme.PebTipTheme
import com.example.pebtip.upload.StoredDataUploader
import androidx.compose.material3.*
import androidx.compose.ui.*
import com.example.pebtip.ui.api.TokenStorage
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@Composable
fun App(
    accelStorage: AccelStorage? = null,
    uploader: StoredDataUploader? = null,
    sensorRepository: SensorRepository? = null,
) {
    //Dark mode
    val tokenStorage = remember { TokenStorage() }
    val systemDarkTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(tokenStorage.getDarkMode() ?: systemDarkTheme) }
    var isAutoUploadEnabled by remember { mutableStateOf(tokenStorage.getAutoUploadEnabled() ?: false) }

    PebTipTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val loginViewModel = remember { LoginViewModel() }
            val repository = remember { sensorRepository ?: SensorRepository() }
            val mainViewModel = remember { MainViewModel(repository) }

            // Create a default DataViewModel or use provided dependencies
            val dataViewModel = remember {
                val storage = accelStorage ?: object : AccelStorage {
                    override val totalCount = flowOf(0)
                    override val recentSamples = flowOf(emptyList<AccelRecord>())
                    override suspend fun insertAll(records: List<AccelRecord>) {}
                    override suspend fun clearAll() {}
                }
                val up = uploader ?: object : StoredDataUploader {
                    override suspend fun uploadAllStoredData() =
                        com.example.pebtip.upload.UploadAllResult(0, 0, 0)
                }
                DataViewModel(repository, storage, up)
            }

            LaunchedEffect(isAutoUploadEnabled, uploader) {
                if (!isAutoUploadEnabled || uploader == null) return@LaunchedEffect
                while (true) {
                    delay(3 * 60 * 1000L)
                    uploader.uploadAllStoredData()
                }
            }

            //State based navigation
            var currentScreen by remember { mutableStateOf("login") }

            when (currentScreen) {
                "login" -> LogIn(
                    viewModel = loginViewModel,
                    onLoginSuccess = { currentScreen = "main" }
                )

                "main" -> MainScreen(
                    viewModel = mainViewModel,
                    dataViewModel = dataViewModel,
                    onLogout = {
                        loginViewModel.logout()
                        currentScreen = "login"
                    },
                    isDarkTheme = isDarkTheme,
                    isAutoUploadEnabled = isAutoUploadEnabled,
                    //When user toggles dark mode, update state and save
                    onThemeToggle = {
                        isDarkTheme = it
                        tokenStorage.saveDarkMode(it)
                    },
                    onAutoUploadToggle = {
                        isAutoUploadEnabled = it
                        tokenStorage.saveAutoUpload(it)
                    }
                )
            }
        }
    }
}