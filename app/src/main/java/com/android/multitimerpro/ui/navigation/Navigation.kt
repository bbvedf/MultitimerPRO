package com.android.multitimerpro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.GoogleAuthClient
import com.android.multitimerpro.ui.screens.*
import com.android.multitimerpro.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Timers : Screen("timers", "TIMERS", Icons.Default.Timer)
    object Presets : Screen("presets", "PRESETS", Icons.Default.LibraryBooks)
    object Stats : Screen("stats", "STATS", Icons.Default.BarChart)
    object History : Screen("history", "HISTORIAL", Icons.Default.History)
    object Settings : Screen("settings", "AJUSTES", Icons.Default.Settings)
    object Login : Screen("login", "LOGIN")
    object CreateTimer : Screen("create_timer?timerId={timerId}", "CREATE") {
        fun createRoute(timerId: Int? = null) = if (timerId != null) "create_timer?timerId=$timerId" else "create_timer"
    }
    object LiveTimer : Screen("live_timer/{timerId}", "LIVE") {
        fun createRoute(timerId: Int) = "live_timer/$timerId"
    }
}

@Composable
fun MainNavigation(onGoogleSignIn: () -> Unit) {
    val navController = rememberNavController()
    val viewModel: TimerViewModel = hiltViewModel()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(Screen.Timers.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    val items = listOf(
        Screen.Timers,
        Screen.Presets,
        Screen.Stats,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (isAuthenticated) {
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
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) Screen.Timers.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onGoogleLogin = onGoogleSignIn,
                    onEmailLogin = { email, password ->
                        viewModel.signInWithEmail(email, password)
                    },
                    onRegisterClick = {
                        viewModel.signUpWithEmail("test@test.com", "123456")
                    }
                )
            }
            composable(Screen.Timers.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = { timerId ->
                        navController.navigate(Screen.CreateTimer.createRoute(timerId))
                    },
                    onNavigateToLive = { timerId ->
                        navController.navigate(Screen.LiveTimer.createRoute(timerId))
                    }
                )
            }
            composable(Screen.Presets.route) { PresetsScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.LiveTimer.route,
                arguments = listOf(navArgument("timerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getInt("timerId") ?: -1
                LiveTimerScreen(
                    timerId = timerId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.CreateTimer.route,
                arguments = listOf(navArgument("timerId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getInt("timerId") ?: -1
                CreateTimerScreen(
                    viewModel = viewModel,
                    timerId = if (timerId != -1) timerId else null,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
