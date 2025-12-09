package app.excoda.core.facegestures

import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GestureMode
import java.util.Locale
import kotlin.math.abs

class BlendShapeDetector(
    var gestureMode: GestureMode = GestureMode.MOUTH_MOVEMENTS
) : GestureDetector {

    private var frameCount = 0
    private val logPeriod = 30

    override fun detect(face: FaceResult): Map<FaceGestureType, Float> {
        val blendshapes = face.blendshapes ?: return emptyMap()
        val rawScores = blendshapes.associate { it.categoryName() to it.score() }

        frameCount++
        if (frameCount % logPeriod == 0) {
            val active = rawScores.filter { it.value > 0.1f }
            if (active.isNotEmpty()) {
                LxLog.d("BlendShapes", "Active: ${active.map { "${it.key}=${String.format(Locale.US, "%.2f", it.value)}" }.joinToString(", ")}")
            }
        }

        return when (gestureMode) {
            GestureMode.MOUTH_MOVEMENTS -> detectMouthMovements(rawScores)
            GestureMode.BROW_SMILE -> detectBrowSmile(rawScores)
        }
    }

    private fun detectMouthMovements(scores: Map<String, Float>): Map<FaceGestureType, Float> {
        return buildMap {
            val mouthRight = scores["mouthRight"] ?: 0f
            val dimpleRight = scores["mouthDimpleRight"] ?: 0f
            val rightScore = maxOf(mouthRight, dimpleRight)
            if (rightScore > 0.15f) {
                val source = if (dimpleRight >= mouthRight) "dimple" else "mouth"
                LxLog.d("BlendShapes", "MouthStretchRight via $source: mouth=${String.format(Locale.US, "%.2f", mouthRight)} dimple=${String.format(Locale.US, "%.2f", dimpleRight)}")
                put(FaceGestureType.MouthStretchRight, rightScore)
            }

            val mouthLeft = scores["mouthLeft"] ?: 0f
            val dimpleLeft = scores["mouthDimpleLeft"] ?: 0f
            val leftScore = maxOf(mouthLeft, dimpleLeft)
            if (leftScore > 0.15f) {
                val source = if (dimpleLeft >= mouthLeft) "dimple" else "mouth"
                LxLog.d("BlendShapes", "MouthStretchLeft via $source: mouth=${String.format(Locale.US, "%.2f", mouthLeft)} dimple=${String.format(Locale.US, "%.2f", dimpleLeft)}")
                put(FaceGestureType.MouthStretchLeft, leftScore)
            }
        }
    }

    private fun detectBrowSmile(scores: Map<String, Float>): Map<FaceGestureType, Float> {
        return buildMap {
            val browOuterUpRight = scores["browOuterUpRight"] ?: 0f
            val browOuterUpLeft = scores["browOuterUpLeft"] ?: 0f

            val asymmetry = browOuterUpRight - browOuterUpLeft

            if (browOuterUpRight > 0.08f && asymmetry > 0.12f) {
                val elevationScore = browOuterUpRight.coerceIn(0f, 1f)
                val asymmetryScore = (asymmetry / 0.3f).coerceIn(0f, 1f)
                val finalScore = (elevationScore * 0.7f + asymmetryScore * 0.3f).coerceIn(0f, 1f)
                put(FaceGestureType.BrowRaiseRight, finalScore)
            }

            val smileLeft = scores["mouthSmileLeft"] ?: 0f
            val smileRight = scores["mouthSmileRight"] ?: 0f
            val averageSmile = (smileLeft + smileRight) / 2f
            val smileDiff = abs(smileLeft - smileRight)

            if (averageSmile > 0.3f && smileDiff < 0.3f) {
                put(FaceGestureType.Smile, averageSmile)
            }
        }
    }

}