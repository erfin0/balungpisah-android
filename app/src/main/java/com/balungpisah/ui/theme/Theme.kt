package com.balungpisah.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BalungPisahDarkGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E6D3),
    onPrimaryContainer = BalungPisahDarkGreen,
    secondary = BalungPisahOrange,
    onSecondary = Color.White,
    background = BalungPisahOffWhite,
    onBackground = BalungPisahBlack,
    surface = Color.White,
    onSurface = BalungPisahBlack,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = BalungPisahOrange,
    onPrimary = Color.White,
    primaryContainer = BalungPisahDarkGreen,
    onPrimaryContainer = BalungPisahGold,
    secondary = BalungPisahGold,
    onSecondary = BalungPisahBlack,
    background = Color(0xFF080C0B),
    onBackground = Color(0xFFEBEBEB),
    surface = Color(0xFF121816),
    onSurface = Color(0xFFEBEBEB),
    surfaceVariant = Color(0xFF242B28),
    onSurfaceVariant = Color(0xFFC2C7C4),
    outline = Color(0xFF3F4945)
)

@Composable
fun BalungPisahTheme(
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