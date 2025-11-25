package app.excoda.core.facegestures

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class FaceResult(
    val landmarks: List<NormalizedLandmark>,
    val blendshapes: List<com.google.mediapipe.tasks.components.containers.Category>?
)

interface GestureDetector {
    fun detect(face: FaceResult): Map<FaceGestureType, Float>
}

class CompositeGestureDetector(
    private val detectors: List<GestureDetector>
) : GestureDetector {
    override fun detect(face: FaceResult): Map<FaceGestureType, Float> {
        return detectors
            .flatMap { it.detect(face).entries }
            .associate { it.key to it.value }
    }
}