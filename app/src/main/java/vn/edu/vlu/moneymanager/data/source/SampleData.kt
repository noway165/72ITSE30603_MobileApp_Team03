package vn.edu.vlu.moneymanager.data.source

import vn.edu.vlu.moneymanager.data.model.Budget
import vn.edu.vlu.moneymanager.data.model.Category
import vn.edu.vlu.moneymanager.data.model.ExchangeRates
import vn.edu.vlu.moneymanager.data.model.MonthKey
import vn.edu.vlu.moneymanager.data.model.Transaction
import vn.edu.vlu.moneymanager.data.model.TransactionType.EXPENSE
import vn.edu.vlu.moneymanager.data.model.TransactionType.INCOME
import java.util.Calendar

/**
 * Static demo data that used to live only as tools:text in the layouts.
 * Repositories copy it into their StateFlows; ViewModels read it from there.
 * Dates are relative to today so Dashboard / Budgets / Reports always have data for the current month.
 */
object SampleData {

    const val DEMO_EMAIL = "demo@vlu.edu.vn"
    const val DEMO_PASSWORD = "demo1234"
    const val DEFAULT_CURRENCY = "VND"

    val currencies = listOf("VND", "USD", "EUR", "JPY", "KRW", "GBP")

    val categories = listOf(
        Category(1, "Food & Drinks", EXPENSE, "#EF4444"),
        Category(2, "Transport", EXPENSE, "#3B82F6"),
        Category(3, "Shopping", EXPENSE, "#EC4899"),
        Category(4, "Bills", EXPENSE, "#F59E0B"),
        Category(5, "Entertainment", EXPENSE, "#8B5CF6"),
        Category(6, "Salary", INCOME, "#22C55E"),
        Category(7, "Freelance", INCOME, "#14B8A6"),
        Category(8, "Gifts", INCOME, "#F97316")
    )

    val transactions: List<Transaction> = buildList {
        var id = 1L
        // Five full months of history, then the current month up to today.
        for (monthsAgo in 5 downTo 1) {
            add(Transaction(id++, INCOME, 15_000_000.0 + monthsAgo * 250_000, "VND", 6, dateOf(monthsAgo, 5), "Monthly salary"))
            add(Transaction(id++, EXPENSE, 2_400_000.0 + monthsAgo * 120_000, "VND", 1, dateOf(monthsAgo, 9), "Groceries"))
            add(Transaction(id++, EXPENSE, 650_000.0, "VND", 2, dateOf(monthsAgo, 12), "Grab rides"))
            add(Transaction(id++, EXPENSE, 1_800_000.0, "VND", 4, dateOf(monthsAgo, 15), "Electricity & internet"))
            add(Transaction(id++, EXPENSE, 900_000.0 + monthsAgo * 300_000, "VND", 3, dateOf(monthsAgo, 21), "Clothes"))
            if (monthsAgo % 2 == 0) {
                add(Transaction(id++, INCOME, 120.0, "USD", 7, dateOf(monthsAgo, 24), "Logo design"))
            }
        }
        add(Transaction(id++, INCOME, 15_500_000.0, "VND", 6, dateOf(0, 1), "Monthly salary"))
        add(Transaction(id++, EXPENSE, 85_000.0, "VND", 1, dateOf(0, 2), "Lunch with team"))
        add(Transaction(id++, EXPENSE, 1_750_000.0, "VND", 4, dateOf(0, 3), "Electricity & internet"))
        add(Transaction(id++, EXPENSE, 320_000.0, "VND", 2, dateOf(0, 4), "Bus pass"))
        add(Transaction(id++, EXPENSE, 45.0, "USD", 5, dateOf(0, 6), "Concert ticket"))
        add(Transaction(id++, EXPENSE, 1_250_000.0, "VND", 1, dateOf(0, 8), "Groceries"))
        add(Transaction(id++, INCOME, 500_000.0, "VND", 8, dateOf(0, 10), "Birthday gift"))
        add(Transaction(id++, EXPENSE, 560_000.0, "VND", 2, dateOf(0, 12), "Grab rides", pendingSync = true))
        add(Transaction(id, EXPENSE, 1_100_000.0, "VND", 3, dateOf(0, 14), "New shoes", pendingSync = true))
    }

    val budgets: List<Budget> = MonthKey.current().let { month ->
        listOf(
            Budget(1, 1, month, 3_000_000.0),
            Budget(2, 2, month, 1_000_000.0),
            Budget(3, 3, month, 1_000_000.0),
            Budget(4, 4, month, 2_000_000.0)
        )
    }

    /** 1 USD = rates[code] code (offline snapshot, refreshed by CurrencyRepository). */
    val exchangeRates = ExchangeRates(
        base = "USD",
        rates = mapOf(
            "USD" to 1.0,
            "VND" to 26_300.0,
            "EUR" to 0.92,
            "JPY" to 151.0,
            "KRW" to 1_380.0,
            "GBP" to 0.79
        ),
        updatedAtMillis = null
    )

    /** Day [day] of the month that is [monthsAgo] months before today, capped at today for the current month. */
    private fun dateOf(monthsAgo: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.add(Calendar.MONTH, -monthsAgo)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val safeDay = if (monthsAgo == 0) day.coerceAtMost(today) else day.coerceAtMost(lastDay)
        calendar.set(Calendar.DAY_OF_MONTH, safeDay)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
