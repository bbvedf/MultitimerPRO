package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
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
fun HistoryScreen() {
    val historyItems = listOf(
        HistoryItem("1", "Focus Deep Work", "Productividad", "Hoy, 14:30", "02:30:00", NeonBlue, Icons.Default.Work),
        HistoryItem("2", "HIIT Training", "Deporte", "Ayer, 08:15", "00:45:12", NeonGreen, Icons.Default.FitnessCenter),
        HistoryItem("3", "Meditación Nocturna", "Bienestar", "12 Oct, 22:00", "00:20:00", NeonPurple, Icons.Default.SelfImprovement)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(64.dp)) }

        // Editorial Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Historial de",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sesiones",
                    style = MaterialTheme.typography.displayLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Text(
                    text = "Un registro detallado de cada segundo optimizado. Tu rendimiento, fragmentado en intervalos de precisión absoluta.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Total Time Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "TIEMPO TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "124:45:12",
                        style = MaterialTheme.typography.displayMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp
                    )
                }
            }
        }

        item {
            Text(
                text = "SESIONES RECIENTES",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        items(historyItems) { item ->
            HistoryEntryCard(item)
        }

        item {
            Button(
                onClick = { /* Load More */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "CARGAR MÁS SESIONES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun HistoryEntryCard(item: HistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${item.category} • ${item.date}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = item.duration, style = MaterialTheme.typography.headlineSmall, color = item.color, fontWeight = FontWeight.Bold)
                    Text(text = "DURACIÓN TOTAL", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 8.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { /* Analytics */ },
                    modifier = Modifier.background(SurfaceHigh, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonBlue)
                }
                IconButton(
                    onClick = { /* Delete */ },
                    modifier = Modifier.background(SurfaceHigh, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }
    }
}

data class HistoryItem(
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val duration: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
