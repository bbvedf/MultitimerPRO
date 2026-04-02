package com.android.multitimerpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.data.local.TimerEntity
import com.android.multitimerpro.ui.theme.SpaceGrotesk
import java.util.concurrent.TimeUnit

@Composable
fun TimerCard(
    timer: TimerEntity,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neonBlue = Color(0xFF69DAFF)
    val neonGreen = Color(0xFF2FF801)
    val cardBackground = Color(0xFF131313)
    
    val accentColor = when {
        timer.isCompleted -> neonGreen
        timer.isRunning -> neonBlue
        else -> Color.White.copy(alpha = 0.4f)
    }

    val hours = TimeUnit.MILLISECONDS.toHours(timer.remainingTimeMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timer.remainingTimeMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timer.remainingTimeMs) % 60
    val timeStr = if (hours > 0) String.format("%02d:%02d", hours, minutes) else String.format("%02d:%02d", minutes, seconds)
    val msStr = String.format(".%02d", (timer.remainingTimeMs % 1000) / 10)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .padding(start = 12.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de estado
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .padding(vertical = 12.dp)
                    .clip(CircleShape)
                    .background(if (timer.isRunning || timer.isCompleted) accentColor else Color.White.copy(alpha = 0.1f))
            )

            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timer.name.uppercase(),
                            color = Color(0xFFADAAAA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (timer.isRunning) "LIVE" else if (timer.isCompleted) "READY" else "IDLE",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeStr,
                            fontFamily = SpaceGrotesk,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = msStr,
                            fontFamily = SpaceGrotesk,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = onToggle,
                            shape = CircleShape,
                            color = if (timer.isRunning || timer.isCompleted) accentColor else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = if (timer.isRunning || timer.isCompleted) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Barra de progreso inferior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    // Simulación de progreso
                    val progress = if (timer.isRunning) 0.4f else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }
        }
    }
}
