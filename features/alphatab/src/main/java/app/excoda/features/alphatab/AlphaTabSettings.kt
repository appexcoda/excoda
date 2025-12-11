package app.excoda.features.alphatab

import app.excoda.core.logging.LxLog
import app.excoda.core.settings.ModuleSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    encodeDefaults = true
}

@Serializable
data class AlphaTabSettings(
    override val schemaVersion: Int = 1,
    val selectedTrackIndex: Int = 0
) : ModuleSettings {
    override fun toJson(): String = json.encodeToString(this)

    companion object {
        val DEFAULT = AlphaTabSettings()
        const val MODULE_NAME = "AlphaTab"

        fun fromJson(jsonString: String): AlphaTabSettings {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                LxLog.d("AlphaTabSettings", "Failed to parse AlphaTabSettings from JSON: $jsonString", e)
                DEFAULT
            }
        }
    }
}

@Serializable
data class AlphaTabTrackSettings(
    override val schemaVersion: Int = 1,
    val staveProfile: String = "default",
    val scale: Float = 1.0f,
    val stretch: Float = 1.0f,
    val layoutMode: String = "page",
    val barsPerRow: Int = -1,
    val isAutomaticBarsPerRow: Boolean = true,
    val firstSystemPaddingTop: Int = 5,
    val systemPaddingTop: Int = 5,
    val systemPaddingBottom: Int = 5,
    val tabRhythmMode: String = "automatic",
    val padding: List<Int> = listOf(5, 5, 5, 5),
    val showTempo: Boolean = true,
    val showTuning: Boolean = true,
    val showChordDiagrams: Boolean = true,
    val showChordNames: Boolean = true,
    val showBeatBarre: Boolean = true,
    val showDynamics: Boolean = true,
    val showText: Boolean = true,
    val showPickStroke: Boolean = true,
    val showLyrics: Boolean = true,
    val showTrackNames: Boolean = false
) : ModuleSettings {
    override fun toJson(): String = json.encodeToString(this)

    companion object {
        val DEFAULT = AlphaTabTrackSettings()

        fun fromJson(jsonString: String): AlphaTabTrackSettings {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                LxLog.d("AlphaTabTrackSettings", "Failed to parse AlphaTabTrackSettings from JSON: $jsonString", e)
                DEFAULT
            }
        }
    }
}