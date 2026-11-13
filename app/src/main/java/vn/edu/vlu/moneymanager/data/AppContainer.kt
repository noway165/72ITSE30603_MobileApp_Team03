package vn.edu.vlu.moneymanager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import vn.edu.vlu.moneymanager.BuildConfig
import vn.edu.vlu.moneymanager.data.local.AppDatabase
import vn.edu.vlu.moneymanager.data.local.securePreferences
import vn.edu.vlu.moneymanager.data.remote.ApiClient
import vn.edu.vlu.moneymanager.data.remote.ExchangeRateApi
import vn.edu.vlu.moneymanager.data.remote.NetworkMonitor
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.repository.BudgetRepository
import vn.edu.vlu.moneymanager.data.repository.CategoryRepository
import vn.edu.vlu.moneymanager.data.repository.CurrencyRepository
import vn.edu.vlu.moneymanager.data.repository.FirebaseAuthRepository
import vn.edu.vlu.moneymanager.data.repository.SettingsRepository
import vn.edu.vlu.moneymanager.data.repository.TransactionRepository

object AppContainer {

    lateinit var database: AppDatabase
        private set

    // Stays null in JVM unit tests (no init): settings then live in memory only
    private var settingsPrefs: SharedPreferences? = null

    fun init(context: Context) {
        database = AppDatabase.getInstance(context)
        NetworkMonitor.start(context)
        settingsPrefs = securePreferences(context.applicationContext)
    }

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }

    private var transactionRepositoryInstance: TransactionRepository? = null

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

    val exchangeRateApi: ExchangeRateApi by lazy {
        ApiClient.create("https://v6.exchangerate-api.com/")
    }

    var currencyRepository: CurrencyRepository
        get() = currencyRepositoryInstance
            ?: CurrencyRepository(
                database.exchangeRateDao(),
                exchangeRateApi
            ).also { currencyRepositoryInstance = it }
        @VisibleForTesting
        set(value) {
            currencyRepositoryInstance = value
        }

    val settingsRepository by lazy { SettingsRepository(settingsPrefs) }
}