package app.excoda

import android.app.Application
import app.excoda.core.settings.GlobalSettingsMigrations
import app.excoda.features.alphatab.AlphaTabModule
import app.excoda.features.pdf.PdfJsModule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExcodaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        GlobalSettingsMigrations.register()

        AlphaTabModule
        PdfJsModule
    }
}