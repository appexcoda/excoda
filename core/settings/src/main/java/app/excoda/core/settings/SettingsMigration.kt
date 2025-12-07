package app.excoda.core.settings

import org.json.JSONObject

interface SettingsMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(json: JSONObject): JSONObject
}

class SettingsMigrationBuilder(
    private val fromVersion: Int,
    private val toVersion: Int
) {
    private var migrationBlock: (JSONObject) -> JSONObject = { it }

    fun transform(block: (JSONObject) -> JSONObject) {
        migrationBlock = block
    }

    fun build(): SettingsMigration = object : SettingsMigration {
        override val fromVersion = this@SettingsMigrationBuilder.fromVersion
        override val toVersion = this@SettingsMigrationBuilder.toVersion
        override fun migrate(json: JSONObject) = migrationBlock(json)
    }
}

fun migration(from: Int, to: Int, block: SettingsMigrationBuilder.() -> Unit): SettingsMigration {
    return SettingsMigrationBuilder(from, to).apply(block).build()
}