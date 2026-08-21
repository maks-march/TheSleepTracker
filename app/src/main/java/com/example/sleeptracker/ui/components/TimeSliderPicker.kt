package com.example.sleeptracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleeptracker.R
import java.time.LocalTime

/**
 * Выбор времени двумя слайдерами (часы и минуты) вместо кругового циферблата.
 * Минуты идут с шагом 5.
 */
@Composable
fun TimeSliderPickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initial.hour) }
    // приводим к ближайшим 5 минутам
    var minute by remember { mutableIntStateOf((initial.minute / 5) * 5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.time_picker_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // крупное превью выбранного времени
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                SliderRow(
                    label = stringResource(R.string.time_picker_hours),
                    value = hour,
                    valueText = "%02d".format(hour),
                    range = 0f..23f,
                    steps = 22,
                    onValueChange = { hour = it },
                )

                Spacer(Modifier.height(12.dp))

                SliderRow(
                    label = stringResource(R.string.time_picker_minutes),
                    value = minute,
                    valueText = "%02d".format(minute),
                    range = 0f..55f,
                    steps = 10, // 0,5,10 … 55
                    onValueChange = { minute = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) {
                Text(stringResource(R.string.time_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.time_picker_cancel))
            }
        },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = steps,
        )
    }
}
