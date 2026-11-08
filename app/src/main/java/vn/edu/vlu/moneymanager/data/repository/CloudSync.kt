package vn.edu.vlu.moneymanager.data.repository

import vn.edu.vlu.moneymanager.data.remote.UiState

/**
 * Full sync with MockAPI, used when the app opens and by "Sync now". Order matters: categories first,
 * because transactions and budgets point to them; pending transactions are uploaded before the download,
 * so the server already has them. Stops at the first error and returns it.
 */
suspend fun syncAll(
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository
): UiState<Unit> {
    val steps = listOf(
        suspend { categoryRepository.refresh() },
        suspend { transactionRepository.syncPending() },
        suspend { transactionRepository.refresh() },
        suspend { budgetRepository.refresh() }
    )
    for (step in steps) {
        val result = step()
        if (result is UiState.Error) return result
    }
    return UiState.Success(Unit)
}
