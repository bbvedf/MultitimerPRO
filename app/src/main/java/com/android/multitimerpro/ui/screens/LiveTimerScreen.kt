package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.ui.components.*
import com.android.multitimerpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveTimerScreen(
    timerId: String,
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val timers by viewModel.allTimers.collectAsState()
    val timer = timers.find { it.id == timerId } ?: return

    val currentIntervals = remember(timer.intervalsJson) {
        if (timer.intervalsJson == "[]" || timer.intervalsJson.isBlank()) emptyList<String>()
        else timer.intervalsJson.removeSurrounding("[", "]").split(", ").map { it.removeSurrounding("\"") }
    }

    var showAddMarkDialog by remember { mutableStateOf(false) }
    var markLabel by remember { mutableStateOf("") }
    
    // Generar nombre por defecto incremental (m01, m02...)
    val nextMarkName = "m${String.format(Locale.getDefault(), "%02d", currentIntervals.size + 1)}"

    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: androidx.compose.foundation.isSystemInDarkTheme()

    if (showAddMarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddMarkDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { 
                Text(
                    stringResource(R.string.live_add_mark), 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                TextField(
                    value = markLabel,
                    onValueChange = { markLabel = it },
                    placeholder = { 
                        Text(
                            stringResource(R.string.live_mark_default, nextMarkName), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalLabel = if (markLabel.isBlank()) nextMarkName else markLabel
                    viewModel.addInterval(timer, finalLabel)
                    markLabel = ""
                    showAddMarkDialog = false
                }) {
                    Text(stringResource(R.string.add), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMarkDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.main_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { /* Profile */ }) {
                    Surface(
                        modifier = Modifier.size(32.dp), 
                        shape = CircleShape, 
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MoreVert, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular Progress
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Cálculo de progreso intuitivo:
                // Si es un snooze, mostramos el progreso de ese snooze específico (100% -> 0%)
                // Si no es snooze, el progreso normal sobre la duración base.
                val progress = if (timer.isSnoozed && timer.lastSnoozeDuration > 0) {
                    timer.remainingTime.toFloat() / timer.lastSnoozeDuration.toFloat()
                } else {
                    if (timer.duration > 0) timer.remainingTime.toFloat() / timer.duration.toFloat() else 0f
                }
                
                val timerColor = Color(timer.color)

                Canvas(modifier = Modifier.size(280.dp)) {
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                        style = Stroke(width = 12.dp.toPx())
                    )
                    drawArc(
                        color = timerColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = translateCategory(timer.category).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                    
                    val hours = (timer.remainingTime / 3600000)
                    val minutes = (timer.remainingTime % 3600000) / 60000
                    val seconds = (timer.remainingTime % 60000) / 1000
                    
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp
                    )
                    Text(
                        text = translateStatusLocal(timer.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = timerColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Timer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = timer.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timer.description.ifBlank { stringResource(R.string.live_no_desc) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.live_target), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        letterSpacing = 1.sp
                    )
                    
                    val metaHours = (timer.duration / 3600000)
                    val metaMinutes = (timer.duration % 3600000) / 60000
                    val metaSeconds = (timer.duration % 60000) / 1000
                    
                    val metaStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", metaHours, metaMinutes, metaSeconds)

                    Text(
                        text = metaStr, 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.onBackground, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.update(timer) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        if (timer.status == "LIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isDark) DeepBlack else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timer.status == "LIVE") stringResource(R.string.live_pause) else stringResource(R.string.live_start),
                        color = if (isDark) DeepBlack else Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { viewModel.resetTimer(timer) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Progreso real acumulado: (Duración actual - Tiempo restante) / Duración actual
                val progressPercent = if (timer.duration > 0) {
                    (((timer.duration - timer.remainingTime).toFloat() / timer.duration.toFloat()) * 100).toInt().coerceIn(0, 100)
                } else 0
                StatMiniCard(
                    title = stringResource(R.string.live_progress),
                    value = "$progressPercent%",
                    icon = Icons.Default.TrendingUp,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val endTime = Date(System.currentTimeMillis() + timer.remainingTime)
                StatMiniCard(
                    title = stringResource(R.string.live_ends_at),
                    value = sdf.format(endTime),
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Intervals Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.live_intervals), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    letterSpacing = 2.sp
                )
                TextButton(onClick = { 
                    markLabel = "" // Limpiar para usar el placeholder
                    showAddMarkDialog = true 
                }) {
                    Text(
                        text = stringResource(R.string.live_add_mark),
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dynamic Intervals
            Column(modifier = Modifier.fillMaxWidth()) {
                currentIntervals.forEachIndexed { index, interval ->
                    val parts = interval.split(" - ")
                    val time = parts.getOrNull(0) ?: ""
                    val label = parts.getOrNull(1) ?: ""
                    IntervalItem(
                        number = String.format(Locale.getDefault(), "%02d", index + 1),
                        name = label,
                        time = time,
                        color = if (index == currentIntervals.size - 1) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun translateStatusLocal(status: String): String {
    return when (status.uppercase()) {
        "READY" -> stringResource(R.string.status_ready)
        "LIVE" -> stringResource(R.string.status_live)
        "PAUSED" -> stringResource(R.string.status_paused)
        "FINISHED" -> stringResource(R.string.status_finished)
        else -> status
    }
}

@Composable
fun IntervalItem(number: String, name: String, time: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = number, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = name, 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Medium
                )
            }
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatMiniCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 8.sp, 
                letterSpacing = 1.sp
            )
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge, 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
