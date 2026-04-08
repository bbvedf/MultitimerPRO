package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
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

@Composable
fun HistoryScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val historyItems by viewModel.history.collectAsState()
    val totalTimeMillis by viewModel.totalTimeSpent.collectAsState()
    var historyToDelete by remember { mutableStateOf<HistoryEntity?>(null) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(64.dp)) }

        // Editorial Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Historial de",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sesiones",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Text(
                    text = "Un registro detallado de cada segundo optimizado. Tu rendimiento, fragmentado en intervalos de precisión absoluta.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Total Time Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "TIEMPO TOTAL ENFOCADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formatMillisToTime(totalTimeMillis),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp
                    )
                }
            }
        }

        item {
            Text(
                text = "SESIONES RECIENTES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        if (historyItems.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No hay sesiones registradas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(historyItems) { item ->
                HistoryEntryCard(
                    item = item, 
                    onDelete = { historyToDelete = item },
                    onClick = { onNavigateToDetail(item.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun HistoryEntryCard(
    item: HistoryEntity, 
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.completedAt))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timer, 
                            contentDescription = null, 
                            tint = Color(item.color), 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.timerName, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.category} • $dateStr", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMillisToTimeShort(item.durationMillis),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(item.color),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DURACIÓN", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Analytics, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null, 
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }
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
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (minutes > 0) {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "00:%02d", seconds)
    }
}
