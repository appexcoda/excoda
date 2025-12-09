package app.excoda

data class LauncherState(
    val navigationState: NavigationState = NavigationState.LauncherEmpty,
    val showModuleSettingsDialog: Boolean = false,
    val showGlobalSettingsDialog: Boolean = false,
    val errorMessage: String? = null
)