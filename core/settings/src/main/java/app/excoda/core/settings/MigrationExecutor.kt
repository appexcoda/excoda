package app.excoda.core.settings

import app.excoda.core.logging.LxLog
import org.json.JSONObject

object MigrationExecutor {

    fun migrateModuleSettings(
        moduleName: String,
        jsonString: String,
        targetVersion: Int
    ): String? {
        return try {
            val json = JSONObject(jsonString)
            val currentVersion = json.optInt("schemaVersion", 1)

            if (currentVersion == targetVersion) {
                return jsonString
            }

            if (currentVersion > targetVersion) {
                LxLog.w("Migration", "$moduleName: current v$currentVersion > target v$targetVersion, using defaults")
                return null
            }

            val migrations = MigrationRegistry.getModuleMigrations(moduleName)
                .filter { it.fromVersion >= currentVersion && it.toVersion <= targetVersion }
                .sortedBy { it.fromVersion }

            var migrated = json
            for (migration in migrations) {
                if (migrated.optInt("schemaVersion", 1) == migration.fromVersion) {
                    LxLog.d("Migration", "$moduleName: applying v${migration.fromVersion} → v${migration.toVersion}")
                    migrated = migration.migrate(migrated)
                    migrated.put("schemaVersion", migration.toVersion)
                }
            }

            migrated.toString()
        } catch (e: Exception) {
            LxLog.e("Migration", "Failed to migrate $moduleName", e)
            null
        }
    }

    fun migrateGlobalSettings(
        jsonString: String,
        targetVersion: Int
    ): String? {
        return try {
            val json = JSONObject(jsonString)
            val currentVersion = json.optInt("schemaVersion", 1)

            if (currentVersion == targetVersion) {
                return jsonString
            }

            if (currentVersion > targetVersion) {
                LxLog.w("Migration", "Global: current v$currentVersion > target v$targetVersion")
                return null
            }

            val migrations = MigrationRegistry.getGlobalMigrations()
                .filter { it.fromVersion >= currentVersion && it.toVersion <= targetVersion }
                .sortedBy { it.fromVersion }

            var migrated = json
            for (migration in migrations) {
                if (migrated.optInt("schemaVersion", 1) == migration.fromVersion) {
                    LxLog.d("Migration", "Global: applying v${migration.fromVersion} → v${migration.toVersion}")
                    migrated = migration.migrate(migrated)
                    migrated.put("schemaVersion", migration.toVersion)
                }
            }

            migrated.toString()
        } catch (e: Exception) {
            LxLog.e("Migration", "Failed to migrate global settings", e)
            null
        }
    }
}