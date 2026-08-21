package com.example.sleeptracker.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleeptracker.R
import com.example.sleeptracker.ui.components.screenBackgroundColor
import com.example.sleeptracker.analytics.formatMinutes
import com.example.sleeptracker.data.SleepEntry
import com.example.sleeptracker.ui.SleepViewModel
import com.example.sleeptracker.ui.components.showDatePicker
import com.example.sleeptracker.ui.components.showTimePicker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun dateFmt() = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    vm: SleepViewModel,
    entryId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    // по умолчанию: лёг вчера в 23:00, встал сегодня в 7:00
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
        containerColor = screenBackgroundColor(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (entryId == 0L) R.string.editor_new else R.string.editor_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBackgroundColor(),
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
                colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.editor_sleep),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (valid) formatMinutes(context, sleepMinutes) else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!valid) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.editor_invalid_range),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.editor_bed_time))
            DateTimeRow(
                dateText = bedTime.format(dateFmt()),
                timeText = bedTime.format(timeFmt),
                onPickDate = {
                    showDatePicker(context, bedTime.toLocalDate()) { d ->
                        bedTime = LocalDateTime.of(d, bedTime.toLocalTime())
                    }
                },
                onPickTime = {
                    showTimePicker(context, bedTime.toLocalTime()) { t ->
                        bedTime = LocalDateTime.of(bedTime.toLocalDate(), t)
                    }
                },
            )

            SectionTitle(stringResource(R.string.editor_wake_time))
            DateTimeRow(
                dateText = wakeTime.format(dateFmt()),
                timeText = wakeTime.format(timeFmt),
                onPickDate = {
                    showDatePicker(context, wakeTime.toLocalDate()) { d ->
                        wakeTime = LocalDateTime.of(d, wakeTime.toLocalTime())
                    }
                },
                onPickTime = {
                    showTimePicker(context, wakeTime.toLocalTime()) { t ->
                        wakeTime = LocalDateTime.of(wakeTime.toLocalDate(), t)
                    }
                },
            )

            SectionTitle(stringResource(R.string.editor_fall_asleep, fallAsleep))
            Slider(
                value = fallAsleep.toFloat(),
                onValueChange = { fallAsleep = it.toInt() },
                valueRange = 0f..120f,
                steps = 23, // шаг 5 минут
            )

            SectionTitle(stringResource(R.string.editor_quality, quality))
            Slider(
                value = quality.toFloat(),
                onValueChange = { quality = it.toInt() },
                valueRange = 1f..10f,
                steps = 8,
            )

            SectionTitle(stringResource(R.string.editor_notes))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.editor_notes_hint)) },
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
                Text(stringResource(R.string.editor_save))
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
