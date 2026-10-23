package vn.edu.vlu.moneymanager.feature.auth.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.MainActivity
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.ActivityRegisterBinding
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.PasswordStrength
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.RegisterEvent
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.RegisterUiState
import vn.edu.vlu.moneymanager.feature.auth.viewmodel.RegisterViewModel
import vn.edu.vlu.moneymanager.util.applySystemBarsPadding
import vn.edu.vlu.moneymanager.util.repeatOnStarted

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // The AppBarLayout already pads for the status bar; only the bottom needs the navigation bar inset.
        binding.root.applySystemBarsPadding(top = false)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnGoSignIn.setOnClickListener { finish() }
        binding.edtEmail.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
        binding.edtPassword.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
        binding.edtConfirmPassword.doAfterTextChanged { viewModel.onConfirmChanged(it.toString()) }
        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.edtEmail.text.toString(),
                binding.edtPassword.text.toString(),
                binding.edtConfirmPassword.text.toString()
            )
        }

        repeatOnStarted {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    private fun render(state: RegisterUiState) {
        binding.tilEmail.error = state.emailError?.let(::getString)
        binding.tilPassword.error = state.passwordError?.let(::getString)
        binding.tilConfirmPassword.error = state.confirmError?.let(::getString)

        val strengthColor = ContextCompat.getColor(
            this,
            when (state.strength) {
                PasswordStrength.WEAK -> R.color.expense
                PasswordStrength.MEDIUM -> R.color.warning
                PasswordStrength.STRONG -> R.color.income
            }
        )
        binding.progressStrength.setProgressCompat(state.strength.progress, true)
        binding.progressStrength.setIndicatorColor(strengthColor)
        binding.tvStrength.setText(state.strength.labelRes)
        binding.tvStrength.setTextColor(strengthColor)

        binding.progressRegister.isVisible = state.isLoading
        binding.btnRegister.isEnabled = !state.isLoading
    }

    private fun handleEvent(event: RegisterEvent) {
        when (event) {
            RegisterEvent.Registered -> {
                // Clear Login + Register from the back stack so Back from Main closes the app.
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            is RegisterEvent.ShowMessage -> Snackbar.make(binding.root, event.messageRes, Snackbar.LENGTH_LONG).show()
        }
    }
}
