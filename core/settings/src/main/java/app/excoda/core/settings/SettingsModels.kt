package app.excoda.core.settings

interface ModuleSettings {
    val schemaVersion: Int
    fun toJson(): String
}