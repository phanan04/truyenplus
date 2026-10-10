# Khám phá truyện

Tab Khám phá dùng dữ liệu công khai của [novel-tracker](https://github.com/dylan-byte-max/novel-tracker), thu thập từ Qidian, Fanqie và Jinjiang. Không cần cài nguồn/extension hoặc đăng nhập. Tên, tác giả và mô tả giữ nguyên ngôn ngữ nguồn.

- Qidian: bảng bán chạy trong ngày.
- Fanqie: bảng hot của thư viện.
- Jinjiang: bảng điểm tháng.
- Truyện hot: tối đa 10 vị trí đầu của bảng hiện tại, riêng từng nguồn.
- Mới lên bảng: bản ghi có `rank_change = "new"`. Đây không phải ngày xuất bản; nguồn không cung cấp ngày này nhất quán.
- Thể loại: các tag thực tế của nguồn. Thứ hạng giữ nguyên; không tạo bảng tổng hợp hoặc tự gán hạng.

## Kiến trúc

`DiscoveryRepository` và các model nằm trong domain. Data có parser riêng và repository dùng Ktor hiện có. Presentation dùng ViewModel, chip, theme, thanh điều hướng và bộ tải ảnh có sẵn. Không thay thế trình đọc, TTS, cơ sở dữ liệu hoặc cơ chế nhập ebook.

Ba endpoint:

```
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/qidian/latest.json
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/fanqie/latest.json
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/jjwxc/latest.json
```

Cache JSON theo nguồn trong thư mục dữ liệu riêng của app (`discovery/`), ghi qua tệp tạm và đổi tên nguyên tử. TTL 6 giờ; nút làm mới bỏ qua TTL. Cache được đọc trước khi gọi mạng. Lỗi một nguồn giữ lại bản cũ của nguồn đó, không gia hạn thời gian kiểm tra thành công. Ngày cập nhật do nguồn cung cấp luôn hiển thị để người dùng biết dữ liệu cũ. Bộ tải ảnh dùng cache bộ nhớ và đĩa có sẵn; một số URL bìa Fanqie có chữ ký có thể hết hạn.

Parser kiểm tra platform, liên kết sách chính thức, ID, ngày cập nhật và giới hạn kích thước. Bản ghi thiếu thông tin bắt buộc bị bỏ qua. Dữ liệu mẫu của bộ sinh demo trong repo tham khảo bị từ chối; không có fallback chứa truyện giả. Khi chưa tải được dữ liệu và chưa có cache, trang hiển thị lỗi/thử lại.

Bấm truyện xem bìa, tên, tác giả, tag, mô tả và liên kết trang chính thức. Feed chỉ có metadata; không giả lập chương đọc hoặc đưa truyện chưa có nội dung vào thư viện. Việc đọc trong app tiếp tục dùng sách đã nhập.

## Kiểm tra

Fixture trong `presentation/src/desktopTest/resources/discovery` là trích đoạn dữ liệu thật, có ghi commit nguồn trong `PROVENANCE.md`; không được đóng gói thành dữ liệu khởi tạo của app. Kiểm tra parser, cache qua lần mở app, TTL, lỗi từng nguồn, hủy và làm mới đồng thời. Kiểm tra Compose trên chiều rộng điện thoại và tablet.

Nguồn tham khảo đã đối chiếu tại commit `80f3dfaf2cc4328758f0e24e8f4b7236d14dd735`. Tên/tác giả truyện đầu bảng Fanqie và Jinjiang đã đối chiếu với trang chính thức. Qidian từ chối kết nối trực tiếp trong môi trường phát triển; nguồn này đọc snapshot do novel-tracker công bố. Khả năng tải phụ thuộc GitHub và việc các crawler của repo tiếp tục hoạt động.

## Nhập ebook trong nền

Bỏ hộp tiến trình chắn màn hình ở ReaderShellRoute. EPUB/PDF tiếp tục dùng pipeline IReader đã chạy I/O ở luồng nền và tự cập nhật thư viện qua thông báo thay đổi cơ sở dữ liệu. Người dùng có thể chuyển tab khi đang nhập; không cần viết lại importer. Công việc này tồn tại trong tiến trình app, không cam kết tiếp tục nếu Android đóng app.
