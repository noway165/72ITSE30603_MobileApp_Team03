# Finance Tracker – 72ITSE30603 Mobile App, Team 03

Ứng dụng Android quản lý thu chi cá nhân (Kotlin, XML Views, Material 3).

## Milestone 1 – Thiết kế giao diện

- **Figma:** https://www.figma.com/design/ujtXzUejBKSCBRCD9yK8Tm (Design System + 12 màn hình)
- **Layout XML:** `app/src/main/res/layout/` – tất cả màn hình, kèm màu sáng/tối, icon, chuỗi tiếng Anh + tiếng Việt
- **Bảng màn hình ↔ file ↔ người phụ trách:** [docs/UI_LAYOUTS.md](docs/UI_LAYOUTS.md)

Milestone này chỉ có giao diện, chưa có code Kotlin nên app chưa chạy trên máy ảo.
Xem layout bằng Android Studio: mở file trong `res/layout` → tab **Design** / **Split**.

## Mở project

1. Clone vào thư mục **không có dấu tiếng Việt**, ví dụ `C:\Projects\` (Android Gradle Plugin báo lỗi với đường dẫn có dấu).
2. Android Studio → **File → Open** → chọn thư mục repo → chờ Gradle Sync.
