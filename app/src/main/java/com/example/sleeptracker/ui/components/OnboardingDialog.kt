package com.example.sleeptracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sleeptracker.R

/** Предложение включить напоминания при первом запуске. */
@Composable
fun OnboardingDialog(
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = { Text(stringResource(R.string.onboarding_text)) },
        confirmButton = {
            TextButton(onClick = onEnable) {
                Text(stringResource(R.string.onboarding_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        },
    )
}
