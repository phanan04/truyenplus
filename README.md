# Truyện+

Ứng dụng đọc và nghe truyện trên Android, phát triển từ IReader với giao diện tối giản.

## Tính năng

- Quản lý thư viện và tìm kiếm sách.
- Đọc toàn bộ chương, tùy chỉnh font, cỡ chữ và màu nền.
- Theo dõi tiến độ và lịch sử đọc.
- Nghe truyện ngay trên trang đọc, chọn đoạn bắt đầu, nhạc nền từ file và hẹn giờ. Dùng giọng đọc hệ thống và chọn giọng có trên thiết bị.
- Nhập EPUB/PDF và xuất EPUB.

## Tải và cập nhật

[Tải APK 2.0.38 cho Android ARM64](https://github.com/phanan04/truyenplus/releases/download/v2.0.38/TruyenPlus-v2.0.38-arm64-v8a.apk) · [Các bản phát hành](https://github.com/phanan04/truyenplus/releases)

Hỗ trợ Android 8.0 trở lên. Cài đè bản cũ để giữ thư viện và tiến độ.

Cập nhật trong app: **Cài đặt → Cập nhật ứng dụng** (bản cũ: **Trang chủ → ⋯**).

## Phát triển

Dự án sử dụng Kotlin và Compose Multiplatform. Xem [hướng dẫn phát hành](docs/APP_UPDATES.md).

Mã nguồn gốc: [IReader](README_IREADER.md). Giấy phép: [Apache 2.0](LICENSE).

Chọn Việt/Anh và giao diện Sáng/Tối/Hệ thống trong Cài đặt. Mã nguồn hiện tại dùng giao diện tối giản với thanh điều hướng nổi và tìm kiếm thư viện riêng. Có trong bản 2.0.36.

Kho truyện Drive: thêm link thư mục, tìm tên truyện và tải EPUB khi cần. Kết nối Android cần cấu hình Google Cloud theo [hướng dẫn](docs/DRIVE_SHELF.md). Có từ bản 2.0.37.

Biên tập chương bằng Gemini/DeepSeek và bộ nhớ nhân vật theo truyện có từ bản 2.0.37. Xem [cách sử dụng](docs/CHAPTER_EDITING.md); cần API key riêng.
