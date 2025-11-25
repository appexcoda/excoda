package app.excoda.core.facegestures

import app.excoda.core.logging.LxLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SimpleFaceGestureHost(
    private val scope: CoroutineScope,
    private val cooldownMillis: Long = 1500L
) : FaceGestureHost {

    private val mutableEnabled = MutableStateFlow(false)
    override val isEnabled: StateFlow<Boolean> = mutableEnabled

    private val mutableFaceTracking = MutableStateFlow(false)
    val isFaceTracking: StateFlow<Boolean> = mutableFaceTracking

    private var isInCooldown = false
    private var cooldownJob: Job? = null
    private var onGestureDetected: ((FaceGestureType) -> Unit)? = null

    override fun setEnabled(enabled: Boolean) {
        mutableEnabled.value = enabled
        if (!enabled) {
            reset()
        }
        LxLog.i("FaceGestures", "Host ${if (enabled) "enabled" else "disabled"}")
    }

    override fun setGestureHandler(handler: (FaceGestureType) -> Unit) {
        onGestureDetected = handler
    }

    override fun clearGestureHandler() {
        onGestureDetected = null
    }

    fun setFaceTracking(isTracking: Boolean) {
        mutableFaceTracking.value = isTracking
    }

    fun onGestureDetected(type: FaceGestureType, confidence: Float) {
        if (!mutableEnabled.value || isInCooldown) return

        LxLog.i("FaceGestures", "Gesture ${type.name} triggered (conf=${String.format("%.3f", confidence)})")

        scope.launch(Dispatchers.Main) {
            onGestureDetected?.invoke(type)
        }

        startCooldown()
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        isInCooldown = true
        LxLog.d("FaceGestures", "Cooldown started (${cooldownMillis}ms)")

        cooldownJob = scope.launch {
            delay(cooldownMillis)
            isInCooldown = false
            LxLog.d("FaceGestures", "Cooldown expired, ready for next gesture")
        }
    }

    fun reset() {
        cooldownJob?.cancel()
        isInCooldown = false
        mutableFaceTracking.value = false
        LxLog.d("FaceGestures", "Host reset")
    }
}