package com.example.sleeptracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Локальная база записей сна.
 *
 * Обновление приложения данные не трогает: файл `sleep.db` живёт во внутреннем
 * хранилище и переживает переустановку поверх.
 *
 * ВАЖНО про будущие версии: если поменять [SleepEntry] (добавить/убрать поле),
 * надо поднять `version` и дописать миграцию в [MIGRATIONS]. Здесь намеренно
 * НЕ вызывается `fallbackToDestructiveMigration()` — иначе Room при
 * несовпадении схемы молча удалил бы все записи пользователя. Без миграции
 * приложение упадёт при старте, и это правильно: лучше заметная ошибка на
 * этапе разработки, чем потерянные данные у людей.
 */
@Database(entities = [SleepEntry::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao

    companion object {
        private const val DB_NAME = "sleep.db"

        /**
         * Миграции между версиями схемы.
         *
         * Пример для версии 2 (добавили колонку):
         * ```
         * private val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE sleep_entries ADD COLUMN mood INTEGER NOT NULL DEFAULT 0")
         *     }
         * }
         * ```
         */
        private val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

        @Volatile
        private var INSTANCE: SleepDatabase? = null

        fun get(context: Context): SleepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
