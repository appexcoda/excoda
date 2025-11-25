plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.excoda.core.logging"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // No external dependencies to keep logger lightweight.
}