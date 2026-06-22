package com.example.pebtip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.pebtip.navigation.Screen
import com.example.pebtip.ui.components.BottomNavigationBar
import com.example.pebtip.ui.screens.data.DataScreen
import com.example.pebtip.ui.screens.data.DataViewModel
import com.example.pebtip.ui.screens.home.HomeScreen
import com.example.pebtip.ui.screens.connect.ConnectScreen
import com.example.pebtip.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    dataViewModel: DataViewModel,
    onLogout: () -> Unit = {},
    isDarkTheme: Boolean,
    isAutoUploadEnabled: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onAutoUploadToggle: (Boolean) -> Unit,
) {
    var currentRoute by remember { mutableStateOf(Screen.Home.route)}

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentRoute) {
                Screen.Home.route -> HomeScreen(viewModel)
                Screen.Data.route -> DataScreen(dataViewModel)
                Screen.Connect.route -> ConnectScreen()
                Screen.Settings.route -> SettingsScreen(
                    onLogout = onLogout,
                    isDarkTheme = isDarkTheme,
                    isAutoUploadEnabled = isAutoUploadEnabled,
                    onThemeToggle = onThemeToggle,
                    onAutoUploadToggle = onAutoUploadToggle
                    )
            }
        }
    }
}