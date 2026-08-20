package com.example.sleeptracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SleepEntry::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null

        fun get(context: Context): SleepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep.db",
                ).build().also { INSTANCE = it }
            }
    }
}
