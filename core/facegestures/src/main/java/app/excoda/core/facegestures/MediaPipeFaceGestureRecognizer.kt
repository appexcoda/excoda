package app.excoda.core.facegestures

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.excoda.core.logging.LxLog
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.atomic.AtomicBoolean

class MediaPipeFaceGestureRecognizer(
    context: Context,
    private val host: SimpleFaceGestureHost,
    private val config: GestureConfig,
    private val detector: GestureDetector,
    private val filter: SimpleGestureFilter
) : ImageAnalysis.Analyzer {

    private val analyzer: LandmarkAnalyzer
    private var hadFaceLastFrame = false
    private val isReleased = AtomicBoolean(false)

    val gestureDetector: GestureDetector get() = detector

    init {
        analyzer = MediaPipeLandmarkAnalyzer(
            context = context,
            config = config,
            onResult = ::handleFaceResult,
            onError = ::handleError
        )
        LxLog.i("FaceGestures", "Recognizer initialized")
    }

    override fun analyze(imageProxy: ImageProxy) {
        // Check if released before processing
        if (isReleased.get()) {
            imageProxy.close()
            return
        }

        if (!host.isEnabled.value) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()

            // DON'T close imageProxy yet - MediaPipe needs it
            // Pass it to analyzer so it can close it after processing
            analyzer.analyzeAsync(mpImage, imageProxy.imageInfo.timestamp / 1_000_000)

            // Close immediately since we copied the bitmap
            // The bitmap data is now owned by mpImage
            imageProxy.close()
        } catch (e: Exception) {
            LxLog.e("FaceGestures", "Error in analyze", e)
            imageProxy.close()
        }
    }

    private fun handleFaceResult(face: FaceResult?) {
        // Don't process results if released
        if (isReleased.get()) return

        if (face == null) {
            if (hadFaceLastFrame) {
                filter.reset()
                host.setFaceTracking(false)
                LxLog.d("FaceGestures", "Face lost")
                hadFaceLastFrame = false
            }
            return
        }

        if (!hadFaceLastFrame) {
            host.setFaceTracking(true)
            LxLog.d("FaceGestures", "Face tracking")
            hadFaceLastFrame = true
        }

        val detections = detector.detect(face)

        detections.forEach { (type, rawScore) ->
            val (shouldTrigger, filtered) = filter.shouldTrigger(type, rawScore)

            if (shouldTrigger) {
                host.onGestureDetected(type, filtered)
            }
        }
    }

    private fun handleError(error: RuntimeException) {
        if (!isReleased.get()) {
            LxLog.e("FaceGestures", "MediaPipe error: ${error.message}", error)
        }
    }

    fun release() {
        if (isReleased.getAndSet(true)) {
            LxLog.w("FaceGestures", "Recognizer already released")
            return
        }

        LxLog.i("FaceGestures", "Releasing recognizer")
        try {
            analyzer.close()
            hadFaceLastFrame = false
            LxLog.i("FaceGestures", "Recognizer released successfully")
        } catch (e: Exception) {
            LxLog.e("FaceGestures", "Error releasing recognizer", e)
        }
    }
}