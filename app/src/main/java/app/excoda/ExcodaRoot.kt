package app.excoda

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.excoda.core.fab.FabMenu
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.fab.FabMenuItem
import app.excoda.core.fab.FaceTrackingIndicator
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.facegestures.SimpleFaceGestureHost
import app.excoda.core.launcher.api.FileModule
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsDialog
import app.excoda.core.facegestures.GestureConsentDialog
import app.excoda.core.ui.ExcodaTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext

@Composable
fun ExcodaRoot(
    state: LauncherState,
    onOpenFile: (Uri) -> Unit,
    onAddToRecents: (Uri, String) -> Unit,
    onOpenDemo: () -> Unit,
    onShowSettings: () -> Unit,
    onHideSettings: () -> Unit,
    onBackPress: () -> Boolean
) {
    val shared: LauncherSharedViewModel = hiltViewModel()
    val faceGestureHost: FaceGestureHost = shared.faceGestureHost
    val simpleFaceGestureHost = faceGestureHost as? SimpleFaceGestureHost
    val faceGestureController: FaceGestureController = shared.faceGestureController
    val fabMenuHost: FabMenuHost = shared.fabMenuHost

    val scope = rememberCoroutineScope()
    var showGestureConsent by remember { mutableStateOf(false) }
    val gestureConsentGiven by shared.globalSettingsRepository.gestureConsentGiven.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        shared.fileSwitchRequests.collect { uri ->
            LxLog.i("ExcodaRoot", "File switch requested: $uri")
            if (uri == Uri.EMPTY) {
                LxLog.i("ExcodaRoot", "Empty URI received, closing module")
                scope.launch {
                    shared.triggerSaveAndWait()
                    onBackPress()
                }
            } else {
                onOpenFile(uri)
            }
        }
    }

    val isCenterZoneHidden by fabMenuHost.isCenterZoneHidden.collectAsStateWithLifecycle()

    val moduleInfo = when (val navState = state.navigationState) {
        is NavigationState.ModuleActive -> Pair(navState.module, navState.uri)
        is NavigationState.LauncherEmpty -> null
    }

    val module: FileModule? = moduleInfo?.first
    val uri: String? = moduleInfo?.second
    val isModuleActive = state.navigationState is NavigationState.ModuleActive

    BackHandler(enabled = isModuleActive) {
        LxLog.i("ExcodaRoot", "BackHandler triggered, calling triggerSaveAndWait")
        scope.launch {
            shared.triggerSaveAndWait()
            onBackPress()
        }
    }

    val viewModel: LauncherViewModel = hiltViewModel()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            shared.showSnackbar(message, requireDismiss = true)
            viewModel.clearError()
        }
    }

    val context = LocalContext.current

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selected ->
        selected?.let {
            val mimeType = context.contentResolver.getType(it)
            LxLog.i("ExcodaRoot", "File selected from FAB, MIME type: $mimeType")
            scope.launch {
                if (isModuleActive) {
                    LxLog.i("ExcodaRoot", "Module active, triggering auto-save")
                    shared.triggerSaveAndWait()
                } else {
                    LxLog.i("ExcodaRoot", "No module active, skipping auto-save")
                }
                onOpenFile(it)
            }
        }
    }

    val openFileItem = remember(openDocumentLauncher) {
        FabMenuItem(
            id = "global-open-file",
            label = "Open File",
            icon = Icons.Rounded.FolderOpen,
            order = 0,
            onClick = {
                // openDocumentLauncher.launch(arrayOf("*/*"))
                openDocumentLauncher.launch(arrayOf(
                    "application/pdf",
                    "application/octet-stream",
                    "text/xml",
                    "application/xml",
                    "application/vnd.recordare.musicxml+xml",
                    "application/vnd.recordare.musicxml",
                    "application/gpx+xml"
                ))
            }
        )
    }

    val settingsItem = remember(onShowSettings) {
        FabMenuItem(
            id = "global-settings",
            label = "Settings",
            icon = Icons.Rounded.Settings,
            order = 2,
            onClick = {
                LxLog.d("Launcher", "Settings action tapped")
                onShowSettings()
            }
        )
    }

    var showRegistraDialog by remember { mutableStateOf(false) }
    val registraItem = remember {
        FabMenuItem(
            id = "global-registra",
            label = "Registra",
            icon = Icons.Rounded.Storage,
            order = 3,
            onClick = {
                showRegistraDialog = true
            }
        )
    }

    val faceGestureItem = remember(faceGestureController, gestureConsentGiven) {
        FabMenuItem(
            id = "face-gestures",
            label = "Gestures",
            icon = Icons.Rounded.Camera,
            order = 1,
            isToggle = true,
            isActive = faceGestureController.isEnabled,
            onClick = {
                if (!gestureConsentGiven) {  // MODIFIED
                    showGestureConsent = true
                } else {
                    faceGestureController.toggle()
                }
            }
        )
    }

    DisposableEffect(fabMenuHost, openFileItem, settingsItem, registraItem) {
        fabMenuHost.register(openFileItem)
        fabMenuHost.register(settingsItem)
        fabMenuHost.register(registraItem)
        onDispose {
            fabMenuHost.unregister(openFileItem.id)
            fabMenuHost.unregister(settingsItem.id)
            fabMenuHost.unregister(registraItem.id)
        }
    }

    LaunchedEffect(module?.descriptor, uri) {
        val supportsFaceGestures = module?.descriptor?.supportsFaceGestures == true

        if (faceGestureController.isEnabled.value) {
            LxLog.i("Launcher", "Disabling gestures due to file/module change")
            faceGestureController.disable()
        }

        faceGestureController.configure(supportsFaceGestures)
        fabMenuHost.unregister(faceGestureItem.id)
        if (supportsFaceGestures && uri != null) {
            fabMenuHost.register(faceGestureItem)
        }

        fabMenuHost.setCenterZoneHidden(false)
    }

    ExcodaTheme(darkTheme = false) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = shared.snackbarHostState)
            },
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FaceTrackingIndicator(
                        isTracking = simpleFaceGestureHost?.isFaceTracking?.collectAsStateWithLifecycle()?.value ?: false,
                        isVisible = faceGestureController.isEnabled.collectAsStateWithLifecycle().value,
                        isCenterZoneHidden = isCenterZoneHidden,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    FabMenu(
                        host = fabMenuHost,
                        isGesturesEnabled = faceGestureController.isEnabled.collectAsStateWithLifecycle().value,
                        isCenterZoneHidden = isCenterZoneHidden
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (module != null && uri != null) {
                    LaunchedEffect(module, uri) {
                        shared.clearSaveCallback()
                    }
                    module.Content(
                        fileUri = uri,
                        faceGestureHost = faceGestureHost,
                        faceGestureController = faceGestureController,
                        fabMenuHost = fabMenuHost,
                        onShowSnackbar = { message, requireDismiss ->
                            LxLog.i("ExcodaRoot", "onShowSnackbar called: $message, requireDismiss=$requireDismiss")
                            shared.showSnackbar(message, requireDismiss)
                        },
                        onSwitchFile = { fileUri ->
                            LxLog.i("ExcodaRoot", "onSwitchFile called: $fileUri")
                            shared.requestFileSwitch(fileUri)
                        },
                        onRegisterSaveCallback = { callback ->
                            LxLog.i("ExcodaRoot", "Save callback registered")
                            shared.registerSaveCallback(callback)
                        },
                        onAddToRecents = { fileUri, displayName ->
                            LxLog.i("ExcodaRoot", "Adding to recents: $displayName")
                            onAddToRecents(fileUri, displayName)
                        }
                    )
                } else {
                    LauncherPlaceholder(
                        padding = paddingValues,
                        recentFiles = state.recentFiles,
                        onOpenDemo = onOpenDemo,
                        onRecentFileClick = { recentUri ->
                            LxLog.i("ExcodaRoot", "Recent file clicked")
                            scope.launch {
                                if (isModuleActive) {
                                    LxLog.i("ExcodaRoot", "Module active, triggering auto-save")
                                    shared.triggerSaveAndWait()
                                } else {
                                    LxLog.i("ExcodaRoot", "No module active, skipping auto-save")
                                }
                                onOpenFile(Uri.parse(recentUri))
                            }
                        }
                    )
                }
            }
        }
    }

    if (state.showModuleSettingsDialog && module != null && uri != null) {
        val contributors = module.settingsContributors()
        if (contributors.isNotEmpty()) {
            contributors.first().SettingsEntry(
                fileUri = uri,
                onClose = onHideSettings
            )
        }
    }

    if (state.showGlobalSettingsDialog) {
        GlobalSettingsDialog(
            repository = shared.globalSettingsRepository,
            connectionChecker = shared.connectionChecker,
            onDismiss = onHideSettings
        )
    }

    if (showRegistraDialog) {
        app.excoda.features.registra.RegistraDialog(
            onDismiss = { showRegistraDialog = false }
        )
    }

    if (showGestureConsent) {
        GestureConsentDialog(
            onAccept = {
                scope.launch {
                    shared.globalSettingsRepository.setGestureConsentGiven(true)
                    faceGestureController.enable()
                    showGestureConsent = false
                }
            },
            onDecline = {
                showGestureConsent = false
            }
        )
    }
}