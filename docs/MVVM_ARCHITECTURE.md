# Milestone 2 – Cấu trúc MVVM

Toàn bộ code Kotlin nằm trong `app/src/main/java/vn/edu/vlu/moneymanager/`, chia theo 3 lớp của MVVM:

```
vn.edu.vlu.moneymanager
├── MainActivity.kt              View: toolbar + NavHostFragment + bottom navigation
├── data/                        ── MODEL ──
│   ├── AppContainer.kt          1 instance dùng chung cho mỗi repository
│   ├── model/                   data class: Transaction, Category, Budget, ExchangeRates, MonthKey...
│   ├── source/SampleData.kt     dữ liệu mẫu tĩnh (trước đây chỉ là tools:text trong layout)
│   └── repository/              giữ dữ liệu trong MutableStateFlow, expose ra StateFlow
├── feature/<màn hình>/
│   ├── viewmodel/               ── VIEWMODEL ── UiState + StateFlow, không tham chiếu View/Activity/Fragment/Context
│   └── ui/                      ── VIEW ── Activity / Fragment / Adapter, chỉ dùng ViewBinding
└── util/                        formatter tiền/ngày, validator, extension cho lifecycle và insets
```

| Feature | View (`ui/`) | ViewModel (`viewmodel/`) |
|---|---|---|
| auth | `LoginActivity`, `RegisterActivity` | `LoginViewModel`, `RegisterViewModel` |
| dashboard | `DashboardFragment` | `DashboardViewModel` |
| transaction | `TransactionListFragment`, `AddEditTransactionActivity`, `TransactionAdapter` | `TransactionListViewModel`, `AddEditTransactionViewModel` |
| budget | `BudgetFragment`, `BudgetAdapter` | `BudgetViewModel` |
| report | `ReportFragment` | `ReportViewModel` |
| currency | `CurrencyConverterFragment` | `CurrencyConverterViewModel` |
| profile | `ProfileFragment` | `ProfileViewModel` |
| category | `CategoryFragment`, `CategoryAdapter` | `CategoryViewModel` |

## Luồng dữ liệu

```
Repository (StateFlow)  ──combine──▶  ViewModel.uiState (StateFlow<XxxUiState>)  ──collect──▶  Fragment.render(state)
        ▲                                                                                        │
        └──────────────── ViewModel.save()/delete()/setFilter() ◀── click / text change ─────────┘
```

- Mỗi ViewModel gộp các StateFlow của repository bằng `combine` và `stateIn(viewModelScope, WhileSubscribed(5000), ...)`.
- Sự kiện một lần (Snackbar, chuyển màn hình) đi qua `Channel` → `receiveAsFlow()`, nên không bị phát lại khi xoay màn hình.
- Vì các ViewModel dùng chung repository, thêm giao dịch ở màn Add sẽ tự cập nhật Dashboard, Budgets và Reports.
- Lỗi validation được trả về dạng `@StringRes Int`, View tự gọi `getString`, nên ViewModel không cần `Context`.
- `AddEditTransactionViewModel` đọc `transaction_id` qua `SavedStateHandle` (factory mặc định copy Intent extras vào đó), không chạm vào Intent.

## Checklist yêu cầu Milestone 2

| Yêu cầu | Cách làm | Kiểm tra |
|---|---|---|
| Bỏ hết `findViewById`, dùng View Binding cho Activity/Fragment/Adapter | `buildFeatures { viewBinding = true }`; Activity dùng `XxxBinding.inflate(layoutInflater)`, Adapter dùng `ItemXxxBinding` trong ViewHolder, dialog dùng `DialogXxxBinding` | `grep -r findViewById app/src` → không có kết quả |
| Dữ liệu tĩnh chuyển sang lớp ViewModel, quan sát qua LiveData/StateFlow | `SampleData` → repository (`StateFlow`) → ViewModel `uiState: StateFlow` | mọi màn đều render từ `uiState` |
| ViewModel không tham chiếu View/Activity/Fragment/UI Context | ViewModel chỉ import `androidx.lifecycle`, `kotlinx.coroutines`, `data.*`, `R` (để lấy id chuỗi) | `grep -rE "import android\.(view\|widget\|content\.Context)" feature/*/viewmodel` → không có kết quả |
| Observe theo lifecycle trong Fragment | `repeatOnViewStarted { }` = `viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(STARTED) { ... } }` (xem `util/ViewExtensions.kt`) | 7/7 Fragment |
| `_binding = null` trong `onDestroyView()` | Mỗi Fragment: `private var _binding`, `private val binding get() = _binding!!` | 7/7 Fragment |

## Sửa thêm trong Milestone 2

- Gộp bộ `res/` ở thư mục gốc (bản đầy đủ của Milestone 1) vào `app/src/main/res` và xóa bản trùng. Đổi theme sang `Theme.FinanceTracker`, sửa package trong `nav_graph.xml`.
- `fragment_dashboard.xml`: thẻ số dư bị vỡ khi chạy thật, dù preview trong Android Studio vẫn đúng. Lý do: Barrier tham chiếu View nền có chiều cao `0dp`, và các TextView lại neo vào chính View nền đó nên tạo vòng phụ thuộc. Giờ các TextView neo vào banner / `tvBalance`, còn Barrier tham chiếu các TextView.
- `.gitignore` theo Listing 16; bỏ `.idea/` khỏi git.
- Màn hình mở đầu là `LoginActivity`. Tài khoản demo: `demo@vlu.edu.vn` / `demo1234`.
