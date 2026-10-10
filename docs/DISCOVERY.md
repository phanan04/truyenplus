# Khám phá truyện

Tab Khám phá lấy thông tin tiếng Việt trực tiếp từ [TruyenFull](https://truyenfull.live/), và dữ liệu Qidian, Fanqie, Jinjiang qua [novel-tracker](https://github.com/dylan-byte-max/novel-tracker). Không cần cài nguồn/extension hoặc đăng nhập. Giao diện và bản dịch thông tin theo lựa chọn Tiếng Việt/English trong Cài đặt. Tên tác giả giữ nguyên từ nguồn; chi tiết truyện có nút xem lại tên và mô tả gốc khi có bản dịch máy.

- Qidian: bảng bán chạy trong ngày.
- Fanqie: bảng hot của thư viện.
- Jinjiang: bảng điểm tháng.
- TruyenFull: bảng đọc nhiều trong ngày, tối đa 20 truyện hot và 20 truyện mới cập nhật chương. Chỉ lấy thứ hạng được in trong bảng bên nguồn; thứ tự danh sách hot/mới không bị gán thành hạng.
- Truyện hot của ba nguồn Trung: tối đa 10 vị trí đầu của bảng hiện tại, riêng từng nguồn.
- Mới lên bảng của ba nguồn Trung: bản ghi có `rank_change = "new"`. TruyenFull dùng nhãn Mới cập nhật, không coi đây là ngày xuất bản.
- Thể loại: các tag thực tế của nguồn. Thứ hạng giữ nguyên; không tạo bảng tổng hợp hoặc tự gán hạng.

## Kiến trúc

`DiscoveryRepository` và các model nằm trong domain. Data có parser riêng và repository dùng Ktor hiện có. Presentation dùng ViewModel, chip, theme, thanh điều hướng và bộ tải ảnh có sẵn. Không thay thế trình đọc, TTS, cơ sở dữ liệu hoặc cơ chế nhập ebook.

Ba endpoint:

```
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/qidian/latest.json
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/fanqie/latest.json
https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/jjwxc/latest.json
```

TruyenFull dùng HTML công khai tại `/danh-sach/truyen-hot/` và `/danh-sach/truyen-moi/`, rồi lấy bìa, tác giả, mô tả, thể loại, trạng thái từ trang chi tiết. `TruyenFullParser` dùng Ksoup đã có trong data; `TruyenFullDiscoveryFetcher` dùng HTTP client Ktor có sẵn, giới hạn ba yêu cầu chi tiết đồng thời. HTML được chuẩn hóa thành JSON để dùng nguyên repository/cache hiện có. Trang sai truyện, thiếu metadata hoặc bị chặn được bỏ qua và hiển thị số truyện chưa tải được; khi không có truyện hợp lệ vẫn giữ cache cũ. Không lấy chương, không vượt trang đăng nhập hoặc chặn truy cập. Liên kết và số hạng có thể thay đổi phía nguồn; hạng bị thiếu không được đánh số lại.

Ngày TruyenFull hiển thị là thời điểm tải dữ liệu (UTC), vì nguồn không công bố thời điểm cập nhật bảng. Cache 6 giờ và nút làm mới cũng áp dụng cho nguồn này. Lần đầu cần thêm yêu cầu mạng để tải metadata chi tiết, các lần mở trong TTL dùng snapshot trên máy.

Cache JSON theo nguồn trong thư mục dữ liệu riêng của app (`discovery/`), ghi qua tệp tạm và đổi tên nguyên tử. TTL 6 giờ; nút làm mới bỏ qua TTL. Cache được đọc trước khi gọi mạng. Lỗi một nguồn giữ lại bản cũ của nguồn đó, không gia hạn thời gian kiểm tra thành công. Ngày cập nhật do nguồn cung cấp luôn hiển thị để người dùng biết dữ liệu cũ. Bộ tải ảnh dùng cache bộ nhớ và đĩa có sẵn; một số URL bìa Fanqie có chữ ký có thể hết hạn.

Parser kiểm tra platform, liên kết sách chính thức, ID, ngày cập nhật và giới hạn kích thước. Bản ghi thiếu thông tin bắt buộc bị bỏ qua. Dữ liệu mẫu của bộ sinh demo trong repo tham khảo bị từ chối; không có fallback chứa truyện giả. Khi chưa tải được dữ liệu và chưa có cache, trang hiển thị lỗi/thử lại.

Bấm truyện xem bìa, tên, tác giả, tag, mô tả và liên kết trang chính thức. Feed chỉ có metadata; không giả lập chương đọc hoặc đưa truyện chưa có nội dung vào thư viện. Việc đọc trong app tiếp tục dùng sách đã nhập.

## Ngôn ngữ và bản dịch

Thanh điều hướng gồm Trang chủ, Khám phá, Thư viện, Cài đặt. Thống kê và lịch sử đọc vẫn có trong Cài đặt → Tiến độ đọc. Lựa chọn Việt/Anh dùng `UiPreferences.language()` và `LocaleHelper` hiện có, lưu qua lần mở app và cập nhật các nhãn Compose Resources. Nội dung ebook đã nhập không tự bị dịch hoặc sửa đổi khi đổi ngôn ngữ giao diện.

`DiscoveryTranslations` nằm ở domain; `CachedDiscoveryTranslations` ở data. TruyenFull có ngôn ngữ nguồn `vi`: khi giao diện là Tiếng Việt, ViewModel bỏ qua cả đọc cache dịch, tải model và gọi dịch; không hiển thị huy hiệu Google Translate. Khi chọn English, Android dịch từ `vi` sang `en`. Ba nguồn Trung vẫn dịch từ `zh` sang `vi` hoặc `en`. Android tận dụng SDK Google ML Kit đã có trong dự án (17.0.3). Lần đầu dịch cần Wi-Fi để tải gói ngôn ngữ; không dùng API key hay endpoint Google Translate không chính thức. Desktop hiện chỉ dùng bản gốc khi chưa có cache; ML Kit on-device này chỉ có ở Android.

Tên, mô tả, trạng thái và các tag chưa có nhãn cố định được dịch ngầm theo nguồn đang xem, hiện dần từng truyện. Thể loại phổ biến và các nhánh Qidian có bảng nhãn Việt/Anh cố định, gồm các giai đoạn Tống–Nguyên–Minh, Tấn–Tùy–Đường và các tag Fanqie/Jinjiang thường gặp. Các nhãn này luôn ưu tiên hơn bản dịch máy đã lưu, không cần xóa cache để sửa thể loại. ViewModel hủy hàng đợi cũ khi đổi nguồn/ngôn ngữ hoặc nội dung feed thay đổi; cache được khôi phục trước khi chờ model. Khi dịch lỗi vẫn đọc được metadata gốc và có nút thử lại.

Cache bản dịch trong `discovery-translations/`, riêng từng truyện và ngôn ngữ. Chữ ký nội dung gốc bằng SHA-256 ngăn dùng lại bản dịch sau khi tên/mô tả/tag/trạng thái thay đổi. Ghi nguyên tử; không cache kết quả trống, thiếu trường hoặc trả nguyên văn tiếng Trung. Thứ hạng, tác giả, bìa và liên kết vẫn dùng nguồn thật. Tìm kiếm khớp cả tên gốc và tên dịch.

Bản dịch tự động do Google Translate cung cấp qua [ML Kit](https://developers.google.com/ml-kit/language/translation); chất lượng cần kiểm tra trên thiết bị thực tế, đặc biệt tên riêng và Trung → Việt qua tiếng Anh. Đây là dịch metadata, không phải dịch toàn bộ chương. Huy hiệu `google_translate_attribution.png` lấy nguyên bản từ [bộ huy hiệu chính thức](https://docs.cloud.google.com/static/translate/images/google-translate-attribution.zip), hiển thị cạnh thông tin đã dịch theo [hướng dẫn ghi nguồn](https://docs.cloud.google.com/translate/attribution).

## Kiểm tra

Fixture trong `presentation/src/desktopTest/resources/discovery` là trích đoạn dữ liệu thật, có ghi commit nguồn trong `PROVENANCE.md`; không được đóng gói thành dữ liệu khởi tạo của app. Kiểm tra parser, cache qua lần mở app, TTL, lỗi từng nguồn, hủy và làm mới đồng thời. Kiểm tra Compose trên chiều rộng điện thoại và tablet.

Nguồn tham khảo đã đối chiếu tại commit `80f3dfaf2cc4328758f0e24e8f4b7236d14dd735`. Tên/tác giả truyện đầu bảng Fanqie và Jinjiang đã đối chiếu với trang chính thức. Qidian từ chối kết nối trực tiếp trong môi trường phát triển; nguồn này đọc snapshot do novel-tracker công bố. Khả năng tải phụ thuộc GitHub và việc các crawler của repo tiếp tục hoạt động.

## Nhập ebook trong nền

Bỏ hộp tiến trình chắn màn hình ở ReaderShellRoute. EPUB/PDF tiếp tục dùng pipeline IReader đã chạy I/O ở luồng nền và tự cập nhật thư viện qua thông báo thay đổi cơ sở dữ liệu. Người dùng có thể chuyển tab khi đang nhập; không cần viết lại importer. Công việc này tồn tại trong tiến trình app, không cam kết tiếp tục nếu Android đóng app.
