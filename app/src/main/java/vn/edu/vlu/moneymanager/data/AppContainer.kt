package vn.edu.vlu.moneymanager.data

import android.content.Context
import vn.edu.vlu.moneymanager.data.local.AppDatabase
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

/**
 * Manual dependency container: one shared instance of each repository for the whole app,
 * so every ViewModel sees the same data (adding a transaction updates Dashboard, Budgets and Reports).
 */
object AppContainer {

    /** Room database, opened by [init] from FinanceTrackerApp.onCreate. Repositories get their DAOs from it. */
    lateinit var database: AppDatabase
        private set

    fun init(context: Context) {
        database = AppDatabase.getInstance(context)
    }

    val authRepository by lazy { AuthRepository() }
    val transactionRepository by lazy { TransactionRepository() }
    val categoryRepository by lazy { CategoryRepository() }
    val budgetRepository by lazy { BudgetRepository() }
    val currencyRepository by lazy { CurrencyRepository() }
    val settingsRepository by lazy { SettingsRepository() }
}
