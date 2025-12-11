package app.excoda.features.alphatab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private data class StyleSetting(
    val label: String,
    val isChecked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

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

    var barsPerRowText by remember { mutableStateOf("") }

    LaunchedEffect(trackSettings) {
        barsPerRowText = if (trackSettings.barsPerRow > 0) trackSettings.barsPerRow.toString() else "4"
    }

    val styleSettings by remember(localTrackSettings) {
        mutableStateOf(
            listOf(
                StyleSetting("Tempo", localTrackSettings.showTempo) { localTrackSettings = localTrackSettings.copy(showTempo = it) },
                StyleSetting("Tuning", localTrackSettings.showTuning) { localTrackSettings = localTrackSettings.copy(showTuning = it) },
                StyleSetting("Dynamics", localTrackSettings.showDynamics) { localTrackSettings = localTrackSettings.copy(showDynamics = it) },
                StyleSetting("Text", localTrackSettings.showText) { localTrackSettings = localTrackSettings.copy(showText = it) },
                StyleSetting("Beat barre", localTrackSettings.showBeatBarre) { localTrackSettings = localTrackSettings.copy(showBeatBarre = it) },
                StyleSetting("Pick stroke", localTrackSettings.showPickStroke) { localTrackSettings = localTrackSettings.copy(showPickStroke = it) },
                StyleSetting("Chord diagrams", localTrackSettings.showChordDiagrams) { localTrackSettings = localTrackSettings.copy(showChordDiagrams = it) },
                StyleSetting("Chord names", localTrackSettings.showChordNames) { localTrackSettings = localTrackSettings.copy(showChordNames = it) },
                StyleSetting("Lyrics", localTrackSettings.showLyrics) { localTrackSettings = localTrackSettings.copy(showLyrics = it) }
            )
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            drawerState.open()
        }
    }

    val currentOnFileSettingsChanged by rememberUpdatedState(onFileSettingsChanged)
    val currentOnTrackSettingsChanged by rememberUpdatedState(onTrackSettingsChanged)
    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .drop(1)
            .filter { it == DrawerValue.Closed }
            .collect {
                if (localFileSettings != fileSettings) {
                    currentOnFileSettingsChanged(localFileSettings)
                }
                if (localTrackSettings != trackSettings) {
                    currentOnTrackSettingsChanged(localTrackSettings)
                }
                currentOnClose()
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
                    HorizontalDivider()
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsSectionHeader("General")

                                    val staveProfileOptions = remember {
                                        listOf(
                                            "default" to "Auto",
                                            "tabmixed" to "Tab Mixed",
                                            "scoretab" to "Score Tab",
                                            "score" to "Score",
                                            "tab" to "Tab"
                                        )
                                    }

                                    DropdownSetting(
                                        label = "Stave profile",
                                        options = staveProfileOptions,
                                        selectedValue = localTrackSettings.staveProfile,
                                        onValueChange = {
                                            localTrackSettings =
                                                localTrackSettings.copy(staveProfile = it)
                                        }
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
                                                    value = availableTracks.getOrNull(localFileSettings.selectedTrackIndex)?.name
                                                        ?: "Track 1",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    enabled = availableTracks.size > 1,
                                                    trailingIcon = {
                                                        if (availableTracks.size > 1) {
                                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                                expanded = trackDropdownExpanded
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor(
                                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                            enabled = availableTracks.size > 1
                                                        ),
                                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                                )

                                                if (availableTracks.size > 1) {
                                                    ExposedDropdownMenu(
                                                        expanded = trackDropdownExpanded,
                                                        onDismissRequest = { trackDropdownExpanded = false }
                                                    ) {
                                                        availableTracks.forEachIndexed { index, track ->
                                                            val isSelected =
                                                                track.index == localFileSettings.selectedTrackIndex
                                                            DropdownMenuItem(
                                                                text = { Text(track.name) },
                                                                onClick = {
                                                                    localFileSettings =
                                                                        localFileSettings.copy(
                                                                            selectedTrackIndex = track.index
                                                                        )
                                                                    onFileSettingsChanged(
                                                                        localFileSettings
                                                                    )
                                                                    trackDropdownExpanded = false
                                                                },
                                                                leadingIcon = {
                                                                    if (isSelected) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "Selected",
                                                                            tint = MaterialTheme.colorScheme.primary
                                                                        )
                                                                    }
                                                                },
                                                                colors = MenuDefaults.itemColors(
                                                                    textColor = if (isSelected) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    } else {
                                                                        MaterialTheme.colorScheme.onSurface
                                                                    }
                                                                )
                                                            )

                                                            if (index < availableTracks.size - 1) {
                                                                HorizontalDivider(
                                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                                    thickness = 1.5.dp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val scaleOptions = remember {
                                        listOf(
                                            0.7f to "0.7",
                                            0.8f to "0.8",
                                            0.9f to "0.9",
                                            1.0f to "1",
                                            1.25f to "1.25",
                                            1.5f to "1.5"
                                        )
                                    }

                                    DropdownSetting(
                                        label = "Scale",
                                        options = scaleOptions,
                                        selectedValue = localTrackSettings.scale,
                                        onValueChange = {
                                            localTrackSettings = localTrackSettings.copy(scale = it)
                                        }
                                    )

                                    val stretchOptions = remember {
                                        listOf(
                                            0.7f to "0.7",
                                            0.8f to "0.8",
                                            0.9f to "0.9",
                                            1.0f to "1",
                                            1.25f to "1.25",
                                            1.5f to "1.5"
                                        )
                                    }

                                    DropdownSetting(
                                        label = "Stretch",
                                        options = stretchOptions,
                                        selectedValue = localTrackSettings.stretch,
                                        onValueChange = {
                                            localTrackSettings = localTrackSettings.copy(stretch = it)
                                        }
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val tabRhythmModeOptions = remember {
                                            listOf(
                                                "automatic" to "Automatic",
                                                "showwithbars" to "Show with bars",
                                                "showwithbeams" to "Show with beams",
                                                "hidden" to "Hidden"
                                            )
                                        }

                                        DropdownSetting(
                                            label = "Tab rhythm mode",
                                            options = tabRhythmModeOptions,
                                            selectedValue = localTrackSettings.tabRhythmMode,
                                            onValueChange = {
                                                localTrackSettings =
                                                    localTrackSettings.copy(tabRhythmMode = it)
                                            }
                                        )
                                    }

                                    AutoManualNumericSetting(
                                        label = "Bars per system",
                                        isAuto = localTrackSettings.isAutomaticBarsPerRow,
                                        onAutoChange = { isAuto ->
                                            localTrackSettings = if (isAuto) {
                                                localTrackSettings.copy(isAutomaticBarsPerRow = true)
                                            } else {
                                                val value = barsPerRowText.toIntOrNull()?.coerceIn(1, 50) ?: 4
                                                localTrackSettings.copy(isAutomaticBarsPerRow = false, barsPerRow = value)
                                            }
                                        },
                                        textValue = barsPerRowText,
                                        onTextChange = { newValue ->
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
                                        placeholder = "4"
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            item {
                                SettingsSectionHeader("Style")
                            }

                            items(styleSettings.size) { index ->
                                val setting = styleSettings[index]
                                SwitchSetting(
                                    label = setting.label,
                                    checked = setting.isChecked,
                                    onCheckedChange = setting.onCheckedChange
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun AutoManualNumericSetting(
    label: String,
    isAuto: Boolean,
    onAutoChange: (Boolean) -> Unit,
    textValue: String,
    onTextChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isAuto,
                    onCheckedChange = onAutoChange
                )
                Text("Auto")
            }
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextChange,
                enabled = !isAuto,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
                singleLine = true,
                placeholder = { Text(placeholder) }
            )
        }
    }
}

@Composable
private fun NumericSetting(
    label: String,
    textValue: String,
    onTextChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) }
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
                onValueChange = { },
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
