package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.*
import com.android.multitimerpro.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerScreen(
    viewModel: TimerViewModel,
    timerId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timers by viewModel.allTimers.collectAsState()
    val existingTimer = remember(timerId, timers) {
        timers.find { it.id == timerId }
    }

    var name by remember { mutableStateOf(existingTimer?.name ?: "") }
    var hours by remember {
        val h = (existingTimer?.duration ?: 0) / 3600000
        mutableStateOf(String.format(Locale.getDefault(), "%02d", h))
    }
    var minutes by remember {
        val m = ((existingTimer?.duration ?: 0) % 3600000) / 60000
        mutableStateOf(String.format(Locale.getDefault(), "%02d", m))
    }
    var seconds by remember {
        val s = ((existingTimer?.duration ?: 0) % 60000) / 1000
        mutableStateOf(String.format(Locale.getDefault(), "%02d", s))
    }
    
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeOverride ?: androidx.compose.foundation.isSystemInDarkTheme()
    val isPro by viewModel.isPro.collectAsState()
    
    var showProDialog by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(existingTimer?.let { Color(it.color) } ?: NeonBlue) }
    var selectedCategory by remember { mutableStateOf(existingTimer?.category ?: "GENERAL") }
    var description by remember { mutableStateOf(existingTimer?.description ?: "") }

    val freeColors = listOf(NeonBlue, NeonGreen, NeonPurple)
    val proColors = listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77), Color(0xFF6A1B9A))
    val allColors = freeColors + proColors

    val categories = listOf(
        Pair("GENERAL", R.string.cat_general),
        Pair("WORK", R.string.cat_work),
        Pair("LEISURE", R.string.cat_leisure),
        Pair("OTHERS", R.string.cat_other)
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(existingTimer, name, hours, minutes, seconds, selectedColor, selectedCategory, description) {
        val currentDuration = (hours.toLongOrNull() ?: 0) * 3600000 + (minutes.toLongOrNull() ?: 0) * 60000 + (seconds.toLongOrNull() ?: 0) * 1000
        val originalDuration = existingTimer?.duration ?: 0L
        
        name != (existingTimer?.name ?: "") ||
        currentDuration != originalDuration ||
        selectedColor.toArgb() != (existingTimer?.color ?: NeonBlue.toArgb()) ||
        selectedCategory != (existingTimer?.category ?: "GENERAL") ||
        description != (existingTimer?.description ?: "")
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showProDialog) {
        UpgradeProDialog(
            onDismiss = { showProDialog = false },
            onUpgrade = { 
                viewModel.toggleProStatus() 
                showProDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                if (hasChanges) showDiscardDialog = true else onBack()
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (timerId == null) R.string.timer_new else R.string.timer_edit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name Input
        Text(
            text = stringResource(R.string.timer_name_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            placeholder = { 
                Text(
                    stringResource(R.string.timer_name_placeholder), 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Category Selection
        Text(
            text = stringResource(R.string.timer_category_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { (internalName, labelRes) ->
                FilterChip(
                    selected = selectedCategory == internalName,
                    onClick = { selectedCategory = internalName },
                    label = { Text(stringResource(labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (isDark) DeepBlack else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Picker
        Text(
            text = stringResource(R.string.timer_duration_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeInput(
                value = hours, 
                onValueChange = { if (it.length <= 2) hours = it }, 
                label = stringResource(R.string.hours_label_full),
                isDark = isDark
            )
            Text(
                ":", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 24.sp
            )
            TimeInput(
                value = minutes, 
                onValueChange = { if (it.length <= 2) minutes = it }, 
                label = stringResource(R.string.minutes_label_full),
                isDark = isDark
            )
            Text(
                ":", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 24.sp
            )
            TimeInput(
                value = seconds, 
                onValueChange = { if (it.length <= 2) seconds = it }, 
                label = stringResource(R.string.seconds_label_full),
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Color Selection
        Text(
            text = stringResource(R.string.timer_color_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            allColors.forEach { color ->
                val isColorPro = proColors.contains(color)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { 
                            if (isColorPro && !isPro) {
                                showProDialog = true
                            } else {
                                selectedColor = color 
                            }
                        }
                ) {
                    if (selectedColor == color) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(Alignment.Center)
                        )
                    } else if (isColorPro && !isPro) {
                        Text(
                            text = stringResource(R.string.pro_indicator),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        val requiredMsg = stringResource(R.string.timer_required_msg)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val s = seconds.toLongOrNull() ?: 0L
                    val totalMs = (h * 3600 + m * 60 + s) * 1000
                    if (name.isNotBlank() && totalMs > 0) {
                        if (existingTimer != null) {
                            viewModel.updateTimer(
                                existingTimer.copy(
                                    name = name,
                                    duration = totalMs,
                                    remainingTime = totalMs,
                                    color = selectedColor.toArgb(),
                                    category = selectedCategory,
                                    description = description
                                )
                            )
                            onBack()
                        } else {
                            if (timers.size >= 3 && !isPro) {
                                showProDialog = true
                            } else {
                                viewModel.insert(name, totalMs, selectedColor.toArgb(), selectedCategory, description)
                                onBack()
                            }
                        }
                    } else {
                        viewModel.showMessage(requiredMsg)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(if (timerId == null) R.string.timer_create_btn else R.string.timer_save_btn),
                    color = if (isDark) DeepBlack else Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = {
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val s = seconds.toLongOrNull() ?: 0L
                    val totalMs = (h * 3600 + m * 60 + s) * 1000
                    if (name.isNotBlank() && totalMs > 0) {
                        viewModel.saveAsPreset(name, totalMs, selectedColor.toArgb(), selectedCategory, description)
                    } else {
                        viewModel.showMessage(requiredMsg)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.timer_preset_btn),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun TimeInput(value: String, onValueChange: (String) -> Unit, label: String, isDark: Boolean) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (!isFocused) {
            textFieldValue = value
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }
                if (digitsOnly.length <= 2) {
                    textFieldValue = digitsOnly
                    onValueChange(digitsOnly.ifEmpty { "0" })
                    
                    if (digitsOnly.length == 2) {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next)
                    }
                }
            },
            modifier = Modifier
                .width(72.dp)
                .onFocusChanged { 
                    isFocused = it.isFocused
                    if (it.isFocused && (textFieldValue == "0" || textFieldValue == "00")) {
                        textFieldValue = ""
                    }
                },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            singleLine = true
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
