package com.example.sleeptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime

/**
 * Одна запись сна.
 *
 * @param bedTime когда лёг спать
 * @param wakeTime когда проснулся
 * @param fallAsleepMinutes сколько по ощущениям засыпал (минуты)
 * @param quality оценка сна по 10-балльной шкале
 * @param note примечание
 */
@Entity(tableName = "sleep_entries")
data class SleepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bedTime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val fallAsleepMinutes: Int,
    val quality: Int,
    val note: String = "",
) {
    /** Время в кровати, минуты. */
    val timeInBedMinutes: Long
        get() = Duration.between(bedTime, wakeTime).toMinutes().coerceAtLeast(0)

    /** Фактический сон = время в кровати минус засыпание, минуты. */
    val sleepMinutes: Long
        get() = (timeInBedMinutes - fallAsleepMinutes).coerceAtLeast(0)

    /** Фактический сон в часах. */
    val sleepHours: Double
        get() = sleepMinutes / 60.0
}
