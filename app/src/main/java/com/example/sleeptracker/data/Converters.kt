package com.example.sleeptracker.data

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Хранит LocalDateTime как epoch-секунды (UTC). */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }

    @TypeConverter
    fun toTimestamp(dateTime: LocalDateTime?): Long? =
        dateTime?.toEpochSecond(ZoneOffset.UTC)
}
