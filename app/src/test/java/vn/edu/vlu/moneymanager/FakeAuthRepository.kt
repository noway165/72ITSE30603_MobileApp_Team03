package vn.edu.vlu.moneymanager

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.vlu.moneymanager.data.model.User
import vn.edu.vlu.moneymanager.data.repository.AuthRepository

/** In-memory accounts for JVM unit tests. The delay lets tests see the loading state; runTest skips it. */
class FakeAuthRepository : AuthRepository {

    private val accounts = mutableMapOf(DEMO_EMAIL to DEMO_PASSWORD)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Int? {
        delay(600)
        val key = email.lowercase()
        if (accounts[key] != password) return R.string.error_sign_in_failed
        _currentUser.value = User(uid = key, email = key, isFirebaseAccount = false)
        return null
    }

    override suspend fun register(email: String, password: String): Int? {
        delay(600)
        val key = email.lowercase()
        if (accounts.containsKey(key)) return R.string.error_email_taken
        accounts[key] = password
        _currentUser.value = User(uid = key, email = key, isFirebaseAccount = false)
        return null
    }

    override fun signOut() {
        _currentUser.value = null
    }

    companion object {
        const val DEMO_EMAIL = "demo@vlu.edu.vn"
        const val DEMO_PASSWORD = "demo1234"
    }
}
