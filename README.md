# Truyện+

Ứng dụng đọc và nghe truyện trên Android, phát triển từ IReader với giao diện tối giản.

## Tính năng

- Quản lý thư viện và tìm kiếm sách.
- Đọc toàn bộ chương, tùy chỉnh font, cỡ chữ và màu nền.
- Theo dõi tiến độ và lịch sử đọc.
- Nghe truyện ngay trên trang đọc, chọn đoạn bắt đầu, nhạc nền từ file và hẹn giờ. Chọn giọng hệ thống, Piper hoặc VieNeu-TTS v3 Nano offline (tải gói giọng riêng).
- Nhập EPUB/PDF và xuất EPUB.

## Tải và cập nhật

[Tải APK mới nhất cho Android ARM64](https://github.com/phanan04/truyenplus/releases/latest) · [Các bản phát hành](https://github.com/phanan04/truyenplus/releases)

Hỗ trợ Android 8.0 trở lên. Cài đè bản cũ để giữ thư viện và tiến độ.

Cập nhật trong app: **biểu tượng tài khoản ở góc trên → Cài đặt → Cập nhật ứng dụng**.

## Phát triển

Dự án sử dụng Kotlin và Compose Multiplatform. Xem [hướng dẫn phát hành](docs/APP_UPDATES.md).

Push lên `main` sẽ [tự kiểm thử, build APK và phát hành cập nhật](https://github.com/phanan04/truyenplus/actions/workflows/android-release.yml).

Mã nguồn gốc: [IReader](README_IREADER.md). Giấy phép: [Apache 2.0](LICENSE).

Chọn Việt/Anh và giao diện Sáng/Tối/Hệ thống trong Cài đặt. Giao diện theo Apple Books: Trang chủ, Thư viện, Kho truyện và Tìm kiếm riêng; mở Cài đặt từ biểu tượng tài khoản ở góc trên.

Kho truyện Drive: thêm link thư mục, tìm tên truyện và tải EPUB khi cần. Kết nối Android cần cấu hình Google Cloud theo [hướng dẫn](docs/DRIVE_SHELF.md). Có từ bản 2.0.37.

Biên tập chương bằng Gemini/DeepSeek và bộ nhớ nhân vật theo truyện có từ bản 2.0.37. Xem [cách sử dụng](docs/CHAPTER_EDITING.md); cần API key riêng.
