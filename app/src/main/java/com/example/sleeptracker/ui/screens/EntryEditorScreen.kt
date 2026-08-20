package com.example.sleeptracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleeptracker.analytics.formatMinutes
import com.example.sleeptracker.data.SleepEntry
import com.example.sleeptracker.ui.SleepViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    vm: SleepViewModel,
    entryId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    // значения по умолчанию: лёг вчера в 23:00, встал сегодня в 7:00
    var bedTime by remember {
        mutableStateOf(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(23, 0)))
    }
    var wakeTime by remember {
        mutableStateOf(LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0)))
    }
    var fallAsleep by remember { mutableIntStateOf(15) }
    var quality by remember { mutableIntStateOf(7) }
    var note by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(entryId == 0L) }

    LaunchedEffect(entryId) {
        if (entryId != 0L) {
            vm.entries.value.find { it.id == entryId }?.let { e ->
                bedTime = e.bedTime
                wakeTime = e.wakeTime
                fallAsleep = e.fallAsleepMinutes
                quality = e.quality
                note = e.note
            }
            loaded = true
        }
    }

    val totalMinutes = Duration.between(bedTime, wakeTime).toMinutes()
    val sleepMinutes = (totalMinutes - fallAsleep).coerceAtLeast(0)
    val valid = totalMinutes > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == 0L) "Новая запись" else "Изменить запись") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Итог
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Сон",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (valid) formatMinutes(sleepMinutes) else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!valid) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Пробуждение должно быть позже отхода ко сну",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            SectionTitle("Лёг спать")
            DateTimeRow(
                dateText = bedTime.format(dateFmt),
                timeText = bedTime.format(timeFmt),
                onPickDate = {
                    pickDate(context, bedTime) { d ->
                        bedTime = LocalDateTime.of(d, bedTime.toLocalTime())
                    }
                },
                onPickTime = {
                    pickTime(context, bedTime) { t ->
                        bedTime = LocalDateTime.of(bedTime.toLocalDate(), t)
                    }
                },
            )

            SectionTitle("Проснулся")
            DateTimeRow(
                dateText = wakeTime.format(dateFmt),
                timeText = wakeTime.format(timeFmt),
                onPickDate = {
                    pickDate(context, wakeTime) { d ->
                        wakeTime = LocalDateTime.of(d, wakeTime.toLocalTime())
                    }
                },
                onPickTime = {
                    pickTime(context, wakeTime) { t ->
                        wakeTime = LocalDateTime.of(wakeTime.toLocalDate(), t)
                    }
                },
            )

            SectionTitle("Засыпал (по ощущениям): $fallAsleep мин")
            Slider(
                value = fallAsleep.toFloat(),
                onValueChange = { fallAsleep = it.toInt() },
                valueRange = 0f..120f,
                steps = 23, // шаг 5 минут
            )

            SectionTitle("Оценка сна: $quality / 10")
            Slider(
                value = quality.toFloat(),
                onValueChange = { quality = it.toInt() },
                valueRange = 1f..10f,
                steps = 8,
            )

            SectionTitle("Примечания")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например: пил кофе вечером, просыпался ночью…") },
                minLines = 3,
            )

            Button(
                onClick = {
                    vm.save(
                        SleepEntry(
                            id = entryId,
                            bedTime = bedTime,
                            wakeTime = wakeTime,
                            fallAsleepMinutes = fallAsleep,
                            quality = quality,
                            note = note.trim(),
                        )
                    )
                    onDone()
                },
                enabled = valid && loaded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DateTimeRow(
    dateText: String,
    timeText: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onPickDate, modifier = Modifier.weight(1.6f)) { Text(dateText) }
        OutlinedButton(onClick = onPickTime, modifier = Modifier.weight(1f)) { Text(timeText) }
    }
}

private fun pickDate(
    context: android.content.Context,
    current: LocalDateTime,
    onPicked: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth,
    ).show()
}

private fun pickTime(
    context: android.content.Context,
    current: LocalDateTime,
    onPicked: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, h, m -> onPicked(LocalTime.of(h, m)) },
        current.hour,
        current.minute,
        true,
    ).show()
}
