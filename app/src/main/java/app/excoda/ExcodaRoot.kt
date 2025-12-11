package app.excoda

import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.excoda.core.fab.FabMenu
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.fab.FabMenuItem
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceTrackingIndicator
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.facegestures.GestureConsentDialog
import app.excoda.core.facegestures.SimpleFaceGestureHost
import app.excoda.core.launcher.api.FileModule
import app.excoda.core.logging.LxLog
import app.excoda.core.ui.ExcodaTheme
import app.excoda.ui.GlobalSettingsDialog
import app.excoda.ui.MainScreen
import kotlinx.coroutines.launch

@Composable
private fun ExcodaRootContent(
    shared: LauncherSharedViewModel,
    module: FileModule?,
    uri: String?,
    isModuleActive: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    faceGestureHost: FaceGestureHost,
    faceGestureController: FaceGestureController,
    fabMenuHost: FabMenuHost,
    simpleFaceGestureHost: SimpleFaceGestureHost?,
    isCenterZoneHidden: Boolean,
    fabMenuBottomPadding: androidx.compose.ui.unit.Dp,
    onOpenFile: (Uri) -> Unit,
    onTakePersistablePermission: (Uri) -> Unit
) {
    Box {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = shared.snackbarHostState) },
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FaceTrackingIndicator(
                        modifier = Modifier.padding(bottom = fabMenuBottomPadding),
                        isTracking = simpleFaceGestureHost?.isFaceTracking?.collectAsStateWithLifecycle()?.value
                            ?: false,
                        isVisible = faceGestureController.isEnabled.collectAsStateWithLifecycle().value,
                        isCenterZoneHidden = isCenterZoneHidden
                    )
                    FabMenu(
                        host = fabMenuHost,
                        isGesturesEnabled = faceGestureController.isEnabled.collectAsStateWithLifecycle().value,
                        isCenterZoneHidden = isCenterZoneHidden,
                        bottomPadding = fabMenuBottomPadding
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
                        }
                    )
                } else {
                    val launcherViewModel: app.excoda.viewmodel.MainViewModel = hiltViewModel()
                    MainScreen(
                        viewModel = launcherViewModel,
                        onFileClick = { fileUri ->
                            LxLog.i("ExcodaRoot", "File clicked from tab: $fileUri")
                            scope.launch {
                                val parsedUri = fileUri.toUri()

                                if (isModuleActive) {
                                    LxLog.i("ExcodaRoot", "Module active, triggering auto-save")
                                    shared.triggerSaveAndWait()
                                } else {
                                    LxLog.i("ExcodaRoot", "No module active, skipping auto-save")
                                }
                                onOpenFile(parsedUri)
                            }
                        },
                        onTakePersistablePermission = onTakePersistablePermission
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() })
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun ExcodaRoot(
    state: LauncherState,
    onOpenFile: (Uri) -> Unit,
    onShowSettings: () -> Unit,
    onHideSettings: () -> Unit,
    onBackPress: () -> Boolean,
    onTakePersistablePermission: (Uri) -> Unit = {}
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

    LaunchedEffect(viewModel, shared) {
        viewModel.snackbarMessage.collect { message ->
            shared.showSnackbar(message, requireDismiss = true)
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
                openDocumentLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/octet-stream",
                        "text/xml",
                        "application/xml",
                        "application/vnd.recordare.musicxml+xml",
                        "application/vnd.recordare.musicxml",
                        "application/gpx+xml",
                        "application/vnd.recordare.musicxml.mscz"
                    )
                )
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
            onClick = { showRegistraDialog = true }
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
                if (!gestureConsentGiven) {
                    showGestureConsent = true
                } else {
                    faceGestureController.toggle()
                }
            }
        )
    }

    DisposableEffect(fabMenuHost, settingsItem, registraItem, module) {
        val isAlphaTabModule = module?.descriptor?.displayName == "AlphaTab"
        val isPdfJsModule = module?.descriptor?.displayName == "PDF.js"
        val shouldShowRegistra = !isAlphaTabModule && !isPdfJsModule

        if (shouldShowRegistra) {
            fabMenuHost.register(registraItem)
        }

        if (!isPdfJsModule) {
            fabMenuHost.register(settingsItem)
        }

        onDispose {
            if (shouldShowRegistra) {
                fabMenuHost.unregister(registraItem.id)
            }
            if (!isPdfJsModule) {
                fabMenuHost.unregister(settingsItem.id)
            }
        }
    }

    LaunchedEffect(isModuleActive) {
        if (!isModuleActive) {
            fabMenuHost.register(openFileItem)
        } else {
            fabMenuHost.unregister(openFileItem.id)
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

    BoxWithConstraints {
        val screenHeight = this.maxHeight
        val fabMenuBottomPadding = screenHeight * 0.03f

        ExcodaTheme(darkTheme = false) {
            ExcodaRootContent(
                shared = shared,
                module = module,
                uri = uri,
                isModuleActive = isModuleActive,
                scope = scope,
                faceGestureHost = faceGestureHost,
                faceGestureController = faceGestureController,
                fabMenuHost = fabMenuHost,
                simpleFaceGestureHost = simpleFaceGestureHost,
                isCenterZoneHidden = isCenterZoneHidden,
                fabMenuBottomPadding = fabMenuBottomPadding,
                onOpenFile = onOpenFile,
                onTakePersistablePermission = onTakePersistablePermission
            )
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
            registraConnectionChecker = shared.registraConnectionChecker,
            onDismiss = onHideSettings
        )
    }

    if (showRegistraDialog) {
        app.excoda.features.registra.RegistraDialog(
            onDismiss = { showRegistraDialog = false },
            onPreview = { previewUri -> onOpenFile(previewUri) }
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
            onDecline = { showGestureConsent = false }
        )
    }
}