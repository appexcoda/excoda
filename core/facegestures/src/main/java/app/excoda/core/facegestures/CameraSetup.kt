package app.excoda.core.facegestures

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import app.excoda.core.logging.LxLog

class CameraSetup(
    private val config: GestureConfig = GestureConfig()
) {
    fun configure(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        onCameraReady: (ProcessCameraProvider) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            config.cameraResolution,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
                LxLog.i("FaceGestures", "Camera started at ${config.cameraResolution.width}x${config.cameraResolution.height}")

                onCameraReady(cameraProvider)
            } catch (e: Exception) {
                LxLog.e("FaceGestures", "Failed to start camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    companion object {
        fun unbind(cameraProvider: ProcessCameraProvider?) {
            cameraProvider?.let {
                try {
                    it.unbindAll()
                    LxLog.i("FaceGestures", "Camera unbound successfully")
                } catch (e: Exception) {
                    LxLog.e("FaceGestures", "Error unbinding camera", e)
                }
            }
        }
    }
}