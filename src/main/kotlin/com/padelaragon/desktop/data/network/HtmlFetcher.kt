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
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

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

            return builder.build()
        }
    }
}
