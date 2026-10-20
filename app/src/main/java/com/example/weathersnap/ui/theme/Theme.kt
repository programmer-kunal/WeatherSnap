package com.example.weathersnap.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LimeAccent,
    onPrimary = ForestDeepDark,
    primaryContainer = ForestGreenPrimary,
    onPrimaryContainer = WarmCreamText,
    secondary = ForestGreenLight,
    onSecondary = WarmCreamText,
    secondaryContainer = ForestGreenContainer,
    onSecondaryContainer = WarmCreamText,
    tertiary = SkyBlueAccent,
    onTertiary = ForestDeepDark,
    background = ForestBlackBackground,
    onBackground = WarmCreamText,
    surface = ForestSurface,
    onSurface = WarmCreamText,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = SageHelperText,
    outline = ForestBorder,
    error = ErrorRed,
    onError = ForestDeepDark
)

@Composable
fun WeatherSnapTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkColorScheme.background.toArgb()
            window.navigationBarColor = DarkColorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
