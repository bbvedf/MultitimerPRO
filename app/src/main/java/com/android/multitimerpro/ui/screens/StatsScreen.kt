package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.ui.theme.*

@Composable
fun StatsScreen() {
    val categories = listOf(
        CategoryStat("PROYECTOS", 0.4f, false),
        CategoryStat("FOCUS", 0.7f, true),
        CategoryStat("REPOSO", 0.2f, false),
        CategoryStat("FITNESS", 0.8f, false),
        CategoryStat("HOGAR", 0.5f, false)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item { Spacer(modifier = Modifier.height(64.dp)) }

        // Editorial Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DASHBOARD DE PRECISIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonBlue,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Estadísticas\nSemanales.",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    lineHeight = 56.sp
                )
                Text(
                    text = "Análisis profundo de su rendimiento temporal a través de capas cinéticas de datos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Main Chart Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uso por Categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LUN — DOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier
                                .background(SurfaceVariant, RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        categories.forEach { stat ->
                            CategoryBar(stat, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Insights Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InsightCard(
                    title = "INSIGHTS DE EFICIENCIA",
                    description = "Has aumentado tu tiempo de Deep Work en un 12% respecto a la semana pasada.",
                    icon = Icons.Default.TrendingUp,
                    accentColor = NeonGreen
                )
                InsightCard(
                    title = "SUGERENCIA PRO",
                    description = "Tu pico de productividad ocurre a las 10:30 AM. Considera programar tus timers de \"Proyectos\" en esa franja.",
                    icon = Icons.Default.Bolt,
                    accentColor = NeonBlue
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun CategoryBar(stat: CategoryStat, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(stat.percentage)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    if (stat.isActive) {
                        Brush.verticalGradient(
                            listOf(NeonBlue, NeonBlue.copy(alpha = 0.2f))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(SurfaceVariant, SurfaceVariant)
                        )
                    }
                )
        )
        Text(
            text = stat.name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = if (stat.isActive) NeonBlue else OnSurfaceVariant,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun InsightCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceHigh,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .drawBehindLeftBorder(accentColor),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

private fun Modifier.drawBehindLeftBorder(color: Color) = this.drawBehind {
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, size.height),
        strokeWidth = 4.dp.toPx()
    )
}

data class CategoryStat(val name: String, val percentage: Float, val isActive: Boolean)
