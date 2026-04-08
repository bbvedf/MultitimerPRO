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
import java.util.Locale

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
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(timer.color)))

            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = timer.name.uppercase(), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Text(
                            text = timer.status, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color(timer.color), 
                            fontWeight = FontWeight.Black, 
                            fontSize = 8.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val hours = (timer.remainingTime / 3600000)
                        val minutes = (timer.remainingTime % 3600000) / 60000
                        val seconds = (timer.remainingTime % 60000) / 1000
                        
                        val timeStr = if (hours > 0) {
                            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        }
                        
                        Text(
                            text = timeStr, 
                            style = if (hours > 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (timer.duration > 0) timer.remainingTime.toFloat() / timer.duration.toFloat() else 0f },
                        modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Color(timer.color),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (timer.status == "LIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onToggle
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val isDark = MaterialTheme.colorScheme.background == DeepBlack
                            Icon(
                                if (timer.status == "LIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (timer.status == "LIVE") (if (isDark) DeepBlack else Color.White) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
