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
import com.example.sleeptracker.reminder.ReminderScheduler
import com.example.sleeptracker.settings.AppSettings
import com.example.sleeptracker.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SleepViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SleepRepository(SleepDatabase.get(app).sleepDao())

    val entries: StateFlow<List<SleepEntry>> = repo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<SettingsState> = AppSettings.state

    private val _period = MutableStateFlow(Period.WEEK)
    val period: StateFlow<Period> = _period

    val summary: StateFlow<PeriodSummary> =
        combine(repo.entries, _period) { list, p -> buildSummary(list, p) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildSummary(emptyList(), Period.WEEK),
            )

    /** Среднее время отхода ко сну — показывается в настройках. */
    val averageBedtime: StateFlow<LocalTime> = repo.entries
        .map { ReminderScheduler.averageBedtime(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReminderScheduler.DEFAULT_BEDTIME,
        )

    /** Среднее время подъёма — время утреннего напоминания. */
    val averageWakeTime: StateFlow<LocalTime> = repo.entries
        .map { ReminderScheduler.averageWakeTime(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReminderScheduler.DEFAULT_WAKE_TIME,
        )

    init {
        // записи меняются -> среднее время сдвигается -> переносим будильники
        viewModelScope.launch {
            repo.entries.drop(1).collect { list ->
                ReminderScheduler.rescheduleAll(getApplication(), list)
            }
        }
    }

    fun setPeriod(p: Period) {
        _period.value = p
    }

    fun save(entry: SleepEntry) = viewModelScope.launch { repo.save(entry) }

    fun delete(entry: SleepEntry) = viewModelScope.launch { repo.delete(entry) }

    /** Включает/выключает напоминание и сразу перепланирует будильники. */
    fun setBedtimeReminder(enabled: Boolean) {
        val app = getApplication<Application>()
        AppSettings.setBedtimeReminder(app, enabled)
        ReminderScheduler.rescheduleAll(app, entries.value)
    }

    fun setMorningReminder(enabled: Boolean) {
        val app = getApplication<Application>()
        AppSettings.setMorningReminder(app, enabled)
        ReminderScheduler.rescheduleAll(app, entries.value)
    }

    /** Ответ на предложение при первом запуске. */
    fun completeOnboarding(enableReminders: Boolean) {
        val app = getApplication<Application>()
        if (enableReminders) {
            AppSettings.setBedtimeReminder(app, true)
            AppSettings.setMorningReminder(app, true)
            ReminderScheduler.rescheduleAll(app, entries.value)
        }
        AppSettings.markOnboardingShown(app)
    }

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
