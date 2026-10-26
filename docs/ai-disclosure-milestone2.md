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

## Tăng Thoại Lâm – Account & Settings slice (`feature/lam-account-slice`)

- **Tool used:** Claude (Claude Code)
- **What it did:**
  - Wrote the unit tests of the slice in `app/src/test/java/vn/edu/vlu/moneymanager/feature/auth/` and `.../feature/category/`:
    - `AuthRepositoryTest`: demo sign-in, wrong password, unknown email, email case, register then sign in again, duplicate email, sign out.
    - `CategoryRepositoryTest`: new id per save, edit in place, `get` with null / unknown id, delete.
    - `LoginViewModelTest`: invalid email, empty password, loading then `SignedIn`, wrong password message, errors kept when the same text comes back after rotation.
    - `RegisterViewModelTest`: Weak / Medium / Strong password, rule and mismatch errors, email already used, `Registered` event.
    - `CategoryViewModelTest`: expense / income tabs, usage count, blank name, add / edit, deleting a category makes its transactions Uncategorized and removes its budgets.
  - Fixes found by running the slice on the `Small_Phone` emulator (360×640 dp) in portrait and landscape:
    - `themes.xml` (`Widget.FinanceTracker.ColorChip`): the color swatches of the category dialog were invisible (only a thin red bar showed). The chip has no text, so the 48dp touch-target insets took the whole 40dp swatch; `ensureMinTouchTargetSize=false` and a centered check mark fix it.
    - `layout-land/activity_main.xml`: in landscape the toolbar and bottom bar left ~170dp for the content. A `NavigationRailView` on the start edge replaces the bottom bar in landscape (`MainActivity` sets up whichever of the two the layout has).
    - `MainActivity.highlightParentTab`: after rotating on Categories or the currency converter, the bar highlighted Home; it now highlights the tab the screen was opened from.
- **How I checked it:** ran `gradlew :app:testDebugUnitTest` (112 tests pass) and `lintDebug` (no errors), then signed in on the emulator and opened Login, Register, Profile, Categories and the category dialog in portrait and landscape.
