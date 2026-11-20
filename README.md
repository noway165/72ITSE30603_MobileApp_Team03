# Finance Tracker – 72ITSE30603 Mobile App, Team 03

Ứng dụng Android quản lý thu chi cá nhân nhiều loại tiền, dùng được khi mất mạng và đồng bộ theo tài khoản (Kotlin, XML Views, Material 3, MVVM).

- **Bản phát hành:** [v1.0.0-release](https://github.com/noway165/72ITSE30603_MobileApp_Team03/releases/tag/v1.0.0-release) (APK đã ký bằng keystore của nhóm)
- **Công nghệ:** Room · Retrofit + Gson · Coil · Firebase Authentication · EncryptedSharedPreferences · Coroutines + StateFlow · R8

| Thành viên | GitHub | Phần phụ trách (theo chiều dọc: giao diện → ViewModel → Room → API) |
|---|---|---|
| Tăng Thoại Lâm (nhóm trưởng) | [noway165](https://github.com/noway165) | Đăng nhập Firebase, danh mục, cá nhân, nền mạng, mã hóa cài đặt, ký APK, release |
| Kiều Bảo Giang | [Zang-1](https://github.com/Zang-1) | Giao dịch, ngân sách, đồng bộ offline, R8 |
| Nguyễn Nam Khoa | [namkhoa306](https://github.com/namkhoa306) | Trang chủ, báo cáo, quy đổi tiền tệ, LeakCanary và kiểm tra hiệu năng |

## Milestone 1 – Thiết kế giao diện

- **Figma:** https://www.figma.com/design/ujtXzUejBKSCBRCD9yK8Tm (Design System + 12 màn hình)
- **Layout XML:** `app/src/main/res/layout/`, gồm tất cả màn hình, màu sáng/tối, icon, chuỗi tiếng Anh + tiếng Việt
- **Bảng màn hình ↔ file ↔ người phụ trách:** [docs/UI_LAYOUTS.md](docs/UI_LAYOUTS.md)

## Milestone 2 – MVVM

- Cấu trúc package `data/` (Model) · `feature/<màn hình>/viewmodel` (ViewModel) · `feature/<màn hình>/ui` (View)
- ViewBinding cho mọi Activity / Fragment / Adapter, không còn `findViewById`
- ViewModel expose `StateFlow`; Fragment collect bằng `viewLifecycleOwner` + `repeatOnLifecycle(STARTED)`; `_binding = null` trong `onDestroyView()`
- Chi tiết và checklist yêu cầu: [docs/MVVM_ARCHITECTURE.md](docs/MVVM_ARCHITECTURE.md)

## Milestone 3 – Lưu dữ liệu offline với Room

- **Tính năng offline:** danh mục, giao dịch và ngân sách lưu trong database Room `finance_tracker.db`, còn nguyên sau khi tắt app; tỷ giá được cache để màn đổi tiền và báo cáo chạy khi mất mạng.
- **Luồng:** `Entity` → `DAO` (đọc trả `Flow`, ghi là `suspend`) → `AppDatabase` singleton an toàn đa luồng (`@Volatile` + `synchronized`, mở trong `FinanceTrackerApp`) → `Repository` → `ViewModel` → Fragment ViewBinding. Lần chạy đầu, `SeedDatabaseCallback` nạp danh mục và tỷ giá mẫu.
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

## Milestone 4 – Mạng và dịch vụ đám mây

- **Firebase Authentication (Email/Password):** đăng ký, đăng nhập, giữ phiên sau khi tắt app; đăng xuất xóa giao dịch và ngân sách trên máy để tài khoản sau không thấy.
- **Retrofit + MockAPI:** danh mục, giao dịch và ngân sách nằm trên server theo `userId` của Firebase; Room là bản sao offline mà mọi màn hình đọc.
- **ExchangeRate-API:** tỷ giá trực tiếp, lưu vào bảng `exchange_rates` để dùng khi mất mạng.
- **Coil:** cờ quốc gia trong danh sách tỷ giá, có placeholder, ảnh lỗi và cache bộ nhớ + đĩa.
- **3 trạng thái:** mọi màn có mạng dùng `UiState` (Loading · Success · Error); lỗi hiện banner kèm nút Thử lại, app không crash.
- **Offline:** giao dịch tạo khi mất mạng được lưu với `pending_sync`; **Đồng bộ ngay** ở màn Cá nhân (hoặc mở lại app) đẩy lên server rồi tải dữ liệu mới về.

| Endpoint | Phương thức | Người làm |
|---|---|---|
| `categories?userId=`, `categories/{id}` | GET · POST · PUT · DELETE | Lâm |
| `transactions?userId=`, `transactions/{id}` | GET · POST · PUT · DELETE | Giang |
| `budgets?userId=`, `budgets/{id}` | GET · POST · PUT · DELETE | Giang |
| `v6/{API_KEY}/latest/USD` (ExchangeRate-API) | GET | Khoa |

- **Pull Request:** [#10 `feature/network-cloud-integration` → `main`](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/10) (gộp PR #11 giao dịch & ngân sách, PR #13 tỷ giá)

## Milestone 5 – Bản phát hành, bảo mật và hiệu năng

| Hạng mục | Cách làm | Pull Request |
|---|---|---|
| Thu gọn code | R8 (`optimization { enable = true }` của AGP 9) + `isShrinkResources`; keep rules cho DTO Gson ở `app/src/main/keepRules/rules.keep`. APK 12,7 MB → 3,7 MB | [#14](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/14) Giang |
| Bảo mật | URL MockAPI và API key nạp vào `BuildConfig` từ `local.properties`; cài đặt lưu bằng `EncryptedSharedPreferences` (AES-256, khóa trong Android Keystore) | [#15](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/15) Lâm |
| Ký APK | `signingConfigs` đọc keystore và mật khẩu từ `local.properties`; `*.jks` không đưa lên git | [#15](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/15) Lâm |
| Bộ nhớ | LeakCanary (chỉ bản debug): 0 leak; danh sách tỷ giá tái sử dụng dòng; rà soát RecyclerView và `Dispatchers.IO`, xem [docs/milestone5/performance_audit.md](docs/milestone5/performance_audit.md) | [#16](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/16) Khoa |
| Lint | Bỏ `@StringRes` khỏi hàm suspend trong `AuthRepository` | [#17](https://github.com/noway165/72ITSE30603_MobileApp_Team03/pull/17) Giang |

## Mở project

1. Clone vào thư mục **không có dấu tiếng Việt**, ví dụ `C:\Projects\` (Android Gradle Plugin báo lỗi với đường dẫn có dấu).
2. Chép `google-services.json` của nhóm vào `app/` (file này không có trên git).
3. Thêm vào `local.properties` ở thư mục gốc (không có trên git; URL kết thúc bằng `/`):
   ```properties
   MOCKAPI_CATEGORIES_URL=...
   MOCKAPI_FINANCE_URL=...
   EXCHANGE_RATE_API_KEY=...
   ```
   Muốn build bản release đã ký thì thêm `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`; thiếu các dòng này thì bản release chỉ không được ký.
4. Android Studio → **File → Open** → chọn thư mục repo → chờ Gradle Sync → Run `app`, tạo tài khoản bằng nút Đăng ký.

## Kiểm thử

- `gradlew :app:testDebugUnitTest`: 145 unit test (ViewModel và Repository với DAO, API giả trong bộ nhớ)
- `gradlew :app:connectedDebugAndroidTest` khi máy ảo đang bật: 27 test trên thiết bị (DAO với SQLite thật, cài đặt mã hóa)
- `gradlew :app:lintDebug`: 0 lỗi
