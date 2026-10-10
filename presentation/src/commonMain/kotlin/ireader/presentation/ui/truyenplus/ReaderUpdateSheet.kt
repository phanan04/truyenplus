package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderUpdateSheet(version: String, endpoint: String, supported: Boolean, state: ReaderUpdateState,
    onCheck: (String) -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit,
    onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ReaderUpdateContent(version, endpoint, supported, state, onCheck, onDownload, onInstall, onCancel)
    }
}

@Composable
fun ReaderUpdateContent(version: String, endpoint: String, supported: Boolean, state: ReaderUpdateState,
    onCheck: (String) -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit) {
    var address by rememberSaveable(endpoint) { mutableStateOf(endpoint) }
    var showAddress by rememberSaveable { mutableStateOf(endpoint.isBlank()) }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            Text("Cập nhật ứng dụng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Truyện+ · Phiên bản $version", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!supported) {
                Text("Cập nhật trong ứng dụng hiện hỗ trợ Android.")
            } else {
                if (showAddress) {
                    Text("Địa chỉ cập nhật", style = MaterialTheme.typography.titleSmall)
                    Text("Nhập liên kết do người phát hành Truyện+ cung cấp.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                        label = { Text("Liên kết HTTPS") }, singleLine = true)
                } else {
                    TextButton(onClick = { showAddress = true }, enabled = !state.busy) { Text("Đổi địa chỉ cập nhật") }
                }
                when (state.stage) {
                    ReaderUpdateStage.Idle -> Text(if (endpoint.isBlank()) "Chưa thiết lập nơi nhận bản cập nhật." else "Kiểm tra để nhận phiên bản Truyện+ mới nhất.")
                    ReaderUpdateStage.Checking -> { Text("Đang kiểm tra phiên bản…"); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    ReaderUpdateStage.Current -> Text("Bạn đang dùng phiên bản mới nhất được phát hành tại địa chỉ này.")
                    ReaderUpdateStage.Available, ReaderUpdateStage.Downloading, ReaderUpdateStage.Ready -> {
                        state.update?.let { update ->
                            Text("Có phiên bản ${update.release.versionName}", style = MaterialTheme.typography.titleMedium)
                            Text("${update.asset.sizeBytes / 1_048_576} MB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (update.release.notes.isNotBlank()) Text(update.release.notes.take(6000), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (state.stage == ReaderUpdateStage.Downloading) {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            Text("Đang tải · ${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.stage == ReaderUpdateStage.Ready) Text("Đã tải và kiểm tra. Sẵn sàng cài đặt.")
                    }
                    ReaderUpdateStage.Error -> Unit
                }
                state.message?.let { Text(it, color = if (state.stage == ReaderUpdateStage.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                when (state.stage) {
                    ReaderUpdateStage.Checking, ReaderUpdateStage.Downloading -> OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Hủy") }
                    ReaderUpdateStage.Available -> Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Tải bản mới") }
                    ReaderUpdateStage.Ready -> Button(onClick = onInstall, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.installing) "Đang kiểm tra tệp…" else "Cài đặt bản mới") }
                    else -> Button(onClick = { onCheck(address) }, enabled = address.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra cập nhật") }
                }
                if (showAddress && state.stage in listOf(ReaderUpdateStage.Available, ReaderUpdateStage.Ready, ReaderUpdateStage.Current)) {
                    TextButton(onClick = { onCheck(address) }, enabled = !state.busy && address.isNotBlank()) { Text("Kiểm tra địa chỉ này") }
                }
                Text("Android sẽ yêu cầu bạn xác nhận khi cài đặt bản mới.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
}
