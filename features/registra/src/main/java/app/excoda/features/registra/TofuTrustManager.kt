package app.excoda.features.registra

import app.excoda.core.logging.LxLog
import app.excoda.core.settings.CertificateInfo
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.X509TrustManager

class TofuTrustManager(
    private val host: String,
    private val certStore: RegistraCertificateStore,
    private val onCertificateNeedsAcceptance: suspend (CertificateInfo) -> Boolean
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        // Not used for client
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        if (chain.isEmpty()) {
            throw CertificateException("Certificate chain is empty")
        }

        val cert = chain[0]
        val fingerprint = getCertificateFingerprint(cert)

        runBlocking {
            val storedFingerprint = certStore.getStoredFingerprint(host)

            when {
                storedFingerprint == null -> {
                    // First use - ask user
                    LxLog.d("TOFU", "First connection to $host, fingerprint: $fingerprint")
                    val accepted = onCertificateNeedsAcceptance(toCertificateInfo(cert, fingerprint))
                    if (!accepted) {
                        throw CertificateException("User rejected certificate for $host")
                    }
                }
                storedFingerprint != fingerprint -> {
                    // Certificate changed
                    LxLog.w("TOFU", "Certificate changed for $host! Stored: $storedFingerprint, Got: $fingerprint")
                    val accepted = onCertificateNeedsAcceptance(toCertificateInfo(cert, fingerprint))
                    if (!accepted) {
                        throw CertificateException("Certificate changed for $host (possible MITM attack)")
                    }
                }
                else -> {
                    // Matches stored certificate, trust it
                    LxLog.d("TOFU", "Certificate verified for $host")
                }
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    private fun getCertificateFingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cert.encoded)
        return hash.joinToString(":") { "%02X".format(it) }
    }

    private fun toCertificateInfo(cert: X509Certificate, fingerprint: String): CertificateInfo {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return CertificateInfo(
            fingerprint = fingerprint,
            subject = cert.subjectDN.name,
            issuer = cert.issuerDN.name,
            validFrom = dateFormat.format(cert.notBefore),
            validTo = dateFormat.format(cert.notAfter),
            host = host
        )
    }
}