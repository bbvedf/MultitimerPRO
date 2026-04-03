package com.android.multitimerpro.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.android.multitimerpro.ui.theme.*

@Composable
fun DeleteConfirmationDialog(
    timerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceHigh,
        title = {
            Text(
                text = "BORRAR CONTADOR",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que quieres eliminar '$timerName'? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "BORRAR",
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
