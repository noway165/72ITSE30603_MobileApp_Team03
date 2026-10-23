package vn.edu.vlu.moneymanager.feature.category.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.databinding.DialogCategoryBinding
import vn.edu.vlu.moneymanager.databinding.FragmentCategoryBinding
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryItem
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryUiState
import vn.edu.vlu.moneymanager.feature.category.viewmodel.CategoryViewModel
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = CategoryAdapter { item -> showCategoryDialog(item) }
        binding.rvCategories.adapter = adapter

        binding.tabsCategoryType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.selectType(if (tab.position == TAB_INCOME) TransactionType.INCOME else TransactionType.EXPENSE)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.fabAddCategory.setOnClickListener { showCategoryDialog(null) }

        binding.emptyCategories.ivEmptyIcon.setImageResource(R.drawable.ic_label)
        binding.emptyCategories.tvEmptyTitle.setText(R.string.empty_categories_title)
        binding.emptyCategories.tvEmptyMessage.setText(R.string.empty_categories_message)

        repeatOnViewStarted {
            launch { viewModel.uiState.collect { render(it, adapter) } }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: CategoryUiState, adapter: CategoryAdapter) {
        val tabIndex = if (state.selectedType == TransactionType.INCOME) TAB_INCOME else TAB_EXPENSE
        if (binding.tabsCategoryType.selectedTabPosition != tabIndex) {
            binding.tabsCategoryType.selectTab(binding.tabsCategoryType.getTabAt(tabIndex))
        }
        adapter.submitList(state.items)
        binding.rvCategories.isVisible = state.items.isNotEmpty()
        binding.emptyCategories.root.isVisible = state.items.isEmpty()
    }

    /** Add ([editing] == null) or edit a category. The color is read from the android:tag of each chip. */
    private fun showCategoryDialog(editing: CategoryItem?) {
        val dialogBinding = DialogCategoryBinding.inflate(layoutInflater)
        val colorChips = dialogBinding.chipGroupColors.children.filterIsInstance<Chip>().toList()
        val type = editing?.category?.type ?: viewModel.uiState.value.selectedType

        editing?.category?.let { category ->
            dialogBinding.edtCategoryName.setText(category.name)
            colorChips.firstOrNull { (it.tag as? String).equals(category.colorHex, ignoreCase = true) }?.isChecked = true
        }
        dialogBinding.toggleCategoryType.check(
            if (type == TransactionType.INCOME) R.id.btnCategoryIncome else R.id.btnCategoryExpense
        )
        dialogBinding.edtCategoryName.doAfterTextChanged { dialogBinding.tilCategoryName.error = null }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editing == null) R.string.title_add_category else R.string.title_edit_category)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
        if (editing != null) {
            builder.setNeutralButton(R.string.action_delete) { _, _ -> confirmDelete(editing) }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val selectedType = if (dialogBinding.toggleCategoryType.checkedButtonId == R.id.btnCategoryIncome) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                val colorHex = colorChips.firstOrNull { it.isChecked }?.tag as? String ?: DEFAULT_COLOR
                val error = viewModel.saveCategory(
                    editing?.category?.id,
                    dialogBinding.edtCategoryName.text.toString(),
                    selectedType,
                    colorHex
                )
                dialogBinding.tilCategoryName.error = error?.let(::getString)
                if (error == null) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(item: CategoryItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.confirm_delete_category, item.category.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteCategory(item.category.id)
                Snackbar.make(binding.root, R.string.category_deleted, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fabAddCategory)
                    .show()
            }
            .show()
    }

    private companion object {
        const val TAB_EXPENSE = 0
        const val TAB_INCOME = 1
        const val DEFAULT_COLOR = "#EF4444"
    }
}
