# Phát hành cập nhật Truyện+

Luồng mới nằm ở **Trang chủ → ⋯ → Cập nhật ứng dụng**. Đây là cập nhật chính ứng dụng, không phải tải chương mới hoặc cập nhật nguồn truyện. Không gọi máy chủ phát hành của IReader.

## Cấu hình một lần

Cấu hình mặc định dùng GitHub Releases của repo `phanan04/truyenplus`, trả về tệp `update.json`:

`https://github.com/phanan04/truyenplus/releases/latest/download/update.json`

Nhập địa chỉ trong app, hoặc đặt Gradle property `truyenplusUpdateUrl` trước khi đóng gói bản phát hành. Property này được đưa vào manifest Android làm địa chỉ mặc định; không chứa khóa bí mật. Cấu hình do người dùng nhập sẽ ghi đè địa chỉ mặc định sau khi kiểm tra thành công.

Repo đã được chỉ định nhưng chưa có release và APK mới được phát hành. URL mặc định chỉ hoạt động sau khi release chứa `update.json` được đăng. Trước đó app báo lỗi nguồn chưa sẵn sàng, không giả báo đang dùng bản mới nhất. Repo riêng tư cần cơ chế xác thực riêng, chưa được hỗ trợ.

## Chuẩn bị một bản mới

1. Tăng `versionCode` trong `buildSrc/src/main/kotlin/ProjectConfig.kt` (bản hiện tại là 80). Giữ nguyên package và khóa ký của bản đã cài. Không thay debug key giữa các lần phát hành; bản release không thể cập nhật đè bản debug ký bằng khóa khác.
2. Chỉ build khi người dùng yêu cầu. Tạo các APK cho kiến trúc cần hỗ trợ.
3. Chạy script dưới đây trên APK đã có. Script đọc package/phiên bản/kiến trúc từ APK và tính kích thước, SHA-256; không build hoặc tải lên mạng.

```sh
ANDROID_HOME=/path/to/android-sdk python3 scripts/create-update-manifest.py \
  --apk /path/to/TruyenPlus-arm64.apk \
  --download-base-url https://github.com/phanan04/truyenplus/releases/download/v2.0.27 \
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
