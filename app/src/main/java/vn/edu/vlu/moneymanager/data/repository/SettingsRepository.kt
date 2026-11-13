package vn.edu.vlu.moneymanager.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.vlu.moneymanager.data.source.SampleData

/**
 * User preferences. In the app [prefs] is EncryptedSharedPreferences (see SecurePreferences), so the values
 * survive restarts and are encrypted on disk; null keeps them in memory only, for JVM unit tests.
 * darkMode == null means "follow the system".
 */
class SettingsRepository(private val prefs: SharedPreferences? = null) {

    private val _defaultCurrency = MutableStateFlow(
        prefs?.getString(KEY_DEFAULT_CURRENCY, null) ?: SampleData.DEFAULT_CURRENCY
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _darkMode = MutableStateFlow<Boolean?>(
        prefs?.takeIf { it.contains(KEY_DARK_MODE) }?.getBoolean(KEY_DARK_MODE, false)
    )
    val darkMode: StateFlow<Boolean?> = _darkMode.asStateFlow()

    private val _lastSyncMillis = MutableStateFlow<Long?>(
        prefs?.takeIf { it.contains(KEY_LAST_SYNC) }?.getLong(KEY_LAST_SYNC, 0L)
    )
    val lastSyncMillis: StateFlow<Long?> = _lastSyncMillis.asStateFlow()

    fun setDefaultCurrency(code: String) {
        _defaultCurrency.value = code
        prefs?.edit { putString(KEY_DEFAULT_CURRENCY, code) }
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs?.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    fun setLastSync(millis: Long) {
        _lastSyncMillis.value = millis
        prefs?.edit { putLong(KEY_LAST_SYNC, millis) }
    }

    private companion object {
        const val KEY_DEFAULT_CURRENCY = "default_currency"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LAST_SYNC = "last_sync_millis"
    }
}
