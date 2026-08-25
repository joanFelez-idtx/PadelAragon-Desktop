package com.padelaragon.desktop.data.network

import com.padelaragon.desktop.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class HtmlFetcher(cacheDir: java.io.File? = null) {
    private val latin1: Charset = Charsets.ISO_8859_1

    private val client: OkHttpClient = if (cacheDir != null) {
        sharedClient.newBuilder()
            .cache(okhttp3.Cache(java.io.File(cacheDir, "http_cache"), CACHE_SIZE))
            .build()
    } else {
        sharedClient
    }

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        execute(request)
    }

    suspend fun getWithStatus(url: String): HtmlResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        executeWithStatus(request)
    }

    suspend fun post(url: String, params: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val formBodyBuilder = FormBody.Builder(latin1)
            params.forEach { (key, value) -> formBodyBuilder.add(key, value) }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .post(formBodyBuilder.build())
                .build()

            execute(request)
        }

    private fun execute(request: Request): String {
        Logger.d("HtmlFetcher", "Fetching: ${request.url}")
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${request.url}")
                }
                val bytes = response.body.bytes()
                val result = bytes.toString(latin1)
                Logger.d(
                    "HtmlFetcher",
                    "Received ${result.length} chars from ${request.url}"
                )
                return result
            }
        } catch (e: SSLHandshakeException) {
            Logger.e("HtmlFetcher", "SSL handshake failed for ${request.url}", e)
            throw IOException("SSL error connecting to ${request.url}: ${e.message}", e)
        }
    }

    private fun executeWithStatus(request: Request): HtmlResponse {
        Logger.d("HtmlFetcher", "Fetching with status: ${request.url}")
        client.newCall(request).execute().use { response ->
            val bytes = response.body.bytes()
            val result = bytes.toString(latin1)
            Logger.d(
                "HtmlFetcher",
                "Received ${result.length} chars (HTTP ${response.code}) from ${request.url}"
            )
            return HtmlResponse(statusCode = response.code, body = result)
        }
    }

    data class HtmlResponse(
        val statusCode: Int,
        val body: String
    )

    companion object {
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        private const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB

        private val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 15
        }

        private val connectionPool = ConnectionPool(15, 2, TimeUnit.MINUTES)

        /**
         * Extra root/intermediate CA certificates (PEM) bundled as app resources so
         * they're trusted regardless of OS or how the app is run (native, or under
         * Wine/Bottles, which does not expose the real Windows certificate store).
         * Add more .pem files under src/main/resources/certs/ as needed.
         */
        private const val BUNDLED_CERTS_RESOURCE_DIR = "/certs/"
        private val BUNDLED_CERT_FILES = listOf(
            "itx-root-ca.pem",
            "itx-ssl-proxy-intermediate.pem"
        )

        val sharedClient: OkHttpClient = buildClient()

        /**
         * Pre-establish HTTPS connection to the target host.
         * Call from a background thread during app startup to avoid TLS handshake
         * latency on the first real request.
         */
        fun prewarmConnection(url: String) {
            Thread {
                runCatching {
                    val request = Request.Builder()
                        .url(url)
                        .head()
                        .header("User-Agent", MOBILE_USER_AGENT)
                        .build()
                    sharedClient.newCall(request).execute().close()
                }
            }.start()
        }

        private fun buildClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)

            // Some networks (e.g. corporate VPNs/proxies) perform TLS inspection on
            // padelfederacion.es, re-signing its certificate with a private root CA
            // that is trusted by the OS (via Group Policy/manual install) but NOT by
            // the JVM's own cacerts bundle - causing "PKIX path building failed" even
            // though the connection itself succeeds. This affects every environment
            // equally (Linux, real Windows, and Windows-via-Wine/Bottles), since it's
            // about which network the app is currently connected through, not the OS.
            // We build a trust manager that trusts the JVM's default CA bundle PLUS
            // any bundled extra roots (see BUNDLED_EXTRA_ROOTS) PLUS, on Windows only,
            // the OS certificate store - covering all known causes without weakening
            // validation against unknown/untrusted certificates.
            buildAugmentedSslSocketFactory()?.let { (sslSocketFactory, trustManager) ->
                builder.sslSocketFactory(sslSocketFactory, trustManager)
            }

            return builder.build()
        }

        /**
         * Extra root/intermediate CA certificates (PEM) bundled as app resources so
         * they're trusted regardless of OS or how the app is run (native, or under
         * Wine/Bottles, which does not expose the real Windows certificate store).
         * Add more .pem files under src/main/resources/certs/ as needed.
         */
        private fun loadBundledCertificates(): List<X509Certificate> {
            val certFactory = CertificateFactory.getInstance("X.509")
            return BUNDLED_CERT_FILES.mapNotNull { fileName ->
                runCatching {
                    HtmlFetcher::class.java.getResourceAsStream("$BUNDLED_CERTS_RESOURCE_DIR$fileName")
                        ?.use { certFactory.generateCertificate(it) as X509Certificate }
                }.onFailure { e ->
                    Logger.e("HtmlFetcher", "Failed to load bundled certificate $fileName", e)
                }.getOrNull()
            }
        }

        private fun buildAugmentedSslSocketFactory(): Pair<javax.net.ssl.SSLSocketFactory, X509TrustManager>? {
            return runCatching {
                fun trustManagersFor(keyStore: KeyStore?): List<X509TrustManager> {
                    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    factory.init(keyStore)
                    return factory.trustManagers.filterIsInstance<X509TrustManager>()
                }

                // JVM's bundled cacerts (default trust store) - covers the normal
                // public-CA case (e.g. Let's Encrypt) with no network interception.
                val defaultTrustManagers = trustManagersFor(null)

                // Extra CAs bundled with the app (e.g. corporate TLS-inspection roots
                // known to intercept padelfederacion.es on some networks).
                val bundledCerts = loadBundledCertificates()
                val bundledTrustManagers = if (bundledCerts.isNotEmpty()) {
                    val bundledStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                        load(null, null)
                        bundledCerts.forEachIndexed { index, cert ->
                            setCertificateEntry("bundled-$index", cert)
                        }
                    }
                    trustManagersFor(bundledStore)
                } else {
                    emptyList()
                }

                // On Windows, also trust the OS certificate store (covers other
                // corporate/network root CAs installed via Group Policy or IT, when
                // running as a native Windows build rather than under Wine/Bottles).
                val isWindows = System.getProperty("os.name")
                    ?.contains("windows", ignoreCase = true) == true
                val windowsTrustManagers = if (isWindows) {
                    runCatching {
                        val windowsRootStore = KeyStore.getInstance("Windows-ROOT").apply { load(null, null) }
                        trustManagersFor(windowsRootStore)
                    }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }

                val allTrustManagers = defaultTrustManagers + bundledTrustManagers + windowsTrustManagers
                val combinedTrustManager = CompositeX509TrustManager(allTrustManagers)

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(combinedTrustManager), null)
                sslContext.socketFactory to combinedTrustManager
            }.onFailure { e ->
                Logger.e("HtmlFetcher", "Failed to build augmented trust manager, using JVM default", e)
            }.getOrNull()
        }
    }

    /**
     * Trusts a certificate chain if ANY of the delegate trust managers trust it.
     * Used to merge the JVM's default CA bundle with the Windows OS trust store.
     */
    private class CompositeX509TrustManager(
        private val delegates: List<X509TrustManager>
    ) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
            val errors = mutableListOf<Exception>()
            for (delegate in delegates) {
                try {
                    delegate.checkClientTrusted(chain, authType)
                    return
                } catch (e: Exception) {
                    errors += e
                }
            }
            throw errors.lastOrNull() ?: SecurityException("No trust manager trusted this client certificate")
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            val errors = mutableListOf<Exception>()
            for (delegate in delegates) {
                try {
                    delegate.checkServerTrusted(chain, authType)
                    return
                } catch (e: Exception) {
                    errors += e
                }
            }
            throw errors.lastOrNull() ?: SecurityException("No trust manager trusted this server certificate")
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            delegates.flatMap { it.acceptedIssuers.toList() }.toTypedArray()
    }
}
