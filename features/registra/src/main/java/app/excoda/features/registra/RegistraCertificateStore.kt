package app.excoda.features.registra

import app.excoda.core.settings.GlobalSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistraCertificateStore @Inject constructor(
    private val globalSettings: GlobalSettingsRepository
) {
    suspend fun getStoredFingerprint(host: String): String? {
        return globalSettings.getCertificate(host).first()
    }

    suspend fun storeFingerprint(host: String, fingerprint: String) {
        globalSettings.storeCertificate(host, fingerprint)
    }

    suspend fun clearFingerprint(host: String) {
        globalSettings.clearCertificate(host)
    }
}