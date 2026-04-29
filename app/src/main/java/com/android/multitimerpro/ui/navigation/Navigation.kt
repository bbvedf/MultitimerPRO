package com.android.multitimerpro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
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
import com.android.multitimerpro.ui.components.UpgradeProDialog
import com.android.multitimerpro.ui.screens.*
import com.android.multitimerpro.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.rememberCoroutineScope

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector? = null) {
    object Timers : Screen("timers", R.string.nav_timers, Icons.Default.Timer)
    object Presets : Screen("presets", R.string.nav_presets, Icons.AutoMirrored.Filled.LibraryBooks)
    object Stats : Screen("stats", R.string.nav_stats, Icons.Default.BarChart)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Login : Screen("login", R.string.app_name)
    object Register : Screen("register", R.string.login_register_now)
    object ForgotPassword : Screen("forgot_password", R.string.login_password_label)
    object ResetPassword : Screen("reset_password", R.string.login_password_label)
    object CreateTimer : Screen("create_timer?timerId={timerId}&isPreset={isPreset}", R.string.timer_new) {
        fun createRoute(timerId: String? = null, isPreset: Boolean = false) = 
            "create_timer?timerId=${timerId ?: ""}&isPreset=$isPreset"
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
    val isRecoveryMode by viewModel.isRecoveryMode.collectAsState()
    val showProUpgradeDialog by viewModel.showProUpgradeDialog.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementDialog by remember { mutableStateOf<String?>(null) }
    val showCollectionCompleteDialog by viewModel.showCollectionCompleteDialog.collectAsState()

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
            medalIdWithTier = showAchievementDialog!!,
            viewModel = viewModel,
            onDismiss = { showAchievementDialog = null }
        )
    }

    if (showProUpgradeDialog) {
        UpgradeProDialog(
            onDismiss = { viewModel.dismissProUpgradeDialog() },
            onUpgrade = { /* TODO: Implementar navegación a suscripción */ }
        )
    }

    if (showCollectionCompleteDialog != null && showAchievementDialog == null) {
        CollectionCompleteDialog(
            tier = showCollectionCompleteDialog!!,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissCollectionDialog() }
        )
    }

    val scope = rememberCoroutineScope()
    val items = listOf(
        Screen.Timers,
        Screen.Presets,
        Screen.Stats,
        Screen.History,
        Screen.Settings
    )
    
    val showBottomBar = currentDestination?.route in items.map { it.route } || currentDestination?.route == "main_pager"
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Sincronizar Pager con la selección de la BottomBar (si se navega vía click)
    LaunchedEffect(currentDestination) {
        val index = items.indexOfFirst { it.route == currentDestination?.route }
        if (index != -1 && index != pagerState.currentPage) {
            pagerState.scrollToPage(index)
        }
    }

    // Sincronizar BottomBar con el Swipe del Pager
    LaunchedEffect(pagerState.currentPage) {
        val targetRoute = items[pagerState.currentPage].route
        if (currentDestination?.route != targetRoute && 
            items.any { it.route == currentDestination?.route }) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.height(80.dp),
                    tonalElevation = 8.dp
                ) {
                    items.forEachIndexed { index, screen ->
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
                                scope.launch { pagerState.animateScrollToPage(index) }
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
            startDestination = if (isAuthenticated) "main_pager" else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Ruta contenedora del Pager para las pestañas principales
            composable("main_pager") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1 // Mantiene la página contigua cargada para suavidad
                ) { page ->
                    when (val screen = items[page]) {
                        Screen.Timers -> MultiTimerHomeScreen(
                            viewModel = viewModel,
                            onNavigateToCreate = { id -> navController.navigate(Screen.CreateTimer.createRoute(id)) },
                            onNavigateToLive = { id -> navController.navigate(Screen.LiveTimer.createRoute(id)) }
                        )
                        Screen.Presets -> PresetsScreen(
                            viewModel = viewModel,
                            onNavigateToCreate = { id -> navController.navigate(Screen.CreateTimer.createRoute(id, isPreset = true)) }
                        )
                        Screen.Stats -> StatsScreen(viewModel)
                        Screen.History -> HistoryScreen(
                            viewModel = viewModel,
                            onNavigateToDetail = { id -> navController.navigate(Screen.HistoryDetail.createRoute(id)) }
                        )
                        Screen.Settings -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { /* En el pager, el back no aplica igual */ }
                        )
                        else -> {
                            // Exhaustive when handling for other screens that might be in the list
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onGoogleLogin = onGoogleSignIn,
                    onEmailLogin = { email, password ->
                        viewModel.signIn(email, password)
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
                        viewModel.signUp(email, password)
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
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(
                    viewModel = viewModel,
                    onBack = { 
                        viewModel.setRecoveryMode(false)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
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
                    onNavigateToCreate = { id ->
                        navController.navigate(Screen.CreateTimer.createRoute(id, isPreset = true))
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
                arguments = listOf(
                    navArgument("timerId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("isPreset") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getString("timerId").takeIf { !it.isNullOrEmpty() }
                val isPreset = backStackEntry.arguments?.getBoolean("isPreset") ?: false
                CreateTimerScreen(
                    viewModel = viewModel,
                    timerId = timerId,
                    isPresetMode = isPreset,
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
fun AchievementUnlockedDialog(medalIdWithTier: String, viewModel: TimerViewModel, onDismiss: () -> Unit) {
    val baseId = medalIdWithTier.substringBeforeLast("_")
    val tier = medalIdWithTier.substringAfterLast("_").toIntOrNull() ?: 1
    
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()

    val tierName = when(tier) {
        3 -> stringResource(R.string.medal_tier_gold)
        2 -> stringResource(R.string.medal_tier_silver)
        1 -> stringResource(R.string.medal_tier_bronze)
        else -> ""
    }

    val tierColor = when(tier) {
        3 -> if (isDark) MedalGold else MedalGoldLight
        2 -> if (isDark) MedalSilver else MedalSilverLight
        1 -> if (isDark) MedalBronze else MedalBronzeLight
        else -> MaterialTheme.colorScheme.primary
    }

    val dialogBg = when {
        isDark -> when(tier) {
            3 -> GoldBgDark
            2 -> SilverBgDark
            else -> BronzeBgDark
        }
        else -> when(tier) {
            3 -> GoldBgLight
            2 -> SilverBgLight
            else -> BronzeBgLight
        }
    }
    
    val textColor = if (isDark) Color.White else DeepGrey

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            tonalElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, tierColor.copy(alpha = if (isDark) 0.5f else 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tierColor.copy(alpha = if (isDark) 0.2f else 0.15f), CircleShape)
                    )
                    Icon(
                        imageVector = getMedalIconLocal(baseId),
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.new_medal_unlocked).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tierColor,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(translateMedalLocal(baseId)).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        color = tierColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tierName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                    }
                }

                Text(
                    text = stringResource(translateMedalDescLocal(baseId)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tierColor,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.protocol_understood).uppercase(),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Botón de Compartir alineado a la derecha
                val context = androidx.compose.ui.platform.LocalContext.current
                val shareTitle = stringResource(R.string.new_medal_unlocked)
                val medalName = stringResource(translateMedalLocal(baseId))
                val shareBody = stringResource(R.string.share_text_medal, tierName, medalName)
                val chooserTitle = stringResource(R.string.share_achievement)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, shareTitle)
                                putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = tierColor)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(chooserTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionCompleteDialog(tier: String, viewModel: TimerViewModel, onDismiss: () -> Unit) {
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()

    val tierColor = when (tier) {
        "GOLD" -> if (isDark) MedalGold else MedalGoldLight
        "SILVER" -> if (isDark) MedalSilver else MedalSilverLight
        else -> if (isDark) MedalBronze else MedalBronzeLight
    }

    val dialogBg = when {
        isDark -> when(tier) {
            "GOLD" -> GoldBgDark
            "SILVER" -> SilverBgDark
            else -> BronzeBgDark
        }
        else -> when(tier) {
            "GOLD" -> GoldBgLight
            "SILVER" -> SilverBgLight
            else -> BronzeBgLight
        }
    }

    val textColor = if (isDark) Color.White else DeepGrey

    val isPro by viewModel.isPro.collectAsState()

    val rewardText = when (tier) {
        "BRONZE" -> stringResource(R.string.reward_bronze)
        "SILVER" -> stringResource(R.string.reward_silver)
        "GOLD" -> stringResource(R.string.reward_gold)
        else -> ""
    }

    val showRewardCard = when (tier) {
        "BRONZE", "SILVER" -> !isPro
        else -> true // GOLD reward (Badge) is always shown
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, tierColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(80.dp)
                )
                Text(
                    text = stringResource(R.string.collection_complete_title, tier),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.collection_complete_msg, tier),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.8f)
                )
                if (showRewardCard) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = tierColor.copy(alpha = if (isDark) 0.15f else 0.1f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = rewardText,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = tierColor,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tierColor, contentColor = if (isDark) Color.Black else Color.White)
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.Black)
                }

                // Botón de Compartir alineado a la derecha
                val context = androidx.compose.ui.platform.LocalContext.current
                val shareBody = stringResource(R.string.share_text_collection, tier)
                val chooserTitle = stringResource(R.string.share_achievement)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, tier)
                                putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = tierColor)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(chooserTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun getMedalIconLocal(id: String) = when(id) {
    "medal_deep_work" -> Icons.Default.HistoryEdu
    "medal_early_bird" -> Icons.Default.WbTwilight
    "medal_night_owl" -> Icons.Default.NightsStay
    "medal_weekend" -> Icons.Default.FitnessCenter
    "medal_collector" -> Icons.Default.AutoAwesomeMotion
    "medal_veteran" -> Icons.Default.VerifiedUser
    "medal_hyperfocus" -> Icons.Default.Bolt
    "medal_consistency" -> Icons.Default.CalendarToday
    "medal_architect" -> Icons.Default.Architecture
    "medal_finisher" -> Icons.Default.TaskAlt
    "medal_polymath" -> Icons.Default.Psychology
    "medal_zen_master" -> Icons.Default.SelfImprovement
    else -> Icons.Default.MilitaryTech
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
    "medal_architect" -> R.string.medal_architect
    "medal_finisher" -> R.string.medal_finisher
    "medal_polymath" -> R.string.medal_polymath
    "medal_zen_master" -> R.string.medal_zen_master
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
    "medal_architect" -> R.string.medal_architect_desc
    "medal_finisher" -> R.string.medal_finisher_desc
    "medal_polymath" -> R.string.medal_polymath_desc
    "medal_zen_master" -> R.string.medal_zen_master_desc
    else -> R.string.app_name
}
