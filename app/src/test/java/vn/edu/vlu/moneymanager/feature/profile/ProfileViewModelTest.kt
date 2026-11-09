package vn.edu.vlu.moneymanager.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.vlu.moneymanager.FakeAuthRepository
import vn.edu.vlu.moneymanager.FakeTransactionApi
import vn.edu.vlu.moneymanager.MainDispatcherRule
import vn.edu.vlu.moneymanager.data.model.TransactionType
import vn.edu.vlu.moneymanager.feature.profile.viewmodel.ProfileEvent
import vn.edu.vlu.moneymanager.feature.profile.viewmodel.ProfileViewModel
import vn.edu.vlu.moneymanager.sampleBudgetRepository
import vn.edu.vlu.moneymanager.sampleCategoryRepository
import vn.edu.vlu.moneymanager.sampleCurrencyRepository
import vn.edu.vlu.moneymanager.sampleTransactionRepository
import vn.edu.vlu.moneymanager.saved
import vn.edu.vlu.moneymanager.testTransaction
import vn.edu.vlu.moneymanager.usdSettings
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signOut_removesTheAccountsRows_includingPendingOnes() = runTest {
        val api = FakeTransactionApi().apply { failure = IOException("Airplane Mode") }
        val transactions = sampleTransactionRepository(api)
        val budgets = sampleBudgetRepository()
        val pending = transactions.saved(testTransaction(TransactionType.EXPENSE, 10.0, System.currentTimeMillis()))
        assertTrue(pending.pendingSync)
        val viewModel = ProfileViewModel(
            FakeAuthRepository(),
            usdSettings(),
            transactions,
            sampleCurrencyRepository(),
            sampleCategoryRepository(),
            budgets
        )

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(transactions.transactions.first().isEmpty())
        assertTrue(budgets.budgets.first().isEmpty())
        assertEquals(ProfileEvent.SignedOut, viewModel.events.first())
    }
}
