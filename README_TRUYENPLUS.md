# Truyện+ — ứng dụng đọc ebook từ IReader

Dự án sao chép mã nguồn `IReader-master` để dùng lại cơ sở dữ liệu, bộ đọc, nhập/xuất EPUB/PDF và dịch vụ TTS. Bản gốc ở thư mục bên cạnh không bị sửa. Giấy phép và ghi công IReader được giữ nguyên.

## Phạm vi ứng dụng

Chỉ giữ năm nhóm chức năng người dùng yêu cầu:

1. Quản lý thư viện: tìm trong sách đã nhập, bộ sưu tập, sắp xếp, chọn nhiều sách, gỡ sách và đánh dấu đã/chưa đọc.
2. Đọc truyện và tùy chỉnh: danh sách chương, dấu trang, cỡ chữ, font, màu nền, giãn dòng, chế độ đọc và độ sáng.
3. Theo dõi quá trình đọc: chương đã đọc, phần trăm sách, lịch sử, thời gian và chuỗi ngày đọc từ dữ liệu thật.
4. Nghe bằng TTS: mở toàn bộ chương ngay khi vào màn nghe, chạm vào đoạn để chọn chỗ bắt đầu, điều khiển phát/tạm dừng luôn hiện bên dưới. Giữ chuyển đoạn/chương, giọng, tốc độ, cao độ và hẹn giờ bằng dịch vụ TTS gốc.
5. Nhập và xuất ebook: nhập EPUB/PDF từ thiết bị, xuất EPUB từ màn sách bằng quy trình gốc.

Điều hướng có ba tab: **Trang chủ · Thư viện · Tiến độ**. Màn sách nối trực tiếp đến đọc, nghe và xuất EPUB. Không đăng ký các màn Nguồn, cập nhật chương, tìm truyện trực tuyến, tài khoản, đồng bộ cloud, cộng đồng hoặc cửa hàng plugin. Bộ đọc không còn các nút bình luận, dịch, AI và plugin. Các module nghiệp vụ gốc vẫn nằm trong mã nguồn để tránh viết lại bộ đọc.

