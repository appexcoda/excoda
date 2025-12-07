package app.excoda.core.facegestures

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface FaceGestureController {
    val isEnabled: StateFlow<Boolean>
    fun configure(supportsFaceGestures: Boolean)
    fun enable()
    fun disable()
    fun toggle()
}

class SimpleFaceGestureController(
    private val host: SimpleFaceGestureHost
) : FaceGestureController {

    private val supportsGestures = MutableStateFlow(false)
    private val mutableEnabled = MutableStateFlow(false)

    override val isEnabled: StateFlow<Boolean> = mutableEnabled

    override fun configure(supportsFaceGestures: Boolean) {
        supportsGestures.value = supportsFaceGestures
        if (!supportsGestures.value) {
            disable()
        }
    }

    override fun enable() {
        if (!supportsGestures.value) return
        mutableEnabled.value = true
        host.setEnabled(true)
    }

    override fun disable() {
        mutableEnabled.value = false
        host.setEnabled(false)
    }

    override fun toggle() {
        if (mutableEnabled.value) disable() else enable()
    }
}