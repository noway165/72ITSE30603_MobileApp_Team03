package vn.edu.vlu.moneymanager.data.model

import java.util.Calendar

// Model layer: plain Kotlin data classes, no Android UI types.

enum class TransactionType { INCOME, EXPENSE }

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val colorHex: String
)

data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val categoryId: Long?,
    val dateMillis: Long,
    val note: String,
    /** true while the change only exists on this device (shown as the "waiting to sync" icon) */
    val pendingSync: Boolean = false
)

/** A transaction joined with its category, ready to be shown in a list row. */
data class TransactionDetail(
    val transaction: Transaction,
    val category: Category?
)

/** Monthly spending limit of one expense category, stored in the default currency. */
data class Budget(
    val id: Long,
    val categoryId: Long,
    val month: MonthKey,
    val limit: Double
)

data class BudgetProgress(
    val budget: Budget,
    val category: Category?,
    val spent: Double
) {
    val percent: Int get() = if (budget.limit <= 0) 0 else (spent * 100 / budget.limit).toInt()
    val remaining: Double get() = budget.limit - spent
}

data class User(
    val email: String,
    val isFirebaseAccount: Boolean
)

/** Exchange rates relative to [base] (1 base = rates[code] code). */
data class ExchangeRates(
    val base: String,
    val rates: Map<String, Double>,
    val updatedAtMillis: Long?
) {
    fun rate(from: String, to: String): Double? {
        if (from == to) return 1.0
        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null
        return toRate / fromRate
    }

    fun convert(amount: Double, from: String, to: String): Double? = rate(from, to)?.let { amount * it }
}

/** A calendar month (month is 1..12). Avoids java.time so the app still runs on API 24/25. */
data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {

    fun plusMonths(count: Int): MonthKey {
        val index = year * 12 + (month - 1) + count
        return MonthKey(index / 12, index % 12 + 1)
    }

    fun contains(millis: Long): Boolean = of(millis) == this

    override fun compareTo(other: MonthKey): Int = compareValuesBy(this, other, MonthKey::year, MonthKey::month)

    companion object {
        fun of(millis: Long): MonthKey {
            val calendar = Calendar.getInstance().apply { timeInMillis = millis }
            return MonthKey(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }

        fun current(): MonthKey = of(System.currentTimeMillis())
    }
}
