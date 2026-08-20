package com.example.sleeptracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Тёмная палитра
val NightBackground = Color(0xFF0E1117)
val NightSurface = Color(0xFF171B24)
val NightSurfaceVariant = Color(0xFF212734)
val Accent = Color(0xFF7C9CFF)
val AccentSoft = Color(0xFF2A3550)
val TextPrimary = Color(0xFFE7EAF0)
val TextSecondary = Color(0xFF98A2B8)
val Danger = Color(0xFFFF6B6B)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0B1020),
    primaryContainer = AccentSoft,
    onPrimaryContainer = TextPrimary,
    secondary = Accent,
    onSecondary = Color(0xFF0B1020),
    background = NightBackground,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    outline = Color(0xFF394152),
    outlineVariant = Color(0xFF2A3040),
)

@Composable
fun SleepTrackerTheme(
    // приложение всегда тёмное
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NightBackground.value.toInt()
            window.navigationBarColor = NightBackground.value.toInt()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
