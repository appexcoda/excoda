pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Excoda"

include(
    ":app",
    ":core:ui",
    ":core:facegestures",
    ":core:fab",
    ":core:launcher-api",
    ":core:logging",
    ":core:settings",
    ":features:alphatab",
    ":features:pdf",
    ":features:registra"
)