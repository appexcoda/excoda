package app.excoda.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import app.excoda.core.logging.LxLog

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "excoda_settings"
)

class SettingsRepository(private val context: Context) {
    private val dataStore = context.settingsDataStore

    fun createTrackKey(fileUri: String, moduleName: String, trackIndex: Int): Preferences.Key<String> {
        val fileId = generateFileId(fileUri)
        return stringPreferencesKey("${fileId}:${moduleName}:track${trackIndex}")
    }


    suspend fun saveTrackSettings(
        fileUri: String,
        moduleName: String,
        trackIndex: Int,
        settings: ModuleSettings
    ) {
        val key = createTrackKey(fileUri, moduleName, trackIndex)
        val json = settings.toJson()
        LxLog.d("SettingsRepository", "Saving track $trackIndex settings - key: ${key.name}, json: $json")
        dataStore.edit { preferences ->
            preferences[key] = json
        }
        LxLog.d("SettingsRepository", "Track $trackIndex settings saved successfully")
    }

    fun <T : ModuleSettings> observeTrackSettings(
        fileUri: String,
        moduleName: String,
        trackIndex: Int,
        defaultSettings: T,
        deserializer: (String) -> T
    ): Flow<T> {
        val key = createTrackKey(fileUri, moduleName, trackIndex)
        return dataStore.data.map { preferences ->
            try {
                val json = preferences[key] ?: return@map defaultSettings

                val migrated = MigrationExecutor.migrateModuleSettings(
                    moduleName = moduleName,
                    jsonString = json,
                    targetVersion = defaultSettings.schemaVersion
                )

                migrated?.let { deserializer(it) } ?: defaultSettings
            } catch (e: Exception) {
                LxLog.e("SettingsRepository", "Error observing track $trackIndex settings", e)
                defaultSettings
            }
        }
    }

    fun generateFileId(fileUri: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(fileUri.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun createKey(fileUri: String, moduleName: String): Preferences.Key<String> {
        val fileId = generateFileId(fileUri)
        return stringPreferencesKey("${fileId}:${moduleName}")
    }

    suspend fun saveSettings(
        fileUri: String,
        moduleName: String,
        settings: ModuleSettings
    ) {
        val key = createKey(fileUri, moduleName)
        val json = settings.toJson()
        LxLog.d("SettingsRepository", "Saving settings - key: ${key.name}, json: $json")
        dataStore.edit { preferences ->
            preferences[key] = json
        }
        LxLog.d("SettingsRepository", "Settings saved successfully")
    }


    fun <T : ModuleSettings> observeSettings(
        fileUri: String,
        moduleName: String,
        defaultSettings: T,
        deserializer: (String) -> T
    ): Flow<T> {
        val key = createKey(fileUri, moduleName)
        return dataStore.data.map { preferences ->
            try {
                val json = preferences[key] ?: return@map defaultSettings

                val migrated = MigrationExecutor.migrateModuleSettings(
                    moduleName = moduleName,
                    jsonString = json,
                    targetVersion = defaultSettings.schemaVersion
                )

                migrated?.let { deserializer(it) } ?: defaultSettings
            } catch (e: Exception) {
                LxLog.e("SettingsRepository", "Error observing settings", e)
                defaultSettings
            }
        }
    }
}