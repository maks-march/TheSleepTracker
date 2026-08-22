package com.example.sleeptracker.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sleeptracker.util.DateFormats
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Резервная копия записей сна в JSON.
 *
 * Нужна там, где обновиться «поверх» нельзя и приходится удалять приложение:
 * например, при смене ключа подписи. Excel-выгрузка для этого не годится —
 * её нельзя загрузить обратно, а этот файл можно.
 */
object SleepBackup {

    private const val MIME_JSON = "application/json"
    private const val FORMAT_VERSION = 1

    /** Итог операции — имя файла либо число восстановленных записей. */
    data class Saved(val fileName: String)
    data class Restored(val added: Int, val skipped: Int)

    // ---------- сохранение ----------

    /**
     * Пишет все записи в «Загрузки» одним .json файлом.
     *
     * @return null, если сохранять нечего или запись не удалась
     */
    fun exportToDownloads(context: Context, entries: List<SleepEntry>): Saved? {
        if (entries.isEmpty()) return null

        val fileName = "TheSleepTracker-backup-" +
            LocalDate.now().format(DateFormats.fileStamp) + ".json"
        val payload = toJson(entries).toString(2).toByteArray()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_JSON)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(payload) } ?: return null
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val dir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .apply { mkdirs() }
                File(dir, fileName).writeBytes(payload)
            }
            Saved(fileName)
        } catch (e: Exception) {
            null
        }
    }

    private fun toJson(entries: List<SleepEntry>): JSONObject {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject().apply {
                    put("bedTime", e.bedTime.toEpochSecond(ZoneOffset.UTC))
                    put("wakeTime", e.wakeTime.toEpochSecond(ZoneOffset.UTC))
                    put("fallAsleepMinutes", e.fallAsleepMinutes)
                    put("quality", e.quality)
                    put("note", e.note)
                }
            )
        }
        return JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("exported", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
            put("entries", array)
        }
    }

    // ---------- восстановление ----------

    /**
     * Читает файл и добавляет записи в базу.
     *
     * Существующие записи не удаляются, а дубликаты пропускаются: совпадением
     * считается та же пара «лёг/проснулся». Поэтому повторный импорт того же
     * файла ничего не испортит.
     *
     * @return null, если файл не читается или это не наш бэкап
     */
    suspend fun importFrom(context: Context, uri: Uri, dao: SleepDao): Restored? {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return null
        } catch (e: Exception) {
            return null
        }

        val parsed = try {
            parse(text)
        } catch (e: Exception) {
            return null
        } ?: return null

        val existing = dao.getAllOnce()
            .map { it.bedTime to it.wakeTime }
            .toHashSet()

        var added = 0
        var skipped = 0
        parsed.forEach { entry ->
            if ((entry.bedTime to entry.wakeTime) in existing) {
                skipped++
            } else {
                dao.insert(entry)
                added++
            }
        }
        return Restored(added, skipped)
    }

    /** Разбирает JSON; null — если структура не наша. */
    private fun parse(text: String): List<SleepEntry>? {
        val root = JSONObject(text)
        val array = root.optJSONArray("entries") ?: return null

        val result = ArrayList<SleepEntry>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val bed = o.optLong("bedTime", Long.MIN_VALUE)
            val wake = o.optLong("wakeTime", Long.MIN_VALUE)
            if (bed == Long.MIN_VALUE || wake == Long.MIN_VALUE) continue

            result += SleepEntry(
                // id не переносим: пусть база выдаст свой
                bedTime = LocalDateTime.ofEpochSecond(bed, 0, ZoneOffset.UTC),
                wakeTime = LocalDateTime.ofEpochSecond(wake, 0, ZoneOffset.UTC),
                fallAsleepMinutes = o.optInt("fallAsleepMinutes", 0),
                quality = o.optInt("quality", 0),
                note = o.optString("note", ""),
            )
        }
        return result
    }
}
