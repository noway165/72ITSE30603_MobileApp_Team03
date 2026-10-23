package vn.edu.vlu.moneymanager.feature.auth.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.AppContainer
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.source.SampleData
import vn.edu.vlu.moneymanager.util.Validators

data class LoginUiState(
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val demoEmail: String = SampleData.DEMO_EMAIL,
    val demoPassword: String = SampleData.DEMO_PASSWORD
)

sealed interface LoginEvent {
    data object SignedIn : LoginEvent
    data class ShowMessage(@param:StringRes val messageRes: Int) : LoginEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // One-off events (navigation, snackbar) must not be replayed after rotation, so they go through a Channel.
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    val isSignedIn: Boolean get() = authRepository.currentUser.value != null

    // Last text seen per field: after rotation the EditText sends the same text again, which must not clear an error.
    private var email = ""
    private var password = ""

    fun onEmailChanged(text: String) {
        if (text == email) return
        email = text
        _uiState.update { it.copy(emailError = null) }
    }

    fun onPasswordChanged(text: String) {
        if (text == password) return
        password = text
        _uiState.update { it.copy(passwordError = null) }
    }

    fun signIn(email: String, password: String) {
        val emailError = if (Validators.isValidEmail(email)) null else R.string.error_email_invalid
        val passwordError = if (password.isNotEmpty()) null else R.string.error_password_required
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = authRepository.signIn(email.trim(), password)
            _uiState.update { it.copy(isLoading = false) }
            _events.send(if (success) LoginEvent.SignedIn else LoginEvent.ShowMessage(R.string.error_sign_in_failed))
        }
    }
}
