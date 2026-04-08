package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.ui.components.TimerCard
import com.android.multitimerpro.ui.components.DeleteConfirmationDialog
import com.android.multitimerpro.ui.theme.*

@Composable
fun MultiTimerHomeScreen(
    viewModel: TimerViewModel,
    onNavigateToCreate: (String?) -> Unit,
    onNavigateToLive: (String) -> Unit
) {
    val timers by viewModel.allTimers.collectAsState()
    var timerToDelete by remember { mutableStateOf<TimerEntity?>(null) }

    if (timerToDelete != null) {
        DeleteConfirmationDialog(
            timerName = timerToDelete!!.name,
            onConfirm = {
                viewModel.delete(timerToDelete!!)
                timerToDelete = null
            },
            onDismiss = { timerToDelete = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Instrumentos\nActivos",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${timers.count { it.status == "LIVE" }} ACTIVOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    onToggle = { viewModel.update(timer) },
                    onReset = { viewModel.resetTimer(timer) },
                    onDelete = { timerToDelete = timer },
                    onEdit = { onNavigateToCreate(timer.id) },
                    onClick = { onNavigateToLive(timer.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        FloatingActionButton(
            onClick = { onNavigateToCreate(null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (MaterialTheme.colorScheme.primary == NeonBlue) DeepBlack else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}
