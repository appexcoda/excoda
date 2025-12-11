package app.excoda.features.registra

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistraDialog(
    onDismiss: () -> Unit,
    onPreview: (android.net.Uri) -> Unit,
    viewModel: RegistraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.previewUri) {
        uiState.previewUri?.let { uri ->
            onPreview(uri)
            viewModel.clearPreviewUri()
            onDismiss()
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registra",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, "Close")
                            }
                        }
                        PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                            Tab(
                                selected = uiState.selectedTab == RegistraTab.SEARCH,
                                onClick = { viewModel.setTab(RegistraTab.SEARCH) },
                                text = { Text("Search") }
                            )
                            Tab(
                                selected = uiState.selectedTab == RegistraTab.UPLOAD,
                                onClick = { viewModel.setTab(RegistraTab.UPLOAD) },
                                text = { Text("Upload") }
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (uiState.selectedTab) {
                        RegistraTab.SEARCH -> SearchTab(viewModel, uiState)
                        RegistraTab.UPLOAD -> UploadTab(viewModel, uiState)
                    }
                }
            }
        }
    }

    if (uiState.downloadConfirmation != null) {
        DownloadConfirmationDialog(
            file = uiState.downloadConfirmation!!,
            isDownloading = uiState.isDownloading,
            onConfirm = { viewModel.downloadFile() },
            onPreview = { viewModel.previewFile() },
            onDismiss = { viewModel.dismissDownloadConfirmation() }
        )
    }

    uiState.overwriteConfirmation?.let { confirmation ->
        OverwriteConfirmationDialog(
            fileName = confirmation.fileName,
            isProcessing = uiState.isUploading,
            onConfirm = { viewModel.confirmOverwrite() },
            onDismiss = { viewModel.dismissOverwriteConfirmation() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTab(viewModel: RegistraViewModel, state: RegistraUiState) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var fileTypeExpanded by remember { mutableStateOf(false) }
    val fileTypes = listOf(
        "" to "All",
        "gp" to "GP/GPX",
        "pdf" to "PDF",
        "mscz" to "MSCZ",
        "musicxml" to "MusicXML"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.searchText,
            onValueChange = { viewModel.setSearchText(it) },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (state.searchText.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchText("") }) {
                        Icon(Icons.Rounded.Clear, "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchArtist,
            onValueChange = { viewModel.setSearchArtist(it) },
            label = { Text("Artist") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (state.searchArtist.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchArtist("") }) {
                        Icon(Icons.Rounded.Clear, "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchTitle,
            onValueChange = { viewModel.setSearchTitle(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (state.searchTitle.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchTitle("") }) {
                        Icon(Icons.Rounded.Clear, "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = fileTypeExpanded,
            onExpandedChange = { fileTypeExpanded = !fileTypeExpanded }
        ) {
            OutlinedTextField(
                value = fileTypes.find { it.first == state.searchFileType }?.second ?: "All",
                onValueChange = {},
                readOnly = true,
                label = { Text("File type") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fileTypeExpanded) }
            )
            ExposedDropdownMenu(
                expanded = fileTypeExpanded,
                onDismissRequest = { fileTypeExpanded = false }
            ) {
                fileTypes.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.setSearchFileType(value)
                            fileTypeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                viewModel.search()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSearching
        ) {
            if (state.isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching...")
            } else {
                Text("Search")
            }
        }

        state.searchError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.searchResults.isEmpty() && !state.isSearching && state.searchError == null) {
            if (state.currentPage > 0) {
                Text(
                    text = "No results",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            Text(
                text = "Results: ${state.totalResults} files, ${state.totalPages} pages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.searchResults) { file ->
                    SearchResultItem(
                        file = file,
                        onClick = { viewModel.showDownloadConfirmation(file) }
                    )
                }
            }

            if (state.totalPages > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.searchPage(state.currentPage - 1) },
                        enabled = state.currentPage > 1 && !state.isSearching
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Previous page")
                    }
                    Text("Page ${state.currentPage} of ${state.totalPages}")
                    IconButton(
                        onClick = { viewModel.searchPage(state.currentPage + 1) },
                        enabled = state.currentPage < state.totalPages && !state.isSearching
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Next page")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(file: FileResult, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (file.isUploaded) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Uploaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            val metadata = buildList {
                file.artist?.takeIf { it.isNotBlank() }?.let { add(it) }
                file.title?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metadata.joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val extraInfo = buildList {
                file.subtitle?.takeIf { it.isNotBlank() }?.let { add(it) }
                file.album?.takeIf { it.isNotBlank() }?.let { add("♫ $it") }
            }
            if (extraInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = extraInfo.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun UploadTab(viewModel: RegistraViewModel, state: RegistraUiState) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.selectFile(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = state.selectedFileName,
            onValueChange = {},
            label = { Text("Selected file") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            placeholder = { Text("Tap 'Select File' below") }
        )

        Button(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select File")
        }

        Button(
            onClick = { viewModel.upload() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedFile != null && !state.isUploading
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading...")
            } else {
                Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload")
            }
        }

        state.uploadError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadConfirmationDialog(
    file: FileResult,
    isDownloading: Boolean,
    onConfirm: () -> Unit,
    onPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeAction by remember { mutableStateOf<String?>(null) }
    
    BasicAlertDialog(
        onDismissRequest = onDismiss
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
                    text = "File Actions",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("What would you like to do with ${file.fileName}?")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        TextButton(
                            onClick = {
                                activeAction = "preview"
                                onPreview()
                            },
                            enabled = !isDownloading
                        ) {
                            if (isDownloading && activeAction == "preview") {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Preview")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                activeAction = "download"
                                onConfirm()
                            },
                            enabled = !isDownloading
                        ) {
                            if (isDownloading && activeAction == "download") {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Download")
                            }
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isDownloading
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverwriteConfirmationDialog(
    fileName: String,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = if (isProcessing) ({}) else onDismiss
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
                    text = "File with the same name already exists",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("File '$fileName' already exists on the server. Do you want to overwrite it?")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onConfirm,
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Overwrite")
                    }
                }
            }
        }
    }
}