package app.excoda.core.facegestures

import kotlinx.coroutines.flow.StateFlow

enum class FaceGestureType {
    BrowRaiseRight,
    Smile,
    MouthStretchRight,
    MouthStretchLeft,
}

interface FaceGestureHost {
    val isEnabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
    fun setGestureHandler(handler: (FaceGestureType) -> Unit)
    fun clearGestureHandler()
}