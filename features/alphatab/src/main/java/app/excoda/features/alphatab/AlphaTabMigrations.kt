package app.excoda.features.alphatab

import app.excoda.core.settings.MigrationRegistry
import app.excoda.core.settings.migration

object AlphaTabMigrations {

    fun register() {
        MigrationRegistry.registerModuleMigrations(
            moduleName = AlphaTabSettings.MODULE_NAME,
            migrations = listOf()
        )
    }
}