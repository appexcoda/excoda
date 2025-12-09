package app.excoda.di

import android.content.ContentResolver
import android.content.Context
import app.excoda.core.fab.DefaultFabMenuHost
import app.excoda.core.fab.FabMenuHost
import app.excoda.core.facegestures.FaceGestureController
import app.excoda.core.facegestures.FaceGestureHost
import app.excoda.core.facegestures.GestureConfig
import app.excoda.core.facegestures.SimpleFaceGestureController
import app.excoda.core.facegestures.SimpleFaceGestureHost
import app.excoda.core.facegestures.SimpleGestureFilter
import app.excoda.core.settings.GlobalSettingsRepository
import app.excoda.core.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideGestureConfig(): GestureConfig = GestureConfig()

    @Provides
    @Singleton
    fun provideGestureFilter(config: GestureConfig): SimpleGestureFilter =
        SimpleGestureFilter(config)

    @Provides
    @Singleton
    fun provideFaceGestureHost(
        @ApplicationScope scope: CoroutineScope
    ): SimpleFaceGestureHost = SimpleFaceGestureHost(scope)

    @Provides
    @Singleton
    fun provideFaceGestureHostInterface(
        host: SimpleFaceGestureHost
    ): FaceGestureHost = host

    @Provides
    @Singleton
    fun provideFaceGestureController(
        host: SimpleFaceGestureHost
    ): FaceGestureController = SimpleFaceGestureController(host)

    @Provides
    @Singleton
    fun provideFabMenuHost(): FabMenuHost = DefaultFabMenuHost()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideGlobalSettingsRepository(
        @ApplicationContext context: Context
    ): GlobalSettingsRepository = GlobalSettingsRepository(context)
}