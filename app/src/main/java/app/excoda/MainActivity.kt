package app.excoda

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        enableEdgeToEdge()

        setContent {
            val state = viewModel.state.collectAsState()
            ExcodaRoot(
                state = state.value,
                onOpenFile = { uri: Uri ->
                    takePersistableUriPermission(uri)
                    viewModel.onFileOpened(uri)
                },
                onShowSettings = {
                    val navState = state.value.navigationState
                    if (navState is NavigationState.ModuleActive) {
                        viewModel.showModuleSettingsDialog()
                    } else {
                        viewModel.showGlobalSettingsDialog()
                    }
                },
                onHideSettings = {
                    viewModel.hideModuleSettingsDialog()
                    viewModel.hideGlobalSettingsDialog()
                },
                onBackPress = {
                    viewModel.handleBackPress()
                },
                onTakePersistablePermission = { uri: Uri ->
                    takePersistableUriPermission(uri)
                }
            )
        }
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            takePersistableUriPermission(uri)
            viewModel.handleIntent(intent)
        }
    }

    private fun takePersistableUriPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
    }
}