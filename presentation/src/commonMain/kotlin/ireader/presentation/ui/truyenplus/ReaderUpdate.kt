package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.Composable
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

@Serializable
data class ReaderUpdateAsset(val abi: String, val url: String, val sha256: String, val sizeBytes: Long)

@Serializable
data class ReaderUpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val packageName: String,
    val minSdk: Int = 26,
    val notes: String = "",
    val assets: List<ReaderUpdateAsset>,
)

data class ReaderInstalledApp(val packageName: String, val versionCode: Long, val versionName: String,
    val sdk: Int, val abis: List<String>)
data class ReaderAvailableUpdate(val release: ReaderUpdateManifest, val asset: ReaderUpdateAsset)

fun requireReaderUpdateUrl(value: String): String {
    val text = value.trim()
    require(text.isNotBlank()) { "Chưa có địa chỉ cập nhật. Hãy thêm địa chỉ do người phát hành Truyện+ cung cấp." }
    val url = try { Url(text) } catch (_: Exception) { throw IllegalArgumentException("Địa chỉ cập nhật không hợp lệ.") }
    require(url.protocol.name == "https" && url.host.isNotBlank() && url.user.isNullOrEmpty() &&
        url.password.isNullOrEmpty() && url.fragment.isEmpty()) { "Địa chỉ cập nhật phải là liên kết HTTPS không chứa tài khoản hoặc mật khẩu." }
    return text
}

fun selectReaderUpdate(release: ReaderUpdateManifest, installed: ReaderInstalledApp): ReaderAvailableUpdate? {
    require(release.packageName == installed.packageName) { "Bản cập nhật không dành cho ứng dụng này." }
    require(release.versionCode > 0 && release.versionName.isNotBlank()) { "Thông tin phiên bản không hợp lệ." }
    if (release.versionCode <= installed.versionCode) return null
    require(release.minSdk <= installed.sdk) { "Bản mới yêu cầu phiên bản Android cao hơn." }
    val asset = installed.abis.firstNotNullOfOrNull { abi -> release.assets.firstOrNull { it.abi == abi } }
        ?: release.assets.firstOrNull { it.abi == "universal" }
        ?: throw IllegalArgumentException("Chưa có bản cập nhật tương thích với thiết bị này.")
    requireReaderUpdateUrl(asset.url)
    require(Regex("[a-fA-F0-9]{64}").matches(asset.sha256)) { "Bản cập nhật thiếu mã kiểm tra toàn vẹn hợp lệ." }
    require(asset.sizeBytes in 1..536_870_912L) { "Kích thước bản cập nhật không hợp lệ." }
    return ReaderAvailableUpdate(release, asset)
}

interface ReaderUpdatePlatform {
    val installedApp: ReaderInstalledApp
    val supported: Boolean
    var endpoint: String
    suspend fun fetchManifest(url: String): ReaderUpdateManifest
    /** Returns only after checking file integrity and installed-app compatibility. */
    suspend fun download(update: ReaderAvailableUpdate, onProgress: (Float) -> Unit): String
    /** False means Android's install permission settings were opened; the user can retry. */
    suspend fun install(path: String, update: ReaderAvailableUpdate): Boolean
}

@Composable
expect fun rememberReaderUpdatePlatform(): ReaderUpdatePlatform

enum class ReaderUpdateStage { Idle, Checking, Available, Current, Downloading, Ready, Error }
data class ReaderUpdateState(
    val stage: ReaderUpdateStage = ReaderUpdateStage.Idle,
    val update: ReaderAvailableUpdate? = null,
    val progress: Float = 0f,
    val downloadedPath: String? = null,
    val message: String? = null,
    val installing: Boolean = false,
) {
    val busy get() = installing || stage == ReaderUpdateStage.Checking || stage == ReaderUpdateStage.Downloading
}

class ReaderUpdateManager(private val platform: ReaderUpdatePlatform) {
    private val mutableState = MutableStateFlow(ReaderUpdateState())
    val state = mutableState.asStateFlow()

    suspend fun check(endpoint: String) {
        if (state.value.busy) return
        mutableState.value = ReaderUpdateState(stage = ReaderUpdateStage.Checking)
        try {
            require(platform.supported) { "Cập nhật trong ứng dụng hiện hỗ trợ Android." }
            val url = requireReaderUpdateUrl(endpoint)
            val update = selectReaderUpdate(platform.fetchManifest(url), platform.installedApp)
            platform.endpoint = url
            mutableState.value = ReaderUpdateState(
                stage = if (update == null) ReaderUpdateStage.Current else ReaderUpdateStage.Available, update = update)
        } catch (e: CancellationException) {
            mutableState.value = ReaderUpdateState()
            throw e
        } catch (e: Exception) {
            mutableState.value = ReaderUpdateState(stage = ReaderUpdateStage.Error,
                message = e.message ?: "Không kiểm tra được bản cập nhật. Hãy thử lại.")
        }
    }

    suspend fun download() {
        val current = state.value
        val update = current.update ?: return
        if (current.stage != ReaderUpdateStage.Available) return
        mutableState.value = current.copy(stage = ReaderUpdateStage.Downloading, progress = 0f, message = null, downloadedPath = null)
        try {
            val path = platform.download(update) { progress -> mutableState.update { it.copy(progress = progress.coerceIn(0f, 1f)) } }
            mutableState.value = state.value.copy(stage = ReaderUpdateStage.Ready, downloadedPath = path, progress = 1f)
        } catch (e: CancellationException) {
            mutableState.value = current.copy(message = "Đã hủy tải. Bạn có thể tải lại khi sẵn sàng.")
            throw e
        } catch (e: Exception) {
            mutableState.value = current.copy(message = e.message ?: "Tải bản cập nhật thất bại. Hãy thử lại.")
        }
    }

    suspend fun install() {
        val current = state.value
        val update = current.update ?: return
        val path = current.downloadedPath ?: return
        if (current.stage != ReaderUpdateStage.Ready || current.busy) return
        mutableState.value = current.copy(installing = true)
        try {
            val opened = platform.install(path, update)
            mutableState.update { it.copy(installing = false, message = if (opened) "Hãy xác nhận cập nhật trong cửa sổ Android."
                else "Bật quyền cài đặt cho Truyện+, quay lại rồi nhấn Cài đặt bản mới.") }
        } catch (e: CancellationException) {
            mutableState.value = current
            throw e
        } catch (e: Exception) {
            mutableState.value = current.copy(stage = ReaderUpdateStage.Available, downloadedPath = null,
                message = e.message ?: "Chưa thể mở trình cài đặt. Hãy tải lại bản cập nhật.")
        }
    }
}
