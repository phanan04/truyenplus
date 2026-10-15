# Phát hành cập nhật Truyện+

Luồng mới nằm ở **Cài đặt → Cập nhật ứng dụng**; bản cũ dùng **Trang chủ → ⋯ → Cập nhật ứng dụng**. Đây là cập nhật chính ứng dụng, không phải tải chương mới hoặc cập nhật nguồn truyện. Không gọi máy chủ phát hành của IReader.

## Cấu hình một lần

Cấu hình mặc định dùng GitHub Releases của repo `phanan04/truyenplus`, trả về tệp `update.json`:

`https://github.com/phanan04/truyenplus/releases/latest/download/update.json`

Nhập địa chỉ trong app, hoặc đặt Gradle property `truyenplusUpdateUrl` trước khi đóng gói bản phát hành. Property này được đưa vào manifest Android làm địa chỉ mặc định; không chứa khóa bí mật. Cấu hình do người dùng nhập sẽ ghi đè địa chỉ mặc định sau khi kiểm tra thành công.

Release [v2.0.32](https://github.com/phanan04/truyenplus/releases/tag/v2.0.32) đã cung cấp `update.json` và APK ARM64 (versionCode 86). URL mặc định trỏ tới release mới nhất. Mỗi bản phát hành cần APK và manifest tương ứng. Trước đó app báo lỗi nguồn chưa sẵn sàng, không giả báo đang dùng bản mới nhất. Repo riêng tư cần cơ chế xác thực riêng, chưa được hỗ trợ.

## Chuẩn bị một bản mới

### Tự động qua GitHub Actions

Push lên `main` chạy [Build and release Android](https://github.com/phanan04/truyenplus/actions/workflows/android-release.yml).
Không cần build APK trên máy hoặc tạo tag bằng tay. APK do CI phát hành tắt `debuggable`; hậu tố package `.debug` được giữ để cập nhật đè, không có nghĩa runtime còn bật chế độ debug. Pipeline từ chối APK nếu cờ debug bật lại. Có thể chọn **Run workflow** trên nhánh `main` để chạy thủ công.

- Workflow kiểm thử, build APK ARM64 với package hiện tại `.debug`, xác minh khóa ký cũ và tạo `update.json` bằng script có sẵn.
- APK, `update.json` và `SHA256SUMS` được tải lên một **draft** trước. Chỉ khi kích thước/hash đã khớp mới công khai và đánh dấu latest; sau đó tải lại APK công khai để kiểm tra toàn vẹn.
- Hai secrets bắt buộc: `ANDROID_KEYSTORE_BASE64` và `TRUYENPLUS_DRIVE_API_KEY`. Không đưa chúng vào Git. Khóa ký phải trùng certificate của bản đang dùng, được workflow kiểm tra trước khi build.
- Khóa debug cũ mặc định dùng alias `androiddebugkey`, mật khẩu `android`. Nếu khóa có cấu hình khác, đặt thêm `ANDROID_KEY_ALIAS`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD`.
- `ProjectConfig` là mốc phiên bản. CI cộng số thứ tự workflow vào versionCode và số patch: mốc `2.0.46 / 100`, run 1 là `2.0.47 / 101`, run 3 là `2.0.49 / 103`. Run lỗi có thể để trống một số phiên bản; rerun giữ nguyên phiên bản.
- CI chỉ sửa phiên bản trong checkout của runner, không commit ngược vào `main`. Không cần tăng mốc cho mỗi lần phát hành. Khi đổi tên/reset workflow hoặc đổi mốc thủ công, vẫn phải giữ versionCode cao hơn release mới nhất; workflow chặn phát hành ngược phiên bản.
- Các lượt build được xếp hàng; commit đã bị thay thế không được công khai đè lên bản mới. Release đã công khai không bị thay APK khi rerun.
- Nếu build lỗi, bản latest cũ vẫn dùng được. Xem bước đỏ trong Actions hoặc tải test reports; chọn **Re-run failed jobs** sau khi sửa cấu hình cần thiết.
- Thư viện/tiến độ trên máy người dùng không bị tác động bởi CI. Android vẫn yêu cầu người dùng xác nhận cài bản cập nhật.

### Thủ công dự phòng

1. Tăng `versionCode` trong `buildSrc/src/main/kotlin/ProjectConfig.kt` (bản 2.0.32 là 86). Giữ nguyên package và khóa ký của bản đã cài. Không thay debug key giữa các lần phát hành; bản release không thể cập nhật đè bản debug ký bằng khóa khác.
2. Tạo các APK cho kiến trúc cần hỗ trợ bằng `:android:assembleStandardDebug -PtruyenplusPerformance=true` với cùng khóa ký. Chọn versionCode cao hơn release tự động mới nhất. Không truyền các tham số `android.injected.*` của Android Studio: chúng có thể gắn `android:testOnly=true`, khiến trình cài đặt Android từ chối APK. Script tạo manifest sẽ chặn APK có cờ này.
3. Chạy script dưới đây trên APK đã có. Script đọc package/phiên bản/kiến trúc từ APK và tính kích thước, SHA-256; không build hoặc tải lên mạng.

```sh
ANDROID_HOME=/path/to/android-sdk python3 scripts/create-update-manifest.py \
  --apk /path/to/TruyenPlus-arm64.apk \
  --download-base-url https://github.com/phanan04/truyenplus/releases/download/v2.0.32 \
  --notes-file /path/to/release-notes.txt \
  --output /path/to/update.json
```

Có thể truyền nhiều `--apk` (mỗi ABI một file). Dùng URL có tag cố định cho APK, để thông tin kiểm tra toàn vẹn luôn trỏ đúng file của phiên bản đó.

4. Đăng APK và `update.json` cùng một release công khai, sau đó đánh dấu release mới nhất. Với máy chủ riêng, đăng APK trước rồi thay `update.json` sau.
5. Trong bản app có hỗ trợ updater: kiểm tra → tải → cài đặt. Android yêu cầu cấp quyền cài từ Truyện+ nếu chưa có và xác nhận cập nhật; không cần gỡ ứng dụng.

## Hành vi và giới hạn

- Chỉ nhận phiên bản có versionCode cao hơn, đúng package, Android tối thiểu và ABI tương thích.
- Mọi URL và chuyển hướng đều phải dùng HTTPS. Kiểm tra kích thước, SHA-256, package APK, versionCode và chữ ký cùng khóa với ứng dụng hiện tại trước khi mở trình cài.
- Hỗ trợ chữ ký cùng khóa; chưa hỗ trợ đổi/luân chuyển khóa ký.
- Tải trong tiến trình ứng dụng, có tiến độ, hủy và tải lại. Không có tải nền bền vững hay tiếp tục từ phần đã tải sau khi hệ thống đóng tiến trình. Nên giữ app mở khi tải; đóng bảng cập nhật vẫn cho phép tác vụ chạy trong màn chính.
- Có trạng thái rõ cho mất mạng, nguồn sai, không có bản phù hợp và chưa cấu hình. Không coi lỗi mạng là “đang dùng bản mới nhất”.
- Chỉ Android hỗ trợ cài từ app. Desktop/iOS hiện thông báo chưa hỗ trợ.
- APK cũ đã giao chưa có updater: cần cài thủ công một lần bản có tính năng này, sau đó mới cập nhật qua mạng được.

Tham khảo quyền cài ứng dụng của Android: https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()

Đo hiệu năng QA bằng `-PtruyenplusQa=true -PtruyenplusPerformance=true` để dùng cùng cờ runtime như bản phát hành; debug thường chỉ dùng phát triển/chức năng. Trình cập nhật manifest Truyện+ tiếp tục thay thế trình kiểm tra release IReader kế thừa.
