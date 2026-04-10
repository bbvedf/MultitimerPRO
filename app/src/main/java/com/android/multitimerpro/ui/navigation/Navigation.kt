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
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Timers : Screen("timers", "TIMERS", Icons.Default.Timer)
    object Presets : Screen("presets", "PRESETS", Icons.Default.LibraryBooks)
    object Stats : Screen("stats", "STATS", Icons.Default.BarChart)
    object History : Screen("history", "HISTORIAL", Icons.Default.History)
    object Settings : Screen("settings", "AJUSTES", Icons.Default.Settings)
    object Login : Screen("login", "LOGIN")
    object CreateTimer : Screen("create_timer?timerId={timerId}", "CREATE") {
        fun createRoute(timerId: String? = null) = if (timerId != null) "create_timer?timerId=$timerId" else "create_timer"
    }
    object LiveTimer : Screen("live_timer/{timerId}", "LIVE") {
        fun createRoute(timerId: String) = "live_timer/$timerId"
    }
    object HistoryDetail : Screen("history_detail/{historyId}", "DETAIL") {
        fun createRoute(historyId: String) = "history_detail/$historyId"
    }
}

@Composable
fun MainNavigation(
    viewModel: TimerViewModel, // Recibimos el ViewModel de la Activity
    onGoogleSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isAuthenticated && items.any { it.route == currentDestination?.route || currentDestination?.route?.contains(it.route.split("?")[0]) == true }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.height(80.dp),
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        it,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                    viewModel = viewModel,
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
            composable(Screen.Presets.route) { 
                PresetsScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateTimer.createRoute())
                    }
                ) 
            }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
            composable(Screen.History.route) { 
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { historyId ->
                        navController.navigate(Screen.HistoryDetail.createRoute(historyId))
                    }
                ) 
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.LiveTimer.route,
                arguments = listOf(navArgument("timerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getString("timerId") ?: ""
                LiveTimerScreen(
                    timerId = timerId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.CreateTimer.route,
                arguments = listOf(navArgument("timerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getString("timerId")
                CreateTimerScreen(
                    viewModel = viewModel,
                    timerId = timerId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.HistoryDetail.route,
                arguments = listOf(navArgument("historyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val historyId = backStackEntry.arguments?.getString("historyId") ?: ""
                HistoryDetailScreen(
                    historyId = historyId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
