package com.example.sleeptracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sleeptracker.R
import com.example.sleeptracker.ui.components.OnboardingDialog
import com.example.sleeptracker.ui.screens.AnalyticsScreen
import com.example.sleeptracker.ui.screens.EntryEditorScreen
import com.example.sleeptracker.ui.screens.JournalScreen
import com.example.sleeptracker.ui.screens.SettingsScreen

private object Routes {
    const val JOURNAL = "journal"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val EDITOR = "editor"
}

@Composable
fun SleepApp(hasBackgroundImage: Boolean = false) {
    val navController = rememberNavController()
    val vm: SleepViewModel = viewModel(factory = SleepViewModel.Factory)

    val settings by vm.settings.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute == Routes.JOURNAL || currentRoute == Routes.ANALYTICS

    // разрешение на уведомления нужно только с Android 13
    var pendingEnable by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // напоминания включаем в любом случае: если пользователь позже разрешит
        // уведомления в системных настройках, они начнут приходить без доп. действий
        if (pendingEnable) {
            vm.completeOnboarding(enableReminders = true)
            pendingEnable = false
        }
    }

    if (!settings.onboardingShown) {
        OnboardingDialog(
            onEnable = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pendingEnable = true
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.completeOnboarding(enableReminders = true)
                }
            },
            onSkip = { vm.completeOnboarding(enableReminders = false) },
        )
    }

    Scaffold(
        // с фото на фоне подложки должны просвечивать
        containerColor = if (hasBackgroundImage) Color.Transparent
        else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = if (hasBackgroundImage)
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.JOURNAL,
                        onClick = { navController.navigateTab(Routes.JOURNAL) },
                        icon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_journal)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ANALYTICS,
                        onClick = { navController.navigateTab(Routes.ANALYTICS) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_analytics)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.JOURNAL,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.JOURNAL) {
                JournalScreen(
                    vm = vm,
                    onAdd = { navController.navigate("${Routes.EDITOR}/0") },
                    onEdit = { id -> navController.navigate("${Routes.EDITOR}/$id") },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(vm = vm)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("${Routes.EDITOR}/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EntryEditorScreen(
                    vm = vm,
                    entryId = id,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
