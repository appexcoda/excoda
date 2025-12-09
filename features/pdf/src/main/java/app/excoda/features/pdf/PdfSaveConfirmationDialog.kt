package app.excoda.features.pdf

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PdfSaveConfirmationDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Annotations?") },
        text = { Text("Do you want to save your annotations before leaving?") },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("No")
            }
        }
    )
}