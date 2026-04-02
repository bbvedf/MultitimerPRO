package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.ui.theme.*

@Composable
fun LiveTimerScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(24.dp)) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnSurfaceVariant)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Progress
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SurfaceVariant,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = NeonGreen,
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SESIÓN DE FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "24:58",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 72.sp
                    )
                    Text(
                        text = "ACTIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Timer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Preparación\nde Café",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 40.sp
                    )
                    Text(
                        text = "Método V60 • Tueste Ligero",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "META", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                    Text(text = "30:00", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { /* Pause */ },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = DeepBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "PAUSAR", color = DeepBlack, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatMiniCard(
                    title = "PROGRESO",
                    value = "83%",
                    icon = Icons.Default.TrendingUp,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "FINALIZA A LAS",
                    value = "14:45",
                    icon = Icons.Default.Timer,
                    color = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
            }
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
