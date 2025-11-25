package app.excoda.features.alphatab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaTabSettingsDrawer(
    fileSettings: AlphaTabSettings,
    trackSettings: AlphaTabTrackSettings,
    availableTracks: List<TrackInfo>,
    onFileSettingsChanged: (AlphaTabSettings) -> Unit,
    onTrackSettingsChanged: (AlphaTabTrackSettings) -> Unit,
    onClose: () -> Unit
) {
    var localFileSettings by remember(fileSettings) { mutableStateOf(fileSettings) }
    var localTrackSettings by remember(trackSettings) { mutableStateOf(trackSettings) }

    var barsPerRowText by remember(trackSettings) {
        mutableStateOf(
            if (trackSettings.barsPerRow > 0) trackSettings.barsPerRow.toString()
            else "4"
        )
    }
    var systemsSpacingText by remember(trackSettings) {
        mutableStateOf(
            if (trackSettings.systemsSpacing > 0) trackSettings.systemsSpacing.toString()
            else "30"
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Open)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            if (localFileSettings != fileSettings) {
                onFileSettingsChanged(localFileSettings)
            }
            if (localTrackSettings != trackSettings) {
                onTrackSettingsChanged(localTrackSettings)
            }
            onClose()
        }
    }

    val closeDrawer: () -> Unit = {
        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.30f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        IconButton(onClick = closeDrawer) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.shadow(
                            elevation = if (scrollState.canScrollBackward) 4.dp else 0.dp
                        )
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            SettingsSectionHeader("General")

                            DropdownSetting(
                                label = "Stave profile",
                                options = listOf(
                                    "default" to "Auto",
                                    "tabmixed" to "TabMixed",
                                    "scoretab" to "ScoreTab",
                                    "score" to "Score",
                                    "tab" to "Tab"
                                ),
                                selectedValue = localTrackSettings.staveProfile,
                                onValueChange = { localTrackSettings = localTrackSettings.copy(staveProfile = it) }
                            )

                            if (availableTracks.isNotEmpty()) {
                                var trackDropdownExpanded by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Track",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = trackDropdownExpanded,
                                        onExpandedChange = {
                                            if (availableTracks.size > 1) {
                                                trackDropdownExpanded = !trackDropdownExpanded
                                            }
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = availableTracks.getOrNull(localFileSettings.selectedTrackIndex)?.name ?: "Track 1",
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = availableTracks.size > 1,
                                            trailingIcon = {
                                                if (availableTracks.size > 1) {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackDropdownExpanded)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = availableTracks.size > 1),
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )

                                        if (availableTracks.size > 1) {
                                            ExposedDropdownMenu(
                                                expanded = trackDropdownExpanded,
                                                onDismissRequest = { trackDropdownExpanded = false }
                                            ) {
                                                availableTracks.forEach { track ->
                                                    DropdownMenuItem(
                                                        text = { Text(track.name) },
                                                        onClick = {
                                                            localFileSettings = localFileSettings.copy(selectedTrackIndex = track.index)
                                                            onFileSettingsChanged(localFileSettings)
                                                            trackDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DropdownSetting(
                                label = "Scale",
                                options = listOf(
                                    0.7f to "0.7",
                                    0.8f to "0.8",
                                    0.9f to "0.9",
                                    1.0f to "1",
                                    1.25f to "1.25",
                                    1.5f to "1.5"
                                ),
                                selectedValue = localTrackSettings.scale,
                                onValueChange = { localTrackSettings = localTrackSettings.copy(scale = it) }
                            )

                            DropdownSetting(
                                label = "Stretch",
                                options = listOf(
                                    0.7f to "0.7",
                                    0.8f to "0.8",
                                    0.9f to "0.9",
                                    1.0f to "1",
                                    1.25f to "1.25",
                                    1.5f to "1.5"
                                ),
                                selectedValue = localTrackSettings.stretch,
                                onValueChange = { localTrackSettings = localTrackSettings.copy(stretch = it) }
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DropdownSetting(
                                    label = "Tab rhythm mode",
                                    options = listOf(
                                        "automatic" to "Automatic",
                                        "showwithbars" to "Show with bars",
                                        "showwithbeams" to "Show with beams",
                                        "hidden" to "Hidden"
                                    ),
                                    selectedValue = localTrackSettings.tabRhythmMode,
                                    onValueChange = { localTrackSettings = localTrackSettings.copy(tabRhythmMode = it) }
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Bars per System",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = localTrackSettings.isAutomaticBarsPerRow,
                                            onCheckedChange = { isAuto ->
                                                localTrackSettings = if (isAuto) {
                                                    localTrackSettings.copy(isAutomaticBarsPerRow = true)
                                                } else {
                                                    val value = barsPerRowText.toIntOrNull()?.coerceIn(1, 50) ?: 4
                                                    localTrackSettings.copy(
                                                        isAutomaticBarsPerRow = false,
                                                        barsPerRow = value
                                                    )
                                                }
                                            }
                                        )
                                        Text("Auto")
                                    }
                                    OutlinedTextField(
                                        value = barsPerRowText,
                                        onValueChange = { newValue ->
                                            barsPerRowText = newValue
                                            newValue.toIntOrNull()?.let { value ->
                                                if (value in 1..50) {
                                                    localTrackSettings = localTrackSettings.copy(
                                                        barsPerRow = value,
                                                        isAutomaticBarsPerRow = false
                                                    )
                                                }
                                            }
                                        },
                                        enabled = !localTrackSettings.isAutomaticBarsPerRow,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true,
                                        placeholder = { Text("4") }
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Systems Spacing",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = localTrackSettings.isAutomaticSystemsSpacing,
                                            onCheckedChange = { isAuto ->
                                                localTrackSettings = if (isAuto) {
                                                    localTrackSettings.copy(isAutomaticSystemsSpacing = true)
                                                } else {
                                                    val value = systemsSpacingText.toIntOrNull()?.coerceIn(0, 500) ?: 30
                                                    localTrackSettings.copy(
                                                        isAutomaticSystemsSpacing = false,
                                                        systemsSpacing = value
                                                    )
                                                }
                                            }
                                        )
                                        Text("Auto")
                                    }
                                    OutlinedTextField(
                                        value = systemsSpacingText,
                                        onValueChange = { newValue ->
                                            systemsSpacingText = newValue
                                            newValue.toIntOrNull()?.let { value ->
                                                if (value in 0..500) {
                                                    localTrackSettings = localTrackSettings.copy(
                                                        systemsSpacing = value,
                                                        isAutomaticSystemsSpacing = false
                                                    )
                                                }
                                            }
                                        },
                                        enabled = !localTrackSettings.isAutomaticSystemsSpacing,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true,
                                        placeholder = { Text("0") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsSectionHeader("Style")

                            SwitchSetting(
                                label = "Tempo",
                                checked = localTrackSettings.showTempo,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showTempo = it) }
                            )
                            SwitchSetting(
                                label = "Tuning",
                                checked = localTrackSettings.showTuning,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showTuning = it) }
                            )
                            SwitchSetting(
                                label = "Dynamics",
                                checked = localTrackSettings.showDynamics,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showDynamics = it) }
                            )
                            SwitchSetting(
                                label = "Text",
                                checked = localTrackSettings.showText,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showText = it) }
                            )
                            SwitchSetting(
                                label = "Beat barre",
                                checked = localTrackSettings.showBeatBarre,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showBeatBarre = it) }
                            )
                            SwitchSetting(
                                label = "Pick stroke",
                                checked = localTrackSettings.showPickStroke,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showPickStroke = it) }
                            )
                            SwitchSetting(
                                label = "Chord diagrams",
                                checked = localTrackSettings.showChordDiagrams,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showChordDiagrams = it) }
                            )
                            SwitchSetting(
                                label = "Chord names",
                                checked = localTrackSettings.showChordNames,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showChordNames = it) }
                            )
                            SwitchSetting(
                                label = "Lyrics",
                                checked = localTrackSettings.showLyrics,
                                onCheckedChange = { localTrackSettings = localTrackSettings.copy(showLyrics = it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (scrollState.canScrollForward) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Scroll for more",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        content = { }
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSetting(
    label: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onValueChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = options.find { it.first == selectedValue }?.second ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, displayName) ->
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}