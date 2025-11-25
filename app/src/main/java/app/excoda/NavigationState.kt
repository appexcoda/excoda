package app.excoda

import app.excoda.core.launcher.api.FileModule

sealed class NavigationState {
    data object LauncherEmpty : NavigationState()

    data class ModuleActive(
        val module: FileModule,
        val uri: String
    ) : NavigationState()
}