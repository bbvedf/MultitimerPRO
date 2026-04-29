package com.android.multitimerpro.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.android.multitimerpro.ui.components.LegendaryBadge
import com.android.multitimerpro.ui.components.translateCategory
import com.android.multitimerpro.ui.theme.*
import com.android.multitimerpro.util.ShareUtils
import java.text.SimpleDateFormat
import java.util.*

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
            val percentageValue = if (totalTimeSpent > 0) time.toFloat() / totalTimeSpent else 0f
            CategoryStat(name, percentageValue, false)
        }.sortedByDescending { it.percentage }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            val isPro by viewModel.isPro.collectAsState()
            val isLegendary by viewModel.isLegendary.collectAsState()
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.stats_performance_analysis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        if (isPro) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isLegendary) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LegendaryBadge()
                        }
                    }
                    Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.setHistoryShowFilters(!showFilters) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val activityList = activityLast7Days.toList()
                        val maxDayTime = activityList.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                        for ((day, time) in activityList) {
                            val progress = time.toFloat() / maxDayTime.toFloat()
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(day.toString(), modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
fun DailyStreakRefinery(viewModel: TimerViewModel) {
    val streak by viewModel.checkInStreak.collectAsState()
    val totalCheckIns by viewModel.totalCheckIns.collectAsState()
    val lastCheckIn by viewModel.lastCheckIn.collectAsState()
    
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val hasCheckedInToday = lastCheckIn.toLong() >= todayStart
    val yesterdayStart = todayStart - 86400000L
    
    // Si la última vez que hizo checkin fue antes de ayer, la racha visual se ha perdido
    val isStreakBroken = lastCheckIn.toLong() < yesterdayStart && lastCheckIn.toLong() != 0L
    val displayStreak = if (isStreakBroken && !hasCheckedInToday) 0 else streak
    
    val rewards = listOf(5, 10, 15, 20, 25, 30, 50)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (hasCheckedInToday) stringResource(R.string.daily_streak_done).uppercase() 
                               else stringResource(R.string.daily_streak_pending).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasCheckedInToday) NeonOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.daily_streak_total, totalCheckIns),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                
                if (!hasCheckedInToday) {
                    Button(
                        onClick = { viewModel.performDailyCheckIn() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_check_in), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..6) {
                    val isActive = i < displayStreak
                    val isTodayGoal = if (hasCheckedInToday) i == displayStreak - 1 else i == displayStreak
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isActive -> NeonOrange.copy(alpha = 0.2f)
                                        isTodayGoal -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                                .let { if (isTodayGoal && !hasCheckedInToday) it.border(1.dp, NeonOrange, CircleShape) else it },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "+${rewards[i]}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
    val minRankXP by viewModel.minRankXP.collectAsState()
    
    val rankStr = rank.toString()
    val rankKey = if (rankStr.contains("|")) rankStr.split("|")[0] else rankStr
    val supremeLevel = if (rankStr.contains("|")) rankStr.split("|")[1].toIntOrNull() else null
    
    val unlockedMedals by viewModel.unlockedMedals.collectAsState()
    val colorTokens by viewModel.colorTokens.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val showTimerAviso by viewModel.showTimerTokenAviso.collectAsState()
    val showProUpgradeDialog by viewModel.showProUpgradeDialog.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.gamification_help_title), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(R.string.gamification_help_desc), lineHeight = 20.sp) },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold) } },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ProGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()
    val proBrush = Brush.linearGradient(
        colors = if (isDark) listOf(ProCardDarkStart, ProCardDarkMiddle, ProCardDarkEnd)
                 else listOf(ProCardLightStart, ProCardLightEnd)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            DailyStreakRefinery(viewModel)
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isPro) Modifier.border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(listOf(ProGlowGold, ProGlowOrange, ProGlowGold)),
                            shape = RoundedCornerShape(28.dp)
                        ) else Modifier
                    ),
                color = if (isPro) Color.Transparent else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                border = if (!isPro) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null,
                shadowElevation = if (isPro) 12.dp else 0.dp,
                tonalElevation = if (isPro) 8.dp else 0.dp
            ) {
                Box(modifier = Modifier.then(if (isPro) Modifier.background(proBrush) else Modifier)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isPro) {
                                        if (isDark) Modifier.border(2.dp, ProGlowGold.copy(alpha = glowAlpha), CircleShape)
                                        else Modifier.border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                                    } else Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = if (isPro) {
                                    if (isDark) ProGlowGold else OnSurfaceLight
                                } else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val rankName = stringResource(translateRankLocal(rankKey)).uppercase()
                        val levelPrefix = stringResource(R.string.level_prefix)
                        Text(
                            text = if (supremeLevel != null) "$rankName $levelPrefix $supremeLevel" else rankName,
                            style = if (rankKey == "rank_supreme") MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.Black, 
                            color = if (isPro) {
                                if (isDark) Color.White else OnSurfaceLight
                            } else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = if (nextRankXP.toLong() > minRankXP.toLong()) {
                            ((xp.toLong() - minRankXP.toLong()).toFloat() / (nextRankXP.toLong() - minRankXP.toLong()).toFloat()).coerceIn(0f, 1f)
                        } else 1f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = if (isPro) {
                                if (isDark) ProGlowGold else OnSurfaceLight
                            } else MaterialTheme.colorScheme.primary,
                            trackColor = if (isPro) {
                                if (isDark) Color.White.copy(alpha = 0.2f) else OnSurfaceLight.copy(alpha = 0.05f)
                            } else MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "$xp ${stringResource(R.string.xp_label)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPro) {
                                    if (isDark) ProGlowGold else OnSurfaceLight
                                } else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.xp_to_next_level, nextRankXP - xp).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPro) {
                                    if (isDark) Color.White.copy(alpha = 0.7f) else OnSurfaceLight.copy(alpha = 0.6f)
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (colorTokens > 0 && !isPro) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TokenReminder(
                                tokens = colorTokens,
                                isColorToken = true,
                                onInfoClick = {}
                            )
                        }

                        if (showTimerAviso && !isPro) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TokenReminder(
                                tokens = 1,
                                isColorToken = false,
                                onInfoClick = { viewModel.redeemTimerToken() }
                            )
                        }
                    }
                }
            }
        }

        item {
            OperationalStatusBoxes(viewModel)
        }

        item {
            ResetAchievementsSection(viewModel)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.operational_achievements_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showHelpDialog = true }) { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        item {
            val allMedalIds = listOf(
                "medal_deep_work", "medal_early_bird", "medal_night_owl", "medal_weekend",
                "medal_collector", "medal_veteran", "medal_hyperfocus", "medal_consistency",
                "medal_architect", "medal_finisher", "medal_polymath", "medal_zen_master"
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                allMedalIds.forEach { id -> 
                    val tier = when {
                        unlockedMedals.contains("${id}_3") -> 3
                        unlockedMedals.contains("${id}_2") -> 2
                        unlockedMedals.contains("${id}_1") -> 1
                        else -> 0
                    }
                    MedalCard(id = id, tier = tier, viewModel = viewModel, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun TokenReminder(
    tokens: Int,
    isColorToken: Boolean,
    onInfoClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    // Usamos los colores de medalla definidos en Color.kt
    val color = if (isColorToken) MedalBronze else MedalSilver
    val icon = if (isColorToken) "🪙" else "⏳"
    
    val label = if (isColorToken) {
        if (tokens == 1) stringResource(R.string.color_token_label) else stringResource(R.string.color_tokens_label)
    } else {
        stringResource(R.string.timer_capacity_label)
    }
    
    val tooltipTitle = if (isColorToken) stringResource(R.string.token_tooltip_title) else stringResource(R.string.timer_capacity_tooltip_title)
    val tooltipDesc = if (isColorToken) 
        stringResource(R.string.token_tooltip_desc)
        else stringResource(R.string.timer_capacity_tooltip_desc)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable { showTooltip = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isColorToken) "$tokens $label" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSystemInDarkTheme()) color.copy(alpha = 0.9f) else color.copy(alpha = 1f),
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color.copy(alpha = 0.7f)
        )
    }

    if (showTooltip) {
        AlertDialog(
            onDismissRequest = { showTooltip = false },
            title = { Text(tooltipTitle, fontWeight = FontWeight.Black, color = color) },
            text = { Text(tooltipDesc, lineHeight = 20.sp) },
            confirmButton = { 
                TextButton(onClick = { 
                    onInfoClick()
                    showTooltip = false 
                }) { 
                    Text(stringResource(R.string.protocol_understood).uppercase(), fontWeight = FontWeight.Bold) 
                } 
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}


@Composable
fun OperationalStatusBoxes(viewModel: TimerViewModel) {
    val isPro by viewModel.isPro.collectAsState()
    val isLegendary by viewModel.isLegendary.collectAsState()
    val rewardStatus by viewModel.rewardStatus.collectAsState()
    val timerLimit by viewModel.freeTimerLimit.collectAsState()
    val showTimerAviso by viewModel.showTimerTokenAviso.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bronzeComplete = false // Stub for rewardStatus which is Boolean in ViewModel

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.operational_status),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (isLegendary) {
                Spacer(Modifier.width(8.dp))
                LegendaryBadge()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusBox(
                title = stringResource(R.string.colors_label_status),
                value = if (isPro) stringResource(R.string.status_max) else stringResource(R.string.limit_format, if (bronzeComplete) 5 else 4),
                isActive = true,
                isDarkModeOverride = isDarkMode,
                modifier = Modifier.weight(1f)
            )

            StatusBox(
                title = stringResource(R.string.timers_label_status),
                value = if (isPro) stringResource(R.string.status_unlimited) else stringResource(R.string.limit_format, timerLimit),
                isActive = true,
                isDarkModeOverride = isDarkMode,
                onClick = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatusBox(
    title: String,
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isDarkModeOverride: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()
    val highlightColor = if (isDark) MaterialTheme.colorScheme.primary else StatsBlue
    val baseBg = if (isDark) highlightColor.copy(alpha = 0.15f) else StatsBlue.copy(alpha = 0.1f)
    val alpha = if (isActive) 1f else 0.4f

    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = baseBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, highlightColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = highlightColor.copy(alpha = 0.8f * alpha))
            Text(text = value.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = highlightColor.copy(alpha = alpha))
        }
    }
}

@Composable
fun MedalCard(id: String, tier: Int, viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rank by viewModel.currentRank.collectAsState()
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()
    
    val isLocked = tier == 0
    val color = when(tier) {
        3 -> if (isDark) MedalGold else MedalGoldLight
        2 -> if (isDark) MedalSilver else MedalSilverLight
        1 -> if (isDark) MedalBronze else MedalBronzeLight
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }
    
    val tierLabel = when(tier) {
        3 -> stringResource(R.string.medal_tier_gold)
        2 -> stringResource(R.string.medal_tier_silver)
        1 -> stringResource(R.string.medal_tier_bronze)
        else -> ""
    }

    val icon = getMedalIcon(id)
    Surface(
        modifier = modifier.height(100.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        onClick = {
            if (!isLocked) {
                val uri = ShareUtils.generateMedalShareCard(
                    context = context,
                    medalName = context.getString(translateMedalLocal(id)),
                    medalDesc = context.getString(translateMedalDescLocal(id)),
                    tier = tier,
                    rankName = context.getString(translateRankLocal(rank.toString()))
                )
                uri?.let {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, it)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_achievement)))
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { 
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp)) 
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(translateMedalLocal(id)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (tier > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (tier > 0) {
                        Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = tierLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(translateMedalDescLocal(id)), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), lineHeight = 14.sp, maxLines = 2)
            }
            
            if (!isLocked) {
                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}


@Composable
fun ResetAchievementsSection(viewModel: TimerViewModel) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.reset_achievements_dialog_title), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(stringResource(R.string.reset_achievements_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllAchievements()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.reset_confirm).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.system_security),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f), contentColor = ErrorRed),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.reset_achievements).uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
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
    "medal_architect" -> Icons.Default.Architecture
    "medal_finisher" -> Icons.Default.TaskAlt
    "medal_polymath" -> Icons.Default.Psychology
    "medal_zen_master" -> Icons.Default.SelfImprovement
    else -> Icons.Default.MilitaryTech
}

private fun translateRankLocal(rank: String): Int {
    val cleanKey = if (rank.contains("|")) rank.split("|")[0] else rank
    return when(cleanKey) {
        "rank_novice" -> R.string.rank_novice
        "rank_apprentice" -> R.string.rank_apprentice
        "rank_initiate" -> R.string.rank_initiate
        "rank_technician" -> R.string.rank_technician
        "rank_specialist" -> R.string.rank_specialist
        "rank_master" -> R.string.rank_master
        "rank_architect" -> R.string.rank_architect
        "rank_grand_architect" -> R.string.rank_grand_architect
        "rank_supreme" -> R.string.rank_supreme
        else -> R.string.rank_novice
    }
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
    val h = millis / 3600000
    val m = (millis % 3600000) / 60000
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

data class CategoryStat(val name: String, val percentage: Float, val isActive: Boolean)
