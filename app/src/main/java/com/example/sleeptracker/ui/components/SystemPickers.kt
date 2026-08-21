package com.example.sleeptracker.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import com.example.sleeptracker.R
import java.time.LocalDate
import java.time.LocalTime

/**
 * Системный выбор времени барабанами (spinner) — крутится колёсиками,
 * а не циферблатом: тема [R.style.SpinnerTimePicker] задаёт timePickerMode=spinner.
 */
fun showTimePicker(
    context: Context,
    initial: LocalTime,
    onPicked: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        R.style.SpinnerTimePicker,
        { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        DateFormat.is24HourFormat(context),
    ).show()
}

/** Системный выбор даты, тоже барабанами. */
fun showDatePicker(
    context: Context,
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        R.style.SpinnerTimePicker,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}
