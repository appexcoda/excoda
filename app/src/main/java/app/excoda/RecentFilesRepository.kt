package app.excoda

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.excoda.core.logging.LxLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recentFilesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recent_files"
)

@Singleton
class RecentFilesRepository @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.recentFilesDataStore

    companion object {
        private val RECENT_FILES_KEY = stringSetPreferencesKey("recent_files_set")
        private const val MAX_RECENT_FILES = 10
        private const val DELIMITER = "||"
    }

    val recentFiles: Flow<List<RecentFile>> = dataStore.data.map { preferences ->
        val serializedSet = preferences[RECENT_FILES_KEY] ?: emptySet()
        serializedSet
            .mapNotNull { deserializeRecentFile(it) }
            .sortedByDescending { it.timestamp }
            .take(MAX_RECENT_FILES)
    }

    suspend fun addRecentFile(uri: String, displayName: String) {
        dataStore.edit { preferences ->
            val currentSet = preferences[RECENT_FILES_KEY]?.toMutableSet() ?: mutableSetOf()

            // Remove existing entry with same URI if present
            currentSet.removeIf { it.startsWith("$uri$DELIMITER") }

            // Add new entry
            val newEntry = serializeRecentFile(
                RecentFile(
                    uri = uri,
                    displayName = displayName,
                    timestamp = System.currentTimeMillis()
                )
            )
            currentSet.add(newEntry)

            // Keep only the most recent MAX_RECENT_FILES
            val sortedFiles = currentSet
                .mapNotNull { deserializeRecentFile(it) }
                .sortedByDescending { it.timestamp }
                .take(MAX_RECENT_FILES)

            preferences[RECENT_FILES_KEY] = sortedFiles
                .map { serializeRecentFile(it) }
                .toSet()

            LxLog.d("RecentFiles", "Added: $displayName (total: ${sortedFiles.size})")
        }
    }

    suspend fun clearRecentFiles() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_FILES_KEY)
            LxLog.d("RecentFiles", "Cleared all recent files")
        }
    }

    private fun serializeRecentFile(file: RecentFile): String {
        return "${file.uri}$DELIMITER${file.displayName}$DELIMITER${file.timestamp}"
    }

    private fun deserializeRecentFile(serialized: String): RecentFile? {
        return try {
            val parts = serialized.split(DELIMITER)
            if (parts.size == 3) {
                RecentFile(
                    uri = parts[0],
                    displayName = parts[1],
                    timestamp = parts[2].toLong()
                )
            } else null
        } catch (e: Exception) {
            LxLog.e("RecentFiles", "Failed to deserialize: $serialized", e)
            null
        }
    }
}