package app.excoda.core.facegestures

import kotlinx.coroutines.flow.StateFlow

enum class FaceGestureType {
    BrowRaiseRight,
    Smile,
    MouthDimpleRight,
    MouthDimpleLeft,
}

interface FaceGestureHost {
    val isEnabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
    fun setGestureHandler(handler: (FaceGestureType) -> Unit)
    fun clearGestureHandler()
}