package com.hoomanconnect.app.profiles

import org.json.JSONObject
import java.util.UUID

/** A local sing-box profile. Raw configuration is sensitive and must never be logged. */
data class ServerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val configJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class ConfigCheck(val valid: Boolean, val message: String)

/** Structural pre-check only; the real libbox engine must perform semantic validation. */
object SingBoxConfigCheck {
    fun check(raw: String): ConfigCheck = try {
        require(raw.isNotBlank()) { "فایل پیکربندی خالی است." }
        require(raw.toByteArray(Charsets.UTF_8).size <= 512 * 1024) { "حجم پیکربندی بیش از ۵۱۲ کیلوبایت است." }
        val root = JSONObject(raw)
        val inbounds = root.optJSONArray("inbounds")
        val outbounds = root.optJSONArray("outbounds")
        require(inbounds != null) { "بخش inbounds در پیکربندی وجود ندارد." }
        require(outbounds != null && outbounds.length() > 0) { "حداقل یک outbound لازم است." }
        ConfigCheck(true, "ساختار اولیه JSON قابل‌خواندن است؛ اعتبارسنجی نهایی با هسته انجام می‌شود.")
    } catch (e: Exception) {
        ConfigCheck(false, e.message ?: "ساختار JSON معتبر نیست.")
    }
}
