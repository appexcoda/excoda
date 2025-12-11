package app.excoda.core.facegestures

import android.util.Size

data class GestureConfig(
    val cameraResolution: Size = Size(640, 480),
    val minFaceConfidence: Float = 0.6f,
    val minTrackingConfidence: Float = 0.6f,
    val detectionThreshold: Float = 0.2f,
    val smoothingAlpha: Float = 0.5f,
    val consecutiveFrames: Int = 3
)