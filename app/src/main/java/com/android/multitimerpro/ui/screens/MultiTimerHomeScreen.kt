package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.ui.components.TimerCard
import com.android.multitimerpro.ui.theme.*

@Composable
fun MultiTimerHomeScreen(viewModel: TimerViewModel) {
    val timers by viewModel.allTimers.collectAsState()

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
                        text = "${timers.count { it.status == "LIVE" }} ACTIVOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.background(SurfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    onToggle = { viewModel.update(timer) },
                    onReset = { viewModel.resetTimer(timer) },
                    onDelete = { viewModel.delete(timer) }
                )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        FloatingActionButton(
            onClick = {
                // Temporary: Add a dummy timer to test persistence
                viewModel.insert(
                    TimerEntity(
                        name = "Timer ${timers.size + 1}",
                        duration = 60000,
                        remainingTime = 60000,
                        status = "PAUSED",
                        color = NeonBlue.toArgb(),
                        category = "General"
                    )
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = NeonBlue,
            contentColor = DeepBlack,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}
