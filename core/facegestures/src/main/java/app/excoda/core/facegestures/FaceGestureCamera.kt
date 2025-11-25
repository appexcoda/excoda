package app.excoda.core.facegestures

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GestureMode
import app.excoda.core.settings.GlobalSettingsViewModel

@Composable
fun FaceGestureCamera(
    host: SimpleFaceGestureHost,
    config: GestureConfig,
    filter: SimpleGestureFilter,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val globalSettings: GlobalSettingsViewModel = hiltViewModel()
    val gestureMode by globalSettings.repository.gestureMode.collectAsState(initial = GestureMode.MOUTH_MOVEMENTS)

    var hasPermission by remember { mutableStateOf(checkCameraPermission(context)) }
    var recognizer by remember { mutableStateOf<MediaPipeFaceGestureRecognizer?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(gestureMode) {
        (recognizer?.gestureDetector as? BlendShapeDetector)?.let {
            it.gestureMode = gestureMode
            LxLog.i("FaceGestures", "Gesture mode changed to: ${gestureMode.displayName}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            LxLog.w("FaceGestures", "Camera permission denied")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            LxLog.d("FaceGestures", "FaceGestureCamera leaving composition - cleaning up")
            recognizer?.release()
            recognizer = null
            CameraSetup.unbind(cameraProvider)
            cameraProvider = null
            filter.reset()
            host.setFaceTracking(false)
        }
    }

    if (!hasPermission) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission required")
        }
        return
    }

    AndroidView(
        modifier = modifier.alpha(0.3f),
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                val gestureRecognizer = MediaPipeFaceGestureRecognizer(
                    ctx,
                    host,
                    config,
                    BlendShapeDetector(gestureMode),
                    filter
                )
                recognizer = gestureRecognizer
                CameraSetup(config).configure(
                    context = ctx,
                    lifecycleOwner = lifecycleOwner,
                    previewView = this,
                    analyzer = gestureRecognizer,
                    onCameraReady = { provider ->
                        cameraProvider = provider
                    }
                )
            }
        },
        onRelease = { previewView ->
            LxLog.d("FaceGestures", "AndroidView releasing PreviewView")
        }
    )
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}