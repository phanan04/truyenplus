package ireader.presentation.ui.truyenplus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Composable
actual fun rememberReaderUpdatePlatform(): ReaderUpdatePlatform {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidReaderUpdatePlatform(context) }
}

@Suppress("DEPRECATION")
private class AndroidReaderUpdatePlatform(private val context: Context) : ReaderUpdatePlatform {
    private val preferences = context.getSharedPreferences("truyenplus_updates", Context.MODE_PRIVATE)
    private val packageManager = context.packageManager
    private val defaultEndpoint = packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        .metaData?.getString("truyenplus.UPDATE_MANIFEST_URL").orEmpty()
    private val installed = packageManager.getPackageInfo(context.packageName, signatureFlags())
    override val installedApp = ReaderInstalledApp(context.packageName, installed.code(),
        installed.versionName.orEmpty(), Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.toList())
    override val supported = true
    override var endpoint: String
        get() = preferences.getString("manifest_url", defaultEndpoint).orEmpty()
        set(value) { preferences.edit().putString("manifest_url", value).apply() }
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchManifest(url: String): ReaderUpdateManifest = withContext(Dispatchers.IO) {
        val connection = connect(url)
        try {
            val bytes = connection.inputStream.use { it.readBytesLimited(1_048_576) }
            json.decodeFromString<ReaderUpdateManifest>(bytes.decodeToString())
        } finally { connection.disconnect() }
    }

    override suspend fun download(update: ReaderAvailableUpdate, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "truyenplus-updates").apply { mkdirs() }
        // Fixed app-owned names; remote filenames never become filesystem paths.
        val partial = File(folder, "download.part.apk")
        val target = File(folder, "update.apk")
        partial.delete()
        target.delete()
        val connection = connect(update.asset.url)
        try {
            val expected = update.asset.sizeBytes
            val declared = connection.contentLengthLong
            require(declared < 0 || declared == expected) { "Kích thước tải về khác thông tin phát hành." }
            var received = 0L
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        require(received <= expected) { "Tệp tải về lớn hơn kích thước dự kiến." }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val percent = (received * 100 / expected).toInt()
                        if (percent != lastPercent) { lastPercent = percent; onProgress(received.toFloat() / expected) }
                    }
                }
            }
            require(received == expected) { "Tệp tải chưa đầy đủ. Hãy thử lại." }
            require(digest.digest().hex().equals(update.asset.sha256, ignoreCase = true)) { "Bản tải về không khớp mã kiểm tra. Hãy kiểm tra lại nơi phát hành." }
            verifyIdentity(partial, update)
            check(partial.renameTo(target)) { "Không lưu được bản cập nhật." }
            target.absolutePath
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }

    override suspend fun install(path: String, update: ReaderAvailableUpdate): Boolean {
        withContext(Dispatchers.IO) {
            val file = File(path)
            require(file.canonicalFile == File(context.cacheDir, "truyenplus-updates/update.apk").canonicalFile && file.isFile) { "Không tìm thấy bản đã tải. Hãy tải lại." }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            require(digest.digest().hex().equals(update.asset.sha256, true)) { "Tệp cập nhật đã thay đổi. Hãy tải lại." }
            verifyIdentity(file, update)
        }
        return withContext(Dispatchers.Main) {
            if (!packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                false
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
                true
            }
        }
    }

    private fun verifyIdentity(file: File, update: ReaderAvailableUpdate) {
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, signatureFlags())
            ?: error("Tệp tải về không phải APK hợp lệ.")
        require(archive.packageName == context.packageName) { "APK này không phải bản cập nhật của Truyện+." }
        require(archive.code() == update.release.versionCode && archive.code() > installedApp.versionCode) { "Phiên bản APK không khớp hoặc không mới hơn bản đang dùng." }
        require((archive.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= Build.VERSION.SDK_INT) { "APK không hỗ trợ phiên bản Android này." }
        val currentSigners = installed.signers()
        val incomingSigners = archive.signers()
        require(currentSigners.isNotEmpty() && currentSigners == incomingSigners) { "Chữ ký bản cập nhật khác bản đang cài. Hãy liên hệ người phát hành." }
    }

    private fun PackageInfo.code(): Long = if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()
    private fun signatureFlags() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    private fun PackageInfo.signers(): Set<String> {
        val certificates = if (Build.VERSION.SDK_INT >= 28) signingInfo?.apkContentsSigners else signatures
        return certificates.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex() }.toSet()
    }

    private fun connect(address: String): HttpURLConnection {
        var url = URL(requireReaderUpdateUrl(address))
        repeat(6) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "TruyenPlus-Android")
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: error("Địa chỉ chuyển hướng không hợp lệ.")
                    url = URL(requireReaderUpdateUrl(URL(url, location).toString()))
                    connection.disconnect()
                } else {
                    check(status == 200) { "Máy chủ cập nhật trả lỗi $status. Hãy thử lại sau." }
                    return connection
                }
            } catch (e: Exception) { connection.disconnect(); throw e }
        }
        error("Địa chỉ cập nhật chuyển hướng quá nhiều lần.")
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= limit) { "Thông tin cập nhật quá lớn." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
