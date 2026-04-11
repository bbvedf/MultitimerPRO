package com.android.multitimerpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.multitimerpro.data.HistoryEntity
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.DeleteHistoryConfirmationDialog
import com.android.multitimerpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class TimeFilter { TODO, HOY, SEMANA, MES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val historyItems by viewModel.history.collectAsState()
    var historyToDelete by remember { mutableStateOf<HistoryEntity?>(null) }
    
    // Using persistent states from ViewModel
    val showFilters by viewModel.historyShowFilters.collectAsState()
    val selectedCategory by viewModel.historySelectedCategory.collectAsState()
    val selectedTimeFilter by viewModel.historySelectedTimeFilter.collectAsState()
    
    val categories = remember(historyItems) {
        val uniqueCats = historyItems.map { it.category }.distinct().sorted()
        listOf("TODAS") + uniqueCats
    }

    val filteredItems = remember(historyItems, selectedCategory, selectedTimeFilter) {
        val now = System.currentTimeMillis()
        historyItems.filter { item ->
            val categoryMatch = selectedCategory == "TODAS" || item.category == selectedCategory
            val timeMatch = when (selectedTimeFilter) {
                TimeFilter.TODO -> true
                TimeFilter.HOY -> {
                    val itemCal = Calendar.getInstance().apply { timeInMillis = item.completedAt }
                    val nowCal = Calendar.getInstance()
                    itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
                    itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                }
                TimeFilter.SEMANA -> item.completedAt >= (now - (7 * 24 * 60 * 60 * 1000L))
                TimeFilter.MES -> item.completedAt >= (now - (30 * 24 * 60 * 60 * 1000L))
            }
            categoryMatch && timeMatch
        }
    }

    if (historyToDelete != null) {
        DeleteHistoryConfirmationDialog(
            timerName = historyToDelete!!.timerName,
            onConfirm = {
                viewModel.deleteHistoryEntry(historyToDelete!!)
                historyToDelete = null
            },
            onDismiss = { historyToDelete = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(64.dp)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HISTORIAL DE ACTIVIDAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text("Sesiones", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { viewModel.setHistoryShowFilters(!showFilters) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = if (showFilters) Color.Black else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                AnimatedVisibility(visible = showFilters, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PERIODO TEMPORAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(TimeFilter.values()) { filter ->
                                        FilterChip(
                                            selected = selectedTimeFilter == filter, 
                                            onClick = { viewModel.setHistorySelectedTimeFilter(filter) }, 
                                            label = { Text(filter.name) }, 
                                            border = null
                                        )
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { category ->
                                        FilterChip(
                                            selected = selectedCategory == category, 
                                            onClick = { viewModel.setHistorySelectedCategory(category) }, 
                                            label = { Text(category) }, 
                                            border = null
                                        )
                                    }
                                }
                            }
                            
                            // EXPORT ACTIONS
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { viewModel.exportHistoryToPDF(filteredItems) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (MaterialTheme.colorScheme.background == DeepBlack) Color.Black else Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("REPORTE PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (MaterialTheme.colorScheme.background == DeepBlack) Color.Black else Color.White)
                                }
                                Button(
                                    onClick = { viewModel.exportHistoryToCSV(filteredItems) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DATOS CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        val filterText = if (selectedCategory == "TODAS") selectedTimeFilter.name else "${selectedTimeFilter.name} • $selectedCategory"
                        Text(text = "TIEMPO ENFOCADO ($filterText)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                        val totalFilteredTime = filteredItems.sumOf { it.durationMillis }
                        Text(text = formatMillisToTime(totalFilteredTime), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Text("No hay sesiones con estos filtros", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(filteredItems) { item ->
                    HistoryEntryCard(item = item, onDelete = { historyToDelete = item }, onClick = { onNavigateToDetail(item.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun HistoryEntryCard(item: HistoryEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.completedAt))
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), onClick = onClick) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(item.color), modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(text = item.timerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "${item.category} • $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatMillisToTimeShort(item.durationMillis), style = MaterialTheme.typography.headlineSmall, color = Color(item.color), fontWeight = FontWeight.Bold)
                    Text(text = "DURACIÓN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onClick, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) { Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f)) }
            }
        }
    }
}

private fun formatMillisToTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatMillisToTimeShort(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis % 60000) / 1000
    return if (minutes > 0) String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds) else String.format(Locale.getDefault(), "00:%02d", seconds)
}
