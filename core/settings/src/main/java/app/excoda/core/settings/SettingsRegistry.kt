package app.excoda.core.settings

object SettingsRegistry {
    private val contributors = mutableListOf<SettingsContributor>()

    fun register(contributor: SettingsContributor) {
        if (contributors.none { it.moduleName == contributor.moduleName }) {
            contributors += contributor
        }
    }

    fun all(): List<SettingsContributor> = contributors.toList()
}