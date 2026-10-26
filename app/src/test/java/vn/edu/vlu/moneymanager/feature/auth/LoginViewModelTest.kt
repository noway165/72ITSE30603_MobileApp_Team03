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
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.LoginEvent
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.LoginViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = AuthRepository()

    // lazy: created inside the test body, after MainDispatcherRule has replaced Dispatchers.Main
    private val viewModel by lazy { LoginViewModel(authRepository) }

    // ---- validation ----

    @Test
    fun signIn_invalidEmail_showsEmailErrorAndDoesNotCallRepository() = runTest {
        viewModel.signIn("not-an-email", SampleData.DEMO_PASSWORD)
        advanceUntilIdle()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun signIn_emptyPassword_showsPasswordRequired() = runTest {
        viewModel.signIn(SampleData.DEMO_EMAIL, "")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.emailError)
        assertEquals(R.string.error_password_required, viewModel.uiState.value.passwordError)
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun signIn_bothFieldsInvalid_showsBothErrors() = runTest {
        viewModel.signIn("", "")

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        assertEquals(R.string.error_password_required, viewModel.uiState.value.passwordError)
    }

    // ---- sign in ----

    @Test
    fun signIn_demoAccount_showsLoadingThenSendsSignedIn() = runTest {
        viewModel.signIn(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD)

        // The repository is still "on the network" (simulated delay)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
        assertTrue(viewModel.isSignedIn)
    }

    @Test
    fun signIn_emailWithSpaces_isTrimmedBeforeSignIn() = runTest {
        viewModel.signIn("  ${SampleData.DEMO_EMAIL}  ", SampleData.DEMO_PASSWORD)
        advanceUntilIdle()

        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
    }

    @Test
    fun signIn_wrongPassword_sendsErrorMessage() = runTest {
        viewModel.signIn(SampleData.DEMO_EMAIL, "wrong-password1")
        advanceUntilIdle()

        assertEquals(LoginEvent.ShowMessage(R.string.error_sign_in_failed), viewModel.events.first())
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.isSignedIn)
    }

    @Test
    fun isSignedIn_followsRepository() = runTest {
        assertFalse(viewModel.isSignedIn)

        authRepository.signIn(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD)

        assertTrue(viewModel.isSignedIn)
    }

    // ---- clearing errors while typing ----

    @Test
    fun onEmailChanged_newText_clearsEmailError() = runTest {
        viewModel.signIn("bad", SampleData.DEMO_PASSWORD)

        viewModel.onEmailChanged("bad@")

        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun onEmailChanged_sameTextAgain_keepsError() = runTest {
        // After rotation the restored EditText sends the text it already had
        viewModel.onEmailChanged("bad")
        viewModel.signIn("bad", SampleData.DEMO_PASSWORD)

        viewModel.onEmailChanged("bad")

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
    }

    @Test
    fun onPasswordChanged_newText_clearsPasswordError() = runTest {
        viewModel.signIn(SampleData.DEMO_EMAIL, "")

        viewModel.onPasswordChanged("d")

        assertNull(viewModel.uiState.value.passwordError)
    }
}
