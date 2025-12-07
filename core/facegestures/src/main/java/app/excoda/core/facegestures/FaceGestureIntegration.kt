package app.excoda.core.facegestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FaceGestureModuleDependencies {
    fun gestureConfig(): GestureConfig
    fun gestureFilter(): SimpleGestureFilter
}

@Composable
fun WithFaceGestures(
    faceGestureHost: FaceGestureHost,
    faceGestureController: FaceGestureController,
    onGesture: (FaceGestureType) -> Unit,
    isCenterZoneHidden: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dependencies = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FaceGestureModuleDependencies::class.java
        )
    }

    val gestureConfig = remember { dependencies.gestureConfig() }
    val gestureFilter = remember { dependencies.gestureFilter() }

    val globalSettings: GlobalSettingsViewModel = hiltViewModel()
    val showCameraPreview by globalSettings.repository.showCameraPreview.collectAsStateWithLifecycle(initialValue = true)

    val isGestureEnabled by faceGestureController.isEnabled.collectAsStateWithLifecycle()

    val cameraAlpha = if (isCenterZoneHidden || !showCameraPreview) 0f else 0.3f

    DisposableEffect(faceGestureHost, onGesture) {
        val host = faceGestureHost as SimpleFaceGestureHost
        host.setGestureHandler(onGesture)
        onDispose {
            LxLog.d("FaceGestures", "WithFaceGestures leaving composition")
            host.clearGestureHandler()
            host.setFaceTracking(false)
            gestureFilter.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (isGestureEnabled) {
            FaceGestureCamera(
                host = faceGestureHost as SimpleFaceGestureHost,
                config = gestureConfig,
                filter = gestureFilter,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(200.dp, 267.dp)
                    .padding(16.dp)
                    .alpha(cameraAlpha)
            )
        }
    }
}