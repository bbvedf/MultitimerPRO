package com.android.multitimerpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.TimeFilter
import com.android.multitimerpro.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun StatsScreen(viewModel: TimerViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.stats_tab_analysis), stringResource(R.string.stats_tab_achievements))
    var activeAchievementId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.newAchievementEvent.collectLatest { achievementId ->
            activeAchievementId = achievementId
        }
    }

    if (activeAchievementId != null) {
        AchievementUnlockedDialog(
            achievementId = activeAchievementId!!,
            onDismiss = { activeAchievementId = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // Tab Selector
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == index) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "TabTransition"
        ) { tabIndex ->
            if (tabIndex == 0) {
                AnalysisTab(viewModel)
            } else {
                AchievementsTab(viewModel)
            }
        }
    }
}

@Composable
fun AchievementUnlockedDialog(achievementId: String, onDismiss: () -> Unit) {
    val medalIcon = getMedalIcon(achievementId)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(24.dp).fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonOrange)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(text = stringResource(R.string.new_medal_unlocked), style = MaterialTheme.typography.labelSmall, color = NeonOrange, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Box(modifier = Modifier.size(120.dp).scale(scale).clip(CircleShape).background(NeonOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = medalIcon, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(64.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(translateMedalLocal(achievementId)).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(translateMedalDescLocal(achievementId)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.protocol_understood), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnalysisTab(viewModel: TimerViewModel) {
    val statsByCategory by viewModel.statsByCategory.collectAsState()
    val totalTimeSpent by viewModel.totalTimeSpent.collectAsState()
    val filteredHistory by viewModel.filteredHistory.collectAsState()
    val historyItems by viewModel.history.collectAsState()
    val activityLast7Days by viewModel.activityLast7Days.collectAsState()
    val averageSessionTime by viewModel.averageSessionTime.collectAsState()
    val mostProductiveDay by viewModel.mostProductiveDay.collectAsState()
    val topTimerName by viewModel.topTimerName.collectAsState()
    val showFilters by viewModel.historyShowFilters.collectAsState()
    val selectedCategory by viewModel.historySelectedCategory.collectAsState()
    val selectedTimeFilter by viewModel.historySelectedTimeFilter.collectAsState()

    val categoriesList = remember(historyItems) {
        val uniqueCats = historyItems.map { it.category }.distinct().sorted()
        listOf("ALL") + uniqueCats
    }

    val categoriesStats = statsByCategory.map { (name, time) ->
        val percentage = if (totalTimeSpent > 0) time.toFloat() / totalTimeSpent else 0f
        CategoryStat(name, percentage, false)
    }.sortedByDescending { it.percentage }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.stats_performance_analysis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.setHistoryShowFilters(!showFilters) }, colors = IconButtonDefaults.iconButtonColors(containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = if (showFilters) Color.Black else MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Filters
        item {
            AnimatedVisibility(visible = showFilters) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.history_period), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(TimeFilter.entries) { filter ->
                                    FilterChip(selected = selectedTimeFilter == filter, onClick = { viewModel.setHistorySelectedTimeFilter(filter) }, label = { Text(translateTimeFilterLocal(filter)) }, border = null)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.history_category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categoriesList) { category ->
                                    FilterChip(selected = selectedCategory == category, onClick = { viewModel.setHistorySelectedCategory(category) }, label = { Text(translateCategoryLocal(category)) }, border = null)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.stats_total_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMillisToTime(totalTimeSpent), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.stats_sessions), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filteredHistory.size}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Metrics Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(stringResource(R.string.stats_avg_session), formatMillisToTimeShort(averageSessionTime), Modifier.weight(1f))
                MetricCard(stringResource(R.string.stats_peak_day), mostProductiveDay, Modifier.weight(1f))
            }
        }

        // Top Timer Badge
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(NeonPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.stats_star_timer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                        Text(text = topTimerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = NeonPurple)
                    }
                }
            }
        }

        // Activity Last 7 Days
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = stringResource(R.string.stats_daily_performance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.stats_daily_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    val maxDayTime = activityLast7Days.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                    activityLast7Days.forEach { (day, time) ->
                        val progress = time.toFloat() / maxDayTime.toFloat()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(day, modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)))))
                            }
                            Text(formatMillisToTimeShort(time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Distribution by Category (RESTAURADO)
        if (categoriesStats.isNotEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = stringResource(R.string.stats_category_dist), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            categoriesStats.take(5).forEach { stat ->
                                CategoryBar(stat, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Focus Analysis Badges (RESTAURADO)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(R.string.stats_focus_analysis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                
                // Dominant Category Badge
                if (categoriesStats.isNotEmpty()) {
                    AnalysisBadge(
                        icon = Icons.Default.Category,
                        color = MaterialTheme.colorScheme.primary,
                        text = stringResource(R.string.stats_dominant_cat, translateCategoryLocal(categoriesStats.first().name))
                    )
                }
                
                // Top Investment Badge
                AnalysisBadge(
                    icon = Icons.Default.Timer,
                    color = NeonOrange,
                    text = stringResource(R.string.stats_top_investment, topTimerName)
                )
            }
        }

        // Today Productivity (RESTAURADO)
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.stats_today_prod), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.stats_today_msg), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun AnalysisBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun CategoryBar(stat: CategoryStat, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(140.dp * stat.percentage.coerceIn(0.05f, 1f)).clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))))
        Text(text = translateCategoryLocal(stat.name).uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AchievementsTab(viewModel: TimerViewModel) {
    val xp by viewModel.currentXP.collectAsState()
    val rank by viewModel.currentRank.collectAsState()
    val nextRankXP by viewModel.nextRankXP.collectAsState()
    val unlockedMedals by viewModel.unlockedMedals.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.gamification_help_title), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(R.string.gamification_help_desc), lineHeight = 20.sp) },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("OK", fontWeight = FontWeight.Bold) } },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(translateRankLocal(rank)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    val progress = (xp.toFloat() / nextRankXP.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$xp XP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.xp_to_next_level, nextRankXP - xp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.operational_achievements_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showHelpDialog = true }) { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        item {
            val allMedals = listOf("medal_deep_work", "medal_early_bird", "medal_night_owl", "medal_weekend", "medal_collector", "medal_veteran", "medal_hyperfocus", "medal_consistency")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                allMedals.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { id -> MedalCard(id = id, isUnlocked = unlockedMedals.contains(id), modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun MedalCard(id: String, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    val color = if (isUnlocked) NeonOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val icon = getMedalIcon(id)
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
            Text(text = stringResource(translateMedalLocal(id)).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(text = stringResource(translateMedalDescLocal(id)), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, lineHeight = 10.sp, maxLines = 2)
        }
    }
}

private fun getMedalIcon(id: String) = when(id) {
    "medal_deep_work" -> Icons.Default.HistoryEdu
    "medal_early_bird" -> Icons.Default.WbTwilight
    "medal_night_owl" -> Icons.Default.NightsStay
    "medal_weekend" -> Icons.Default.FitnessCenter
    "medal_collector" -> Icons.Default.AutoAwesomeMotion
    "medal_veteran" -> Icons.Default.VerifiedUser
    "medal_hyperfocus" -> Icons.Default.Bolt
    "medal_consistency" -> Icons.Default.CalendarToday
    else -> Icons.Default.MilitaryTech
}

private fun translateRankLocal(rank: String) = when(rank) {
    "rank_novice" -> R.string.rank_novice
    "rank_technician" -> R.string.rank_technician
    "rank_master" -> R.string.rank_master
    "rank_architect" -> R.string.rank_architect
    else -> R.string.rank_novice
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

@Composable
private fun translateTimeFilterLocal(filter: TimeFilter) = when(filter) {
    TimeFilter.ALL -> stringResource(R.string.filter_all)
    TimeFilter.TODAY -> stringResource(R.string.filter_today)
    TimeFilter.WEEK -> stringResource(R.string.filter_week)
    TimeFilter.MONTH -> stringResource(R.string.filter_month)
}

@Composable
private fun translateCategoryLocal(internalName: String): String {
    return when(internalName.uppercase()) {
        "ALL" -> stringResource(R.string.category_all)
        "GENERAL" -> stringResource(R.string.cat_general)
        "WORK" -> stringResource(R.string.cat_work)
        "LEISURE" -> stringResource(R.string.cat_leisure)
        "OTHERS" -> stringResource(R.string.cat_other)
        else -> internalName
    }
}

private fun formatMillisToTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatMillisToTimeShort(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return if (hours > 0) String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    else String.format(Locale.getDefault(), "%dm", minutes)
}

data class CategoryStat(val name: String, val percentage: Float, val isActive: Boolean)
