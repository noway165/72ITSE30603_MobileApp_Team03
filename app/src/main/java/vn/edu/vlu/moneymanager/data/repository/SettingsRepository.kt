package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.vlu.moneymanager.data.source.SampleData

/** User preferences (DataStore later). darkMode == null means "follow the system". */
class SettingsRepository {

    private val _defaultCurrency = MutableStateFlow(SampleData.DEFAULT_CURRENCY)
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _darkMode = MutableStateFlow<Boolean?>(null)
    val darkMode: StateFlow<Boolean?> = _darkMode.asStateFlow()

    private val _lastSyncMillis = MutableStateFlow<Long?>(null)
    val lastSyncMillis: StateFlow<Long?> = _lastSyncMillis.asStateFlow()

    fun setDefaultCurrency(code: String) {
        _defaultCurrency.value = code
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
    }

    fun setLastSync(millis: Long) {
        _lastSyncMillis.value = millis
    }
}
