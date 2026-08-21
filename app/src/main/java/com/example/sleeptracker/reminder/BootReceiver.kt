package com.example.sleeptracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleeptracker.data.SleepDatabase
import com.example.sleeptracker.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * После перезагрузки устройства (и обновления приложения) все будильники
 * стираются системой — восстанавливаем их.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppSettings.init(app)
                val entries = SleepDatabase.get(app).sleepDao().getAllOnce()
                ReminderScheduler.rescheduleAll(app, entries)
            } finally {
                pending.finish()
            }
        }
    }
}
