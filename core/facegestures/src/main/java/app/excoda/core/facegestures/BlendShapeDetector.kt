package app.excoda.core.facegestures

import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GestureMode
import kotlin.math.abs

class BlendShapeDetector(
    var gestureMode: GestureMode = GestureMode.MOUTH_MOVEMENTS,
    private val config: GestureConfig = GestureConfig()
) : GestureDetector {

    private var frameCount = 0
    private val baselineScores = mutableMapOf<String, Float>()
    private var baselineFrames = 0
    private val BASELINE_PERIOD = 30

    override fun detect(face: FaceResult): Map<FaceGestureType, Float> {
        val blendshapes = face.blendshapes ?: return emptyMap()
        val rawScores = blendshapes.associate { it.categoryName() to it.score() }

        // Build baseline for relative detection
        if (baselineFrames < BASELINE_PERIOD) {
            rawScores.forEach { (name, value) ->
                baselineScores[name] = (baselineScores[name] ?: 0f) + value / BASELINE_PERIOD
            }
            baselineFrames++
            return emptyMap()
        }

        frameCount++
        if (frameCount % 30 == 0) {
            val active = rawScores.filter { it.value > 0.1f }
            if (active.isNotEmpty()) {
                LxLog.d("BlendShapes", "Active: ${active.map { "${it.key}=${String.format("%.2f", it.value)}" }.joinToString(", ")}")
            }
        }

        return when (gestureMode) {
            GestureMode.MOUTH_MOVEMENTS -> detectMouthMovements(rawScores)
            GestureMode.BROW_SMILE -> detectBrowSmile(rawScores)
        }
    }

    private fun detectMouthMovements(scores: Map<String, Float>): Map<FaceGestureType, Float> {
        return buildMap {
            // Right side - absolute threshold works fine
            val mouthRight = scores["mouthRight"] ?: 0f
            if (mouthRight > 0.2f) {
                put(FaceGestureType.MouthDimpleRight, mouthRight)
            }

            // Left side - use RELATIVE detection from baseline
            val mouthLeft = scores["mouthLeft"] ?: 0f
            val mouthDimpleLeft = scores["mouthDimpleLeft"] ?: 0f
            val mouthStretchLeft = scores["mouthStretchLeft"] ?: 0f

            val baselineLeft = baselineScores["mouthLeft"] ?: 0f
            val baselineDimpleLeft = baselineScores["mouthDimpleLeft"] ?: 0f
            val baselineStretchLeft = baselineScores["mouthStretchLeft"] ?: 0f

            // Calculate delta from neutral baseline
            val leftDelta = maxOf(
                mouthLeft - baselineLeft,
                mouthDimpleLeft - baselineDimpleLeft,
                mouthStretchLeft - baselineStretchLeft
            )

            // Trigger on relative change (works across head poses)
            if (leftDelta > 0.06f) {  // Delta threshold instead of absolute
                val leftScore = maxOf(mouthLeft, mouthDimpleLeft, mouthStretchLeft)
                put(FaceGestureType.MouthDimpleLeft, leftScore.coerceAtLeast(0.2f))
            }
        }
    }

    private fun detectBrowSmile(scores: Map<String, Float>): Map<FaceGestureType, Float> {
        return buildMap {
            val browOuterUpRight = scores["browOuterUpRight"] ?: 0f
            val browOuterUpLeft = scores["browOuterUpLeft"] ?: 0f
            val browInnerUp = scores["browInnerUp"] ?: 0f

            // Get baselines
            val baselineBrowRight = baselineScores["browOuterUpRight"] ?: 0f
            val baselineBrowLeft = baselineScores["browOuterUpLeft"] ?: 0f
            val baselineBrowInner = baselineScores["browInnerUp"] ?: 0f

            // Calculate deltas from neutral (filters out head movement)
            val rightBrowDelta = browOuterUpRight - baselineBrowRight
            val leftBrowDelta = browOuterUpLeft - baselineBrowLeft
            val innerDelta = browInnerUp - baselineBrowInner

            // Use delta for elevation score
            val rightBrowDeltaScore = maxOf(rightBrowDelta, innerDelta * 0.5f)

            // Asymmetry still works with absolute values
            val asymmetry = browOuterUpRight - browOuterUpLeft

            // Require BOTH delta elevation AND asymmetry
            if (rightBrowDeltaScore > 0.08f && asymmetry > 0.12f) {
                val elevationScore = rightBrowDeltaScore.coerceIn(0f, 1f)
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

    fun resetBaseline() {
        baselineScores.clear()
        baselineFrames = 0
    }
}