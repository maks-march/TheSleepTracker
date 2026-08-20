package com.example.sleeptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sleeptracker.ui.SleepApp
import com.example.sleeptracker.ui.theme.SleepTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SleepTrackerTheme {
                SleepApp()
            }
        }
    }
}
