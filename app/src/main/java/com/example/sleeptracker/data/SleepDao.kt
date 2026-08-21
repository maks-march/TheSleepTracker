package com.example.sleeptracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_entries ORDER BY wakeTime DESC")
    fun observeAll(): Flow<List<SleepEntry>>

    /** Разовое чтение — для BroadcastReceiver, где нет места подписке на Flow. */
    @Query("SELECT * FROM sleep_entries ORDER BY wakeTime DESC")
    suspend fun getAllOnce(): List<SleepEntry>

    @Query("SELECT * FROM sleep_entries WHERE id = :id")
    suspend fun getById(id: Long): SleepEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SleepEntry): Long

    @Update
    suspend fun update(entry: SleepEntry)

    @Delete
    suspend fun delete(entry: SleepEntry)
}
