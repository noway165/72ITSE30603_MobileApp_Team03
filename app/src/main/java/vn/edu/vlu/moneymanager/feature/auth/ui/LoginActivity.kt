package vn.edu.vlu.moneymanager.feature.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.MainActivity
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.ActivityLoginBinding
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.LoginEvent
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.LoginUiState
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.LoginViewModel
import vn.edu.vlu.moneymanager.util.applySystemBarsPadding
import vn.edu.vlu.moneymanager.util.repeatOnStarted

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel.isSignedIn) {
            openMain()
            return
        }
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.edtEmail.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
        binding.edtPassword.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
        binding.edtPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            actionId == EditorInfo.IME_ACTION_DONE
        }
        binding.btnSignIn.setOnClickListener { submit() }
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        repeatOnStarted {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    private fun submit() {
        viewModel.signIn(binding.edtEmail.text.toString(), binding.edtPassword.text.toString())
    }

    private fun render(state: LoginUiState) {
        binding.tilEmail.error = state.emailError?.let(::getString)
        binding.tilPassword.error = state.passwordError?.let(::getString)
        binding.progressSignIn.isVisible = state.isLoading
        binding.btnSignIn.isEnabled = !state.isLoading
        binding.tvAuthMode.isVisible = true
        binding.tvAuthMode.text = getString(R.string.auth_demo_mode_with_account, state.demoEmail, state.demoPassword)
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.SignedIn -> openMain()
            is LoginEvent.ShowMessage -> Snackbar.make(binding.root, event.messageRes, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
