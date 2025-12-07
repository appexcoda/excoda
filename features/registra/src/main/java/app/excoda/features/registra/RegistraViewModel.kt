package app.excoda.features.registra

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import javax.inject.Inject

data class OverwriteConfirmation(
    val fileId: Long,
    val fileName: String,
    val originalFile: File
)

data class RegistraUiState(
    val selectedTab: RegistraTab = RegistraTab.SEARCH,
    val searchText: String = "",
    val searchArtist: String = "",
    val searchTitle: String = "",
    val searchFileType: String = "",
    val searchResults: List<FileResult> = emptyList(),
    val totalResults: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedFile: File? = null,
    val selectedFileName: String = "",
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val overwriteConfirmation: OverwriteConfirmation? = null,
    val downloadConfirmation: FileResult? = null,
    val isDownloading: Boolean = false,
    val snackbarMessage: String? = null,
    val previewUri: Uri? = null
)

enum class RegistraTab {
    SEARCH, UPLOAD
}

@HiltViewModel
class RegistraViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: RegistraRepository,
    globalSettings: GlobalSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegistraUiState())
    val uiState: StateFlow<RegistraUiState> = _uiState.asStateFlow()

    private val registraHost = globalSettings.registraHost.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private val registraApiKey = globalSettings.registraApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private fun getServerName(host: String): String {
        return try {
            val url = URL(host)
            url.host
        } catch (e: Exception) {
            LxLog.d("Registra", "Failed to extract server name from host: $host", e)
            "Registra server"
        }
    }

    private suspend fun checkServerHealth(): Boolean {
        val host = registraHost.value
        val apiKey = registraApiKey.value

        if (host.isBlank() || apiKey.isBlank()) {
            _uiState.update {
                it.copy(snackbarMessage = "Registra not configured. Check settings.")
            }
            return false
        }

        when (val result = repository.checkHealth(host, apiKey)) {
            is RegistraResult.Success -> return true
            is RegistraResult.Error -> {
                val serverName = getServerName(host)
                _uiState.update {
                    it.copy(snackbarMessage = "Server $serverName is unavailable: ${result.message}")
                }
                return false
            }

            is RegistraResult.Conflict -> return false
        }
    }

    fun setTab(tab: RegistraTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun setSearchArtist(artist: String) {
        _uiState.update { it.copy(searchArtist = artist) }
    }

    fun setSearchTitle(title: String) {
        _uiState.update { it.copy(searchTitle = title) }
    }

    fun setSearchFileType(fileType: String) {
        _uiState.update { it.copy(searchFileType = fileType) }
    }

    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            if (!checkServerHealth()) {
                _uiState.update { it.copy(isSearching = false) }
                return@launch
            }

            searchPage(1)
        }
    }

    fun searchPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val host = registraHost.value
            val apiKey = registraApiKey.value

            LxLog.d("Registra", "Search: host='$host', apiKey='${apiKey.take(8)}...', page=$page")

            val state = _uiState.value
            val result = repository.search(
                host = host,
                apiKey = apiKey,
                text = state.searchText.takeIf { it.isNotBlank() },
                artist = state.searchArtist.takeIf { it.isNotBlank() },
                title = state.searchTitle.takeIf { it.isNotBlank() },
                fileType = state.searchFileType.takeIf { it.isNotBlank() },
                page = page
            )

            when (result) {
                is RegistraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            searchResults = result.data.results ?: emptyList(),
                            totalResults = result.data.total,
                            currentPage = result.data.page,
                            totalPages = result.data.totalPages,
                            isSearching = false,
                            searchError = null
                        )
                    }
                }

                is RegistraResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchError = result.message
                        )
                    }
                }

                is RegistraResult.Conflict -> {
                    _uiState.update { it.copy(isSearching = false) }
                }
            }
        }
    }

    fun showDownloadConfirmation(file: FileResult) {
        _uiState.update { it.copy(downloadConfirmation = file) }
    }

    fun dismissDownloadConfirmation() {
        _uiState.update { it.copy(downloadConfirmation = null) }
    }

    fun downloadFile() {
        val file = _uiState.value.downloadConfirmation ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }

            val result = repository.download(
                host = registraHost.value,
                apiKey = registraApiKey.value,
                fileId = file.id.toString(),
                fileName = file.fileName
            )

            when (result) {
                is RegistraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            downloadConfirmation = null,
                            isDownloading = false,
                            snackbarMessage = "Downloaded ${result.data}"
                        )
                    }
                }

                is RegistraResult.Error -> {
                    _uiState.update {
                        it.copy(
                            downloadConfirmation = null,
                            isDownloading = false,
                            snackbarMessage = "Download failed: ${result.message}"
                        )
                    }
                }

                is RegistraResult.Conflict -> {
                    _uiState.update { it.copy(isDownloading = false) }
                }
            }
        }
    }

    fun selectFile(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open file")

                val fileName = queryDisplayName(uri) ?: "upload_${System.currentTimeMillis()}"

                val tempFile = File(context.cacheDir, fileName)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                _uiState.update {
                    it.copy(
                        selectedFile = tempFile,
                        selectedFileName = fileName,
                        uploadError = null
                    )
                }
            } catch (e: Exception) {
                LxLog.e("Registra", "Failed to read file", e)
                _uiState.update {
                    it.copy(uploadError = "Failed to read file: ${e.message}")
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.toString().isBlank()) {
            LxLog.w("Registra", "Cannot query display name for blank URI")
            return null
        }
        
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor: android.database.Cursor? = try {
            context.contentResolver.query(uri, projection, null, null, null)
        } catch (e: Exception) {
            LxLog.w("Registra", "Exception during contentResolver.query for $uri.", e)
            null
        }

        if (cursor == null) {
            LxLog.w("Registra", "ContentResolver returned a null cursor for $uri.")
            return null
        }

        var result: String? = null
        try {
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                } else {
                    LxLog.w("Registra", "Display name column not found for $uri.")
                }
            }
        } catch (e: Exception) {
            LxLog.e("Registra", "Exception while reading from cursor for $uri.", e)
        } finally {
            cursor.close()
        }
        return result
    }

    fun upload() {
        val file = _uiState.value.selectedFile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadError = null) }

            if (!checkServerHealth()) {
                _uiState.update { it.copy(isUploading = false) }
                return@launch
            }

            performUpload(file)
        }
    }

    private suspend fun performUpload(file: File) {
        val result = repository.upload(
            host = registraHost.value,
            apiKey = registraApiKey.value,
            file = file
        )

        when (result) {
            is RegistraResult.Success -> {
                _uiState.update {
                    it.copy(
                        selectedFile = null,
                        selectedFileName = "",
                        isUploading = false,
                        snackbarMessage = "Uploaded successfully"
                    )
                }
                file.delete()
            }

            is RegistraResult.Conflict -> {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        overwriteConfirmation = OverwriteConfirmation(
                            fileId = result.existingFileId,
                            fileName = result.existingFileName,
                            originalFile = file
                        )
                    )
                }
            }

            is RegistraResult.Error -> {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadError = result.message
                    )
                }
            }
        }
    }

    fun confirmOverwrite() {
        val confirmation = _uiState.value.overwriteConfirmation ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, overwriteConfirmation = null) }

            val deleteResult = repository.delete(
                host = registraHost.value,
                apiKey = registraApiKey.value,
                fileId = confirmation.fileId
            )

            when (deleteResult) {
                is RegistraResult.Success -> {
                    performUpload(confirmation.originalFile)
                }

                is RegistraResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadError = "Failed to delete existing file: ${deleteResult.message}"
                        )
                    }
                    confirmation.originalFile.delete()
                }

                is RegistraResult.Conflict -> {
                    _uiState.update { it.copy(isUploading = false) }
                }
            }
        }
    }

    fun dismissOverwriteConfirmation() {
        _uiState.value.overwriteConfirmation?.originalFile?.delete()
        _uiState.update {
            it.copy(
                overwriteConfirmation = null,
                selectedFile = null,
                selectedFileName = ""
            )
        }
    }
    fun previewFile() {
        val file = _uiState.value.downloadConfirmation ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }

            val result = repository.preview(
                host = registraHost.value,
                apiKey = registraApiKey.value,
                fileId = file.id.toString()
            )

            when (result) {
                is RegistraResult.Success -> {
                    try {
                        val originalExtension = file.fileName.substringAfterLast('.', "")
                        val extension = if (originalExtension.lowercase() == "mscz") "musicxml" else originalExtension
                        val tempFile = File(context.cacheDir, "preview_${System.currentTimeMillis()}.$extension")
                        tempFile.writeBytes(result.data)

                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )

                        _uiState.update {
                            it.copy(
                                downloadConfirmation = null,
                                isDownloading = false,
                                previewUri = uri
                            )
                        }
                    } catch (e: Exception) {
                        LxLog.e("Registra", "Failed to create preview file", e)
                        _uiState.update {
                            it.copy(
                                downloadConfirmation = null,
                                isDownloading = false,
                                snackbarMessage = "Preview failed: ${e.message}"
                            )
                        }
                    }
                }

                is RegistraResult.Error -> {
                    _uiState.update {
                        it.copy(
                            downloadConfirmation = null,
                            isDownloading = false,
                            snackbarMessage = "Preview failed: ${result.message}"
                        )
                    }
                }

                is RegistraResult.Conflict -> {
                    _uiState.update { it.copy(isDownloading = false) }
                }
            }
        }
    }

    fun clearPreviewUri() {
        _uiState.update { it.copy(previewUri = null) }
    }


    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}