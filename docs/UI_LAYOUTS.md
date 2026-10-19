# Finance Tracker – Layout XML & Figma

**Figma:** https://www.figma.com/design/ujtXzUejBKSCBRCD9yK8Tm
(page *Finance Tracker UI*: Design System board + 12 screens, 360×800 dp)

## Màn hình ↔ file XML ↔ người phụ trách code

| # | Màn hình (frame Figma) | Layout XML | Phụ trách |
|---|---|---|---|
| — | Khung chính: toolbar + bottom nav | `activity_main.xml`, `menu/bottom_nav_menu.xml`, `navigation/nav_graph.xml` | Lâm |
| 01 | Login | `activity_login.xml` | Lâm |
| 02 | Register | `activity_register.xml` | Lâm |
| 03 | Dashboard | `fragment_dashboard.xml` (dùng lại `item_transaction.xml`) | Khoa |
| 04 | Transactions | `fragment_transaction_list.xml`, `item_transaction.xml` | Giang |
| 05 | Add / Edit transaction | `activity_add_edit_transaction.xml`, `menu/menu_add_edit_transaction.xml` | Giang |
| 06 | Budgets | `fragment_budget.xml`, `item_budget.xml` | Giang |
| 07 | Add budget (dialog) | `dialog_budget.xml` | Giang |
| 08 | Reports | `fragment_report.xml` (PieChart + BarChart của MPAndroidChart) | Khoa |
| 09 | Currency converter | `fragment_currency_converter.xml` | Khoa |
| 10 | Profile | `fragment_profile.xml` | Lâm |
| 11 | Categories | `fragment_category.xml`, `item_category.xml` | Lâm |
| 12 | Add category (dialog) | `dialog_category.xml` | Lâm |
| — | Dùng chung | `layout_month_selector.xml` (Budgets, Reports), `layout_empty_state.xml` (mọi danh sách) | — |

## Tài nguyên dùng chung

- `values/colors.xml` + `values-night/colors.xml`: bảng màu sáng / tối (dark mode tự đổi màu).
- `values/themes.xml`: `Theme.FinanceTracker` (Material 3 DayNight), style card, text appearance.
- `values/strings.xml` (tiếng Anh, mặc định) + `values-vi/strings.xml` (tiếng Việt). Không hardcode chữ trong code.
- `drawable/ic_*.xml`: icon Material (vector), `bg_*.xml`: nền tròn, nền gradient của thẻ số dư.

## Quy ước khi viết code cho các layout này

- Dùng **ViewBinding** (`buildFeatures { viewBinding = true }` đã bật). Id trong XML viết camelCase, ví dụ `tvAmount` → `binding.tvAmount`.
- `<include>` đều có id (`monthSelector`, `emptyTransactions`...) → truy cập bằng `binding.monthSelector.tvMonthTitle`.
- Màu danh mục: tô vòng tròn bằng `tvCategoryInitial.backgroundTintList = ColorStateList.valueOf(Color.parseColor(category.colorHex))`.
- Trong `dialog_category.xml`, mã màu nằm ở `android:tag` của từng chip.
- `item_budget.xml`: tiến độ ≥ 80% dùng màu `warning`, > 100% dùng `expense` và hiện `tvOverBudget`.
- Màn hình dài (Dashboard, Report, Profile, Currency) nằm trong `NestedScrollView`; danh sách có `paddingBottom="@dimen/fab_clearance"` để FAB không che dòng cuối.

## Lưu ý quan trọng

Không mở project từ thư mục có dấu tiếng Việt (ví dụ `Lập trình ứng dụng di động`): Android Gradle Plugin sẽ báo lỗi
*"Your project path contains non-ASCII characters"*. Hãy clone/copy vào thư mục không dấu, ví dụ `D:\Projects\FinanceTracker`.
