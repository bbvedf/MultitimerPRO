package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.android.multitimerpro.ui.TimerViewModel
import com.android.multitimerpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerScreen(
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var seconds by remember { mutableStateOf("00") }
    var selectedColor by remember { mutableStateOf(NeonBlue) }
    var selectedCategory by remember { mutableStateOf("General") }

    val colors = listOf(NeonBlue, NeonGreen, NeonPurple, Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NUEVO INSTRUMENTO",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name Input
        Text(
            text = "NOMBRE DEL TEMPORIZADOR",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontSize = 10.sp
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = NeonBlue,
                focusedIndicatorColor = NeonBlue,
                unfocusedIndicatorColor = SurfaceVariant
            ),
            placeholder = { Text("Ej. Entrenamiento", color = OnSurfaceVariant.copy(alpha = 0.5f)) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Time Picker
        Text(
            text = "DURACIÓN",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontSize = 10.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeInput(value = hours, onValueChange = { if (it.length <= 2) hours = it }, label = "HH")
            Text(":", color = OnSurfaceVariant, fontSize = 24.sp)
            TimeInput(value = minutes, onValueChange = { if (it.length <= 2) minutes = it }, label = "MM")
            Text(":", color = OnSurfaceVariant, fontSize = 24.sp)
            TimeInput(value = seconds, onValueChange = { if (it.length <= 2) seconds = it }, label = "SS")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Color Selection
        Text(
            text = "COLOR",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
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

        // Create Button
        Button(
            onClick = {
                val h = hours.toIntOrNull() ?: 0
                val m = minutes.toIntOrNull() ?: 0
                val s = seconds.toIntOrNull() ?: 0
                if (name.isNotBlank() && (h > 0 || m > 0 || s > 0)) {
                    viewModel.addTimer(name, h, m, s, selectedColor.toArgb(), selectedCategory)
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CREAR TEMPORIZADOR",
                color = DeepBlack,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TimeInput(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
            modifier = Modifier.width(64.dp),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.White
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = NeonBlue,
                unfocusedIndicatorColor = SurfaceVariant
            ),
            singleLine = true
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}
