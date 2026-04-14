package com.android.multitimerpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimeFilter
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.translateCategory
import com.android.multitimerpro.ui.theme.NeonOrange
import com.android.multitimerpro.ui.theme.NeonPurple

@Composable
fun StatsScreen(viewModel: TimerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.stats_tab_analysis), stringResource(R.string.stats_tab_achievements))

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

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

    val categoriesStats = remember(statsByCategory, totalTimeSpent) {
        statsByCategory.map { (name, time) ->
            val percentage = if (totalTimeSpent > 0) time.toFloat() / totalTimeSpent else 0f
            CategoryStat(name, percentage, false)
        }.sortedByDescending { it.percentage }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.stats_performance_analysis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.setHistoryShowFilters(!showFilters) }, colors = IconButtonDefaults.iconButtonColors(containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = if (showFilters) Color.Black else MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            AnimatedVisibility(visible = showFilters) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.history_period), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(TimeFilter.entries.toTypedArray()) { filter ->
                                    FilterChip(selected = selectedTimeFilter == filter, onClick = { viewModel.setHistorySelectedTimeFilter(filter) }, label = { Text(stringResource(translateTimeFilterLocal(filter))) }, border = null)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.history_category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categoriesList) { category ->
                                    FilterChip(selected = selectedCategory == category, onClick = { viewModel.setHistorySelectedCategory(category) }, label = { Text(translateCategory(category)) }, border = null)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (totalTimeSpent == 0L) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Text(text = stringResource(R.string.stats_no_data) + "\n" + stringResource(R.string.stats_start_sessions), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {

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

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricCard(stringResource(R.string.stats_avg_session), formatMillisToTimeShort(averageSessionTime), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.stats_peak_day), mostProductiveDay, Modifier.weight(1f))
                }
            }

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

            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = stringResource(R.string.stats_daily_performance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.stats_daily_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        val maxDayTime = activityLast7Days.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                        for ((day, time) in activityLast7Days) {
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

            if (categoriesStats.isNotEmpty()) {
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(text = stringResource(R.string.stats_category_dist), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                for (stat in categoriesStats.take(5)) {
                                    CategoryBar(stat, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = stringResource(R.string.stats_focus_analysis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    
                    if (categoriesStats.isNotEmpty()) {
                        AnalysisBadge(
                            icon = Icons.Default.Category,
                            color = MaterialTheme.colorScheme.primary,
                            text = stringResource(R.string.stats_dominant_cat, translateCategory(categoriesStats.first().name))
                        )
                    }
                    
                    AnalysisBadge(
                        icon = Icons.Default.Timer,
                        color = NeonOrange,
                        text = stringResource(R.string.stats_top_investment, topTimerName)
                    )
                }
            }

            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.stats_today_prod), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.stats_today_msg), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun AnalysisBadge(icon: ImageVector, color: Color, text: String) {
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
        Text(text = translateCategory(stat.name).uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, maxLines = 1)
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
            Surface(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
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
    Surface(
        modifier = modifier.height(140.dp), // Altura fija para consistencia
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centrado vertical
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { 
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) 
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(translateMedalLocal(id)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1 // Evitar saltos de línea que rompan el diseño
            )
            Text(
                text = stringResource(translateMedalDescLocal(id)),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.sp, // Un pelín más pequeña
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 9.sp,
                maxLines = 2
            )
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

private fun translateTimeFilterLocal(filter: TimeFilter): Int = when (filter) {
    TimeFilter.ALL -> R.string.filter_all
    TimeFilter.TODAY -> R.string.filter_today
    TimeFilter.WEEK -> R.string.filter_week
    TimeFilter.MONTH -> R.string.filter_month
}

private fun formatMillisToTime(millis: Long): String {
    val h = millis / 3600000
    val m = (millis % 3600000) / 60000
    val s = (millis % 60000) / 1000
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatMillisToTimeShort(millis: Long): String {
    val m = millis / 60000
    return "${m}m"
}

data class CategoryStat(val name: String, val percentage: Float, val isActive: Boolean)
