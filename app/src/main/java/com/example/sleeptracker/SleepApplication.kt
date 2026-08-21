package com.example.sleeptracker

import android.app.Application
import com.example.sleeptracker.reminder.Notifications
import com.example.sleeptracker.settings.AppSettings

class SleepApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // читаем настройки и применяем сохранённый язык до создания Activity
        AppSettings.init(this)
        Notifications.createChannels(this)
    }
}
