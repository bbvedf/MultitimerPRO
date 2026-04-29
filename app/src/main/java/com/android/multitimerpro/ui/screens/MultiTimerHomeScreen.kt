package com.android.multitimerpro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import java.util.Calendar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.R
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
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
                            text = stringResource(R.string.home_status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stringResource(R.string.home_active_instruments),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            val timersCount by viewModel.timersCount.collectAsState()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = timersCount.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Mover ACTIVES aquí, junto al contador total
                            val allTimersList = timers
                            if (allTimersList.any { it.status == "LIVE" }) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${allTimersList.count { it.status == "LIVE" }} ${stringResource(R.string.home_actives_suffix)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // --- NUEVO MENÚ DE CONTROL ---
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteAllConfirm by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search (PRO)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.showMessage("FEATURE EXCLUSIVE TO PRO MEMBERS")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Filter (PRO)") },
                                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.showMessage("FEATURE EXCLUSIVE TO PRO MEMBERS")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort (PRO)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.showMessage("FEATURE EXCLUSIVE TO PRO MEMBERS")
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            DropdownMenuItem(
                                text = { Text("Delete All", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    showMenu = false
                                    showDeleteAllConfirm = true
                                }
                            )
                        }
                    }

                    if (showDeleteAllConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAllConfirm = false },
                            title = { Text("Wipe All Data?") },
                            text = { Text("This will remove all active timers. This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteAllTimers()
                                        showDeleteAllConfirm = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("DELETE ALL") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAllConfirm = false }) { Text("CANCEL") }
                            }
                        )
                    }

                    // Eliminamos el bloque duplicado de abajo
                }
            }

            val allTimersListForEmpty = timers
            if (allTimersListForEmpty.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Text(
                                text = stringResource(R.string.presets_no_presets) + "\n" + stringResource(R.string.presets_add_msg),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    isDarkMode = isDarkMode ?: false,
                    onToggle = { viewModel.toggleTimer(timer) },
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


