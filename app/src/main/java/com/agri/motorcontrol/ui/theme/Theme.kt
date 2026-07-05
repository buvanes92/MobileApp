package com.agri.motorcontrol.ui.theme

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

// Curated Sleek Color Palette
val EmeraldPrimary = Color(0xFF00C853) // Vibrant Emerald Green
val EmeraldVariant = Color(0xFF009624)
val WaterBlue = Color(0xFF0288D1) // Bright Water Blue
val AlertRed = Color(0xFFD50000) // Crimson Red for critical alerts
val WarningOrange = Color(0xFFFF6D00) // Amber Orange for warnings

// Dark Theme Colors (Sleek Slate)
private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = WaterBlue,
    tertiary = Color(0xFF81C784),
    background = Color(0xFF0A0F1D), // Dark Navy/Black
    surface = Color(0xFF151D33), // Slate Card Background
    error = AlertRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFECEFF1),
    onError = Color.White
)

// Light Theme Colors (Clean & Modern)
private val LightColorScheme = lightColorScheme(
    primary = EmeraldVariant,
    secondary = WaterBlue,
    tertiary = Color(0xFF388E3C),
    background = Color(0xFFF4F6F9), // Light Greyish Blue
    surface = Color.White,
    error = AlertRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF334155),
    onError = Color.White
)

@Composable
fun AgriMotorControlTheme(
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
        content = content
    )
}
