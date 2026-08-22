package com.example.sleeptracker.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sleeptracker.R
import com.example.sleeptracker.data.SleepEntry
import com.example.sleeptracker.util.DateFormats
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

/** Выгружает записи сна в .xlsx прямо в папку «Загрузки». */
object SleepExporter {

    private const val MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Результат экспорта: имя файла для сообщения пользователю. */
    data class Saved(val fileName: String)

    /**
     * Пишет файл в общую папку Downloads.
     *
     * На Android 10+ используется MediaStore (разрешения не нужны), на более
     * старых версиях — прямая запись в публичную директорию.
     *
     * @return null, если экспортировать нечего или запись не удалась
     */
    fun exportToDownloads(context: Context, entries: List<SleepEntry>): Saved? {
        if (entries.isEmpty()) return null

        val fileName = buildFileName()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, fileName, entries)
            } else {
                saveToPublicDir(context, fileName, entries)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        entries: List<SleepEntry>,
    ): Saved? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_XLSX)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null

        resolver.openOutputStream(uri)?.use { out -> writeWorkbook(context, out, entries) }
            ?: return null

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return Saved(fileName)
    }

    private fun saveToPublicDir(
        context: Context,
        fileName: String,
        entries: List<SleepEntry>,
    ): Saved {
        val dir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { out -> writeWorkbook(context, out, entries) }
        return Saved(fileName)
    }

    private fun writeWorkbook(context: Context, out: OutputStream, entries: List<SleepEntry>) {
        val dateFmt = DateFormats.exportDate()
        val timeFmt = DateFormats.time

        val header = listOf(
            context.getString(R.string.excel_col_date),
            context.getString(R.string.excel_col_bed_time),
            context.getString(R.string.excel_col_wake_time),
            context.getString(R.string.excel_col_fall_asleep),
            context.getString(R.string.excel_col_sleep_hours),
            context.getString(R.string.excel_col_quality),
            context.getString(R.string.excel_col_note),
        )

        val rows = entries
            .sortedBy { it.wakeTime }
            .map { e ->
                listOf(
                    XlsxWriter.Cell.Text(e.wakeTime.toLocalDate().format(dateFmt)),
                    XlsxWriter.Cell.Text(e.bedTime.format(timeFmt)),
                    XlsxWriter.Cell.Text(e.wakeTime.format(timeFmt)),
                    XlsxWriter.Cell.Number(e.fallAsleepMinutes.toDouble()),
                    XlsxWriter.Cell.Number(Math.round(e.sleepHours * 100) / 100.0),
                    XlsxWriter.Cell.Number(e.quality.toDouble()),
                    XlsxWriter.Cell.Text(e.note),
                )
            }

        XlsxWriter.write(
            out = out,
            sheetName = context.getString(R.string.excel_sheet_name),
            header = header,
            rows = rows,
            columnWidths = listOf(12, 10, 10, 16, 10, 8, 40),
        )
    }

    /** Имя с датой, чтобы выгрузки не затирали друг друга. */
    private fun buildFileName(): String {
        val stamp = LocalDate.now().format(DateFormats.fileStamp)
        return "TheSleepTracker-$stamp.xlsx"
    }
}
