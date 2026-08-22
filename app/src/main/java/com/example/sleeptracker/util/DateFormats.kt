package com.example.sleeptracker.util

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Форматтеры дат и времени, общие для всего приложения.
 *
 * Те, что зависят от языка, объявлены функциями: локаль меняется в настройках
 * без перезапуска процесса, поэтому кэшировать их в `val` нельзя — иначе после
 * смены языка подписи останутся на старом.
 */
object DateFormats {

    /** «23:45» — от локали не зависит. */
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** «17 марта 2026» — заголовок записи в редакторе. */
    fun fullDate(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    /** «вторник, 17 марта» — строка списка в журнале. */
    fun dayWithWeekday(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

    /** «17.03» — подпись оси X за неделю и месяц. */
    fun shortDay(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d.MM", Locale.getDefault())

    /** «мар» — подпись оси X за год. */
    fun shortMonth(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLL", Locale.getDefault())

    /** «17.03.2026» — колонка даты в выгрузке Excel. */
    fun exportDate(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    /** «2026-03-17» — для имени файла выгрузки, всегда одинаковый. */
    val fileStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
