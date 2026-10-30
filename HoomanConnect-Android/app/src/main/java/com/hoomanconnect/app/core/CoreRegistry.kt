package com.hoomanconnect.app.core

/** Registry intentionally starts empty until a core's provenance, license and API are verified. */
class CoreRegistry(adapters: List<CoreAdapter> = emptyList()) {
    private val registered = adapters.associateBy { it.id }
    fun all(): List<CoreAdapter> = registered.values.toList()
    fun find(id: String): CoreAdapter? = registered[id]
}
