# AI Usage Disclosure – Milestone 3 (Room)

## Kiều Bảo Giang – Transactions & Budgets on Room (`feature/room-database-giang`)

- **Tool used:** Claude (Claude Code)
- **What it did:**
  - Tables `transactions` and `budgets` (`data/local/entity/TransactionEntity.kt`, `BudgetEntity.kt`) with the columns, foreign keys, indexes and UNIQUE constraint of the schema in the Milestone 3 plan, plus `toModel()` / `fromModel()`.
  - `TransactionDao` (`observeAll()` as `Flow` newest first, `getById`, `insert`, `update`, `deleteById`, `markAllSynced`) and `BudgetDao` (`observeAll()`, `insert`, `update`, `deleteById`), registered in `AppDatabase`; `seedTransactions` / `seedBudgets` added to `SeedDatabaseCallback` after `seedCategories`, keeping the SampleData ids so the foreign keys match.
  - `TransactionRepository` / `BudgetRepository` now take their DAO in the constructor and expose `Flow`; every write is `suspend` and the ViewModels call it in `viewModelScope.launch`. `AddEditTransactionViewModel` loads the transaction to edit with a suspend read; undo delete uses `restore()` (insert with the old id).
  - Deleting a category: the old manual `clearCategory` / `deleteByCategory` calls in `CategoryViewModel` are removed because the foreign keys (ON DELETE SET NULL on transactions, ON DELETE CASCADE on budgets) now do it in SQLite. `CategoryViewModelTest` was adjusted to match.
  - Tests: `FakeTransactionDao` / `FakeBudgetDao` (lists in a `MutableStateFlow`) for the JVM unit tests of the slice; new device tests `TransactionDaoTest` and `BudgetDaoTest` on an in-memory database (seed, CRUD, `markAllSynced`, UNIQUE, SET NULL and CASCADE when a category is deleted, unknown category rejected).
- **How I checked it:**
  - `gradlew :app:testDebugUnitTest`: 117 tests pass.
  - `gradlew :app:connectedDebugAndroidTest` on a Samsung SM-A225F (Android 13): 21 DAO tests pass (Category, Transaction, Budget).
  - Ran the app on the same phone after clearing its data: Dashboard, Transactions and Budgets read the seeded rows from `finance_tracker.db`; a transaction added in the app is still there after force-stopping and reopening the app.
  - Database Inspector screenshots of the `transactions` and `budgets` tables after adding / editing / deleting in the app.
