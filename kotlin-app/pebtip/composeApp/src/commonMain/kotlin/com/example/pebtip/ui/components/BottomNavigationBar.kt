package com.example.pebtip.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.composables.icons.lucide.*
import com.example.pebtip.navigation.Screen
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.*
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar (
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onPrimary,
            shape = RectangleShape
        )
    ) {
        NavigationBarItem(
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.surface,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.surface,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                indicatorColor = MaterialTheme.colorScheme.onPrimary
            ),
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = {
                Icon(
                    imageVector = Lucide.House,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") }
        )

        NavigationBarItem(
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.surface,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.surface,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                indicatorColor = MaterialTheme.colorScheme.onPrimary
            ),
            selected = currentRoute == Screen.Data.route,
            onClick = { onNavigate(Screen.Data.route) },
            icon = {
                Icon(
                    imageVector = Lucide.ChartLine,
                    contentDescription = "Data"
                )
            },
            label = { Text("Data") }
        )

        NavigationBarItem(
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.surface,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.surface,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                indicatorColor = MaterialTheme.colorScheme.onPrimary
            ),
            selected = currentRoute == Screen.Connect.route,
            onClick = { onNavigate(Screen.Connect.route) },
            icon = {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Connect"
                )
            },
            label = { Text("Add device") }
        )

        NavigationBarItem(
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.surface,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.surface,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                indicatorColor = MaterialTheme.colorScheme.onPrimary
            ),
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = {
                Icon(
                    imageVector = Lucide.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") }
        )
    }
}


