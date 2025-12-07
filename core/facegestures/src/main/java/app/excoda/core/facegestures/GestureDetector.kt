package app.excoda.core.facegestures

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class FaceResult(
    val landmarks: List<NormalizedLandmark>,
    val blendshapes: List<com.google.mediapipe.tasks.components.containers.Category>?
)

interface GestureDetector {
    fun detect(face: FaceResult): Map<FaceGestureType, Float>
}