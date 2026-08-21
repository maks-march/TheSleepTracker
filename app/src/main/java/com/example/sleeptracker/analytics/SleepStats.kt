package com.example.sleeptracker.analytics

import android.content.Context
import androidx.annotation.StringRes
import com.example.sleeptracker.R
import com.example.sleeptracker.data.SleepEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Период аналитики. */
enum class Period(@StringRes val titleRes: Int) {
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    YEAR(R.string.period_year),
}

/** Один столбик графика. */
data class ChartPoint(
    val label: String,
    val hours: Double,
    val quality: Double,
)

/** Сводка за период. */
data class PeriodSummary(
    val points: List<ChartPoint>,
    val avgSleepHours: Double,
    val avgQuality: Double,
    val avgFallAsleepMinutes: Double,
    val entryCount: Int,
    /** Самая долгая ночь за период, часы (0, если данных нет). */
    val bestNightHours: Double = 0.0,
) {
    val hasData: Boolean get() = entryCount > 0
}

// форматтеры зависят от текущей локали, поэтому создаются на каждый вызов
private fun dayLabel() = DateTimeFormatter.ofPattern("d.MM", Locale.getDefault())
private fun monthLabel() = DateTimeFormatter.ofPattern("LLL", Locale.getDefault())

/**
 * Считает сводку за период. Записи группируются по дате пробуждения:
 * неделя — 7 дней, месяц — 30 дней, год — 12 месяцев.
 */
fun buildSummary(
    entries: List<SleepEntry>,
    period: Period,
    today: LocalDate = LocalDate.now(),
): PeriodSummary = when (period) {
    Period.WEEK -> byDays(entries, today, days = 7)
    Period.MONTH -> byDays(entries, today, days = 30)
    Period.YEAR -> byMonths(entries, today, months = 12)
}

private fun byDays(entries: List<SleepEntry>, today: LocalDate, days: Int): PeriodSummary {
    val from = today.minusDays((days - 1).toLong())
    val inRange = entries.filter {
        val d = it.wakeTime.toLocalDate()
        !d.isBefore(from) && !d.isAfter(today)
    }
    val grouped = inRange.groupBy { it.wakeTime.toLocalDate() }
    val fmt = dayLabel()

    val points = (0 until days).map { offset ->
        val date = from.plusDays(offset.toLong())
        val dayEntries = grouped[date].orEmpty()
        ChartPoint(
            label = date.format(fmt),
            hours = dayEntries.sumOf { it.sleepHours },
            quality = dayEntries.map { it.quality }.averageOrZero(),
        )
    }
    return summary(points, inRange)
}

private fun byMonths(entries: List<SleepEntry>, today: LocalDate, months: Int): PeriodSummary {
    val current = YearMonth.from(today)
    val from = current.minusMonths((months - 1).toLong())
    val inRange = entries.filter {
        val ym = YearMonth.from(it.wakeTime.toLocalDate())
        !ym.isBefore(from) && !ym.isAfter(current)
    }
    val grouped = inRange.groupBy { YearMonth.from(it.wakeTime.toLocalDate()) }
    val fmt = monthLabel()

    val points = (0 until months).map { offset ->
        val ym = from.plusMonths(offset.toLong())
        val monthEntries = grouped[ym].orEmpty()
        ChartPoint(
            // за месяц показываем средний сон за ночь, иначе столбики несопоставимы
            label = ym.atDay(1).format(fmt),
            hours = monthEntries.map { it.sleepHours }.averageOrZero(),
            quality = monthEntries.map { it.quality }.averageOrZero(),
        )
    }
    return summary(points, inRange)
}

private fun summary(points: List<ChartPoint>, entries: List<SleepEntry>) = PeriodSummary(
    points = points,
    avgSleepHours = entries.map { it.sleepHours }.averageOrZero(),
    avgQuality = entries.map { it.quality }.averageOrZero(),
    avgFallAsleepMinutes = entries.map { it.fallAsleepMinutes }.averageOrZero(),
    entryCount = entries.size,
    bestNightHours = entries.maxOfOrNull { it.sleepHours } ?: 0.0,
)

private fun <T : Number> List<T>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size

/** «7 h 30 min» / «7 ч 30 мин» из минут — с учётом текущего языка. */
fun formatMinutes(context: Context, minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return when {
        h > 0 && m > 0 -> context.getString(R.string.unit_h_min, h, m)
        h > 0 -> context.getString(R.string.unit_h, h)
        else -> context.getString(R.string.unit_min, m)
    }
}

/** То же самое, но из часов (Double). */
fun formatHours(context: Context, hours: Double): String =
    formatMinutes(context, Math.round(hours * 60))
