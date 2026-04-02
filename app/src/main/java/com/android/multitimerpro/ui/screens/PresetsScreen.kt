package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.ui.theme.*

@Composable
fun PresetsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = "Presets\nGuardados",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 56.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Main Featured Preset
            FeaturedPresetCard()

            Spacer(modifier = Modifier.height(24.dp))

            // Grid of smaller presets
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallPresetCard(
                    title = "Quick Workout",
                    category = "HIIT",
                    time = "10:00",
                    color = NeonGreen,
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
                SmallPresetCard(
                    title = "Tea Brew",
                    category = "COCINA",
                    time = "03:00",
                    color = NeonPurple,
                    icon = Icons.Default.Coffee,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add New Button
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(32.dp))
                    Text(text = "CREAR NUEVO PRESET", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun FeaturedPresetCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box {
            Box(modifier = Modifier.fillMaxHeight().width(8.dp).background(NeonBlue).align(Alignment.CenterStart))

            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Icon(Icons.Default.Book, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Focus 25m", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(text = "SESIÓN DE TRABAJO PROFUNDO", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                    }
                    Text(text = "25:00", style = MaterialTheme.typography.displayMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { /* Start */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "INICIAR SESIÓN", color = DeepBlack, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun SmallPresetCard(title: String, category: String, time: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(160.dp),
        color = SurfaceHigh,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(text = category, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 8.sp)
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = time, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = SurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
