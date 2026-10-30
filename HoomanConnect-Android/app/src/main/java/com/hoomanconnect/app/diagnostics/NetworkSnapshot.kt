package com.hoomanconnect.app.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/** Read-only snapshot of Android's current default network; no traffic is redirected. */
data class NetworkSnapshot(
    val transport: String,
    val internetAvailable: Boolean,
    val validated: Boolean,
    val dnsServers: List<String>,
    val ipv4Addresses: List<String>,
    val ipv6Addresses: List<String>,
    val message: String
) {
    companion object {
        fun read(context: Context): NetworkSnapshot {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
                ?: return NetworkSnapshot("نامشخص", false, false, emptyList(), emptyList(), emptyList(), "شبکه فعالی پیدا نشد.")
            val caps = manager.getNetworkCapabilities(network)
            val links = manager.getLinkProperties(network)
            val transport = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "داده همراه"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                else -> "سایر / نامشخص"
            }
            val dns = links?.dnsServers?.map { it.hostAddress ?: "" }?.filter { it.isNotBlank() } ?: emptyList()
            val addresses = links?.linkAddresses?.mapNotNull { it.address.hostAddress } ?: emptyList()
            val v4 = addresses.filter { !it.contains(":") }
            val v6 = addresses.filter { it.contains(":") }
            val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            val message = when {
                validated -> "اندروید دسترسی عمومی اینترنت را تأیید کرده است."
                internet -> "شبکه اینترنت را اعلام می‌کند، اما اعتبارسنجی اندروید کامل نیست."
                else -> "شبکه فعلی دسترسی تأییدشده به اینترنت ندارد."
            }
            return NetworkSnapshot(transport, internet, validated, dns, v4, v6, message)
        }
    }
}
