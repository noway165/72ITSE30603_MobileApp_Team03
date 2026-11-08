package vn.edu.vlu.moneymanager.data.remote

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import vn.edu.vlu.moneymanager.R
import java.io.IOException
import java.net.SocketTimeoutException

/** The three states of every networked screen. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(@param:StringRes val messageRes: Int) : UiState<Nothing>
}

/**
 * Runs a network call on Dispatchers.IO and turns any failure into [UiState.Error], so a timeout,
 * Airplane Mode, an HTTP error or bad JSON shows a banner instead of crashing the app.
 */
suspend fun <T> apiCall(block: suspend () -> T): UiState<T> = withContext(Dispatchers.IO) {
    try {
        UiState.Success(block())
    } catch (e: CancellationException) {
        throw e // leaving the screen is not an error
    } catch (e: Exception) {
        UiState.Error(e.toErrorMessage())
    }
}

@StringRes
fun Throwable.toErrorMessage(): Int = when (this) {
    is SocketTimeoutException -> R.string.error_network_timeout
    is IOException -> R.string.error_network_offline
    is HttpException -> when (code()) {
        400 -> R.string.error_http_bad_request
        404 -> R.string.error_http_not_found
        in 500..599 -> R.string.error_http_server
        else -> R.string.error_network_unknown
    }
    else -> R.string.error_network_unknown
}
