package app.excoda.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.globalSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "global_settings"
)

class GlobalSettingsRepository(private val context: Context) {
    private val dataStore = context.globalSettingsDataStore

    private val gestureModeKey = stringPreferencesKey("gesture_mode")
    private val registraHostKey = stringPreferencesKey("registra_host")
    private val registraRestApiKey = stringPreferencesKey("registra_api_key")
    private val gestureConsentGivenKey = booleanPreferencesKey("gesture_consent_given")
    private val autoEnableGesturesKey = booleanPreferencesKey("auto_enable_gestures")
    private val showCameraPreviewKey = booleanPreferencesKey("show_camera_preview")

    val gestureConsentGiven: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[gestureConsentGivenKey] ?: false
    }

    suspend fun setGestureConsentGiven(given: Boolean) {
        dataStore.edit { preferences ->
            preferences[gestureConsentGivenKey] = given
        }
    }

    val autoEnableGestures: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoEnableGesturesKey] ?: false
    }

    suspend fun setAutoEnableGestures(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoEnableGesturesKey] = enabled
        }
    }

    val showCameraPreview: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[showCameraPreviewKey] ?: true
    }

    suspend fun setShowCameraPreview(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[showCameraPreviewKey] = show
        }
    }

    val gestureMode: Flow<GestureMode> = dataStore.data.map { preferences ->
        val modeString = preferences[gestureModeKey] ?: GestureMode.MOUTH_MOVEMENTS.name
        GestureMode.fromString(modeString)
    }

    suspend fun setGestureMode(mode: GestureMode) {
        dataStore.edit { preferences ->
            preferences[gestureModeKey] = mode.name
        }
    }

    val registraHost: Flow<String> = dataStore.data.map { preferences ->
        preferences[registraHostKey] ?: ""
    }

    suspend fun setRegistraHost(host: String) {
        dataStore.edit { preferences ->
            preferences[registraHostKey] = host
        }
    }

    val registraApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[registraRestApiKey] ?: ""
    }

    suspend fun setRegistraApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[registraRestApiKey] = apiKey
        }
    }

    suspend fun storeCertificate(host: String, fingerprint: String) {
        val key = stringPreferencesKey("cert_${host.replace(":", "_")}")
        dataStore.edit { preferences ->
            preferences[key] = fingerprint
        }
    }

    fun getCertificate(host: String): Flow<String?> {
        val key = stringPreferencesKey("cert_${host.replace(":", "_")}")
        return dataStore.data.map { preferences ->
            preferences[key]
        }
    }
}
