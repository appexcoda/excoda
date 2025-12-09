package app.excoda

import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsRepository
import app.excoda.features.registra.RegistraConnectionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherSharedViewModel @Inject constructor(
    val faceGestureHost: FaceGestureHost,
    val faceGestureController: FaceGestureController,
    val fabMenuHost: FabMenuHost,
    val globalSettingsRepository: GlobalSettingsRepository,
    val registraConnectionChecker: RegistraConnectionChecker
) : ViewModel() {

    val snackbarHostState = SnackbarHostState()

    private val _fileSwitchRequests = MutableSharedFlow<Uri>()
    val fileSwitchRequests: SharedFlow<Uri> = _fileSwitchRequests

    private var currentSaveCallback: (suspend () -> Boolean)? = null

    fun showSnackbar(message: String, requireDismiss: Boolean = true) {
        LxLog.i("LauncherShared", "showSnackbar called: $message, requireDismiss=$requireDismiss")
        viewModelScope.launch {
            if (requireDismiss) {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Indefinite
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    fun requestFileSwitch(uri: Uri) {
        LxLog.i("LauncherShared", "requestFileSwitch called: $uri")
        viewModelScope.launch {
            val proceed = currentSaveCallback?.invoke() ?: true
            if (proceed) {
                _fileSwitchRequests.emit(uri)
            } else {
                LxLog.i("LauncherShared", "File switch cancelled by user")
            }
        }
    }

    fun registerSaveCallback(callback: suspend () -> Boolean) {
        LxLog.i("LauncherShared", "Save callback registered")
        currentSaveCallback = callback
    }

    suspend fun triggerSaveAndWait(): Boolean {
        LxLog.i("LauncherShared", "triggerSaveAndWait called")
        return currentSaveCallback?.invoke() ?: true
    }

    fun clearSaveCallback() {
        LxLog.i("LauncherShared", "Save callback cleared")
        currentSaveCallback = null
    }
}