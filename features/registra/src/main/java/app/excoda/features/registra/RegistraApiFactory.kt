package app.excoda.features.registra

import app.excoda.core.logging.LxLog
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

@Singleton
class RegistraApiFactory @Inject constructor(
    private val certStore: RegistraCertificateStore
) {
    fun create(
        baseUrl: String,
        apiKey: String,
        onCertificateNeedsAcceptance: suspend (CertificateInfo) -> Boolean = { false }
    ): RegistraApi {
        val normalizedUrl = baseUrl
            .let { if (it.endsWith("/")) it else "$it/" }
            .let { "${it}api/v1/" }

        val host = try {
            URL(baseUrl).host
        } catch (e: Exception) {
            LxLog.d("RegistraApiFactory", "Failed to parse URL from baseUrl: $baseUrl", e)
            "unknown"
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val trustManager = TofuTrustManager(host, certStore, onCertificateNeedsAcceptance)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        val hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RegistraApi::class.java)
    }
}