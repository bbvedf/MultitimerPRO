package com.android.multitimerpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.ui.theme.*

@Composable
fun TimerCard(
    timer: TimerEntity,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(timer.color)))

            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = timer.name, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                        Text(text = timer.status, style = MaterialTheme.typography.labelSmall, color = Color(timer.color), fontWeight = FontWeight.Black, fontSize = 8.sp)

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val minutes = (timer.remainingTime / 1000) / 60
                        val seconds = (timer.remainingTime / 1000) % 60
                        val timeStr = String.format("%02d:%02d", minutes, seconds)
                        Text(text = timeStr, style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = if (timer.duration > 0) timer.remainingTime.toFloat() / timer.duration.toFloat() else 0f,
                        modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Color(timer.color),
                        trackColor = SurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = OnSurfaceVariant)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (timer.status == "LIVE") NeonBlue else SurfaceVariant,
                        onClick = onToggle
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (timer.status == "LIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (timer.status == "LIVE") DeepBlack else NeonBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
