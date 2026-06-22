package com.example.pebtip.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme


private val lightColor = lightColorScheme (
    background = lightBackground,
    error = lightRed,
    onPrimary = lightInput,
    primary = lightCard,
    onBackground = lightText,
    surface = lightButton,
    surfaceBright = lightStats,
    outline = lightButtonTxt,
    secondary = lightCheck
)

private val darkColor = darkColorScheme (
    background = darkBackground,
    error = darkRed,
    onPrimary = darkInput,
    primary = darkCard,
    // onBackground = darkText,
    onBackground = darkCheck,
    surface = darkButton,
    surfaceBright = darkStats,
    outline = darkButtonTxt,
    // secondary = darkCheck
    secondary = darkText
)

//defaults to devices system setting, but can be overridden manually
@Composable
fun PebTipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    //switch between light and dark color scheme
    val colorScheme = if (darkTheme) darkColor else lightColor

    //Add selected color to all composables inside theme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}