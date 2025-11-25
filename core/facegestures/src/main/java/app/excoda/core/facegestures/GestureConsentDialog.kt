package app.excoda.core.facegestures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDecline
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enable Face Gestures?",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "This feature uses your front camera and Google MediaPipe for on-device face tracking. " +
                            "Your camera feed never leaves your device. MediaPipe may send anonymous usage analytics to Google.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDecline) {
                        Text("Decline")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onAccept) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}