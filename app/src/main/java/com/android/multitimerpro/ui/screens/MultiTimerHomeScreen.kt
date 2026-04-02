package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.ui.theme.*

@Composable
fun MultiTimerHomeScreen() {
    val activeTimers = listOf(
        TimerData("1", "Workout", "LIVE", "12:45", ".28", 0.66f, NeonBlue),
        TimerData("2", "Pomodoro", "READY", "25:00", null, 1.0f, NeonGreen),
        TimerData("3", "Meditation", "PAUSED", "08:12", null, 0.33f, NeonBlue)
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(64.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "ESTADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Instrumentos\nActivos",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "3 ACTIVOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.background(SurfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(activeTimers) { timer ->
                TimerCard(timer)
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        FloatingActionButton(
            onClick = { /* Add Timer */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = NeonBlue,
            contentColor = DeepBlack,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun TimerCard(timer: TimerData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(timer.color))

            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = timer.name, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                        Text(text = timer.status, style = MaterialTheme.typography.labelSmall, color = timer.color, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = timer.time, style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        timer.millis?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                    LinearProgressIndicator(
                        progress = timer.progress,
                        modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = timer.color,
                        trackColor = SurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { /* Reset */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = OnSurfaceVariant)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (timer.status == "LIVE") NeonBlue else SurfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (timer.status == "LIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (timer.status == "LIVE") DeepBlack else NeonBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TimerData(val id: String, val name: String, val status: String, val time: String, val millis: String?, val progress: Float, val color: Color)
