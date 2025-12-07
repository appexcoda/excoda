package app.excoda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.excoda.core.logging.LxLog
import app.excoda.data.AppRepository
import app.excoda.data.AppState
import app.excoda.data.FileItem
import app.excoda.data.TabData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private var saveJob: Job? = null
    private val saveDebounceMs = 300L

    init {
        loadState()
    }
    
    private fun loadState() {
        viewModelScope.launch {
            repository.appState.collect { state ->
                _uiState.value = state
                if (_isLoading.value) {
                    _isLoading.value = false
                    LxLog.d("MainViewModel", "Initial data loaded, loading complete")
                }
            }
        }
    }
    

    fun addNewTab() {
        val newTab = TabData(
            id = "tab_${UUID.randomUUID()}",
            name = "New list",
            files = emptyList()
        )
        val updatedTabs = _uiState.value.tabs + newTab
        updateState(_uiState.value.copy(
            tabs = updatedTabs,
            activeTabId = newTab.id
        ))
    }

    fun selectTab(tabId: String) {
        val startTime = System.currentTimeMillis()
        val currentTab = _uiState.value.tabs.find { it.id == _uiState.value.activeTabId }
        val newTab = _uiState.value.tabs.find { it.id == tabId }

        LxLog.d("MainViewModel", "selectTab: Switching from '${currentTab?.name}' (${currentTab?.files?.size ?: 0} files) to '${newTab?.name}' (${newTab?.files?.size ?: 0} files)")

        updateStateImmediate(_uiState.value.copy(activeTabId = tabId))

        val elapsed = System.currentTimeMillis() - startTime
        LxLog.d("MainViewModel", "selectTab: State update completed in ${elapsed}ms")
    }

    fun addFileToActiveTab(fileName: String, uri: String) {
        val activeTabId = _uiState.value.activeTabId
        val newFileItem = FileItem(id = "file_${UUID.randomUUID()}", fileName = fileName, uri = uri)
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == activeTabId) {
                tab.copy(files = tab.files + newFileItem)
            } else {
                tab
            }
        }
        updateState(_uiState.value.copy(tabs = updatedTabs))
    }

    fun renameTab(tabId: String, newName: String) {
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == tabId) {
                tab.copy(name = newName)
            } else {
                tab
            }
        }
        updateState(_uiState.value.copy(tabs = updatedTabs))
    }

    fun deleteTab(tabId: String) {
        val updatedTabs = _uiState.value.tabs.filter { it.id != tabId }

        val newActiveTabId = if (_uiState.value.activeTabId == tabId) {
            updatedTabs.firstOrNull()?.id ?: ""
        } else {
            _uiState.value.activeTabId
        }

        updateState(_uiState.value.copy(
            tabs = updatedTabs,
            activeTabId = newActiveTabId
        ))
    }

    fun copyTab(tabId: String) {
        val tabToCopy = _uiState.value.tabs.find { it.id == tabId }
        if (tabToCopy != null) {
            val newTab = TabData(
                id = "tab_${UUID.randomUUID()}",
                name = "${tabToCopy.name} - Copy",
                files = tabToCopy.files.map { file ->
                    file.copy(id = "file_${UUID.randomUUID()}")
                }
            )

            val updatedTabs = _uiState.value.tabs + newTab
            updateState(_uiState.value.copy(
                tabs = updatedTabs,
                activeTabId = newTab.id
            ))
        }
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val currentTabs = _uiState.value.tabs.toMutableList()
        val movedTab = currentTabs.removeAt(fromIndex)
        currentTabs.add(toIndex, movedTab)
        updateState(_uiState.value.copy(tabs = currentTabs))
    }

    fun reorderFilesInTab(tabId: String, fromIndex: Int, toIndex: Int) {
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == tabId) {
                val currentFiles = tab.files.toMutableList()
                val movedFile = currentFiles.removeAt(fromIndex)
                currentFiles.add(toIndex, movedFile)
                tab.copy(files = currentFiles)
            } else {
                tab
            }
        }
        updateState(_uiState.value.copy(tabs = updatedTabs))
    }

    private fun updateState(newState: AppState) {
        val startTime = System.currentTimeMillis()
        _uiState.value = newState
        val stateUpdateTime = System.currentTimeMillis() - startTime

        LxLog.d("MainViewModel", "updateState: State assignment took ${stateUpdateTime}ms")

        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(saveDebounceMs)
            val saveStartTime = System.currentTimeMillis()
            repository.saveAppState(newState)
            val saveTime = System.currentTimeMillis() - saveStartTime
            LxLog.d("MainViewModel", "updateState: Repository save took ${saveTime}ms (debounced)")
        }
    }

    private fun updateStateImmediate(newState: AppState) {
        val startTime = System.currentTimeMillis()
        _uiState.value = newState
        val stateUpdateTime = System.currentTimeMillis() - startTime

        LxLog.d("MainViewModel", "updateStateImmediate: State assignment took ${stateUpdateTime}ms")

        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(saveDebounceMs)
            val saveStartTime = System.currentTimeMillis()
            repository.saveAppState(newState)
            val saveTime = System.currentTimeMillis() - saveStartTime
            LxLog.d("MainViewModel", "updateStateImmediate: Repository save took ${saveTime}ms (debounced)")
        }
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedFileIds.value = emptySet()
    }

    fun toggleFileSelection(fileId: String) {
        _selectedFileIds.value = if (_selectedFileIds.value.contains(fileId)) {
            _selectedFileIds.value - fileId
        } else {
            _selectedFileIds.value + fileId
        }

        if (_selectedFileIds.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun moveSelectedFilesToTabs(fromTabId: String, toTabIds: List<String>) {
        val selectedIds = _selectedFileIds.value
        if (selectedIds.isEmpty()) return

        val filesToMove = _uiState.value.tabs
            .find { it.id == fromTabId }
            ?.files
            ?.filter { selectedIds.contains(it.id) }
            ?: emptyList()

        val updatedTabs = _uiState.value.tabs.map { tab ->
            when {
                tab.id == fromTabId -> {
                    tab.copy(files = tab.files.filter { !selectedIds.contains(it.id) })
                }
                toTabIds.contains(tab.id) -> {
                    tab.copy(files = tab.files + filesToMove)
                }
                else -> tab
            }
        }

        updateState(_uiState.value.copy(tabs = updatedTabs))
        exitSelectionMode()
    }

    fun copySelectedFilesToTabs(fromTabId: String, toTabIds: List<String>) {
        val selectedIds = _selectedFileIds.value
        if (selectedIds.isEmpty()) return

        val filesToCopy = _uiState.value.tabs
            .find { it.id == fromTabId }
            ?.files
            ?.filter { selectedIds.contains(it.id) }
            ?: emptyList()

        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (toTabIds.contains(tab.id)) {
                val newFiles = filesToCopy.map { file ->
                    file.copy(id = "file_${UUID.randomUUID()}")
                }
                tab.copy(files = tab.files + newFiles)
            } else {
                tab
            }
        }

        updateState(_uiState.value.copy(tabs = updatedTabs))
        exitSelectionMode()
    }

    fun deleteSelectedFilesFromTab(tabId: String) {
        val selectedIds = _selectedFileIds.value
        if (selectedIds.isEmpty()) return

        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == tabId) {
                tab.copy(files = tab.files.filter { !selectedIds.contains(it.id) })
            } else {
                tab
            }
        }

        updateState(_uiState.value.copy(tabs = updatedTabs))
        exitSelectionMode()
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            LxLog.d("MainViewModel", "onCleared: Forcing final save")
            repository.saveAppState(_uiState.value)
        }
    }
}
