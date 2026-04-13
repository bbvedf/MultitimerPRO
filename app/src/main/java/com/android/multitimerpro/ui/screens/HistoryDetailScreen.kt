package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.android.multitimerpro.R
import com.android.multitimerpro.data.HistoryEntity
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@Composable
fun HistoryDetailScreen(
    historyId: String,
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val historyItems by viewModel.history.collectAsState()
    val item = historyItems.find { it.id == historyId }
    val scope = rememberCoroutineScope()

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var notes by remember { mutableStateOf(item.notes) }
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Parse intervals
    val intervals = remember(item.intervalsJson) {
        if (item.intervalsJson == "[]" || item.intervalsJson.isBlank()) emptyList<String>()
        else item.intervalsJson.removeSurrounding("[", "]").split(", ").map { it.removeSurrounding("\"") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.detail_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Main Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(item.color), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = item.timerName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    InfoRow(icon = Icons.Default.Category, label = stringResource(R.string.detail_category), value = translateCategoryLocal(item.category))
                    InfoRow(icon = Icons.Default.Timer, label = stringResource(R.string.detail_total_duration), value = formatMillisToTime(item.durationMillis))
                    InfoRow(icon = Icons.Default.CalendarToday, label = stringResource(R.string.detail_date), value = dateFormat.format(Date(item.completedAt)))
                    InfoRow(icon = Icons.Default.Timer, label = stringResource(R.string.detail_finished_at), value = timeFormat.format(Date(item.completedAt)))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Notes Section
        item {
            Text(
                text = stringResource(R.string.detail_notes_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(stringResource(R.string.detail_notes_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
            )
        }

        if (intervals.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(32.dp)) }
            
            item {
                Text(
                    text = stringResource(R.string.detail_intervals_breakdown),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // IMPORTANTE: Invertimos el cálculo porque el temporizador es regresivo
            itemsIndexed(intervals) { index, interval ->
                val currentParts = interval.split(" - ")
                val currentTimeStr = currentParts.getOrNull(0) ?: "00:00:00"
                val label = currentParts.getOrNull(1) ?: stringResource(R.string.detail_no_label)
                
                // Cálculo de tramo real para temporizador regresivo:
                // El primer tramo es (Duración Total - Primera Marca)
                // Los siguientes son (Marca Anterior - Marca Actual)
                val currentRemainingMs = parseTimeToMillis(currentTimeStr)
                val lapMillis = if (index == 0) {
                    item.durationMillis - currentRemainingMs
                } else {
                    val previousRemainingMs = parseTimeToMillis(intervals[index - 1].split(" - ").getOrNull(0) ?: "00:00:00")
                    previousRemainingMs - currentRemainingMs
                }

                HistoryIntervalItem(
                    index = index + 1,
                    label = label,
                    absoluteTime = formatToThreeBlocks(currentTimeStr),
                    lapTime = formatMillisToTime(lapMillis.coerceAtLeast(0L))
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.updateHistory(item.copy(notes = notes))
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.timer_save_btn),
                    color = if (MaterialTheme.colorScheme.background == DeepBlack) DeepBlack else Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
fun HistoryIntervalItem(index: Int, label: String, absoluteTime: String, lapTime: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d", index), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.width(32.dp)
                )
                Column {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.detail_on_screen, absoluteTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = lapTime, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text(text = stringResource(R.string.detail_lap_duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun formatMillisToTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun parseTimeToMillis(timeStr: String): Long {
    val parts = timeStr.split(":").map { it.toLongOrNull() ?: 0L }
    return when (parts.size) {
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
        2 -> (parts[0] * 60 + parts[1]) * 1000
        else -> 0L
    }
}

private fun formatToThreeBlocks(timeStr: String): String {
    val parts = timeStr.split(":")
    return if (parts.size == 2) "00:$timeStr" else timeStr
}

@Composable
private fun translateCategoryLocal(internalName: String): String {
    return when(internalName.uppercase()) {
        "ALL" -> stringResource(R.string.category_all)
        "GENERAL" -> stringResource(R.string.cat_general)
        "WORK" -> stringResource(R.string.cat_work)
        "LEISURE" -> stringResource(R.string.cat_leisure)
        "OTHER" -> stringResource(R.string.cat_other)
        else -> internalName
    }
}
