package com.example.sleeptracker.data

import kotlinx.coroutines.flow.Flow

class SleepRepository(private val dao: SleepDao) {

    val entries: Flow<List<SleepEntry>> = dao.observeAll()

    suspend fun getById(id: Long): SleepEntry? = dao.getById(id)

    suspend fun save(entry: SleepEntry) {
        if (entry.id == 0L) dao.insert(entry) else dao.update(entry)
    }

    suspend fun delete(entry: SleepEntry) = dao.delete(entry)
}
