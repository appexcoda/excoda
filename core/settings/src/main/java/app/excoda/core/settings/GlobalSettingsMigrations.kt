package app.excoda.core.settings

object GlobalSettingsMigrations {

    fun register() {
        MigrationRegistry.registerGlobalMigrations(
            migrations = listOf()
        )
    }
}