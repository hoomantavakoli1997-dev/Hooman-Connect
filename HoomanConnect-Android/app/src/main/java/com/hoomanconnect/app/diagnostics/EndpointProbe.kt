package com.hoomanconnect.app.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Network-only diagnostics. It does not sign in, alter Google responses, or test account eligibility. */
object EndpointProbe {
    suspend fun check(host: String): EndpointResult = withContext(Dispatchers.IO) {
        val dns = try {
            InetAddress.getAllByName(host).map { it.hostAddress }
        } catch (e: Exception) {
            return@withContext EndpointResult(host, false, emptyList(), null, "خطای DNS: ${e.javaClass.simpleName}")
        }
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL("https://$host/").openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "HoomanConnect-Diagnostics/0.2")
            }
            val code = connection.responseCode
            val cipher = connection.cipherSuite
            EndpointResult(host, true, dns, code, "اتصال HTTPS برقرار شد؛ $cipher")
        } catch (e: Exception) {
            EndpointResult(host, false, dns, null, "خطای HTTPS: ${e.javaClass.simpleName}")
        } finally {
            connection?.disconnect()
        }
    }
}

data class EndpointResult(
    val host: String,
    val dnsOk: Boolean,
    val addresses: List<String>,
    val httpStatus: Int?,
    val message: String
)
