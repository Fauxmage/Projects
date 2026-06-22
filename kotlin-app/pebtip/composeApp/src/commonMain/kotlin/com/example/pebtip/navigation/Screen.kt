package com.example.pebtip.navigation

// Navbar routes, subject to change
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Data : Screen("data")
    data object Connect : Screen("connect")
}