Theo yêu cầu mới, **Trang chủ → ⋯ → Cập nhật ứng dụng** cho phép kiểm tra, tải và mở trình cài bản Android mới qua HTTPS. Địa chỉ mặc định là GitHub Releases của [phanan04/truyenplus](https://github.com/phanan04/truyenplus); không dùng bản phát hành IReader. Bản [2.0.27](https://github.com/phanan04/truyenplus/releases/tag/v2.0.27) đã phát hành kèm APK ARM64 và `update.json`. Hướng dẫn và công cụ tạo thông tin phát hành: [APP_UPDATES.md](docs/APP_UPDATES.md).

## Giao diện

Màn Đọc giữ nguyên bộ hiển thị toàn chương và cơ chế cuộn/lật trang của IReader, cùng vị trí đọc và các tùy chỉnh. Màn nghe mở toàn chương để chọn đoạn; có thể chuyển sang màn bìa và chạm nội dung để mở lại toàn chương.

Giao diện hiện dùng phong cách tối giản theo yêu cầu tham khảo ChatGPT: nền xám đen phẳng, chữ trắng dịu, nút và bề mặt xám trung tính, không dùng chuyển sắc xanh/tím. Giữ các hàng bìa, nút lọc, trình nghe thu gọn và điều hướng đủ chiều rộng. Trang chủ chỉ có một hàng sách, không lặp lại mục “Trong thư viện”. Bìa dùng dữ liệu ebook; sách thiếu bìa dùng poster xám sinh bằng Compose. Icon sách và màn khởi động cùng tông trung tính.

Ảnh ban đầu của người dùng ở `docs/design/truyenplus-reference.png`. Kiểm tra render bản mới nằm ở `presentation/build/reports/truyenplus-v2/`. Đây là các màn Compose thực với dữ liệu kiểm tra chỉ có trong source set test, không phải ảnh chụp điện thoại hoặc dữ liệu chèn vào app.

## Chạy và kiểm tra

Cấu hình gốc: Java 21 cho Gradle/Desktop, Java 17 cho `android-compat`, Android SDK API 37.

Cấu hình SDK cục bộ, khóa ký, Firebase và API không được đưa lên Git. Firebase mặc định tắt. Nếu cần Firebase riêng, dùng `android/google-services.json` của dự án của bạn và bật `-PtruyenplusFirebase=true`; cấu hình Firebase IReader cũ không được dùng trong bản phát hành Truyện+. Các workflow IReader cũ được giữ ở `.github/upstream-workflows/` để tham khảo và không tự chạy build/phát hành.

```sh
./gradlew :desktop:run
./gradlew :android:assembleStandardDebug
./gradlew :presentation:desktopTest --tests '*ReaderAppScopeTest' --tests '*BookReadingProgressTest' --tests '*ReaderScreensRenderTest' --tests '*ReaderUpdateTest'
```

APK đã giao trước đây: `dist/TruyenPlus-20261010-arm64-debug.apk`. File này **chưa có** thay đổi giao diện trung tính, sửa thanh điều hướng và updater mới. Bản [2.0.27 (versionCode 81)](https://github.com/phanan04/truyenplus/releases/tag/v2.0.27) đã phát hành. [Tải APK ARM64](https://github.com/phanan04/truyenplus/releases/download/v2.0.27/TruyenPlus-v2.0.27-arm64-v8a.apk); bản sao cục bộ: `dist/v2.0.27/TruyenPlus-v2.0.27-arm64-v8a.apk`. Cài đè bản cũ một lần để nhận updater; các bản sau dùng menu cập nhật trong app. Đầu ra Gradle ARM64: `android/build/outputs/apk/standard/debug/android-standard-arm64-v8a-debug.apk`.

Các script `scripts/verify-reader-scope.py`, `scripts/verify-reading-progress.py` và `scripts/verify-ui-source.py` kiểm tra phạm vi tính năng, phép tính tiến độ, cú pháp Kotlin/XML và đối chiếu các lớp nghiệp vụ với bản gốc. Test render kiểm tra tám bố cục ở chiều rộng 360 px: Trang chủ, Thư viện, Tiến độ, Sách, màn Đọc, TTS toàn chương, TTS có bìa và trạng thái chưa có sách. Test tương tác cuộn đến cuối chương TTS và kiểm tra callback chọn đúng đoạn cuối.

Trên máy phát triển, dùng JDK 21 trong thư mục tạm, `-Dorg.gradle.native=false` để tránh nhận sai CPU và init script tạm biên dịch stub `android-compat` bằng Java 21 với target 17. Không sửa cấu hình build gốc.

## Kiểm tra trên thiết bị

- Nhập EPUB/PDF; tìm, đổi bộ sưu tập, nhấn giữ để chọn và quản lý sách.
- Đọc, đổi font/màu/cỡ chữ; đánh dấu; mở lại app để kiểm tra vị trí đọc.
- Mở Nghe: kiểm tra toàn bộ chương, cuộn đến cuối, chọn đoạn rồi nhấn phát; thử chọn đoạn khi đang nghe. Chuyển đoạn/chương, đổi giọng/tốc độ và thử hẹn giờ.
- Xuất EPUB ở màn sách; mở lại tệp xuất và kiểm tra nội dung.
- Kiểm tra Tiến độ/Lịch sử dùng dữ liệu đọc thực tế.

Build và render không thay thế kiểm tra các luồng này trên điện thoại.

## Kết quả bản giao 2026-10-10

- Android standard debug và entry point Desktop biên dịch thành công.
- Bảy test đã qua: phạm vi tính năng (2), tiến độ đọc (3), render tám bố cục và chọn đoạn cuối chương TTS (2).
- Script độc lập: phạm vi đọc, tám trường hợp tiến độ, cú pháp 45 file Kotlin, XML và bảo toàn lớp nghiệp vụ đều qua.
- APK ARM64 đã xác minh chữ ký v2, nhãn Truyện+, icon mới; Android tối thiểu 8.0. Package debug giữ nguyên để cài cập nhật bản trước.
- [Ảnh render giao diện](docs/design/truyenplus-simpmusic-preview.png) dùng dữ liệu kiểm thử, không phải ảnh chụp thiết bị.
- Chưa chạy kiểm tra toàn bộ luồng nhập/đọc/nghe/xuất trên điện thoại thật.

## Bản phát hành 2.0.27 — giao diện trung tính và cập nhật ứng dụng

- Bản 2.0.27 đã build và phát hành với cùng package debug và khóa ký của APK cũ. Đã đối chiếu chữ ký v2, versionCode 81, ARM64, minSdk 26 và địa chỉ cập nhật trong APK; không kèm cấu hình Firebase của IReader.
- Build `:android:assembleStandardDebug` và 14 test Desktop đều qua. Test cập nhật kiểm tra chọn ABI, chặn phiên bản cũ/sai package/không tương thích/thiếu checksum/HTTP, lỗi tải, tải lại, hủy tải và nguồn chưa cấu hình.
- Đã tạo `update.json` từ APK mới, đăng kèm `SHA256SUMS`. GitHub xác nhận dung lượng và SHA-256 của cả ba tệp tải lên. URL `latest/download/update.json` công khai trả đúng manifest; URL APK trả HTTP 200 và dung lượng đúng. Việc tải lại toàn bộ APK tại máy phát triển quá chậm nên chưa đối chiếu checksum qua lượt tải công khai đầy đủ.
- [Ảnh giao diện trung tính](docs/design/truyenplus-neutral-preview.png): Trang chủ, nghe toàn chương, nội dung bảng cập nhật. Dữ liệu và phiên bản trong ảnh là dữ liệu kiểm thử. Bảng cập nhật được render bằng thành phần nội dung thực, không phải ảnh chụp cửa sổ Android.
- Đã phát hành [v2.0.27](https://github.com/phanan04/truyenplus/releases/tag/v2.0.27) từ commit `c55cec6`. Chưa thử cài và chạy các luồng trên điện thoại thật. Xem [hướng dẫn phát hành](docs/APP_UPDATES.md).
