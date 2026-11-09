package vn.edu.vlu.moneymanager.data.repository

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.StateFlow
import vn.edu.vlu.moneymanager.data.model.User

/** An interface so JVM unit tests can replace Firebase, which needs a device. */
interface AuthRepository {

    val currentUser: StateFlow<User?>

    /** Returns null on success, otherwise the message to show. */
    @StringRes
    suspend fun signIn(email: String, password: String): Int?

    /** Returns null on success, otherwise the message to show (R.string.error_email_taken when already registered). */
    @StringRes
    suspend fun register(email: String, password: String): Int?

    fun signOut()
}
