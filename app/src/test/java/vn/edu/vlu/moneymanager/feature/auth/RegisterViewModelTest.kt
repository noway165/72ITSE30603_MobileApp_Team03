package vn.edu.vlu.moneymanager.feature.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.vlu.moneymanager.MainDispatcherRule
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.source.SampleData
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.PasswordStrength
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.RegisterEvent
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.RegisterViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = AuthRepository()

    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy { RegisterViewModel(authRepository) }

    private fun strengthOf(password: String): PasswordStrength {
        viewModel.onPasswordChanged(password)
        return viewModel.uiState.value.strength
    }

    // ---- password strength ----

    @Test
    fun strength_startsWeak() = runTest {
        assertEquals(PasswordStrength.WEAK, viewModel.uiState.value.strength)
    }

    @Test
    fun strength_breaksTheRule_isWeak() = runTest {
        assertEquals(PasswordStrength.WEAK, strengthOf("abc12"))        // too short
        assertEquals(PasswordStrength.WEAK, strengthOf("abcdefghij"))   // no digit
        assertEquals(PasswordStrength.WEAK, strengthOf("1234567890"))   // no letter
    }

    @Test
    fun strength_meetsTheRule_isMedium() = runTest {
        assertEquals(PasswordStrength.MEDIUM, strengthOf("abcd1234"))
        // 12+ characters but only lowercase letters and digits
        assertEquals(PasswordStrength.MEDIUM, strengthOf("abcdefgh1234"))
    }

    @Test
    fun strength_longWithSymbolOrUppercase_isStrong() = runTest {
        assertEquals(PasswordStrength.STRONG, strengthOf("abcdefgh123!"))
        assertEquals(PasswordStrength.STRONG, strengthOf("Abcdefgh1234"))
    }

    @Test
    fun strength_progressGrowsWithStrength() {
        assertTrue(PasswordStrength.WEAK.progress < PasswordStrength.MEDIUM.progress)
        assertTrue(PasswordStrength.MEDIUM.progress < PasswordStrength.STRONG.progress)
        assertEquals(100, PasswordStrength.STRONG.progress)
    }

    // ---- validation ----

    @Test
    fun register_invalidEmail_showsEmailError() = runTest {
        viewModel.register("lam@", "abcd1234", "abcd1234")
        advanceUntilIdle()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun register_weakPassword_showsRuleError() = runTest {
        viewModel.register("lam@vlu.edu.vn", "abc", "abc")
        advanceUntilIdle()

        assertEquals(R.string.error_password_rule, viewModel.uiState.value.passwordError)
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun register_confirmDoesNotMatch_showsMismatchError() = runTest {
        viewModel.register("lam@vlu.edu.vn", "abcd1234", "abcd12345")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(R.string.error_password_mismatch, viewModel.uiState.value.confirmError)
        assertNull(authRepository.currentUser.value)
    }

    // ---- register ----

    @Test
    fun register_newAccount_sendsRegisteredAndSignsIn() = runTest {
        viewModel.register("lam@vlu.edu.vn", "abcd1234", "abcd1234")

        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(RegisterEvent.Registered, viewModel.events.first())
        assertEquals("lam@vlu.edu.vn", authRepository.currentUser.value?.email)
    }

    @Test
    fun register_emailAlreadyUsed_showsEmailTakenOnTheEmailField() = runTest {
        viewModel.register(SampleData.DEMO_EMAIL, "abcd1234", "abcd1234")
        advanceUntilIdle()

        assertEquals(R.string.error_email_taken, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(authRepository.currentUser.value)
    }

    // ---- clearing errors while typing ----

    @Test
    fun onConfirmChanged_newText_clearsMismatch_sameTextKeepsIt() = runTest {
        viewModel.onConfirmChanged("abcd12345")
        viewModel.register("lam@vlu.edu.vn", "abcd1234", "abcd12345")

        viewModel.onConfirmChanged("abcd12345") // same text, e.g. after rotation
        assertEquals(R.string.error_password_mismatch, viewModel.uiState.value.confirmError)

        viewModel.onConfirmChanged("abcd1234")
        assertNull(viewModel.uiState.value.confirmError)
    }

    @Test
    fun onEmailChanged_newText_clearsEmailError() = runTest {
        viewModel.register("lam@", "abcd1234", "abcd1234")

        viewModel.onEmailChanged("lam@v")

        assertNull(viewModel.uiState.value.emailError)
    }
}
