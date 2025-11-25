package app.excoda.core.settings

import androidx.compose.runtime.Composable

interface SettingsContributor {
    val moduleName: String

    @Composable
    fun SettingsEntry(
        fileUri: String,  // MODIFIED - added parameter
        onClose: () -> Unit
    )
}