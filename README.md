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

App chạy được trên máy ảo: màn đầu tiên là đăng nhập, tài khoản demo `demo@vlu.edu.vn` / `demo1234`.
Dữ liệu hiện là dữ liệu mẫu lưu trong bộ nhớ (chưa có Room/Firebase), nên tắt app thì mất các thay đổi.

## Mở project

1. Clone vào thư mục **không có dấu tiếng Việt**, ví dụ `C:\Projects\` (Android Gradle Plugin báo lỗi với đường dẫn có dấu).
2. Android Studio → **File → Open** → chọn thư mục repo → chờ Gradle Sync → Run `app`.
