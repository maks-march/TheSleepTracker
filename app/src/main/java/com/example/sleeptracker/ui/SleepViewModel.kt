package com.example.sleeptracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sleeptracker.analytics.Period
import com.example.sleeptracker.analytics.PeriodSummary
import com.example.sleeptracker.analytics.buildSummary
import com.example.sleeptracker.data.SleepDatabase
import com.example.sleeptracker.data.SleepEntry
import com.example.sleeptracker.data.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SleepViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SleepRepository(SleepDatabase.get(app).sleepDao())

    val entries: StateFlow<List<SleepEntry>> = repo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _period = MutableStateFlow(Period.WEEK)
    val period: StateFlow<Period> = _period

    val summary: StateFlow<PeriodSummary> =
        combine(repo.entries, _period) { list, p -> buildSummary(list, p) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildSummary(emptyList(), Period.WEEK),
            )

    fun setPeriod(p: Period) {
        _period.value = p
    }

    fun save(entry: SleepEntry) = viewModelScope.launch { repo.save(entry) }

    fun delete(entry: SleepEntry) = viewModelScope.launch { repo.delete(entry) }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return SleepViewModel(app) as T
            }
        }
    }
}
