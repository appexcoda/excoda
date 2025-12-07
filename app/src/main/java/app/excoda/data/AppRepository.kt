package app.excoda.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.excoda.core.logging.LxLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "app_state")

class AppRepository(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private val APP_STATE_KEY = stringPreferencesKey("app_state")
    }
    
    val appState: Flow<AppState> = context.dataStore.data
        .map { preferences ->
            val stateJson = preferences[APP_STATE_KEY]
            if (stateJson != null) {
                try {
                    json.decodeFromString<AppState>(stateJson)
                } catch (_: Exception) {
                    AppState()
                }
            } else {
                AppState()
            }
        }
    
    suspend fun saveAppState(state: AppState) {
        val startTime = System.currentTimeMillis()
        
        context.dataStore.edit { preferences ->
            val serializeStartTime = System.currentTimeMillis()
            val stateJson = json.encodeToString(state)
            val serializedSize = stateJson.length
            val serializeTime = System.currentTimeMillis() - serializeStartTime
            
            LxLog.d("AppRepository", "saveAppState: Serialization took ${serializeTime}ms, size=${serializedSize} chars, tabs=${state.tabs.size}, activeTab=${state.activeTabId}")
            
            preferences[APP_STATE_KEY] = stateJson
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        LxLog.d("AppRepository", "saveAppState: Total save completed in ${totalTime}ms")
    }
}