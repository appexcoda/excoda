package app.excoda.features.registra

import app.excoda.core.settings.ConnectionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RegistraModule {

    @Binds
    @Singleton
    abstract fun bindConnectionChecker(
        impl: RegistraConnectionChecker
    ): ConnectionChecker
}