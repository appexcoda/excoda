package app.excoda.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.excoda.core.logging.LxLog
import app.excoda.viewmodel.MainViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onFileClick: (String) -> Unit = {},
    onTakePersistablePermission: (Uri) -> Unit = {}
) {
    LxLog.d("MainScreen", "=== MainScreen RECOMPOSE START ===")
    val composeStartTime = System.currentTimeMillis()
    
    val isLoading by viewModel.isLoading.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val context = LocalContext.current
    
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    var showNoTabsDialog by remember { mutableStateOf(false) }
    var showInvalidFileDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.activeTabId) {
        val activeTab = uiState.tabs.find { it.id == uiState.activeTabId }
        LxLog.d("MainScreen", "Active tab changed to '${activeTab?.name}' with ${activeTab?.files?.size ?: 0} files")
    }
    
    LxLog.d("MainScreen", "MainScreen setup took ${System.currentTimeMillis() - composeStartTime}ms")
    
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            val fileName = getFileNameFromUri(context, uri)
            if (isValidFileExtension(fileName)) {
                onTakePersistablePermission(uri)
                viewModel.addFileToActiveTab(fileName, uri.toString())
            } else {
                showInvalidFileDialog = true
            }
        }
    }
    
    
    val handleAddFile = {
        if (uiState.tabs.isEmpty()) {
            showNoTabsDialog = true
        } else {
            filePickerLauncher.launch(arrayOf(
                "application/pdf",
                "application/octet-stream",
                "text/xml",
                "application/xml",
                "application/vnd.recordare.musicxml+xml",
                "application/vnd.recordare.musicxml",
                "application/gpx+xml",
                "application/vnd.recordare.musicxml.mscz"
            ))
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.addNewTab() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add new tab",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    val lazyListState = rememberLazyListState()
                    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        viewModel.reorderTabs(from.index, to.index)
                    }
                    
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.tabs.size, key = { uiState.tabs[it].id }) { index ->
                            ReorderableItem(reorderableLazyListState, key = uiState.tabs[index].id) { isDragging ->
                                val tab = uiState.tabs[index]
                                TabItem(
                                    scope = this,
                                    tabId = tab.id,
                                    tabName = tab.name,
                                    isActive = tab.id == uiState.activeTabId,
                                    onClick = { viewModel.selectTab(tab.id) },
                                    onRename = { newName -> viewModel.renameTab(tab.id, newName) },
                                    onCopy = { viewModel.copyTab(tab.id) },
                                    onDelete = { viewModel.deleteTab(tab.id) },
                                    isDragging = isDragging
                                )
                            }
                        }
                    }
                }
            }
            
            val fileListStartTime = System.currentTimeMillis()
            LxLog.d("MainScreen", ">>> File list section RECOMPOSE")
            
            val activeTab = uiState.tabs.find { it.id == uiState.activeTabId }
            val files = activeTab?.files ?: emptyList()
            
            LxLog.d("MainScreen", "File lookup took ${System.currentTimeMillis() - fileListStartTime}ms, found ${files.size} files")
            
            var showFileInfoDialog by remember(uiState.activeTabId) { mutableStateOf(false) }
            var showMoveDialog by remember(uiState.activeTabId) { mutableStateOf(false) }
            var showCopyDialog by remember(uiState.activeTabId) { mutableStateOf(false) }
            var showRemoveDialog by remember(uiState.activeTabId) { mutableStateOf(false) }
            
            val stateSetupTime = System.currentTimeMillis() - fileListStartTime
            LxLog.d("MainScreen", "State setup took ${stateSetupTime}ms")
            
            LaunchedEffect(uiState.activeTabId) {
                LxLog.d("MainScreen", "File list section: activeTabId=${uiState.activeTabId}, files=${files.size}")
                if (isSelectionMode) {
                    viewModel.exitSelectionMode()
                }
            }
            
            if (files.isEmpty() && uiState.tabs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No files added yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        val urlText = "https://excoda.app"
                        Text(
                            text = urlText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, urlText.toUri())
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            } else if (uiState.tabs.isNotEmpty()) {
                key(uiState.activeTabId) {
                    val boxStartTime = System.currentTimeMillis()
                    LxLog.d("MainScreen", ">>> Box with files RECOMPOSE")
                    
                    LaunchedEffect(uiState.activeTabId) {
                        LxLog.d("MainScreen", "LazyColumn for tab '${activeTab?.name}' with ${files.size} files")
                    }
                    
                    val fileLazyListState = rememberLazyListState()
                    val reorderableFileListState = rememberReorderableLazyListState(fileLazyListState) { from, to ->
                        viewModel.reorderFilesInTab(uiState.activeTabId, from.index, to.index)
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val lazyColumnStart = System.currentTimeMillis()
                        LxLog.d("MainScreen", ">>>>> LazyColumn COMPOSE START with ${files.size} items")
                        
                        val contentPadding = remember { androidx.compose.foundation.layout.PaddingValues(16.dp) }
                        
                        LazyColumn(
                            state = fileLazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = files,
                                key = { it.id }
                            ) { fileItem ->
                                ReorderableItem(reorderableFileListState, key = fileItem.id) { isDragging ->
                                    FileItemSimple(
                                        scope = this,
                                        fileName = fileItem.fileName,
                                        onFileClick = {
                                            if (fileItem.uri.isNotBlank()) {
                                                onFileClick(fileItem.uri)
                                            } else {
                                                LxLog.w("MainScreen", "Cannot open file ${fileItem.fileName}: empty URI")
                                            }
                                        },
                                        onLongPress = {
                                            viewModel.enterSelectionMode()
                                            viewModel.toggleFileSelection(fileItem.id)
                                        },
                                        isDragging = isDragging,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedFileIds.contains(fileItem.id),
                                        onToggleSelection = {
                                            viewModel.toggleFileSelection(fileItem.id)
                                        }
                                    )
                                }
                            }
                        }
                        
                        LxLog.d("MainScreen", ">>>>> LazyColumn COMPOSE END took ${System.currentTimeMillis() - lazyColumnStart}ms")
                        LxLog.d("MainScreen", ">>> Box END took ${System.currentTimeMillis() - boxStartTime}ms")
                    }
                }
                
                if (showMoveDialog) {
                    MoveToTabDialog(
                        currentTabId = uiState.activeTabId,
                        allTabs = uiState.tabs,
                        onDismiss = { showMoveDialog = false },
                        onConfirm = { tabIds ->
                            viewModel.moveSelectedFilesToTabs(uiState.activeTabId, tabIds)
                            showMoveDialog = false
                        }
                    )
                }
                
                if (showCopyDialog) {
                    CopyToTabDialog(
                        currentTabId = uiState.activeTabId,
                        allTabs = uiState.tabs,
                        onDismiss = { showCopyDialog = false },
                        onConfirm = { tabIds ->
                            viewModel.copySelectedFilesToTabs(uiState.activeTabId, tabIds)
                            showCopyDialog = false
                        }
                    )
                }
                
                if (showRemoveDialog) {
                    val currentTabName = uiState.tabs.find { it.id == uiState.activeTabId }?.name ?: "this"
                    RemoveFileDialog(
                        fileCount = selectedFileIds.size,
                        listName = currentTabName,
                        onDismiss = { showRemoveDialog = false },
                        onConfirm = {
                            viewModel.deleteSelectedFilesFromTab(uiState.activeTabId)
                            showRemoveDialog = false
                        }
                    )
                }
                
                if (showFileInfoDialog) {
                    val selectedFile = files.find { selectedFileIds.contains(it.id) }
                    selectedFile?.let { file ->
                        FileInfoDialog(
                            fileName = file.fileName,
                            onDismiss = { showFileInfoDialog = false }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            
            if (isSelectionMode) {
                FileSelectionToolbar(
                    selectedCount = selectedFileIds.size,
                    onMove = { showMoveDialog = true },
                    onCopy = { showCopyDialog = true },
                    onDelete = { showRemoveDialog = true },
                    onInfo = {
                        if (selectedFileIds.size == 1) {
                            showFileInfoDialog = true
                        }
                    },
                    onCancel = { viewModel.exitSelectionMode() }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilledTonalButton(
                                onClick = handleAddFile,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Add files")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showNoTabsDialog) {
        NoTabsDialog(onDismiss = { showNoTabsDialog = false })
    }
    
    if (showInvalidFileDialog) {
        InvalidFileTypeDialog(onDismiss = { showInvalidFileDialog = false })
    }
}