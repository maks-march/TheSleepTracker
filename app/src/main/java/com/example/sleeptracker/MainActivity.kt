package com.example.sleeptracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sleeptracker.settings.AppSettings
import com.example.sleeptracker.ui.SleepApp
import com.example.sleeptracker.ui.components.AppBackground
import com.example.sleeptracker.ui.theme.SleepTrackerTheme

/**
 * AppCompatActivity — нужна для AppCompatDelegate.setApplicationLocales()
 * (смена языка в настройках на Android ниже 13).
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by AppSettings.state.collectAsState()

            SleepTrackerTheme(
                themeMode = settings.themeMode,
                transparentSystemBars = settings.hasBackgroundImage,
            ) {
                AppBackground(
                    backgroundPath = settings.backgroundPath,
                    dim = settings.backgroundDim,
                ) {
                    SleepApp(hasBackgroundImage = settings.hasBackgroundImage)
                }
            }
        }
    }
}
