package com.filelens.commander.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed store for remote-server passwords and any other small
 * per-connection secrets. Backed by [EncryptedSharedPreferences] with an
 * AES-GCM master key in the Android Keystore, so values are encrypted at
 * rest and can only be decrypted on this device.
 *
 * If the encrypted store fails to initialise (a known issue on some OEM
 * Keystore implementations after factory reset), we fall back to a plain
 * SharedPreferences file so the app still works — secrets are then no
 * better protected than DataStore, but we surface no crash to the user.
 */
@Singleton
class SecretsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { openPrefs() }

    fun setPassword(connectionId: String, password: String) {
        prefs.edit().putString(passwordKey(connectionId), password).apply()
    }

    fun getPassword(connectionId: String): String =
        prefs.getString(passwordKey(connectionId), null).orEmpty()

    fun setHostKey(connectionId: String, hostKey: String) {
        prefs.edit().putString(hostKeyKey(connectionId), hostKey).apply()
    }

    fun getHostKey(connectionId: String): String =
        prefs.getString(hostKeyKey(connectionId), null).orEmpty()

    fun clear(connectionId: String) {
        prefs.edit()
            .remove(passwordKey(connectionId))
            .remove(hostKeyKey(connectionId))
            .apply()
    }

    private fun passwordKey(id: String) = "pwd:$id"
    private fun hostKeyKey(id: String) = "hostkey:$id"

    private fun openPrefs(): SharedPreferences {
        return try {
            val key = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_FILE,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", t)
            context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val TAG = "SecretsRepository"
        const val ENCRYPTED_FILE = "filelens_secrets"
        const val FALLBACK_FILE = "filelens_secrets_plain"
    }
}
