package com.hoomanconnect.app.vpn

enum class VpnSessionState { DISCONNECTED, AUTHORIZATION_REQUIRED, PREPARING, CONNECTED, STOPPING, ERROR }

data class VpnSessionSnapshot(
    val state: VpnSessionState = VpnSessionState.DISCONNECTED,
    val selectedCoreId: String? = null,
    val message: String = "هسته VPN هنوز نصب و تأیید نشده است"
)
