package ireader.presentation.ui.truyenplus

import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*

fun localizeReaderUpdateMessage(message: String, strings: LocalizeHelper): String {
    val resource = when (message) {
        "Chưa có địa chỉ cập nhật. Hãy thêm địa chỉ do người phát hành Truyện+ cung cấp." -> Res.string.tp_update_message_0
        "Địa chỉ cập nhật không hợp lệ." -> Res.string.tp_update_message_1
        "Địa chỉ cập nhật phải là liên kết HTTPS không chứa tài khoản hoặc mật khẩu." -> Res.string.tp_update_message_2
        "Bản cập nhật không dành cho ứng dụng này." -> Res.string.tp_update_message_3
        "Thông tin phiên bản không hợp lệ." -> Res.string.tp_update_message_4
        "Bản mới yêu cầu phiên bản Android cao hơn." -> Res.string.tp_update_message_5
        "Chưa có bản cập nhật tương thích với thiết bị này." -> Res.string.tp_update_message_6
        "Bản cập nhật thiếu mã kiểm tra toàn vẹn hợp lệ." -> Res.string.tp_update_message_7
        "Kích thước bản cập nhật không hợp lệ." -> Res.string.tp_update_message_8
        "Không kiểm tra được bản cập nhật. Hãy thử lại." -> Res.string.tp_update_message_9
        "Đã hủy tải. Bạn có thể tải lại khi sẵn sàng." -> Res.string.tp_update_message_10
        "Tải bản cập nhật thất bại. Hãy thử lại." -> Res.string.tp_update_message_11
        "Hãy xác nhận cập nhật trong cửa sổ Android." -> Res.string.tp_update_message_12
        "Bật quyền cài đặt cho Truyện+, quay lại rồi nhấn Cài đặt bản mới." -> Res.string.tp_update_message_13
        "Chưa thể mở trình cài đặt. Hãy tải lại bản cập nhật." -> Res.string.tp_update_message_14
        "Kích thước tải về khác thông tin phát hành." -> Res.string.tp_update_message_15
        "Tệp tải về lớn hơn kích thước dự kiến." -> Res.string.tp_update_message_16
        "Tệp tải chưa đầy đủ. Hãy thử lại." -> Res.string.tp_update_message_17
        "Bản tải về không khớp mã kiểm tra. Hãy kiểm tra lại nơi phát hành." -> Res.string.tp_update_message_18
        "Không lưu được bản cập nhật." -> Res.string.tp_update_message_19
        "Không tìm thấy bản đã tải. Hãy tải lại." -> Res.string.tp_update_message_20
        "Tệp cập nhật đã thay đổi. Hãy tải lại." -> Res.string.tp_update_message_21
        "Tệp tải về không phải APK hợp lệ." -> Res.string.tp_update_message_22
        "APK này không phải bản cập nhật của Truyện+." -> Res.string.tp_update_message_23
        "Phiên bản APK không khớp hoặc không mới hơn bản đang dùng." -> Res.string.tp_update_message_24
        "APK không hỗ trợ phiên bản Android này." -> Res.string.tp_update_message_25
        "Chữ ký bản cập nhật khác bản đang cài. Hãy liên hệ người phát hành." -> Res.string.tp_update_message_26
        "Địa chỉ chuyển hướng không hợp lệ." -> Res.string.tp_update_message_27
        "Địa chỉ cập nhật chuyển hướng quá nhiều lần." -> Res.string.tp_update_message_28
        "Thông tin cập nhật quá lớn." -> Res.string.tp_update_message_29
        else -> null
    }
    if (resource != null) return strings.localize(resource)
    val status = Regex("Máy chủ cập nhật trả lỗi (\\d+). Hãy thử lại sau.").matchEntire(message)?.groupValues?.get(1)?.toIntOrNull()
    return if (status != null) strings.localize(Res.string.tp_update_http_error, status) else message
}
