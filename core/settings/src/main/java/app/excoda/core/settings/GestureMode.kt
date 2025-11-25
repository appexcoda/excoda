package app.excoda.core.settings

enum class GestureMode(val displayName: String) {
    MOUTH_MOVEMENTS("Mouth movements"),
    BROW_SMILE("Brow and smile");

    fun getDescription(): String = when (this) {
        MOUTH_MOVEMENTS -> "😏 Move mouth right/left to turn pages"
        BROW_SMILE -> "🤨 Right Eyebrow Up → Forward • 😊 Smile → Back"
    }

    companion object {
        fun fromString(value: String): GestureMode {
            return values().find { it.name == value } ?: MOUTH_MOVEMENTS
        }
    }
}