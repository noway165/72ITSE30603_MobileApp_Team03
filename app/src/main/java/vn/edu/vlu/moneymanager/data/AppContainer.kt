package vn.edu.vlu.moneymanager.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import vn.edu.vlu.moneymanager.BuildConfig
import vn.edu.vlu.moneymanager.data.local.AppDatabase
import vn.edu.vlu.moneymanager.data.remote.ApiClient
import vn.edu.vlu.moneymanager.data.remote.NetworkMonitor
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.FirebaseAuthRepository
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
        NetworkMonitor.start(context)
    }

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }

    private var transactionRepositoryInstance: TransactionRepository? = null

    /**
     * Created on first use from the Room DAO; JVM unit tests set a repository over fakes instead.
     * Transactions and budgets share one MockAPI project (the free plan allows 2 resources per project).
     */
    var transactionRepository: TransactionRepository
        get() = transactionRepositoryInstance
            ?: TransactionRepository(
                database.transactionDao(),
                ApiClient.create(BuildConfig.MOCKAPI_FINANCE_URL),
                userId = { authRepository.currentUser.value?.uid }
            ).also { transactionRepositoryInstance = it }
        @VisibleForTesting
        set(value) {
            transactionRepositoryInstance = value
        }

    private var categoryRepositoryInstance: CategoryRepository? = null

    /**
     * Created on first use from the Room DAO. JVM unit tests have no database, so they set this to a repository
     * over a fake DAO before creating a ViewModel that reads it from AppContainer.
     */
    var categoryRepository: CategoryRepository
        get() = categoryRepositoryInstance
            ?: CategoryRepository(
                database.categoryDao(),
                ApiClient.create(BuildConfig.MOCKAPI_CATEGORIES_URL),
                userId = { authRepository.currentUser.value?.uid }
            ).also { categoryRepositoryInstance = it }
        @VisibleForTesting
        set(value) {
            categoryRepositoryInstance = value
        }

    private var budgetRepositoryInstance: BudgetRepository? = null

    /** Created on first use from the Room DAO; JVM unit tests set a repository over fakes instead. */
    var budgetRepository: BudgetRepository
        get() = budgetRepositoryInstance
            ?: BudgetRepository(
                database.budgetDao(),
                ApiClient.create(BuildConfig.MOCKAPI_FINANCE_URL),
                userId = { authRepository.currentUser.value?.uid }
            ).also { budgetRepositoryInstance = it }
        @VisibleForTesting
        set(value) {
            budgetRepositoryInstance = value
        }

    private var currencyRepositoryInstance: CurrencyRepository? = null

    /** Exchange-rate cache in Room; JVM unit tests set a repository over a fake DAO instead. */
    var currencyRepository: CurrencyRepository
        get() = currencyRepositoryInstance
            ?: CurrencyRepository(database.exchangeRateDao()).also { currencyRepositoryInstance = it }
        @VisibleForTesting
        set(value) {
            currencyRepositoryInstance = value
        }

    val settingsRepository by lazy { SettingsRepository() }
}
