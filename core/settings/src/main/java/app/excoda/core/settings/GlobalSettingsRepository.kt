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

    private val GESTURE_MODE = stringPreferencesKey("gesture_mode")
    private val REGISTRA_HOST = stringPreferencesKey("registra_host")
    private val REGISTRA_API_KEY = stringPreferencesKey("registra_api_key")
    private val GESTURE_CONSENT_GIVEN = booleanPreferencesKey("gesture_consent_given")

    val gestureConsentGiven: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[GESTURE_CONSENT_GIVEN] ?: false
    }

    suspend fun setGestureConsentGiven(given: Boolean) {
        dataStore.edit { preferences ->
            preferences[GESTURE_CONSENT_GIVEN] = given
        }
    }

    val gestureMode: Flow<GestureMode> = dataStore.data.map { preferences ->
        val modeString = preferences[GESTURE_MODE] ?: GestureMode.MOUTH_MOVEMENTS.name
        GestureMode.fromString(modeString)
    }

    suspend fun setGestureMode(mode: GestureMode) {
        dataStore.edit { preferences ->
            preferences[GESTURE_MODE] = mode.name
        }
    }

    val registraHost: Flow<String> = dataStore.data.map { preferences ->
        preferences[REGISTRA_HOST] ?: ""
    }

    suspend fun setRegistraHost(host: String) {
        dataStore.edit { preferences ->
            preferences[REGISTRA_HOST] = host
        }
    }

    val registraApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[REGISTRA_API_KEY] ?: ""
    }

    suspend fun setRegistraApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[REGISTRA_API_KEY] = apiKey
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

    suspend fun clearCertificate(host: String) {
        val key = stringPreferencesKey("cert_${host.replace(":", "_")}")
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}