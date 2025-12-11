package app.excoda.core.settings

object MigrationRegistry {
    private val moduleMigrations = mutableMapOf<String, List<SettingsMigration>>()
    private val globalMigrations = mutableListOf<SettingsMigration>()

    fun registerModuleMigrations(moduleName: String, migrations: List<SettingsMigration>) {
        val existing = moduleMigrations[moduleName]?.toMutableList() ?: mutableListOf()
        existing.addAll(migrations)
        moduleMigrations[moduleName] = existing.sortedBy { it.fromVersion }
    }

    fun registerGlobalMigrations(migrations: List<SettingsMigration>) {
        globalMigrations.addAll(migrations)
        globalMigrations.sortBy { it.fromVersion }
    }

    fun getModuleMigrations(moduleName: String): List<SettingsMigration> {
        return moduleMigrations[moduleName] ?: emptyList()
    }

    fun getGlobalMigrations(): List<SettingsMigration> {
        return globalMigrations.toList()
    }
}