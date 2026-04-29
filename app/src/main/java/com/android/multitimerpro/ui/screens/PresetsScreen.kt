package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.R
import com.android.multitimerpro.data.PresetEntity
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.DeletePresetConfirmationDialog
import com.android.multitimerpro.ui.components.translateCategory
import com.android.multitimerpro.ui.theme.*
import java.util.Locale

@Composable
fun PresetsScreen(
    viewModel: TimerViewModel,
    onNavigateToCreate: (String?) -> Unit
) {
    val presets by viewModel.allPresets.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == DeepBlack
    var presetToDelete by remember { mutableStateOf<PresetEntity?>(null) }

    if (presetToDelete != null) {
        DeletePresetConfirmationDialog(
            presetName = presetToDelete!!.name,
            onConfirm = {
                viewModel.deletePreset(presetToDelete!!)
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.presets_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stringResource(R.string.presets_saved),
                            style = MaterialTheme.typography.headlineMedium, // Reducido de headlineLarge
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val presetsCount by viewModel.presetsCount.collectAsState()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = presetsCount.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- NUEVO MENÚ DE CONTROL ---
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteAllConfirm by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            DropdownMenuItem(
                                text = { Text("Delete All", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
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
                            title = { Text("Wipe All Presets?") },
                            text = { Text("This will remove all saved presets. This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteAllPresets()
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
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            Icons.Default.Timer, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        Text(
                            stringResource(R.string.presets_no_presets) + "\n" + stringResource(R.string.presets_add_msg),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(presets) { preset ->
                        PresetCard(
                            preset = preset,
                            isDark = isDark,
                            onStart = { viewModel.startTimerFromPreset(preset) },
                            onDelete = { presetToDelete = preset },
                            onEdit = { onNavigateToCreate(preset.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNavigateToCreate(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isDark) DeepBlack else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun PresetCard(
    preset: PresetEntity, 
    isDark: Boolean,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val hours = preset.durationMillis / 3600000
    val minutes = (preset.durationMillis % 3600000) / 60000
    val seconds = (preset.durationMillis % 60000) / 1000
    val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    if (isDark) DeepBlack else Color.White,
                    MaterialTheme.colorScheme.primary
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono con color en lugar de banda lateral
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(preset.color).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Timer, 
                    contentDescription = null, 
                    tint = Color(preset.color), 
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = translateCategory(preset.category),
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = timeStr, 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                    Surface(
                        modifier = Modifier.size(32.dp), 
                        shape = CircleShape, 
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onStart
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null, 
                                tint = if (isDark) DeepBlack else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
