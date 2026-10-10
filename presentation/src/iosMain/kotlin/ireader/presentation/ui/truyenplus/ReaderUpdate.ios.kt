package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ireader.i18n.BuildKonfig

@Composable
actual fun rememberReaderUpdatePlatform(): ReaderUpdatePlatform = remember {
    object : ReaderUpdatePlatform {
        override val installedApp = ReaderInstalledApp("", BuildKonfig.VERSION_CODE.toLong(), BuildKonfig.VERSION_NAME, 0, emptyList())
        override val supported = false
        override var endpoint = ""
        override suspend fun fetchManifest(url: String): ReaderUpdateManifest = error("Cập nhật trong ứng dụng hiện hỗ trợ Android.")
        override suspend fun download(update: ReaderAvailableUpdate, onProgress: (Float) -> Unit): String = error("Chỉ hỗ trợ Android.")
        override suspend fun install(path: String, update: ReaderAvailableUpdate): Boolean = false
    }
}
