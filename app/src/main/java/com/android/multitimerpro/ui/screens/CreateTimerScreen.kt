package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerScreen(
    viewModel: TimerViewModel,
    timerId: String? = null,
    onBack: () -> Unit
) {
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
    
    var selectedColor by remember { mutableStateOf(existingTimer?.let { Color(it.color) } ?: NeonBlue) }
    var selectedCategory by remember { mutableStateOf(existingTimer?.category ?: "GENERAL") }
    var description by remember { mutableStateOf(existingTimer?.description ?: "") }

    val colors = listOf(NeonBlue, NeonGreen, NeonPurple, Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77))
    val categories = listOf("GENERAL", "TRABAJO", "OCIO", "OTROS")

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
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (timerId == null) "NUEVO INSTRUMENTO" else "EDITAR INSTRUMENTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name Input
        Text(
            text = "NOMBRE DEL TEMPORIZADOR",
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
                    "Ej. Sesión Deep Work", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Category Selection
        Text(
            text = "CATEGORÍA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
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
            text = "DURACIÓN",
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
                label = "HH",
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
                label = "MM",
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
                label = "SS",
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Color Selection
        Text(
            text = "COLOR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { selectedColor = color }
                ) {
                    if (selectedColor == color) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
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
                        } else {
                            viewModel.insert(name, totalMs, selectedColor.toArgb(), selectedCategory, description)
                        }
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
                    text = if (timerId == null) "CREAR TEMPORIZADOR" else "GUARDAR CAMBIOS",
                    color = if (isDark) DeepBlack else Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // AHORA VISIBLE SIEMPRE (Creación y Edición)
            OutlinedButton(
                onClick = {
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val s = seconds.toLongOrNull() ?: 0L
                    val totalMs = (h * 3600 + m * 60 + s) * 1000
                    if (name.isNotBlank() && totalMs > 0) {
                        viewModel.saveAsPreset(name, totalMs, selectedColor.toArgb(), selectedCategory, description)
                    } else {
                        viewModel.showMessage("Nombre y duración requeridos")
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
                    text = "GUARDAR COMO PRESET",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun TimeInput(value: String, onValueChange: (String) -> Unit, label: String, isDark: Boolean) {
    var textFieldValueState by remember(value) {
        mutableStateOf(TextFieldValue(text = value))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = textFieldValueState,
            onValueChange = {
                if (it.text.all { char -> char.isDigit() }) {
                    textFieldValueState = it
                    onValueChange(it.text)
                }
            },
            modifier = Modifier
                .width(64.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        textFieldValueState = textFieldValueState.copy(
                            selection = TextRange(0, textFieldValueState.text.length)
                        )
                    }
                },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
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
