package app.excoda.core.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface FabMenuHost {
    fun register(item: FabMenuItem)
    fun unregister(id: String)
    fun items(): List<FabMenuItem>
    val isCenterZoneHidden: StateFlow<Boolean>
    fun setCenterZoneHidden(hidden: Boolean)
    val isExpanded: StateFlow<Boolean>
    fun setExpanded(expanded: Boolean)
}

@Immutable
data class FabMenuItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: suspend () -> Unit,
    val order: Int = 0,
    val enabled: Boolean = true,
    val isToggle: Boolean = false,
    val isActive: StateFlow<Boolean>? = null
)

class DefaultFabMenuHost : FabMenuHost {
    private val items = mutableStateListOf<FabMenuItem>()

    private val _isCenterZoneHidden = MutableStateFlow(false)
    override val isCenterZoneHidden: StateFlow<Boolean> = _isCenterZoneHidden

    private val _isExpanded = MutableStateFlow(false)
    override val isExpanded: StateFlow<Boolean> = _isExpanded

    override fun setCenterZoneHidden(hidden: Boolean) {
        _isCenterZoneHidden.value = hidden
    }

    override fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    override fun register(item: FabMenuItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items[index] = item
        } else {
            items.add(item)
        }
        items.sortBy { it.order }
    }

    override fun unregister(id: String) {
        items.removeAll { it.id == id }
    }

    override fun items(): List<FabMenuItem> = items.toList()
}

@Composable
fun FabMenu(
    host: FabMenuHost,
    modifier: Modifier = Modifier,
    isGesturesEnabled: Boolean = false,
    isCenterZoneHidden: Boolean = false,
    bottomPadding: Dp = 16.dp
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val items = host.items()

    val fabAlpha = when {
        isCenterZoneHidden -> 0f
        isGesturesEnabled -> 0.3f
        else -> 1f
    }

    AnimatedVisibility(
        visible = !isCenterZoneHidden,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = modifier.padding(bottom = bottomPadding, end = 16.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        items.forEach { item ->
                            val activeState = item.isActive?.collectAsState()
                            val isActive = activeState?.value == true

                            val containerColor = when {
                                !item.enabled -> MaterialTheme.colorScheme.surfaceVariant
                                item.isToggle && isActive -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.alpha(fabAlpha)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        if (item.enabled) {
                                            expanded = false
                                            host.setExpanded(false)
                                            scope.launch { item.onClick() }
                                        }
                                    },
                                    containerColor = containerColor,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 4.dp,
                                        focusedElevation = 2.dp,
                                        hoveredElevation = 3.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                }
                            }
                        }
                    }
                }

                val mainIcon = if (expanded) Icons.Rounded.Close else Icons.Rounded.Menu
                FloatingActionButton(
                    onClick = {
                        expanded = !expanded
                        host.setExpanded(expanded)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(fabAlpha)
                ) {
                    Icon(
                        imageVector = mainIcon,
                        contentDescription = if (expanded) "Close actions" else "Open actions"
                    )
                }
            }
        }
    }
}
