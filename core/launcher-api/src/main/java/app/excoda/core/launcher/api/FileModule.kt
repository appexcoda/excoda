package app.excoda.core.launcher.api

import androidx.compose.runtime.Composable
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.settings.SettingsContributor

interface FileModuleDescriptor {
    val supportedExtensions: Set<String>
    val displayName: String
    val supportsFaceGestures: Boolean get() = false
}

interface FileModule {
    val descriptor: FileModuleDescriptor

    @Composable
    fun Content(
        fileUri: String,
        faceGestureHost: FaceGestureHost,
        faceGestureController: FaceGestureController,
        fabMenuHost: FabMenuHost,
        onShowSnackbar: (String, Boolean) -> Unit = { _, _ -> },
        onSwitchFile: (android.net.Uri) -> Unit = {},
        onRegisterSaveCallback: (suspend () -> Boolean) -> Unit = {}
    )

    fun settingsContributors(): List<SettingsContributor> = emptyList()
}

object FileModuleRegistry {
    private val modules = mutableListOf<FileModule>()

    fun register(module: FileModule) {
        if (modules.none { it.descriptor.displayName == module.descriptor.displayName }) {
            modules += module
        }
    }

    fun all(): List<FileModule> = modules.toList()

    fun findByExtension(extension: String): FileModule? {
        return modules.firstOrNull { extension.lowercase() in it.descriptor.supportedExtensions }
    }
}