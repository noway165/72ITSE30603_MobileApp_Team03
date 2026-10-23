package vn.edu.vlu.moneymanager.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.model.User
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

data class ProfileUiState(
    val user: User? = null,
    val defaultCurrency: String = "",
    val currencies: List<String> = emptyList(),
    /** null = follow the system setting */
    val darkMode: Boolean? = null,
    val lastSyncMillis: Long? = null,
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false
)

sealed interface ProfileEvent {
    data object SyncDone : ProfileEvent
    data object SignedOut : ProfileEvent
}

class ProfileViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository,
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    currencyRepository: CurrencyRepository = AppContainer.currencyRepository
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        combine(settingsRepository.defaultCurrency, settingsRepository.darkMode, settingsRepository.lastSyncMillis, ::Triple),
        transactionRepository.transactions,
        isSyncing
    ) { user, (currency, darkMode, lastSync), transactions, syncing ->
        ProfileUiState(
            user = user,
            defaultCurrency = currency,
            currencies = currencyRepository.currencies,
            darkMode = darkMode,
            lastSyncMillis = lastSync,
            pendingSyncCount = transactions.count { it.pendingSync },
            isSyncing = syncing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setDefaultCurrency(code: String) = settingsRepository.setDefaultCurrency(code)

    fun setDarkMode(enabled: Boolean) = settingsRepository.setDarkMode(enabled)

    /** Simulated upload of pending changes; MockAPI sync comes in a later milestone. */
    fun syncNow() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            delay(SIMULATED_SYNC_MS)
            transactionRepository.markAllSynced()
            settingsRepository.setLastSync(System.currentTimeMillis())
            isSyncing.value = false
            _events.send(ProfileEvent.SyncDone)
        }
    }

    fun signOut() {
        authRepository.signOut()
        viewModelScope.launch { _events.send(ProfileEvent.SignedOut) }
    }

    private companion object {
        const val SIMULATED_SYNC_MS = 1_200L
    }
}
