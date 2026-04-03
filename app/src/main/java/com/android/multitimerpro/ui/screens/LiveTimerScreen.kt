package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveTimerScreen(
    timerId: Int,
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val timers by viewModel.allTimers.collectAsState()
    val timer = timers.find { it.id == timerId } ?: return

    var showAddMarkDialog by remember { mutableStateOf(false) }
    var markLabel by remember { mutableStateOf("") }

    if (showAddMarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddMarkDialog = false },
            containerColor = SurfaceHigh,
            title = { Text("AÑADIR MARCA", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = markLabel,
                    onValueChange = { markLabel = it },
                    placeholder = { Text("Nombre de la marca", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonBlue,
                        focusedIndicatorColor = NeonBlue,
                        unfocusedIndicatorColor = SurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (markLabel.isNotBlank()) {
                        viewModel.addInterval(timer, markLabel)
                        markLabel = ""
                        showAddMarkDialog = false
                    }
                }) {
                    Text("AÑADIR", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMarkDialog = false }) {
                    Text("CANCELAR", color = OnSurfaceVariant)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = "MULTITIMER PRO",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { /* Profile */ }) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = SurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                val progress = if (timer.duration > 0) timer.remainingTime.toFloat() / timer.duration.toFloat() else 0f
                val timerColor = Color(timer.color)

                Canvas(modifier = Modifier.size(280.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
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
                        text = timer.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                    val minutes = (timer.remainingTime / 1000) / 60
                    val seconds = (timer.remainingTime / 1000) % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 80.sp
                    )
                    Text(
                        text = timer.status,
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timer.description.ifBlank { "Sin descripción" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "META", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                    val metaMinutes = (timer.duration / 1000) / 60
                    val metaSeconds = (timer.duration / 1000) % 60
                    Text(text = String.format("%02d:%02d", metaMinutes, metaSeconds), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.update(timer) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        if (timer.status == "LIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timer.status == "LIVE") "PAUSAR" else "INICIAR",
                        color = DeepBlack,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceVariant,
                    onClick = { viewModel.resetTimer(timer) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val progressPercent = if (timer.duration > 0) ((timer.remainingTime.toFloat() / timer.duration.toFloat()) * 100).toInt() else 0
                StatMiniCard(
                    title = "PROGRESO",
                    value = "$progressPercent%",
                    icon = Icons.Default.TrendingUp,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val endTime = Date(System.currentTimeMillis() + timer.remainingTime)
                StatMiniCard(
                    title = "FINALIZA",
                    value = sdf.format(endTime),
                    icon = Icons.Default.Timer,
                    color = NeonBlue,
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
                Text(text = "INTERVALOS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 2.sp)
                TextButton(onClick = { showAddMarkDialog = true }) {
                    Text(text = "AÑADIR MARCA", style = MaterialTheme.typography.labelSmall, color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }

            // Dynamic Intervals
            val intervals = if (timer.intervalsJson == "[]") emptyList<String>()
            else timer.intervalsJson.removeSurrounding("[", "]").split(", ").map { it.removeSurrounding("\"") }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(intervals.size) { index ->
                    val interval = intervals[index]
                    val parts = interval.split(" - ")
                    val time = parts.getOrNull(0) ?: ""
                    val label = parts.getOrNull(1) ?: ""
                    IntervalItem(
                        number = String.format("%02d", index + 1),
                        name = label,
                        time = time,
                        color = if (index == intervals.size - 1) NeonGreen else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun IntervalItem(number: String, name: String, time: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = number, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, modifier = Modifier.width(24.dp))
                Text(text = name, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            }
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatMiniCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 8.sp, letterSpacing = 1.sp)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
