package vn.edu.vlu.moneymanager.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.User

/** Firebase Email/Password. Firebase stores the session itself, so currentUser survives app restarts. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Int? =
        authenticate { auth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun register(email: String, password: String): Int? =
        authenticate { auth.createUserWithEmailAndPassword(email, password).await() }

    override fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private suspend fun authenticate(call: suspend () -> AuthResult): Int? = try {
        _currentUser.value = call().user?.toUser()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthUserCollisionException) {
        R.string.error_email_taken
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        R.string.error_sign_in_failed
    } catch (e: FirebaseAuthInvalidUserException) {
        R.string.error_sign_in_failed
    } catch (e: FirebaseNetworkException) {
        R.string.error_network_offline
    } catch (e: Exception) {
        R.string.error_network_unknown
    }

    private fun FirebaseUser.toUser() = User(uid = uid, email = email.orEmpty(), isFirebaseAccount = true)
}
