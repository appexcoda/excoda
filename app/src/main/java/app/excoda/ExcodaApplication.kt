package app.excoda

import android.app.Application
import app.excoda.core.logging.LxLog
import app.excoda.core.settings.GlobalSettingsMigrations
import app.excoda.core.settings.GlobalSettingsRepository
import app.excoda.features.alphatab.AlphaTabModule
import app.excoda.features.pdf.PdfJsModule
import app.excoda.features.registra.RegistraApiFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExcodaApplication : Application() {
    @Inject
    lateinit var globalSettingsRepository: GlobalSettingsRepository
    
    @Inject
    lateinit var registraApiFactory: RegistraApiFactory

    override fun onCreate() {
        super.onCreate()

        GlobalSettingsMigrations.register()

        AlphaTabModule.initialize(globalSettingsRepository, registraApiFactory)
        PdfJsModule.initialize(globalSettingsRepository)
        
        cleanupPreviewFiles()
    }
    
    private fun cleanupPreviewFiles() {
        try {
            val cacheDir = cacheDir
            val previewFiles = cacheDir.listFiles { file ->
                file.isFile && file.name.startsWith("preview_")
            } ?: emptyArray()
            
            var deletedCount = 0
            var deletedSize = 0L
            
            previewFiles.forEach { file ->
                try {
                    deletedSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    LxLog.w("ExcodaApplication", "Failed to delete preview file: ${file.name}", e)
                }
            }
            
            if (deletedCount > 0) {
                val sizeKb = deletedSize / 1024
                LxLog.i("ExcodaApplication", "Cleaned up $deletedCount preview files ($sizeKb KB)")
            }
        } catch (e: Exception) {
            LxLog.e("ExcodaApplication", "Failed to cleanup preview files", e)
        }
    }
}