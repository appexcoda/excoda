package app.excoda.features.registra

import app.excoda.core.logging.LxLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionResult {
    object Success : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

data class CertificateInfo(
    val fingerprint: String,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val host: String
)

@Singleton
class RegistraConnectionChecker @Inject constructor(
    private val apiFactory: RegistraApiFactory
) {

    suspend fun checkConnection(
        host: String,
        apiKey: String,
        onCertificateNeedsAcceptance: suspend (CertificateInfo) -> Boolean
    ): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            if (host.isBlank()) {
                return@withContext ConnectionResult.Error("Host is empty")
            }
            if (apiKey.isBlank()) {
                return@withContext ConnectionResult.Error("API key is empty")
            }

            val api = apiFactory.create(host, apiKey, onCertificateNeedsAcceptance)
            val response = withTimeout(10000) {
                api.health()
            }

            if (response.isSuccessful && response.body()?.status == "ok") {
                ConnectionResult.Success
            } else {
                ConnectionResult.Error(response.message())
            }
        } catch (e: java.net.UnknownHostException) {
            ConnectionResult.Error("Cannot reach host: ${e.message}")
        } catch (e: java.net.ConnectException) {
            ConnectionResult.Error("Connection refused, ${e.message}")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            ConnectionResult.Error("Connection timeout, ${e.message}")
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            ConnectionResult.Error("SSL handshake failed: ${e.message}")
        } catch (e: java.security.cert.CertificateException) {
            ConnectionResult.Error("Certificate rejected: ${e.message}")
        } catch (e: Exception) {
            LxLog.e("RegistraConnectionChecker", "Health check failed", e)
            ConnectionResult.Error(e.message ?: "Connection failed")
        }
    }
}