package com.hoomanconnect.app.core

/** Contract for an independently licensed engine that owns packet forwarding for an established TUN. */
interface CoreAdapter {
    val id: String
    val displayName: String
    suspend fun validateConfiguration(configuration: String): CoreValidation
    /** Start the engine and forward packets between the supplied TUN file descriptor and remote transport. */
    suspend fun start(configuration: String, tunFileDescriptor: Int): CoreStartResult
    suspend fun stop()
}

data class CoreValidation(val valid: Boolean, val message: String)
data class CoreStartResult(val started: Boolean, val message: String)
enum class CoreAvailability { NOT_INSTALLED, READY, RUNNING, ERROR }
