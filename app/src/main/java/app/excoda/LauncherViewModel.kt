package app.excoda

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.excoda.core.launcher.api.FileModule
import app.excoda.core.launcher.api.FileModuleRegistry
import app.excoda.core.logging.LxLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val recentFilesRepository: RecentFilesRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = mutableState

    init {
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            recentFilesRepository.recentFiles.collect { recentFiles ->
                mutableState.update { it.copy(recentFiles = recentFiles) }
            }
        }
    }

    fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        processUri(uri)
    }

    fun onFileOpened(uri: Uri) {
        processUri(uri)
    }

    fun onRecentFileClicked(uri: String) {
        LxLog.d("Launcher", "Recent file clicked: $uri")
        processUri(Uri.parse(uri))
    }

    fun handleBackPress(): Boolean {
        val currentState = mutableState.value.navigationState
        return when (currentState) {
            is NavigationState.ModuleActive -> {
                LxLog.d("Navigation", "Going back to launcher, destroying module")
                mutableState.update {
                    it.copy(navigationState = NavigationState.LauncherEmpty)
                }
                true
            }

            is NavigationState.LauncherEmpty -> {
                false
            }
        }
    }

    fun showModuleSettingsDialog() {
        mutableState.update { it.copy(showModuleSettingsDialog = true) }
    }

    fun hideModuleSettingsDialog() {
        mutableState.update { it.copy(showModuleSettingsDialog = false) }
    }

    fun showGlobalSettingsDialog() {
        mutableState.update { it.copy(showGlobalSettingsDialog = true) }
    }

    fun hideGlobalSettingsDialog() {
        mutableState.update { it.copy(showGlobalSettingsDialog = false) }
    }

    fun clearError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    fun addToRecents(uri: Uri, displayName: String) {
        viewModelScope.launch {
            recentFilesRepository.addRecentFile(uri.toString(), displayName)
        }
    }

    fun openDemoFile() {
        viewModelScope.launch {
            try {
                val demoFile = withContext(Dispatchers.IO) {
                    val cacheFile = java.io.File(appContext.cacheDir, "demo.gp")
                    appContext.assets.open("demo.gp").use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheFile
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    demoFile
                )
                processUri(uri)
            } catch (e: Exception) {
                LxLog.e("Launcher", "Failed to open demo file", e)
            }
        }
    }

    private fun processUri(uri: Uri) {
        viewModelScope.launch {
            val extension = resolveExtension(uri)
            if (extension == null) {
                LxLog.w("Launcher", "Unable to resolve extension for $uri")
                return@launch
            }

            val module: FileModule? = FileModuleRegistry.findByExtension(extension)
            if (module == null) {
                LxLog.w("Launcher", "No module registered for extension .$extension ($uri)")
                mutableState.update {
                    it.copy(errorMessage = "Unsupported file format: .$extension")
                }
                return@launch
            }

            LxLog.i("Launcher", "Resolved ${module.descriptor.displayName} for $uri")

            // Navigate immediately
            mutableState.update {
                it.copy(
                    navigationState = NavigationState.ModuleActive(
                        module = module,
                        uri = uri.toString()
                    )
                )
            }

            // Save to recent files in background (don't block navigation)
            launch {
                val displayName = resolveDisplayName(uri)
                recentFilesRepository.addRecentFile(uri.toString(), displayName)
            }
        }
    }

    private suspend fun resolveExtension(uri: Uri): String? = withContext(Dispatchers.IO) {
        DocumentFile.fromSingleUri(appContext, uri)
            ?.name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase(Locale.ROOT)
            ?: queryDisplayName(uri)
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.takeIf { it.isNotBlank() }
                ?.lowercase(Locale.ROOT)
            ?: appContext.contentResolver.getType(uri)
                ?.let { mime ->
                    MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mime)
                        ?.lowercase(Locale.ROOT)
                }
    }

    private suspend fun resolveDisplayName(uri: Uri): String = withContext(Dispatchers.IO) {
        DocumentFile.fromSingleUri(appContext, uri)?.name
            ?: queryDisplayName(uri)
            ?: uri.lastPathSegment
            ?: "Unknown"
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}