package com.example.sleeptracker.analytics

import com.example.sleeptracker.data.SleepEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Период аналитики. */
enum class Period(val title: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
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
) {
    val hasData: Boolean get() = entryCount > 0
}

private val dayLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("d.MM", Locale("ru"))
private val monthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL", Locale("ru"))

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
    val inRange = entries.filter { val d = it.wakeTime.toLocalDate(); !d.isBefore(from) && !d.isAfter(today) }
    val grouped = inRange.groupBy { it.wakeTime.toLocalDate() }

    val points = (0 until days).map { offset ->
        val date = from.plusDays(offset.toLong())
        val dayEntries = grouped[date].orEmpty()
        ChartPoint(
            label = date.format(dayLabel),
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

    val points = (0 until months).map { offset ->
        val ym = from.plusMonths(offset.toLong())
        val monthEntries = grouped[ym].orEmpty()
        ChartPoint(
            // за месяц показываем средний сон за ночь, иначе столбики несопоставимы
            label = ym.atDay(1).format(monthLabel),
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
)

private fun <T : Number> List<T>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size

/** «7 ч 30 мин» из минут. */
fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}

/** «7 ч 30 мин» из часов (Double). */
fun formatHours(hours: Double): String = formatMinutes(Math.round(hours * 60))
