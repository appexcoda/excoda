package app.excoda.core.settings

interface ConnectionChecker {
    suspend fun checkConnection(
        host: String,
        apiKey: String,
        onCertificateNeedsAcceptance: suspend (CertificateInfo) -> Boolean = { false }
    ): ConnectionResult
}

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