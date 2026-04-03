package com.android.multitimerpro.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.multitimerpro.ui.TimerViewModel
import com.android.multitimerpro.ui.screens.*
import com.android.multitimerpro.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Timers : Screen("timers", "TIMERS", Icons.Default.Timer)
    object Presets : Screen("presets", "PRESETS", Icons.Default.LibraryBooks)
    object Stats : Screen("stats", "STATS", Icons.Default.BarChart)
    object History : Screen("history", "HISTORIAL", Icons.Default.History)
    object CreateTimer : Screen("create_timer", "CREATE")
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: TimerViewModel = hiltViewModel()
    val items = listOf(
        Screen.Timers,
        Screen.Presets,
        Screen.Stats,
        Screen.History
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DeepBlack.copy(alpha = 0.95f),
                tonalElevation = 0.dp,
                modifier = Modifier.height(80.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            screen.icon?.let {
                                Icon(it, contentDescription = null, modifier = Modifier.size(24.dp)) 
                            }
                        },
                        label = {
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonBlue,
                            selectedTextColor = NeonBlue,
                            unselectedIconColor = OnSurfaceVariant.copy(alpha = 0.5f),
                            unselectedTextColor = OnSurfaceVariant.copy(alpha = 0.5f),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timers.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timers.route) {
                MultiTimerHomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = { navController.navigate(Screen.CreateTimer.route) }
                )
            }
            composable(Screen.Presets.route) { PresetsScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.CreateTimer.route) {
                CreateTimerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
