package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.vlu.moneymanager.data.model.User
import vn.edu.vlu.moneymanager.data.source.SampleData

/** Demo authentication: accounts live in memory until Firebase Auth is added. */
class AuthRepository {

    private val accounts = mutableMapOf(SampleData.DEMO_EMAIL to SampleData.DEMO_PASSWORD)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun signIn(email: String, password: String): Boolean {
        delay(SIMULATED_NETWORK_DELAY_MS)
        val key = email.lowercase()
        if (accounts[key] != password) return false
        _currentUser.value = User(key, isFirebaseAccount = false)
        return true
    }

    /** Returns false when the email is already registered. */
    suspend fun register(email: String, password: String): Boolean {
        delay(SIMULATED_NETWORK_DELAY_MS)
        val key = email.lowercase()
        if (accounts.containsKey(key)) return false
        accounts[key] = password
        _currentUser.value = User(key, isFirebaseAccount = false)
        return true
    }

    fun signOut() {
        _currentUser.value = null
    }

    private companion object {
        const val SIMULATED_NETWORK_DELAY_MS = 600L
    }
}
