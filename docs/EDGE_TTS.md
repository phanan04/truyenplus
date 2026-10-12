# Edge TTS

Android có hai lựa chọn trực tuyến: **Hoài My** (nữ) và **Nam Minh** (nam), đặt trước giọng hệ thống trong **Tùy chỉnh nghe → Giọng đọc**. Lựa chọn được lưu; giọng hệ thống vẫn là mặc định khi chưa chọn Edge. Không yêu cầu người dùng nhập API key.

## Hoạt động

- Dùng bộ điều khiển TTS và dịch vụ nghe hiện có; vẫn chọn đoạn trên trang đọc, nghe tiếp chương, điều chỉnh tốc độ/cao độ, nhạc nền và điều khiển qua tai nghe.
- Tổng hợp qua Internet, chuẩn bị MP3 bất đồng bộ, không tải model về máy.
- Một yêu cầu hiện tại và một đoạn kế tiếp; cache RAM tối đa 3 đoạn / 2 MiB. File MP3 tạm được xóa khi thay đoạn hoặc dừng phát.
- Dừng, đổi đoạn hoặc đổi giọng hủy yêu cầu cũ. Phản hồi muộn không được phát lại.
- Nếu Edge trả lỗi hoặc không có âm thanh, chuyển sang giọng hệ thống tại chính đoạn đó và báo trong bảng tùy chỉnh. Không đánh dấu đoạn lỗi là đã đọc xong. Nếu lỗi xảy ra giữa đoạn, giọng dự phòng đọc lại đoạn từ đầu.

## Giới hạn và kiểm tra

Đây là giao thức Read Aloud của Edge, không phải Azure Speech API có cam kết dịch vụ. Cần Internet; dịch vụ có thể thay đổi hoặc trả âm thanh rỗng. Trong thử nghiệm, cả hai giọng trả MP3 thật ở câu ngắn; Hoài My cũng trả âm thanh cho đoạn dài hơn. Nam Minh có một số đoạn dài trả rỗng ngay cả với client `edge-tts` tham chiếu. Vì vậy không cam kết Nam Minh tổng hợp được mọi đoạn; đường dự phòng giữ việc đọc liên tục.

Transport Kotlin được kiểm tra trực tiếp qua mạng bằng `EdgeLiveProbeTest` khi đặt `EDGE_TTS_LIVE=1`. Kiểm thử thường không cần mạng. `scripts/verify-edge-audio.py` chạy mã MediaPlayer Android thực với callback kiểm soát được; kiểm thử Compose kiểm tra thứ tự giọng và callback chọn giọng. Chưa kiểm thử trên thiết bị Android thật.

Mã giao thức được triển khai độc lập; Python `edge-tts` chỉ dùng làm công cụ đối chiếu trong thư mục phát triển bị Git bỏ qua, không đóng gói vào app.

Tham khảo: [Microsoft voices](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support), [edge-tts](https://github.com/rany2/edge-tts).
