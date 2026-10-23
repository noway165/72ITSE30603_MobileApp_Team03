package vn.edu.vlu.moneymanager.feature.profile.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.BuildConfig
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.FragmentProfileBinding
import vn.edu.vlu.moneymanager.feature.auth.ui.LoginActivity
import vn.edu.vlu.moneymanager.feature.profile.viewmodel.ProfileEvent
import vn.edu.vlu.moneymanager.feature.profile.viewmodel.ProfileUiState
import vn.edu.vlu.moneymanager.feature.profile.viewmodel.ProfileViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var shownCurrencies: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shownCurrencies = emptyList()

        binding.actvDefaultCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDefaultCurrency(shownCurrencies[position])
        }
        binding.switchDarkMode.setOnCheckedChangeListener { button, isChecked ->
            // Ignore the calls made by render() itself; react only to the user's tap.
            if (button.isPressed) viewModel.setDarkMode(isChecked)
        }
        binding.rowCategories.setOnClickListener { findNavController().navigate(R.id.action_profile_to_categories) }
        binding.rowCurrency.setOnClickListener { findNavController().navigate(R.id.action_profile_to_currency) }
        binding.rowSync.setOnClickListener { viewModel.syncNow() }
        binding.btnSignOut.setOnClickListener { confirmSignOut() }
        binding.tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        repeatOnViewStarted {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: ProfileUiState) {
        val email = state.user?.email.orEmpty()
        binding.tvProfileEmail.text = email
        binding.tvAvatarInitial.text = email.firstOrNull()?.uppercase() ?: "?"
        binding.chipAccountType.setText(
            if (state.user?.isFirebaseAccount == true) R.string.account_type_firebase else R.string.account_type_local
        )

        if (state.currencies != shownCurrencies) {
            shownCurrencies = state.currencies
            binding.actvDefaultCurrency.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, shownCurrencies)
            )
        }
        binding.actvDefaultCurrency.setText(state.defaultCurrency, false)

        val darkMode = state.darkMode ?: isSystemInDarkMode()
        binding.switchDarkMode.isChecked = darkMode
        state.darkMode?.let(::applyNightMode)

        binding.tvLastSync.text = state.lastSyncMillis?.let { getString(R.string.last_sync, DateFormatter.dateTime(it)) }
            ?: getString(R.string.last_sync_never)
        binding.progressSync.isVisible = state.isSyncing
        binding.rowSync.isEnabled = !state.isSyncing
    }

    private fun handleEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.SyncDone -> Snackbar.make(binding.root, R.string.sync_done, Snackbar.LENGTH_SHORT).show()
            ProfileEvent.SignedOut -> {
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.confirm_sign_out)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_sign_out) { _, _ -> viewModel.signOut() }
            .show()
    }

    /** Applying the mode recreates the Activity, so only call it when the mode really changes. */
    private fun applyNightMode(enabled: Boolean) {
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isSystemInDarkMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
