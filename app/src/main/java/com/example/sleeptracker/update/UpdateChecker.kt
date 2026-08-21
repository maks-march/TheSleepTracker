package com.example.sleeptracker.update

import com.example.sleeptracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверяет, есть ли в репозитории сборка новее установленной.
 *
 * Источник правды — файл `version.json` в корне ветки `main`. Его достаточно
 * поправить вместе с новым APK, никакой серверной части не нужно.
 *
 * Формат:
 * ```json
 * { "versionCode": 4, "versionName": "1.3", "notes": "Что нового" }
 * ```
 */
object UpdateChecker {

    private const val TIMEOUT_MS = 10_000

    data class Result(
        val versionCode: Int,
        val versionName: String,
        val notes: String,
    ) {
        val isNewer: Boolean get() = versionCode > BuildConfig.VERSION_CODE
    }

    suspend fun check(): Result? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(BuildConfig.VERSION_URL).openConnection() as HttpURLConnection)
                .apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                    // raw.githubusercontent кэширует агрессивно
                    setRequestProperty("Cache-Control", "no-cache")
                }

            connection.use { conn ->
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                Result(
                    versionCode = json.optInt("versionCode", 0),
                    versionName = json.optString("versionName", ""),
                    notes = json.optString("notes", ""),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }
}
