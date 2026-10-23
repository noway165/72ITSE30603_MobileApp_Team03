package vn.edu.vlu.moneymanager.feature.transaction.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.databinding.ActivityAddEditTransactionBinding
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionEvent
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionUiState
import vn.edu.vlu.moneymanager.feature.transaction.viewmodel.AddEditTransactionViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.DateUtils
import vn.edu.vlu.moneymanager.util.applySystemBarsPadding
import vn.edu.vlu.moneymanager.util.repeatOnStarted

class AddEditTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTransactionBinding
    private val viewModel: AddEditTransactionViewModel by viewModels()

    /** Last lists given to the dropdowns, so the adapters are only rebuilt when the data really changes. */
    private var shownCategories: List<Category> = emptyList()
    private var shownCurrencies: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = false)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_delete) confirmDelete()
            item.itemId == R.id.action_delete
        }
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.onTypeSelected(if (checkedId == R.id.btnTypeIncome) TransactionType.INCOME else TransactionType.EXPENSE)
        }
        binding.edtAmount.doAfterTextChanged { viewModel.onAmountChanged(it.toString()) }
        binding.edtNote.doAfterTextChanged { viewModel.onNoteChanged(it.toString()) }
        binding.actvCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCurrencySelected(shownCurrencies[position])
        }
        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCategorySelected(shownCategories[position].id)
        }
        binding.edtDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { viewModel.save() }

        // The picker is a DialogFragment: after rotation it is restored by the FragmentManager, re-attach the listener.
        (supportFragmentManager.findFragmentByTag(TAG_DATE_PICKER) as? MaterialDatePicker<*>)?.let(::listenToDatePicker)

        repeatOnStarted {
            launch { viewModel.uiState.collect(::render) }
            launch { viewModel.events.collect(::handleEvent) }
        }
    }

    private fun render(state: AddEditTransactionUiState) {
        val form = state.form
        binding.toolbar.setTitle(if (state.isEditMode) R.string.title_edit_transaction else R.string.title_add_transaction)
        binding.toolbar.menu.findItem(R.id.action_delete)?.isVisible = state.isEditMode

        val typeButton = if (form.type == TransactionType.INCOME) R.id.btnTypeIncome else R.id.btnTypeExpense
        if (binding.toggleType.checkedButtonId != typeButton) binding.toggleType.check(typeButton)

        // Only write text the user did not type themselves, otherwise the cursor jumps.
        if (binding.edtAmount.text.toString() != form.amount) binding.edtAmount.setText(form.amount)
        if (binding.edtNote.text.toString() != form.note) binding.edtNote.setText(form.note)

        if (state.currencies != shownCurrencies) {
            shownCurrencies = state.currencies
            binding.actvCurrency.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, shownCurrencies))
        }
        binding.actvCurrency.setText(form.currency, false)

        if (state.categories != shownCategories) {
            shownCategories = state.categories
            binding.actvCategory.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_list_item_1, shownCategories.map { it.name })
            )
        }
        binding.actvCategory.setText(state.selectedCategory?.name.orEmpty(), false)

        binding.edtDate.setText(DateFormatter.date(form.dateMillis))
        binding.tilAmount.error = form.amountError?.let(::getString)
        binding.tilCategory.error = form.categoryError?.let(::getString)
    }

    private fun handleEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.Finished -> {
                Toast.makeText(this, event.messageRes, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDatePicker() {
        if (supportFragmentManager.findFragmentByTag(TAG_DATE_PICKER) != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.hint_date)
            .setSelection(DateUtils.toPickerUtc(viewModel.uiState.value.form.dateMillis))
            .build()
        listenToDatePicker(picker)
        picker.show(supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun listenToDatePicker(picker: MaterialDatePicker<*>) {
        picker.addOnPositiveButtonClickListener { selection ->
            (selection as? Long)?.let(viewModel::onDatePicked)
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.confirm_delete_transaction)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    companion object {
        private const val TAG_DATE_PICKER = "transaction_date_picker"

        /** [transactionId] null = add mode. The extra is read by the ViewModel through SavedStateHandle. */
        fun newIntent(context: Context, transactionId: Long? = null): Intent =
            Intent(context, AddEditTransactionActivity::class.java).apply {
                transactionId?.let { putExtra(AddEditTransactionViewModel.EXTRA_TRANSACTION_ID, it) }
            }
    }
}
