package app.excoda.core.facegestures

import app.excoda.core.logging.LxLog
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SimpleGestureFilter(
    private val config: GestureConfig
) {
    private val smoothedScores = ConcurrentHashMap<FaceGestureType, Float>()
    private val consecutiveCounts = ConcurrentHashMap<FaceGestureType, Int>()

    fun shouldTrigger(type: FaceGestureType, rawScore: Float): Pair<Boolean, Float> {
        val currentSmoothed = smoothedScores[type] ?: 0f
        val filtered = config.smoothingAlpha * rawScore + (1 - config.smoothingAlpha) * currentSmoothed
        smoothedScores[type] = filtered

        val count = if (filtered > config.detectionThreshold) {
            (consecutiveCounts[type] ?: 0) + 1
        } else {
            consecutiveCounts[type] = 0
            0
        }
        consecutiveCounts[type] = count

        val shouldTrigger = count >= config.consecutiveFrames

        if (shouldTrigger) {
            consecutiveCounts[type] = 0
            LxLog.i("Filter", "🎯 ${type.name} DETECTED: raw=${String.format(Locale.US, "%.3f", rawScore)} filtered=${String.format(Locale.US, "%.3f", filtered)} consecutive=$count")
        }

        return Pair(shouldTrigger, filtered)
    }

    fun reset() {
        smoothedScores.clear()
        consecutiveCounts.clear()
        LxLog.d("Filter", "Filter reset")
    }
}