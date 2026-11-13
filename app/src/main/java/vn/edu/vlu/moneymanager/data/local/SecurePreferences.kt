package vn.edu.vlu.moneymanager.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal const val SECURE_PREFS_FILE = "secure_settings"

/**
 * EncryptedSharedPreferences: keys are encrypted with AES-256-SIV and values with AES-256-GCM, under a master
 * key that never leaves the Android Keystore. androidx.security deprecated these APIs in favour of using the
 * Keystore directly; the course asks for EncryptedSharedPreferences, so the warnings are suppressed here only.
 */
@Suppress("DEPRECATION")
fun securePreferences(context: Context): SharedPreferences = try {
    openSecurePreferences(context)
} catch (e: Exception) {
    // The file can outlive its key (restored from a backup, Keystore cleared): start again with the defaults
    context.deleteSharedPreferences(SECURE_PREFS_FILE)
    openSecurePreferences(context)
}

@Suppress("DEPRECATION")
private fun openSecurePreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
