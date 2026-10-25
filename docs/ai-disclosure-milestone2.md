# AI Usage Disclosure – Milestone 2

- **Tool used:** Claude (Claude Code)
- **What it did:** restructured the project into MVVM packages (`data/`, `feature/<screen>/ui`, `feature/<screen>/viewmodel`), wrote the ViewModels / repositories / ViewBinding screens, merged the duplicated `res/` folder into `app/src/main/res`, and found the runtime layout bug in `fragment_dashboard.xml` (Barrier referencing 0dp Views).
- **How we checked it:** built with `gradlew assembleDebug` + `lintDebug` (no errors), ran every screen on the Pixel 6 emulator, rotated the Add transaction screen to confirm state and validation errors survive, and grep-checked the five Milestone 2 rules (see `docs/MVVM_ARCHITECTURE.md`).
- **Team responsibility:** each member must be able to explain the ViewModel and Fragment of the screens they own (see `docs/UI_LAYOUTS.md`).

## Kiều Bảo Giang – Transactions & Budgets slice (`feature/giang-transaction-slice`)

- **Tool used:** Claude (Claude Code)
- **What it did:**
  - Wrote the unit tests of the slice in `app/src/test/java/vn/edu/vlu/moneymanager/feature/transaction/` and `.../feature/budget/`:
    - `TransactionRepositoryTest`: new id per save, edit, delete, `clearCategory`, `markAllSynced`, newest-first order.
    - `BudgetRepositoryTest`: add, update in place, delete, `deleteByCategory`.
    - `TransactionListViewModelTest`: filter by type / category / keyword / date range, delete + undo event.
    - `AddEditTransactionViewModelTest`: missing amount or category, error kept on rotation, save new, edit through `SavedStateHandle(mapOf("transaction_id" to 1L))`, delete.
    - `BudgetViewModelTest`: duplicate category, limit ≤ 0, `percent`, spent converted to the default currency, month switch, delete + undo.
  - Layout fixes for short screens (small phone, landscape):
    - `fragment_budget.xml`: the month switcher and summary card now scroll away with the list (AppBarLayout `scroll|enterAlways`) instead of leaving the list one row tall.
    - `dialog_budget.xml`: wrapped in a `NestedScrollView` so the limit field stays reachable above the keyboard.
- **How I checked it:** ran `gradlew :app:testDebugUnitTest`, ran the Transactions, Add / Edit transaction and Budgets screens on a small-screen emulator in portrait and landscape, and read each test against the ViewModel code to make sure I can explain what it checks.
