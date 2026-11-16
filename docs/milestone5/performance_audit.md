# Milestone 5 – Performance audit (Khoa, `feature/m5-performance`)

Scope: LeakCanary, the `rvRates` list, every RecyclerView, every use of `Dispatchers.IO`, and where the
ExchangeRate-API key comes from.

## 1. LeakCanary (debug only)

- `gradle/libs.versions.toml`: `leakcanary = "2.14"`; `app/build.gradle.kts`: `debugImplementation(libs.leakcanary.android)`.
- `debugImplementation` puts the library in the debug APK only. It starts by itself through a
  ContentProvider, so there is no code in `FinanceTrackerApp` and nothing of it in the release APK/AAB.
- It watches destroyed Activities, Fragments, Fragment views and cleared ViewModels. Every Fragment already
  sets `_binding = null` in `onDestroyView()`, which is the usual source of Fragment view leaks.

## 2. `rvRates` (Currency screen)

| | Before | After |
|---|---|---|
| Layout | `rvRates` `wrap_content` inside a `NestedScrollView`, `nestedScrollingEnabled="false"` | `CoordinatorLayout` + `AppBarLayout` (the two cards, `scroll` flag) + `rvRates` `match_parent` with `appbar_scrolling_view_behavior` |
| Rows in the list | all 166 currencies of ExchangeRate-API | the 6 currencies the app offers (`SampleData.currencies`) |
| ViewHolders created | one per row = 166, none ever reused | only the rows that fit on screen |
| Flag images requested on opening | 166 | at most 6 |
| Rows written to Room per refresh | 166 | 6 |

Why the old layout could not recycle: a `NestedScrollView` measures its child with an unlimited height, so a
`wrap_content` RecyclerView grows to the height of all its rows and has to create and bind every one of
them. Recycling needs a list with a bounded height that scrolls by itself.

Code:
- `res/layout/fragment_currency_converter.xml`: new structure, same view ids, so the Fragment code is unchanged.
- `CurrencyRepository.ratesList`: filters to `currencies` and sorts by code (a database filled before this
  change still holds 166 rows; they are ignored by the list).
- `CurrencyRepository.refresh()`: stores only the supported currencies.
- `CurrencyConverterFragment`: `rvRates.setHasFixedSize(true)`.
- `CurrencyRepositoryTest`: two unit tests for the filter.

## 3. RecyclerView audit

| List | Screen | Adapter | Height | Result |
|---|---|---|---|---|
| `rvRates` | Currency | `ListAdapter` + `DiffUtil` | was `wrap_content` in `NestedScrollView` | **Fixed** (section 2) |
| `rvTransactions` | Transactions | `ListAdapter` + `DiffUtil` | `0dp` in `ConstraintLayout` | OK, recycles. Added `setHasFixedSize(true)` |
| `rvBudgets` | Budgets | `ListAdapter` + `DiffUtil` | `match_parent` under `AppBarLayout` | OK, recycles. Added `setHasFixedSize(true)` |
| `rvCategories` | Categories | `ListAdapter` + `DiffUtil` | `match_parent` | OK, recycles. Added `setHasFixedSize(true)` |
| `rvRecent` | Dashboard | `ListAdapter` + `DiffUtil` | `wrap_content` in a scroll view | Accepted: `DashboardViewModel` caps it at `RECENT_COUNT = 5` rows, all visible at once, so there is nothing to recycle |

Other points:
- All five adapters use `ListAdapter` with a `DiffUtil.ItemCallback`, so `submitList` computes the diff on a
  background thread and only changed rows are rebound.
- The only `notifyDataSetChanged()` is in `BudgetFragment`, when the display currency changes: every row
  changes then, so a full rebind is correct.
- `CurrencyAdapter` loads flags with Coil (memory + disk cache); Coil cancels the request of a recycled row.

## 4. `Dispatchers.IO` audit

| Work | Where | Thread |
|---|---|---|
| Retrofit calls (MockAPI, ExchangeRate-API) | every repository, through `apiCall(ioDispatcher)` in `UiState.kt` | `Dispatchers.IO` (`withContext`) |
| Room writes after a network call | inside the same `apiCall` block | `Dispatchers.IO` |
| Other Room reads/writes (`TransactionRepository.get`, `clearLocal`) | `suspend` DAO functions | Room's own query executor (suspend DAOs are main-safe) |
| Room `Flow` queries | `observeAll()` in the four DAOs | Room's query executor; results are collected on Main |
| Firebase sign-in / register | `FirebaseAuthRepository`, `await()` | Firebase's own threads |
| Coroutines started by the UI | `viewModelScope` / `repeatOnViewStarted` only | Main, cancelled with the ViewModel / view |

- The dispatcher is a constructor parameter (`ioDispatcher: CoroutineDispatcher = Dispatchers.IO`) in the
  four repositories, so tests replace it. No `Dispatchers.IO` is hard-coded in a ViewModel or Fragment.
- No `runBlocking`, `GlobalScope`, `Thread`, or `allowMainThreadQueries()` anywhere in `app/src/main`.

Findings left as they are (reported, not changed in this branch):
1. `DashboardViewModel` and `ReportViewModel` total and group the transactions inside `combine` on the Main
   dispatcher. It is O(n) over one user's transactions; if the lists grow, add `.flowOn(Dispatchers.Default)`
   with an injected dispatcher (the tests read `uiState.value` synchronously and would need it).
2. `AppContainer.init` opens `EncryptedSharedPreferences` in `Application.onCreate` on the main thread. The
   first run creates the Keystore key, which costs some start-up time. It is needed before the first screen
   (the saved theme), so it stays.

## 5. API key comes from BuildConfig

- `local.properties` (not in git, see `.gitignore`): `EXCHANGE_RATE_API_KEY=...`
- `app/build.gradle.kts`: `buildConfigField("String", "EXCHANGE_RATE_API_KEY", localProperty("EXCHANGE_RATE_API_KEY"))`
- `CurrencyRepository.refresh()`: `api.getLatestRates(BuildConfig.EXCHANGE_RATE_API_KEY, BASE_CURRENCY)`
- Searching the repository for the key value or for `exchangerate-api.com/v6/<key>` finds nothing: the only
  mention of the host is the base URL `https://v6.exchangerate-api.com/` in `AppContainer`.
