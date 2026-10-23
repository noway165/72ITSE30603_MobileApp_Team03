package vn.edu.vlu.moneymanager.feature.report.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.R
import vn.edu.vlu.moneymanager.databinding.FragmentReportBinding
import vn.edu.vlu.moneymanager.feature.report.viewmodel.ReportUiState
import vn.edu.vlu.moneymanager.feature.report.viewmodel.ReportViewModel
import vn.edu.vlu.moneymanager.util.DateFormatter
import vn.edu.vlu.moneymanager.util.MoneyFormatter
import vn.edu.vlu.moneymanager.util.repeatOnViewStarted

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.monthSelector.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.monthSelector.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        setUpCharts()

        repeatOnViewStarted {
            launch { viewModel.uiState.collect(::render) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: ReportUiState) {
        binding.monthSelector.tvMonthTitle.text = DateFormatter.monthTitle(state.month)
        binding.tvReportIncome.text = MoneyFormatter.format(state.income, state.currency)
        binding.tvReportExpense.text = MoneyFormatter.format(state.expense, state.currency)
        binding.tvReportMissingRates.isVisible = state.hasMissingRates
        renderPie(state)
        renderBars(state)
    }

    private fun renderPie(state: ReportUiState) {
        val hasData = state.slices.isNotEmpty()
        binding.pieChart.isVisible = hasData
        binding.tvPieEmpty.isVisible = !hasData
        if (!hasData) return

        val uncategorized = getString(R.string.uncategorized)
        val outline = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutline)
        val dataSet = PieDataSet(
            state.slices.map { PieEntry(it.amount.toFloat(), it.categoryName ?: uncategorized) },
            ""
        ).apply {
            colors = state.slices.map { slice ->
                slice.colorHex?.let { runCatching { it.toColorInt() }.getOrNull() } ?: outline
            }
            sliceSpace = 2f
            valueTextColor = Color.WHITE
            valueTextSize = 11f
        }
        binding.pieChart.data = PieData(dataSet).apply { setValueFormatter(PercentFormatter(binding.pieChart)) }
        binding.pieChart.centerText = MoneyFormatter.format(state.expense, state.currency)
        binding.pieChart.invalidate()
    }

    private fun renderBars(state: ReportUiState) {
        val incomeSet = BarDataSet(
            state.bars.mapIndexed { index, bar -> BarEntry(index.toFloat(), bar.income.toFloat()) },
            getString(R.string.type_income)
        ).apply { color = ContextCompat.getColor(requireContext(), R.color.income) }
        val expenseSet = BarDataSet(
            state.bars.mapIndexed { index, bar -> BarEntry(index.toFloat(), bar.expense.toFloat()) },
            getString(R.string.type_expense)
        ).apply { color = ContextCompat.getColor(requireContext(), R.color.expense) }

        val data = BarData(incomeSet, expenseSet).apply {
            barWidth = BAR_WIDTH
            setDrawValues(false)
        }
        binding.barChart.data = data
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(state.bars.map { DateFormatter.monthShort(it.month) })
        binding.barChart.xAxis.axisMinimum = 0f
        binding.barChart.xAxis.axisMaximum = data.getGroupWidth(GROUP_SPACE, BAR_SPACE) * state.bars.size
        binding.barChart.groupBars(0f, GROUP_SPACE, BAR_SPACE)
        binding.barChart.invalidate()
    }

    /** Styling that does not depend on the data; colors follow the light / dark theme. */
    private fun setUpCharts() {
        val textColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
        val holeColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainer)

        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setHoleColor(holeColor)
            setCenterTextColor(textColor)
            setEntryLabelColor(Color.WHITE)
            setDrawEntryLabels(false)
            legend.textColor = textColor
            legend.isWordWrapEnabled = true
        }
        binding.barChart.apply {
            description.isEnabled = false
            setPinchZoom(false)
            setScaleEnabled(false)
            axisRight.isEnabled = false
            axisLeft.textColor = textColor
            axisLeft.axisMinimum = 0f
            legend.textColor = textColor
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                this.textColor = textColor
            }
        }
    }

    private companion object {
        // (BAR_WIDTH + BAR_SPACE) * 2 + GROUP_SPACE = 1 -> each month takes one unit on the x axis
        const val BAR_WIDTH = 0.35f
        const val BAR_SPACE = 0.05f
        const val GROUP_SPACE = 0.2f
    }
}
