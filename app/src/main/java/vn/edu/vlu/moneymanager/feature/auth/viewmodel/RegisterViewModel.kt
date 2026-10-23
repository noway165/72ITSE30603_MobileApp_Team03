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
import vn.edu.vlu.moneymanager.util.Validators

enum class PasswordStrength(@param:StringRes val labelRes: Int, val progress: Int) {
    WEAK(R.string.password_weak, 33),
    MEDIUM(R.string.password_medium, 66),
    STRONG(R.string.password_strong, 100)
}

data class RegisterUiState(
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmError: Int? = null,
    val strength: PasswordStrength = PasswordStrength.WEAK,
    val isLoading: Boolean = false
)

sealed interface RegisterEvent {
    data object Registered : RegisterEvent
    data class ShowMessage(@param:StringRes val messageRes: Int) : RegisterEvent
}

class RegisterViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    // Last text seen per field: after rotation the EditText sends the same text again, which must not clear an error.
    private var email = ""
    private var password = ""
    private var confirm = ""

    fun onEmailChanged(text: String) {
        if (text == email) return
        email = text
        _uiState.update { it.copy(emailError = null) }
    }

    fun onPasswordChanged(text: String) {
        if (text == password) return
        password = text
        _uiState.update { it.copy(passwordError = null, strength = strengthOf(text)) }
    }

    fun onConfirmChanged(text: String) {
        if (text == confirm) return
        confirm = text
        _uiState.update { it.copy(confirmError = null) }
    }

    fun register(email: String, password: String, confirm: String) {
        val emailError = if (Validators.isValidEmail(email)) null else R.string.error_email_invalid
        val passwordError = if (Validators.isValidPassword(password)) null else R.string.error_password_rule
        val confirmError = if (confirm == password) null else R.string.error_password_mismatch
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError) }
        if (emailError != null || passwordError != null || confirmError != null || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = authRepository.register(email.trim(), password)
            _uiState.update { it.copy(isLoading = false) }
            if (success) {
                _events.send(RegisterEvent.Registered)
            } else {
                _uiState.update { it.copy(emailError = R.string.error_email_taken) }
            }
        }
    }

    private fun strengthOf(password: String): PasswordStrength = when {
        !Validators.isValidPassword(password) -> PasswordStrength.WEAK
        password.length >= 12 && (password.any { !it.isLetterOrDigit() } || password.any { it.isUpperCase() }) ->
            PasswordStrength.STRONG
        else -> PasswordStrength.MEDIUM
    }
}
