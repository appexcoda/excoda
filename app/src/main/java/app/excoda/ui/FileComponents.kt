package app.excoda.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemSimple(
    scope: ReorderableCollectionItemScope,
    fileName: String,
    onFileClick: () -> Unit,
    onLongPress: () -> Unit,
    isDragging: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val iconColor = getFileIconColor(fileName)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val rippleIndication = remember { ripple(color = primaryColor) }

    val coroutineScope = rememberCoroutineScope()
    var currentPress by remember { mutableStateOf<PressInteraction.Press?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp,
            hoveredElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .indication(interactionSource, rippleIndication)
                .pointerInput(isSelectionMode) {
                    detectTapGestures(
                        onLongPress = {
                            if (!isSelectionMode) {
                                coroutineScope.launch {
                                    currentPress?.let { press ->
                                        interactionSource.emit(PressInteraction.Cancel(press))
                                        currentPress = null
                                    }
                                }

                                onLongPress()
                            }
                        },
                        onPress = { offset ->
                            coroutineScope.launch {
                                val press = PressInteraction.Press(offset)
                                currentPress = press
                                interactionSource.emit(press)
                                val released = tryAwaitRelease()
                                if (released) {
                                    interactionSource.emit(PressInteraction.Release(press))
                                } else {
                                    interactionSource.emit(PressInteraction.Cancel(press))
                                }
                                currentPress = null
                            }
                        },
                        onTap = {
                            if (isSelectionMode) {
                                onToggleSelection()
                            } else {
                                onFileClick()
                            }
                        }
                    )
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isSelectionMode) {
                androidx.compose.material3.IconButton(
                    modifier = with(scope) {
                        Modifier
                            .size(24.dp)
                            .draggableHandle(interactionSource = interactionSource)
                            .clearAndSetSemantics { }
                    },
                    onClick = {}
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "Reorder",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier.size(28.dp, 28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                }
            }
        }
    }
}


@Composable
fun FileSelectionToolbar(
    selectedCount: Int,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onMove,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text("Move")
            }

            FilledTonalButton(
                onClick = onCopy,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text("Copy")
            }

            FilledTonalButton(
                onClick = onDelete,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Remove")
            }

            FilledTonalButton(
                onClick = onInfo,
                enabled = selectedCount == 1,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Info")
            }

            FilledTonalButton(
                onClick = onCancel,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        }
    }
}
