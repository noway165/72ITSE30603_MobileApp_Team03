package vn.edu.vlu.moneymanager.data.repository

import kotlinx.coroutines.flow.StateFlow
import vn.edu.vlu.moneymanager.data.model.User

/** An interface so JVM unit tests can replace Firebase, which needs a device. */
interface AuthRepository {

    val currentUser: StateFlow<User?>

    /**
     * Returns null on success, otherwise the string resource id of the message to show.
     * Not marked @StringRes: a suspend function compiles to a method returning Object, which lint
     * rejects for that annotation.
     */
    suspend fun signIn(email: String, password: String): Int?

    /**
     * Returns null on success, otherwise the string resource id of the message to show
     * (R.string.error_email_taken when already registered). See signIn for why @StringRes is absent.
     */
    suspend fun register(email: String, password: String): Int?

    fun signOut()
}
