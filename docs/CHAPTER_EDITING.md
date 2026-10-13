# Biên tập chương

Trong màn hình đọc, mở `… → Biên tập chương`. Chọn **Sửa ngữ pháp** hoặc **Dịch sang tiếng Việt**, bấm **Tạo bản đề xuất**, xem các đoạn trước/sau rồi **Áp dụng bản sửa**. Có thể sửa tay từng đoạn đề xuất. **Khôi phục bản gốc** trả lại nội dung trước lần biên tập đầu tiên.

## Cấu hình

`Cài đặt → Cài đặt biên tập`: chọn Gemini hoặc DeepSeek, nhập model và API key của dịch vụ đó. Hai dịch vụ có key riêng. Để trống trường key khi lưu sẽ giữ key cũ; có nút xóa key. Key Google Drive không dùng ở đây. Mặc định: `gemini-3.8-flash` / `deepseek-flash`; có thể nhập model khác mà tài khoản hỗ trợ.

API key lưu bằng kho bảo mật Android có sẵn (Android Keystore + EncryptedSharedPreferences), tách khỏi nội dung truyện và loại khỏi Android backup/device transfer. Nền tảng không có kho bảo mật phần cứng không lưu key của chức năng này. Việc khởi tạo kho bảo mật thực hiện ngoài luồng giao diện.

## Nhân vật và quy tắc

- **Nhân vật**: nhập/sửa tên, bí danh/tên gốc, giới tính, quan hệ và cách xưng hô. Lưu riêng theo truyện; gửi cùng chương để AI có ngữ cảnh. Không tự phân tích cả kho sách.
- **Thay thế văn bản**: sử dụng repository và bộ xử lý IReader hiện có cho cả đọc và TTS. Có phạm vi từng truyện hoặc toàn bộ, bật/tắt, phân biệt hoa/thường, Regex, khớp nguyên từ và tìm/thay thông thường. Quy tắc mới ưu tiên cụm tìm dài hơn để tránh tên ngắn thay trước tên đầy đủ. Không thay đổi bản gốc.

## Dữ liệu và hiệu năng

Bản đề xuất chưa áp dụng chỉ nằm trong phiên làm việc. Chỉ sau thao tác áp dụng, nội dung chương được cập nhật trong SQLite; bản gốc được lưu một lần bằng giao dịch. Chỉ cập nhật `content`, giữ tiến độ đọc và bookmark hiện tại. Bản đề xuất cũ bị từ chối nếu chương đã thay đổi. Reader, TTS và xuất EPUB tiếp tục dùng đường đọc dữ liệu có sẵn. Cache dịch cũ của chương được xóa khi sửa/khôi phục để không che bản mới.

Chỉ gửi văn bản của chương được chọn cùng bộ nhớ nhân vật tới API chính thức; không gửi toàn bộ EPUB, key Drive hoặc sách khác. Một chương xử lý tuần tự các nhóm tối đa 6.000 ký tự / 40 đoạn, tối đa 120.000 ký tự mỗi chương. Đoạn đơn dài hơn 6.000 ký tự bị từ chối và cần chia đoạn trước. Không tải model local, không dịch tự động cả truyện, không gọi pipeline hậu kiểm nhiều lần, không tự đổi sang dịch vụ khác hoặc tự thử lại khi bị giới hạn.

Kiểm tra id và thứ tự từng đoạn, đoạn rỗng, trạng thái kết thúc của model; giữ các trang ảnh/media và đoạn trắng. Có tiến độ và hủy. Kiểm tra này bảo vệ cấu trúc; không chứng minh ngữ nghĩa bản sửa đúng. Người đọc cần xem lại trước khi áp dụng. Bộ nhớ nhân vật hướng dẫn AI, không bảo đảm tuyệt đối cách xưng hô.

Triển khai độc lập dựa trên luồng sử dụng NovelTools, không sao chép mã hoặc prompt của website.

## Tài liệu API

- [Gemini models](https://ai.google.dev/gemini-api/docs/models)
- [Gemini generateContent](https://ai.google.dev/api/generate-content)
- [DeepSeek chat completions](https://api-docs.deepseek.com/api/create-chat-completion/)

Không có yêu cầu AI trả phí thật trong kiểm thử; chất lượng trên một chương thực tế cần kiểm tra sau khi người dùng nhập key của mình. Có từ APK 2.0.37.
