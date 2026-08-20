package com.example.sleeptracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sleeptracker.ui.screens.AnalyticsScreen
import com.example.sleeptracker.ui.screens.EntryEditorScreen
import com.example.sleeptracker.ui.screens.JournalScreen

private object Routes {
    const val JOURNAL = "journal"
    const val ANALYTICS = "analytics"
    const val EDITOR = "editor"
}

@Composable
fun SleepApp() {
    val navController = rememberNavController()
    val vm: SleepViewModel = viewModel(factory = SleepViewModel.Factory)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = currentRoute?.hierarchy?.any {
        it.route == Routes.JOURNAL || it.route == Routes.ANALYTICS
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute?.route == Routes.JOURNAL,
                        onClick = { navController.navigateTab(Routes.JOURNAL) },
                        icon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                        label = { Text("Дневник") },
                    )
                    NavigationBarItem(
                        selected = currentRoute?.route == Routes.ANALYTICS,
                        onClick = { navController.navigateTab(Routes.ANALYTICS) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Аналитика") },
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
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(vm = vm)
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
