package app.excoda.core.facegestures

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

interface LandmarkAnalyzer {
    fun analyzeAsync(image: MPImage, timestampMillis: Long)
    fun close()
}

class MediaPipeLandmarkAnalyzer(
    context: Context,
    config: GestureConfig,
    onResult: (FaceResult?) -> Unit,
    onError: (RuntimeException) -> Unit
) : LandmarkAnalyzer {

    private val landmarker: FaceLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(config.minFaceConfidence)
            .setMinTrackingConfidence(config.minTrackingConfidence)
            .setOutputFaceBlendshapes(true)
            .setResultListener { result: FaceLandmarkerResult, _: MPImage ->
                val landmarksList = result.faceLandmarks()

                val face = if (landmarksList.isNotEmpty()) {
                    val blendshapesOptional = result.faceBlendshapes()
                    val blendshapes = if (blendshapesOptional.isPresent) {
                        val blendshapesList = blendshapesOptional.get()
                        if (blendshapesList.isNotEmpty()) blendshapesList[0] else null
                    } else {
                        null
                    }

                    FaceResult(
                        landmarks = landmarksList[0],
                        blendshapes = blendshapes
                    )
                } else {
                    null
                }
                onResult(face)
            }
            .setErrorListener(onError)
            .build()

        landmarker = FaceLandmarker.createFromOptions(context, options)
    }

    override fun analyzeAsync(image: MPImage, timestampMillis: Long) {
        landmarker.detectAsync(image, timestampMillis)
    }

    override fun close() {
        landmarker.close()
    }
}