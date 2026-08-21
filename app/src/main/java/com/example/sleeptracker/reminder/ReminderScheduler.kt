package com.example.sleeptracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sleeptracker.data.SleepEntry
import com.example.sleeptracker.settings.AppSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Тип напоминания. */
enum class ReminderKind(val requestCode: Int, val action: String) {
    /** Вечернее: «пора ложиться». */
    BEDTIME(1001, "com.example.sleeptracker.REMIND_BEDTIME"),

    /** Утреннее: «не забудь внести запись». Беззвучное. */
    MORNING(1002, "com.example.sleeptracker.REMIND_MORNING"),
}

/**
 * Планирует ежедневные напоминания.
 *
 * Время берётся не из настроек, а из истории сна: считается среднее время отхода
 * ко сну и пробуждения по последним записям. Пока записей нет, используются
 * значения по умолчанию (23:00 и 08:00).
 */
object ReminderScheduler {

    /** Сколько последних записей учитывать при расчёте среднего. */
    private const val WINDOW = 14

    val DEFAULT_BEDTIME: LocalTime = LocalTime.of(23, 0)
    val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(8, 0)

    /**
     * Среднее время суток по списку моментов.
     *
     * Наивное усреднение минут ломается на переходе через полночь (23:50 и 00:10
     * дают 12:00), поэтому усредняем по кратчайшей дуге: значения ближе к концу
     * суток, чем к началу, сдвигаются на +24 часа относительно опорной точки.
     */
    private fun averageTime(times: List<LocalTime>): LocalTime? {
        if (times.isEmpty()) return null

        val base = times.first().toSecondOfDay()
        val day = 24 * 60 * 60
        val total = times.sumOf { t ->
            var diff = t.toSecondOfDay() - base
            if (diff > day / 2) diff -= day
            if (diff < -day / 2) diff += day
            diff.toLong()
        }
        val avg = ((base + total / times.size) % day + day) % day
        return LocalTime.ofSecondOfDay(avg)
    }

    /** Среднее время отхода ко сну по последним записям. */
    fun averageBedtime(entries: List<SleepEntry>): LocalTime =
        averageTime(
            entries.sortedByDescending { it.wakeTime }
                .take(WINDOW)
                .map { it.bedTime.toLocalTime() }
        ) ?: DEFAULT_BEDTIME

    /** Среднее время пробуждения по последним записям. */
    fun averageWakeTime(entries: List<SleepEntry>): LocalTime =
        averageTime(
            entries.sortedByDescending { it.wakeTime }
                .take(WINDOW)
                .map { it.wakeTime.toLocalTime() }
        ) ?: DEFAULT_WAKE_TIME

    /**
     * Перепланирует оба напоминания согласно настройкам и статистике.
     * Безопасно вызывать сколько угодно раз — старые будильники заменяются.
     */
    fun rescheduleAll(context: Context, entries: List<SleepEntry>) {
        val settings = AppSettings.state.value

        if (settings.bedtimeReminder) {
            schedule(context, ReminderKind.BEDTIME, averageBedtime(entries))
        } else {
            cancel(context, ReminderKind.BEDTIME)
        }

        if (settings.morningReminder) {
            schedule(context, ReminderKind.MORNING, averageWakeTime(entries))
        } else {
            cancel(context, ReminderKind.MORNING)
        }
    }

    /** Ставит ежедневный будильник на ближайшее наступление [time]. */
    fun schedule(context: Context, kind: ReminderKind, time: LocalTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextOccurrence(time)

        val pending = pendingIntent(context, kind, mutable = false)

        // без разрешения на точные будильники используем неточный — он всё равно сработает
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending,
                )
            } else {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    Duration.ofMinutes(30).toMillis(),
                    pending,
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                Duration.ofMinutes(30).toMillis(),
                pending,
            )
        }
    }

    fun cancel(context: Context, kind: ReminderKind) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context, kind, mutable = false))
    }

    private fun pendingIntent(
        context: Context,
        kind: ReminderKind,
        mutable: Boolean,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = kind.action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context.applicationContext,
            kind.requestCode,
            intent,
            flags,
        )
    }

    /** Ближайший момент, когда наступит [time]: сегодня, если ещё не прошло, иначе завтра. */
    private fun nextOccurrence(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
