package com.vera.guard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted key/value storage (Android Keystore-backed AES) for the profile, contacts, settings and PINs. */
@Singleton
class SecureStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val prefs: SharedPreferences by lazy {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "vera_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getString(k: String): String? = prefs.getString(k, null)
    fun putString(k: String, v: String?) = prefs.edit().apply { if (v == null) remove(k) else putString(k, v) }.apply()
    fun getBool(k: String, d: Boolean = false) = prefs.getBoolean(k, d)
    fun putBool(k: String, v: Boolean) = prefs.edit().putBoolean(k, v).apply()
    fun getLong(k: String, d: Long = 0L) = prefs.getLong(k, d)
    fun putLong(k: String, v: Long) = prefs.edit().putLong(k, v).apply()
    fun clearAll() = prefs.edit().clear().apply()

    /** Random id used by the backend for the caregiver dashboard and "delete all my data". Acts as a secret. */
    fun ownerId(): String = getString("ownerId") ?: (UUID.randomUUID().toString() + UUID.randomUUID().toString().take(8)).also { putString("ownerId", it) }

    /** Passphrase for the SQLCipher database. Generated once, protected by the encrypted prefs. */
    fun dbPassphrase(): String = getString("dbPass") ?: ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }.also { putString("dbPass", it) }

    // ---- PINs (salted SHA-256; the cancel PIN stops everything, the duress PIN fakes a cancel) ----
    private fun hash(pin: String, salt: String) = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
    fun setPin(kind: String, pin: String?) {
        if (pin.isNullOrEmpty()) { putString("pin_$kind", null); putString("pinsalt_$kind", null); return }
        val salt = UUID.randomUUID().toString()
        putString("pinsalt_$kind", salt); putString("pin_$kind", hash(pin, salt))
    }
    fun hasPin(kind: String) = getString("pin_$kind") != null
    fun checkPin(kind: String, pin: String): Boolean {
        val h = getString("pin_$kind") ?: return false
        return hash(pin, getString("pinsalt_$kind") ?: "") == h
    }
}
