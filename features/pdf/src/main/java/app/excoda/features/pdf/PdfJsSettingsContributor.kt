package app.excoda.features.pdf

import androidx.compose.runtime.Composable
import app.excoda.core.settings.SettingsContributor

object PdfJsSettingsContributor : SettingsContributor {
    override val moduleName: String = "PdfJs"

    @Composable
    override fun SettingsEntry(
        fileUri: String,
        onClose: () -> Unit
    ) {
    }
}