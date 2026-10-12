# Truyện+

Ứng dụng đọc và nghe truyện trên Android, phát triển từ IReader với giao diện tối giản.

## Tính năng

- Quản lý thư viện và tìm kiếm sách.
- Khám phá truyện Việt từ TruyenFull, TruyenHH và bảng xếp hạng Qidian, Fanqie, Jinjiang; lọc thể loại, tải thêm và lưu cache.
- Đọc toàn bộ chương, tùy chỉnh font, cỡ chữ và màu nền.
- Theo dõi tiến độ và lịch sử đọc.
- Nghe truyện ngay trên trang đọc, chọn đoạn bắt đầu, nhạc nền từ file và hẹn giờ. Có Edge TTS: Hoài My và Nam Minh (cần Internet), kèm giọng hệ thống dự phòng.
- Nhập EPUB/PDF và xuất EPUB.

## Tải và cập nhật

[Tải APK 2.0.35 cho Android ARM64](https://github.com/phanan04/truyenplus/releases/download/v2.0.35/TruyenPlus-v2.0.35-arm64-v8a.apk) · [Các bản phát hành](https://github.com/phanan04/truyenplus/releases)

Hỗ trợ Android 8.0 trở lên. Cài đè bản cũ để giữ thư viện và tiến độ.

Cập nhật trong app: **Cài đặt → Cập nhật ứng dụng** (bản cũ: **Trang chủ → ⋯**).

## Phát triển

Dự án sử dụng Kotlin và Compose Multiplatform. Xem [hướng dẫn phát hành](docs/APP_UPDATES.md).

Mã nguồn gốc: [IReader](README_IREADER.md). Giấy phép: [Apache 2.0](LICENSE).

Chọn Việt/Anh trong Cài đặt. Các nguồn Việt hiển thị thông tin tiếng Việt nguyên bản; nguồn Trung dùng Google Translate qua [ML Kit](https://developers.google.com/ml-kit/language/translation) khi cần dịch.
