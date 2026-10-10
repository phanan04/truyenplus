package ireader.presentation.ui.truyenplus

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ReaderUpdateTest {
    private val installed = ReaderInstalledApp("app.truyenplus", 80, "1.0", 33, listOf("arm64-v8a", "armeabi-v7a"))
    private val arm = ReaderUpdateAsset("arm64-v8a", "https://example.com/app.apk", "a".repeat(64), 1234)
    private val release = ReaderUpdateManifest(81, "1.1", "app.truyenplus", assets = listOf(arm))

    @Test fun choosesNativeAbiAndRejectsOlderVersions() {
        val all = release.copy(assets = listOf(arm.copy(abi = "universal"), arm))
        assertEquals("arm64-v8a", selectReaderUpdate(all, installed)?.asset?.abi)
        assertNull(selectReaderUpdate(release.copy(versionCode = 80), installed))
        assertNull(selectReaderUpdate(release.copy(versionCode = 79), installed))
    }

    @Test fun rejectsWrongAppIncompatibleDeviceAndUnverifiedDownloads() {
        assertFailsWith<IllegalArgumentException> { selectReaderUpdate(release.copy(packageName = "ireader.original"), installed) }
        assertFailsWith<IllegalArgumentException> { selectReaderUpdate(release.copy(minSdk = 35), installed) }
        assertFailsWith<IllegalArgumentException> { selectReaderUpdate(release.copy(assets = listOf(arm.copy(abi = "x86"))), installed) }
        assertFailsWith<IllegalArgumentException> { selectReaderUpdate(release.copy(assets = listOf(arm.copy(sha256 = ""))), installed) }
        assertFailsWith<IllegalArgumentException> { selectReaderUpdate(release.copy(assets = listOf(arm.copy(url = "http://example.com/app.apk"))), installed) }
        assertFailsWith<IllegalArgumentException> { requireReaderUpdateUrl("https://user:password@example.com/update.json") }
    }

    @Test fun downloadFailureCannotBecomeInstallableAndCanRetry() = runTest {
        val platform = FakePlatform()
        val manager = ReaderUpdateManager(platform)
        manager.check("https://example.com/update.json")
        assertEquals(ReaderUpdateStage.Available, manager.state.value.stage)
        platform.failDownload = true
        manager.download()
        assertEquals(ReaderUpdateStage.Available, manager.state.value.stage)
        assertNull(manager.state.value.downloadedPath)
        manager.install()
        assertEquals(0, platform.installs)
        platform.failDownload = false
        manager.download()
        assertEquals(ReaderUpdateStage.Ready, manager.state.value.stage)
        manager.install()
        assertEquals(1, platform.installs)
    }

    @Test fun noConfiguredEndpointDoesNotMakeNetworkRequests() = runTest {
        val platform = FakePlatform()
        val manager = ReaderUpdateManager(platform)
        manager.check("")
        assertEquals(0, platform.requests)
        assertEquals(ReaderUpdateStage.Error, manager.state.value.stage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cancellingDownloadReturnsToRetryWithoutAnInstallableFile() = runTest {
        val platform = FakePlatform()
        val manager = ReaderUpdateManager(platform)
        manager.check("https://example.com/update.json")
        platform.holdDownload = true
        val job = launch { manager.download() }
        runCurrent()
        assertEquals(ReaderUpdateStage.Downloading, manager.state.value.stage)
        job.cancel()
        job.join()
        assertEquals(ReaderUpdateStage.Available, manager.state.value.stage)
        assertNull(manager.state.value.downloadedPath)
    }

    private inner class FakePlatform : ReaderUpdatePlatform {
        override val installedApp = installed
        override val supported = true
        override var endpoint = ""
        var failDownload = false
        var holdDownload = false
        var requests = 0
        var installs = 0
        override suspend fun fetchManifest(url: String): ReaderUpdateManifest { requests++; return release }
        override suspend fun download(update: ReaderAvailableUpdate, onProgress: (Float) -> Unit): String {
            if (holdDownload) awaitCancellation()
            if (failDownload) error("Mất kết nối")
            onProgress(1f)
            return "/verified.apk"
        }
        override suspend fun install(path: String, update: ReaderAvailableUpdate): Boolean { installs++; return true }
    }
}
