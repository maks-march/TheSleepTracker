package com.example.sleeptracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sleeptracker.BuildConfig
import com.example.sleeptracker.R
import com.example.sleeptracker.export.SleepExporter
import com.example.sleeptracker.settings.AppLanguage
import com.example.sleeptracker.settings.AppSettings
import com.example.sleeptracker.settings.ThemeMode
import com.example.sleeptracker.ui.components.ImageCropperDialog
import com.example.sleeptracker.ui.SleepViewModel
import com.example.sleeptracker.update.ApkDownloader
import com.example.sleeptracker.update.UpdateChecker
import com.example.sleeptracker.util.DateFormats
import com.example.sleeptracker.ui.components.IconRow
import com.example.sleeptracker.ui.components.SwitchRow
import com.example.sleeptracker.ui.components.RadioRow
import com.example.sleeptracker.ui.components.SettingsCard
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SleepViewModel, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val entries by vm.entries.collectAsState()
    val settings by vm.settings.collectAsState()
    val avgBedtime by vm.averageBedtime.collectAsState()
    val avgWakeTime by vm.averageWakeTime.collectAsState()

    var language by remember { mutableStateOf(AppSettings.getLanguage(context)) }

    val transparent = settings.hasBackgroundImage

    Scaffold(
        containerColor = if (transparent) Color.Transparent
        else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
            // ---- Оформление ----
            SettingsCard(title = stringResource(R.string.settings_theme)) {
                RadioRow(
                    label = stringResource(R.string.settings_theme_system),
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                ) { AppSettings.setThemeMode(context, ThemeMode.SYSTEM) }
                RadioRow(
                    label = stringResource(R.string.settings_theme_light),
                    selected = settings.themeMode == ThemeMode.LIGHT,
                ) { AppSettings.setThemeMode(context, ThemeMode.LIGHT) }
                RadioRow(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = settings.themeMode == ThemeMode.DARK,
                ) { AppSettings.setThemeMode(context, ThemeMode.DARK) }
            }

            // ---- Фон ----
            BackgroundCard(settings.backgroundPath, settings.backgroundDim)

            // ---- Напоминания ----
            RemindersCard(
                vm = vm,
                bedtimeEnabled = settings.bedtimeReminder,
                morningEnabled = settings.morningReminder,
                avgBedtime = avgBedtime,
                avgWakeTime = avgWakeTime,
            )

            // ---- Язык ----
            SettingsCard(title = stringResource(R.string.settings_language)) {
                RadioRow(
                    label = stringResource(R.string.settings_language_system),
                    selected = language == AppLanguage.SYSTEM,
                ) {
                    language = AppLanguage.SYSTEM
                    AppSettings.setLanguage(context, AppLanguage.SYSTEM)
                }
                RadioRow(
                    label = stringResource(R.string.settings_language_en),
                    selected = language == AppLanguage.ENGLISH,
                ) {
                    language = AppLanguage.ENGLISH
                    AppSettings.setLanguage(context, AppLanguage.ENGLISH)
                }
                RadioRow(
                    label = stringResource(R.string.settings_language_ru),
                    selected = language == AppLanguage.RUSSIAN,
                ) {
                    language = AppLanguage.RUSSIAN
                    AppSettings.setLanguage(context, AppLanguage.RUSSIAN)
                }
            }

            // ---- Данные ----
            val emptyMsg = stringResource(R.string.settings_export_empty)
            val errorMsg = stringResource(R.string.settings_export_error)
            val savedMsg = stringResource(R.string.settings_export_saved)

            val backupSavedMsg = stringResource(R.string.settings_backup_saved)
            val backupEmptyMsg = stringResource(R.string.settings_backup_empty)
            val backupErrorMsg = stringResource(R.string.settings_backup_error)
            val restoreDoneMsg = stringResource(R.string.settings_restore_done)
            val restoreErrorMsg = stringResource(R.string.settings_restore_error)

            // выбор файла копии для восстановления
            val restorePicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    vm.restore(uri) { result ->
                        val msg =
                            if (result == null) restoreErrorMsg
                            else String.format(restoreDoneMsg, result.added, result.skipped)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_data)) {
                IconRow(
                    icon = { Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                ) {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, emptyMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        val saved = SleepExporter.exportToDownloads(context, entries)
                        val msg =
                            if (saved == null) errorMsg
                            else savedMsg.format(saved.fileName)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }

                IconRow(
                    icon = { Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_backup),
                    subtitle = stringResource(R.string.settings_backup_subtitle),
                ) {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, backupEmptyMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vm.backup { saved ->
                            val msg =
                                if (saved == null) backupErrorMsg
                                else String.format(backupSavedMsg, saved.fileName)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                IconRow(
                    icon = { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_restore),
                    subtitle = stringResource(R.string.settings_restore_subtitle),
                ) {
                    // json отдают под разными mime — берём всё, проверим при разборе
                    restorePicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }

            // ---- Проект и обновление ----
            UpdateCard()

            // ---- О приложении ----
            SettingsCard(title = stringResource(R.string.settings_about)) {
                Text(
                    stringResource(R.string.settings_about_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Выбор фонового фото и его затемнения. */
@Composable
private fun BackgroundCard(backgroundPath: String?, dim: Float) {
    val context = LocalContext.current
    val errorMsg = stringResource(R.string.settings_background_error)

    // локальное значение, чтобы слайдер двигался плавно
    var dimValue by remember(dim) { mutableStateOf(dim) }

    // URI выбранного фото, ждущего обрезки
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> pendingUri = uri }

    pendingUri?.let { uri ->
        ImageCropperDialog(
            sourceUri = uri,
            onCancel = { pendingUri = null },
            onCropped = { bitmap ->
                pendingUri = null
                if (!AppSettings.setBackgroundBitmap(context, bitmap)) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    SettingsCard(title = stringResource(R.string.settings_background)) {
        Text(
            stringResource(R.string.settings_background_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    stringResource(
                        if (backgroundPath != null) R.string.settings_background_change
                        else R.string.settings_background_pick
                    )
                )
            }
            if (backgroundPath != null) {
                TextButton(onClick = { AppSettings.clearBackgroundImage(context) }) {
                    Text(stringResource(R.string.settings_background_remove))
                }
            }
        }

        if (backgroundPath != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_background_dim, (dimValue * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = dimValue,
                onValueChange = { dimValue = it },
                onValueChangeFinished = { AppSettings.setBackgroundDim(context, dimValue) },
                valueRange = 0f..0.9f,
            )
        }
    }
}

/** Переключатели напоминаний с показом рассчитанного времени. */
@Composable
private fun RemindersCard(
    vm: SleepViewModel,
    bedtimeEnabled: Boolean,
    morningEnabled: Boolean,
    avgBedtime: LocalTime,
    avgWakeTime: LocalTime,
) {
    val context = LocalContext.current
    val timeFmt = DateFormats.time

    val deniedMsg = stringResource(R.string.settings_notifications_denied)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, deniedMsg, Toast.LENGTH_LONG).show()
    }

    /** С Android 13 без разрешения уведомления не покажутся — спрашиваем при включении. */
    fun ensurePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    SettingsCard(title = stringResource(R.string.settings_reminders)) {
        SwitchRow(
            title = stringResource(R.string.settings_bedtime_reminder),
            subtitle = stringResource(
                R.string.settings_bedtime_reminder_desc,
                avgBedtime.format(timeFmt),
            ),
            checked = bedtimeEnabled,
        ) { enabled ->
            if (enabled) ensurePermission()
            vm.setBedtimeReminder(enabled)
        }

        Spacer(Modifier.height(4.dp))

        SwitchRow(
            title = stringResource(R.string.settings_morning_reminder),
            subtitle = stringResource(
                R.string.settings_morning_reminder_desc,
                avgWakeTime.format(timeFmt),
            ),
            checked = morningEnabled,
        ) { enabled ->
            if (enabled) ensurePermission()
            vm.setMorningReminder(enabled)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_reminders_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Проверка обновлений и установка новой сборки. */
@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val checkingMsg = stringResource(R.string.update_checking)
    val upToDateMsg = stringResource(R.string.update_up_to_date)
    val noConnectionMsg = stringResource(R.string.update_no_connection)
    val startedMsg = stringResource(R.string.update_started)
    val failedMsg = stringResource(R.string.update_failed)
    val needsPermissionMsg = stringResource(R.string.update_needs_permission)

    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<UpdateChecker.Result?>(null) }

    // диалог с предложением обновиться
    available?.let { result ->
        AlertDialog(
            onDismissRequest = { available = null },
            title = { Text(stringResource(R.string.update_available, result.versionName)) },
            text = { if (result.notes.isNotBlank()) Text(result.notes) },
            confirmButton = {
                TextButton(onClick = {
                    available = null
                    ApkDownloader.enqueue(context) { status ->
                        val msg = when (status) {
                            is ApkDownloader.Status.Running -> startedMsg
                            is ApkDownloader.Status.Failed -> failedMsg
                            is ApkDownloader.Status.NeedsInstallPermission ->
                                needsPermissionMsg
                            else -> null
                        }
                        if (msg != null) {
                            val length =
                                if (status is ApkDownloader.Status.NeedsInstallPermission)
                                    Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                            Toast.makeText(context, msg, length).show()
                        }
                    }
                }) { Text(stringResource(R.string.update_download)) }
            },
            dismissButton = {
                TextButton(onClick = { available = null }) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )
    }

    SettingsCard(title = stringResource(R.string.settings_source)) {
        IconRow(
            icon = {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            title = stringResource(R.string.settings_check_update),
            subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        ) {
            if (checking) return@IconRow
            checking = true
            Toast.makeText(context, checkingMsg, Toast.LENGTH_SHORT).show()
            scope.launch {
                val result = UpdateChecker.check()
                checking = false
                when {
                    result == null ->
                        Toast.makeText(context, noConnectionMsg, Toast.LENGTH_SHORT).show()

                    result.isNewer -> available = result

                    else -> Toast.makeText(
                        context,
                        upToDateMsg.format(BuildConfig.VERSION_NAME),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
}
