package com.example.sleeptracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleeptracker.data.SleepDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Показывает напоминание и сразу планирует следующее (AlarmManager одноразовый).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SleepDatabase.get(app).sleepDao()
                val entries = dao.getAllOnce()

                when (intent.action) {
                    ReminderKind.BEDTIME.action -> Notifications.showBedtime(app)

                    ReminderKind.MORNING.action -> {
                        // не дёргаем человека, если запись за сегодня уже есть
                        val today = LocalDate.now()
                        val alreadyLogged = entries.any {
                            it.wakeTime.toLocalDate() == today
                        }
                        if (!alreadyLogged) Notifications.showMorning(app)
                    }
                }

                // перепланируем на завтра с учётом свежей статистики
                ReminderScheduler.rescheduleAll(app, entries)
            } finally {
                pending.finish()
            }
        }
    }
}
