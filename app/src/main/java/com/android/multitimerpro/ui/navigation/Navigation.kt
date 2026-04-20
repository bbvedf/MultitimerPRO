package com.android.multitimerpro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.GoogleAuthClient
import com.android.multitimerpro.ui.screens.*
import com.android.multitimerpro.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector? = null) {
    object Timers : Screen("timers", R.string.nav_timers, Icons.Default.Timer)
    object Presets : Screen("presets", R.string.nav_presets, Icons.Default.LibraryBooks)
    object Stats : Screen("stats", R.string.nav_stats, Icons.Default.BarChart)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Login : Screen("login", R.string.app_name)
    object Register : Screen("register", R.string.login_register_now)
    object ForgotPassword : Screen("forgot_password", R.string.login_password_label)
    object CreateTimer : Screen("create_timer?timerId={timerId}", R.string.timer_new) {
        fun createRoute(timerId: String? = null) = if (timerId != null) "create_timer?timerId=$timerId" else "create_timer"
    }
    object LiveTimer : Screen("live_timer/{timerId}", R.string.start) {
        fun createRoute(timerId: String) = "live_timer/$timerId"
    }
    object HistoryDetail : Screen("history_detail/{historyId}", R.string.history_duration) {
        fun createRoute(historyId: String) = "history_detail/$historyId"
    }
}

@Composable
fun MainNavigation(
    viewModel: TimerViewModel,
    onGoogleSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.newAchievementEvent.collectLatest { medalId ->
            showAchievementDialog = medalId
        }
    }

    if (showAchievementDialog != null) {
        AchievementUnlockedDialog(
            medalId = showAchievementDialog!!,
            onDismiss = { showAchievementDialog = null }
        )
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
                                    stringResource(screen.labelRes),
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
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgot = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = viewModel,
                    onEmailRegister = { email, password ->
                        viewModel.signUpWithEmail(email, password)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
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

@Composable
fun AchievementUnlockedDialog(medalId: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMedalIconLocal(medalId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.new_medal_unlocked),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(translateMedalLocal(medalId)).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = stringResource(translateMedalDescLocal(medalId)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.protocol_understood), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getMedalIconLocal(id: String) = when(id) {
    "medal_deep_work" -> Icons.Default.Timer
    "medal_early_bird" -> Icons.Default.Timer
    "medal_night_owl" -> Icons.Default.Timer
    "medal_weekend" -> Icons.Default.Timer
    "medal_collector" -> Icons.Default.Timer
    "medal_veteran" -> Icons.Default.Timer
    "medal_hyperfocus" -> Icons.Default.Timer
    "medal_consistency" -> Icons.Default.Timer
    else -> Icons.Default.Timer
}

private fun translateMedalLocal(medal: String) = when(medal) {
    "medal_deep_work" -> R.string.medal_deep_work
    "medal_early_bird" -> R.string.medal_early_bird
    "medal_night_owl" -> R.string.medal_night_owl
    "medal_weekend" -> R.string.medal_weekend
    "medal_collector" -> R.string.medal_collector
    "medal_veteran" -> R.string.medal_veteran
    "medal_hyperfocus" -> R.string.medal_hyperfocus
    "medal_consistency" -> R.string.medal_consistency
    else -> R.string.app_name
}

private fun translateMedalDescLocal(medal: String) = when(medal) {
    "medal_deep_work" -> R.string.medal_deep_work_desc
    "medal_early_bird" -> R.string.medal_early_bird_desc
    "medal_night_owl" -> R.string.medal_night_owl_desc
    "medal_weekend" -> R.string.medal_weekend_desc
    "medal_collector" -> R.string.medal_collector_desc
    "medal_veteran" -> R.string.medal_veteran_desc
    "medal_hyperfocus" -> R.string.medal_hyperfocus_desc
    "medal_consistency" -> R.string.medal_consistency_desc
    else -> R.string.app_name
}
