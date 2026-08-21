package com.example.sleeptracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.sleeptracker.settings.ThemeMode

// ---- Тёмная палитра ----
val NightBackground = Color(0xFF0E1117)
val NightSurface = Color(0xFF171B24)
val NightSurfaceVariant = Color(0xFF212734)
val Accent = Color(0xFF7C9CFF)
val AccentSoft = Color(0xFF2A3550)
val TextPrimary = Color(0xFFE7EAF0)
val TextSecondary = Color(0xFF98A2B8)
val Danger = Color(0xFFFF6B6B)

// ---- Светлая палитра ----
val DayBackground = Color(0xFFF6F7FB)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceVariant = Color(0xFFEBEEF5)
val DayAccent = Color(0xFF3D5AFE)
val DayAccentSoft = Color(0xFFDDE3FF)
val DayTextPrimary = Color(0xFF161A22)
val DayTextSecondary = Color(0xFF5B6474)
val DayDanger = Color(0xFFD32F2F)

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

private val LightColors = lightColorScheme(
    primary = DayAccent,
    onPrimary = Color.White,
    primaryContainer = DayAccentSoft,
    onPrimaryContainer = Color(0xFF0A1A5C),
    secondary = DayAccent,
    onSecondary = Color.White,
    background = DayBackground,
    onBackground = DayTextPrimary,
    surface = DaySurface,
    onSurface = DayTextPrimary,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayTextSecondary,
    error = DayDanger,
    outline = Color(0xFFC3C9D6),
    outlineVariant = Color(0xFFDCE1EB),
)

/** Решает, тёмная ли сейчас тема, с учётом выбора пользователя. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun SleepTrackerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    /** С фото на фоне системные панели делаем прозрачными. */
    transparentSystemBars: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.isDark()
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val barColor =
                if (transparentSystemBars) Color.Transparent
                else colors.background
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            // на светлой теме без фото иконки статус-бара должны быть тёмными
            val lightIcons = !darkTheme && !transparentSystemBars
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightIcons
                isAppearanceLightNavigationBars = lightIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
