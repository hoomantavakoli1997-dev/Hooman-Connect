package com.hoomanconnect.app.profiles

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM encrypted local profile storage backed by Android Keystore. */
class EncryptedProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<ServerProfile> {
        val encrypted = prefs.getString(DATA, null) ?: return emptyList()
        return try {
            val plain = decrypt(Base64.decode(encrypted, Base64.NO_WRAP))
            val array = JSONArray(plain)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                ServerProfile(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    configJson = o.getString("config"),
                    createdAtEpochMs = o.getLong("createdAt")
                )
            }
        } catch (e: Exception) {
            // Fail closed: do not silently overwrite data that could not be decrypted.
            throw IllegalStateException("خواندن امن پروفایل‌ها ناموفق بود؛ داده‌ها دست‌نخورده باقی ماندند.", e)
        }
    }

    @Synchronized
    fun save(profile: ServerProfile) {
        val current = load().filterNot { it.id == profile.id } + profile
        persist(current)
    }

    @Synchronized
    fun delete(id: String) = persist(load().filterNot { it.id == id })

    private fun persist(profiles: List<ServerProfile>) {
        val array = JSONArray()
        profiles.forEach { p ->
            array.put(JSONObject().put("id", p.id).put("name", p.name)
                .put("config", p.configJson).put("createdAt", p.createdAtEpochMs))
        }
        val encrypted = Base64.encodeToString(encrypt(array.toString()), Base64.NO_WRAP)
        check(prefs.edit().putString(DATA, encrypted).commit()) { "ذخیره‌سازی پروفایل انجام نشد." }
    }

    private fun encrypt(plain: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(1 + iv.size + ciphertext.size).put(iv.size.toByte()).put(iv).put(ciphertext).array()
    }

    private fun decrypt(payload: ByteArray): String {
        val buffer = ByteBuffer.wrap(payload)
        val ivSize = buffer.get().toInt() and 0xff
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "داده رمزگذاری‌شده نامعتبر است." }
        val iv = ByteArray(ivSize).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }

    companion object {
        private const val PREFS = "hooman_secure_profiles"
        private const val DATA = "encrypted_profiles_v1"
        private const val KEY_ALIAS = "hooman_profile_aes_gcm_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
