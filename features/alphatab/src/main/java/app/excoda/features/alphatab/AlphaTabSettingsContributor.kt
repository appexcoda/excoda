package app.excoda.features.alphatab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.SettingsContributor
import app.excoda.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlphaTabSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val fileSettingsFlows = mutableMapOf<String, StateFlow<AlphaTabSettings>>()
    private val trackSettingsFlows = mutableMapOf<String, StateFlow<AlphaTabTrackSettings>>()

    private val _availableTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val availableTracks: StateFlow<List<TrackInfo>> = _availableTracks

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex

    fun setAvailableTracks(tracks: List<TrackInfo>) {
        _availableTracks.value = tracks
    }

    fun observeFileSettings(fileUri: String): StateFlow<AlphaTabSettings> {
        return fileSettingsFlows.getOrPut(fileUri) {
            settingsRepository.observeSettings(
                fileUri = fileUri,
                moduleName = AlphaTabSettings.MODULE_NAME,
                defaultSettings = AlphaTabSettings.DEFAULT,
                deserializer = AlphaTabSettings::fromJson
            ).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = AlphaTabSettings.DEFAULT
            )
        }
    }

    fun observeTrackSettings(fileUri: String, trackIndex: Int): StateFlow<AlphaTabTrackSettings> {
        val key = "$fileUri:track$trackIndex"
        return trackSettingsFlows.getOrPut(key) {
            settingsRepository.observeTrackSettings(
                fileUri = fileUri,
                moduleName = AlphaTabSettings.MODULE_NAME,
                trackIndex = trackIndex,
                defaultSettings = AlphaTabTrackSettings.DEFAULT,
                deserializer = AlphaTabTrackSettings::fromJson
            ).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = AlphaTabTrackSettings.DEFAULT
            )
        }
    }

    fun saveFileSettings(fileUri: String, settings: AlphaTabSettings) {
        viewModelScope.launch {
            LxLog.d("AlphaTabSettingsVM", "Saving file settings for $fileUri: $settings")
            settingsRepository.saveSettings(
                fileUri = fileUri,
                moduleName = AlphaTabSettings.MODULE_NAME,
                settings = settings
            )
        }
    }

    fun saveTrackSettings(fileUri: String, trackIndex: Int, settings: AlphaTabTrackSettings) {
        viewModelScope.launch {
            LxLog.d("AlphaTabSettingsVM", "Saving track $trackIndex settings for $fileUri: $settings")
            settingsRepository.saveTrackSettings(
                fileUri = fileUri,
                moduleName = AlphaTabSettings.MODULE_NAME,
                trackIndex = trackIndex,
                settings = settings
            )
        }
    }

    fun setCurrentTrack(trackIndex: Int) {
        _currentTrackIndex.value = trackIndex
    }
}

object AlphaTabSettingsContributor : SettingsContributor {
    override val moduleName: String = AlphaTabSettings.MODULE_NAME

    @Composable
    override fun SettingsEntry(
        fileUri: String,
        onClose: () -> Unit
    ) {
        val viewModel: AlphaTabSettingsViewModel = hiltViewModel()

        val fileSettings by viewModel.observeFileSettings(fileUri).collectAsState()
        val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
        val trackSettings by viewModel.observeTrackSettings(fileUri, currentTrackIndex).collectAsState()
        val availableTracks by viewModel.availableTracks.collectAsState()

        AlphaTabSettingsDrawer(
            fileSettings = fileSettings,
            trackSettings = trackSettings,
            availableTracks = availableTracks,
            onFileSettingsChanged = { newSettings ->
                LxLog.d("AlphaTabSettings", "Saving file settings: $newSettings")
                viewModel.saveFileSettings(fileUri, newSettings)
                viewModel.setCurrentTrack(newSettings.selectedTrackIndex)
            },
            onTrackSettingsChanged = { newSettings ->
                LxLog.d("AlphaTabSettings", "Saving track $currentTrackIndex settings: $newSettings")
                viewModel.saveTrackSettings(fileUri, currentTrackIndex, newSettings)
            },
            onClose = onClose
        )
    }
}