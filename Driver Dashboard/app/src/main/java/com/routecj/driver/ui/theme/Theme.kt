package com.routecj.driver.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RouteCJCyanLight,
    onPrimary = RouteCJNavyDark,
    primaryContainer = RouteCJBlue,
    onPrimaryContainer = RouteCJWhite,
    secondary = RouteCJCyan,
    onSecondary = RouteCJNavyDark,
    background = RouteCJNavyDark,
    onBackground = RouteCJTextPrimaryDark,
    surface = RouteCJNavySurface,
    onSurface = RouteCJTextPrimaryDark,
    error = RouteCJError,
    onError = RouteCJWhite
)

private val LightColorScheme = lightColorScheme(
    primary = RouteCJBlue,
    onPrimary = RouteCJWhite,
    primaryContainer = RouteCJCyanLight,
    onPrimaryContainer = RouteCJNavyDark,
    secondary = RouteCJCyan,
    onSecondary = RouteCJWhite,
    background = RouteCJLightBg,
    onBackground = RouteCJTextPrimaryLight,
    surface = RouteCJSurfaceLight,
    onSurface = RouteCJTextPrimaryLight,
    error = RouteCJError,
    onError = RouteCJWhite
)

@Composable
fun RouteCJDriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}