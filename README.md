# Finance Tracker – 72ITSE30603 Mobile App, Team 03

Ứng dụng Android quản lý thu chi cá nhân (Kotlin, XML Views, Material 3).

## Milestone 1 – Thiết kế giao diện

- **Figma:** https://www.figma.com/design/ujtXzUejBKSCBRCD9yK8Tm (Design System + 12 màn hình)
- **Layout XML:** `app/src/main/res/layout/`, gồm tất cả màn hình, màu sáng/tối, icon, chuỗi tiếng Anh + tiếng Việt
- **Bảng màn hình ↔ file ↔ người phụ trách:** [docs/UI_LAYOUTS.md](docs/UI_LAYOUTS.md)

## Milestone 2 – MVVM

- Cấu trúc package `data/` (Model) · `feature/<màn hình>/viewmodel` (ViewModel) · `feature/<màn hình>/ui` (View)
- ViewBinding cho mọi Activity / Fragment / Adapter, không còn `findViewById`
- ViewModel expose `StateFlow`; Fragment collect bằng `viewLifecycleOwner` + `repeatOnLifecycle(STARTED)`; `_binding = null` trong `onDestroyView()`
- Chi tiết và checklist yêu cầu: [docs/MVVM_ARCHITECTURE.md](docs/MVVM_ARCHITECTURE.md)

App chạy được trên máy ảo: màn đầu tiên là đăng nhập, tạo tài khoản mới bằng nút Register (Firebase Email/Password).

## Milestone 3 – Lưu dữ liệu offline với Room

- **Tính năng offline:** danh mục, giao dịch và ngân sách lưu trong database Room `finance_tracker.db`, còn nguyên sau khi tắt app; tỷ giá được cache để màn đổi tiền và báo cáo chạy khi mất mạng.
- **Luồng:** `Entity` → `DAO` (đọc trả `Flow`, ghi là `suspend`) → `AppDatabase` singleton an toàn đa luồng (`@Volatile` + `synchronized`, mở trong `FinanceTrackerApp`) → `Repository` → `ViewModel` → Fragment ViewBinding. Lần chạy đầu, `SeedDatabaseCallback` nạp dữ liệu mẫu.
- **Code:** `app/src/main/java/vn/edu/vlu/moneymanager/data/local/` (database, entity, DAO); schema Room tự xuất ở `app/schemas/`.

| Bảng | Khóa / ràng buộc | Người làm |
|---|---|---|
| `categories` | PK `id`; UNIQUE (`name`, `type`) | Lâm |
| `transactions` | PK `id`; FK `category_id` → `categories` ON DELETE SET NULL | Giang |
| `budgets` | PK `id`; FK `category_id` → `categories` ON DELETE CASCADE; UNIQUE (`category_id`, `year`, `month`) | Giang |
| `exchange_rates` | PK `currency_code` | Khoa |

- **Sơ đồ bảng:** [docs/milestone3/schema_map.png](docs/milestone3/schema_map.png)
- **Báo cáo (sơ đồ, ảnh Database Inspector, WBS):** [docs/milestone3/Group03_Milestone3_Report.pdf](docs/milestone3/Group03_Milestone3_Report.pdf)
- **Pull Request:** [#8 `feature/room-database` → `main`](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/8) (gộp PR #6 danh mục, PR #7 giao dịch & ngân sách, nhánh tỷ giá)
- **Kiểm thử:** `gradlew :app:testDebugUnitTest` (117 unit test, DAO giả trong bộ nhớ) và `gradlew :app:connectedDebugAndroidTest` khi máy ảo đang bật (22 test DAO trên SQLite thật).

Cài đặt (tiền tệ mặc định, dark mode) vẫn lưu trong bộ nhớ. Sau khi đổi schema database, gỡ app khỏi máy ảo rồi mới chạy lại.

## Mở project

1. Clone vào thư mục **không có dấu tiếng Việt**, ví dụ `C:\Projects\` (Android Gradle Plugin báo lỗi với đường dẫn có dấu).
2. Android Studio → **File → Open** → chọn thư mục repo → chờ Gradle Sync → Run `app`.
