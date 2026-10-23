package vn.edu.vlu.moneymanager.util

import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Formatting for the View layer. ViewModels expose raw numbers and dates; screens turn them into text here. */
object MoneyFormatter {

    private val symbols = mapOf("VND" to "₫", "USD" to "$", "EUR" to "€", "JPY" to "¥", "KRW" to "₩", "GBP" to "£")
    private val locale: Locale = Locale.forLanguageTag("vi-VN")

    /** 1250000.0 + VND -> "1.250.000 ₫", 12.5 + USD -> "12,50 $" */
    fun format(amount: Double, currency: String): String {
        val digits = runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrDefault(2).coerceAtLeast(0)
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else digits
            maximumFractionDigits = digits
        }
        return "${number.format(amount)} ${symbols[currency] ?: currency}"
    }

    /** "+1.250.000 ₫" for income, "-85.000 ₫" for expense */
    fun formatSigned(amount: Double, currency: String, type: TransactionType): String =
        (if (type == TransactionType.INCOME) "+" else "-") + format(amount, currency)

    fun formatRate(rate: Double): String = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (rate >= 100) 0 else 4
    }.format(rate)
}

object DateFormatter {

    fun date(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

    fun dateTime(millis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

    fun shortDate(millis: Long): String = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))

    /** "October 2026" / "tháng 10 2026" */
    fun monthTitle(month: MonthKey): String =
        SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(firstDay(month))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    /** "10/2026" */
    fun monthNumeric(month: MonthKey): String = String.format(Locale.getDefault(), "%02d/%d", month.month, month.year)

    /** "10/26", used under the bars of the report chart */
    fun monthShort(month: MonthKey): String =
        String.format(Locale.getDefault(), "%02d/%02d", month.month, month.year % 100)

    private fun firstDay(month: MonthKey): Date = Calendar.getInstance().apply {
        clear()
        set(month.year, month.month - 1, 1)
    }.time
}
