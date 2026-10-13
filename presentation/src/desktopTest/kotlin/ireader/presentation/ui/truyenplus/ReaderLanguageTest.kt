package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import java.io.File
import java.util.Locale
import kotlin.test.*
import kotlinx.coroutines.cancelAndJoin
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
class ReaderLanguageTest {
    @Test fun vietnameseSourcesShowTheirOwnNamesAndLoadMore() {
        val previous = Locale.getDefault()
        val source = ireader.domain.models.discovery.DiscoverySource.TruyenHH
        val book = ireader.data.discovery.NovelTrackerParser.parse(source,
            requireNotNull(javaClass.getResourceAsStream("/discovery/truyenhh.json")).bufferedReader().use { it.readText() }).books.single()
            .copy(recentlyUpdated = true)
        val feed = ireader.domain.models.discovery.DiscoveryFeed(source, source.board, source.feedUrl, "2026-10-10", "2026-10-10 20:00:00 UTC",
            listOf(book), nextPageUrls = listOf("https://truyenhh.com/truyen/page/2/"))
        var loaded: ireader.domain.models.discovery.DiscoverySource? = null
        try {
            Locale.setDefault(Locale.forLanguageTag("vi"))
            val scene = ImageComposeScene(width = 800, height = 1000, density = Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme = TruyenPlusDarkColors) { Surface {
                        DiscoveryScreen(ireader.domain.models.discovery.DiscoveryState(feeds = mapOf(source to feed)), {}, onLoadMore = { loaded = it })
                    } }
                }
            }
            try {
                scene.render(0); scene.render(1_000_000_000)
                assertTrue("TruyenYY" in texts(scene))
                click(scene, "TruyenHH"); scene.render(2_000_000_000); scene.render(3_000_000_000)
                val visible = texts(scene).joinToString("\n")
                assertTrue("Đọc nhiều trong tuần" in visible)
                assertTrue("trực tiếp từ TruyenHH" in visible)
                assertFalse("qua novel-tracker" in visible)
                click(scene, "Mới cập nhật"); scene.render(4_000_000_000); scene.render(5_000_000_000)
                assertTrue("Tải thêm" in texts(scene))
                assertFalse(texts(scene).any { it.contains("trên TruyenFull") })
                click(scene, "Tải thêm"); assertEquals(source, loaded)
                val output = File("build/reports/truyenplus-v2/discovery-truyenhh-800.png")
                output.parentFile.mkdirs(); output.writeBytes(requireNotNull(scene.render(6_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally { scene.close() }
        } finally { Locale.setDefault(previous) }
    }
    @Test fun vietnameseDiscoveryUsesNativeMetadataAndUpdatedListLabels() {
        val previous = Locale.getDefault()
        val source = ireader.domain.models.discovery.DiscoverySource.TruyenFull
        fun html(name: String) = requireNotNull(javaClass.getResourceAsStream("/discovery/truyenfull-$name.html")).bufferedReader().use { it.readText() }
        val book = ireader.data.discovery.TruyenFullParser.details(ireader.data.discovery.TruyenFullParser.listings(html("hot"), html("new")).first(), html("detail"))
        val feed = ireader.domain.models.discovery.DiscoveryFeed(source, source.board, source.feedUrl, "2026-10-10", "2026-10-10 20:00:00 UTC", listOf(book))
        try {
            Locale.setDefault(Locale.forLanguageTag("vi"))
            for (width in listOf(360, 800)) {
                val scene = ImageComposeScene(width = width, height = 1000, density = Density(1f)) {
                    CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                        MaterialTheme(colorScheme = TruyenPlusDarkColors) { Surface { DiscoveryScreen(ireader.domain.models.discovery.DiscoveryState(feeds = mapOf(source to feed)), {}) } }
                    }
                }
                try {
                    scene.render(0); scene.render(1_000_000_000)
                    click(scene, "TruyenFull")
                    scene.render(2_000_000_000)
                    val bitmap = scene.render(3_000_000_000)
                    val visible = texts(scene).joinToString("\n")
                    assertTrue(book.title in visible && book.author in visible)
                    assertTrue("Mới cập nhật" in visible)
                    assertTrue("Đọc nhiều trong ngày" in visible)
                    assertFalse("qua novel-tracker" in visible)
                    assertFalse("ba nguồn" in visible)
                    assertFalse("Đang chuẩn bị bản dịch" in visible)
                    assertFalse(nodes(scene).any { it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { value -> value.contains("Powered by") } == true })
                    val output = File("build/reports/truyenplus-v2/discovery-vietnamese-$width.png")
                    output.parentFile.mkdirs(); output.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
                    click(scene, "Truyện hot")
                    scene.render(4_000_000_000); scene.render(5_000_000_000)
                    val hotText = texts(scene)
                    assertTrue(hotText.any { it.contains("Hot trong tháng") })
                    assertFalse("#1" in hotText) // The hot list does not publish monthly ranks.
                } finally { scene.close() }
            }
        } finally { Locale.setDefault(previous) }
    }
    @Test fun catalogGenresUseReviewedLabelsInsteadOfCachedMachineTranslations() {
        val previous = Locale.getDefault()
        val book = ireader.data.discovery.NovelTrackerParser.parse(ireader.domain.models.discovery.DiscoverySource.Qidian,
            requireNotNull(javaClass.getResourceAsStream("/discovery/qidian.json")).bufferedReader().use { it.readText() }).books.first()
            .copy(genres = listOf("东方玄幻", "两宋元明", "两晋隋唐", "原生幻想", "衍生同人", "都市高武", "架空历史", "纯爱", "百合"))
        // Regression inputs reproduce the poor cached machine labels reported on Android.
        val translation = ireader.domain.data.repository.DiscoveryTranslation(book, "vi", book.title,
            book.description, book.genres.map { "Hai triều đại bài hát" }, book.status)
        try {
            for ((language, expected) in listOf(
                "vi" to listOf("Huyền huyễn phương Đông", "Tống – Nguyên – Minh", "Tấn – Tùy – Đường", "Kỳ ảo nguyên bản", "Đồng nhân", "Đô thị cao võ", "Lịch sử giả tưởng", "Đam mỹ", "Bách hợp"),
                "en" to listOf("Eastern fantasy", "Song, Yuan and Ming dynasties", "Jin, Sui and Tang dynasties", "Original fantasy", "Fan fiction", "Urban martial fantasy", "Alternate history", "Boys love", "Girls love"))) {
                Locale.setDefault(Locale.forLanguageTag(language))
                val scene = ImageComposeScene(width = 600, height = 1000, density = Density(1f)) {
                    CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                        MaterialTheme { DiscoveryDetailsContent(book, translation, {}) }
                    }
                }
                try {
                    scene.render(0)
                    val visible = texts(scene).joinToString("\n")
                    expected.forEach { assertTrue(it in visible, "Missing $language genre: $it") }
                    assertFalse("Hai triều đại bài hát" in visible)
                } finally { scene.close() }
            }
        } finally { Locale.setDefault(previous) }
    }
    private fun nodes(scene: ImageComposeScene): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return scene.semanticsOwners.flatMap { walk(it.rootSemanticsNode) }
    }
    private fun texts(scene: ImageComposeScene) = nodes(scene).flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
    private fun click(scene: ImageComposeScene, text: String) {
        fun containsText(node: SemanticsNode): Boolean = node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true || node.children.any(::containsText)
        val node = nodes(scene).first { it.config.getOrNull(SemanticsActions.OnClick) != null && containsText(it) }
        assertTrue(requireNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }
    @Test fun settingsSwitchesLanguageAndNavigationWhileKeepingProgressAccessible() {
        val previous = Locale.getDefault()
        try {
            var language by mutableStateOf(ReaderUiLanguage.Vietnamese)
            var openedProgress = false
            var openedUpdate = false
            Locale.setDefault(Locale.forLanguageTag(language.code))
            val scene = ImageComposeScene(width = 360, height = 800, density = Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme = TruyenPlusDarkColors) {
                        key(language) {
                            ReaderBackdrop {
                                ReaderSettingsScreen(language, { language = it; Locale.setDefault(Locale.forLanguageTag(it.code)) }, { openedProgress = true }, { openedUpdate = true })
                                Box(Modifier.align(Alignment.BottomCenter)) { ReaderBottomNav(ReaderDestination.Settings, {}) }
                            }
                        }
                    }
                }
            }
            try {
                scene.render(0)
                assertTrue("Ngôn ngữ ứng dụng" in texts(scene))
                assertFalse("Tiến độ" in texts(scene)) // There is no Progress bottom tab.
                click(scene, "Ngôn ngữ ứng dụng"); scene.render(100_000_000); scene.render(200_000_000)
                click(scene, "English"); scene.render(1_000_000_000); scene.render(1_500_000_000)
                assertTrue("App language" in texts(scene))
                assertTrue("Settings" in texts(scene))
                assertFalse("Cài đặt" in texts(scene))
                click(scene, "Reading progress"); assertTrue(openedProgress)
                click(scene, "App updates"); assertTrue(openedUpdate)
                val output = File("build/reports/truyenplus-v2/settings-en.png"); output.parentFile.mkdirs()
                output.writeBytes(requireNotNull(scene.render(2_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
                click(scene, "App language"); scene.render(2_100_000_000); scene.render(2_200_000_000)
                click(scene, "Tiếng Việt"); scene.render(3_000_000_000); scene.render(3_500_000_000)
                assertTrue("Cài đặt" in texts(scene))
                assertTrue("Tiến độ đọc" in texts(scene))
                File("build/reports/truyenplus-v2/settings-vi.png").writeBytes(requireNotNull(scene.render(4_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally { scene.close() }
        } finally { Locale.setDefault(previous) }
    }
    @Test fun applicationLocaleChangeKeepsRememberedScreenState() {
        val previous = Locale.getDefault()
        val directory = java.nio.file.Files.createTempDirectory("reader-language-").toFile()
        val dataScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = dataScope,
            produceFile = { File(directory, "language.preferences_pb") })
        val preferences = ireader.domain.preferences.prefs.UiPreferences(ireader.core.prefs.DataStorePreferenceStore(dataStore))
        preferences.language().set("vi")
        val helper = ireader.presentation.core.theme.LocaleHelper(preferences)
        helper.setLocaleLang()
        var compositionsCreated = 0
        val scene = ImageComposeScene(width = 360, height = 800, density = Density(1f)) {
            ireader.presentation.core.theme.AppLocaleProvider(helper) {
                remember { compositionsCreated++ }
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme = TruyenPlusDarkColors) {
                        ReaderHomeScreen(emptyList(), false, {}, {}, {}, {})
                    }
                }
            }
        }
        try {
            scene.render(0)
            assertTrue("Không gian đọc của bạn" in texts(scene))
            preferences.language().set("en"); helper.updateLocal(); scene.render(1_000_000_000)
            assertTrue("Your reading space" in texts(scene))
            assertEquals("en", ireader.presentation.core.theme.LocaleHelper(preferences).getCurrentLanguageCode())
            assertEquals(1, compositionsCreated, "Changing language must preserve the current screen and its state")
        } finally { scene.close(); kotlinx.coroutines.runBlocking { requireNotNull(dataScope.coroutineContext[kotlinx.coroutines.Job]).cancelAndJoin() }; directory.deleteRecursively(); Locale.setDefault(previous) }
    }
    @Test fun translatedMetadataCanBeSearchedAndOriginalCanBeOpenedWithoutChangingAuthorOrRank() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            val book = ireader.data.discovery.NovelTrackerParser.parse(ireader.domain.models.discovery.DiscoverySource.Qidian,
                requireNotNull(javaClass.getResourceAsStream("/discovery/qidian.json")).bufferedReader().use { it.readText() }).books.first()
            val translated = ireader.domain.data.repository.DiscoveryTranslation(book, "en", "Test translation of a real source record", "Translated description for UI verification", book.genres.map { "Fantasy" }, "Ongoing")
            val feed = ireader.domain.models.discovery.DiscoveryFeed(book.source, "", "", "2026-10-10", "", listOf(book))
            assertEquals(listOf(book), filterDiscoveryBooks(feed, DiscoverySection.Ranking, "", "translation", mapOf(book.key to translated)))
            assertEquals(listOf(book), filterDiscoveryBooks(feed, DiscoverySection.Ranking, "", book.title, mapOf(book.key to translated)))
            val scene = ImageComposeScene(width = 360, height = 1000, density = Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme = TruyenPlusDarkColors) { DiscoveryDetailsContent(book, translated, {}) }
                }
            }
            try {
                scene.render(0)
                assertTrue(translated.title in texts(scene)); assertTrue(book.author in texts(scene))
                assertTrue(texts(scene).any { it.contains("Rank ${book.rank}") })
                click(scene, "Show original"); scene.render(1_000_000_000)
                assertTrue(book.title in texts(scene)); assertTrue(book.description in texts(scene))
                assertFalse(translated.title in texts(scene))
                click(scene, "Show translation"); scene.render(2_000_000_000)
                assertTrue(translated.title in texts(scene))
            } finally { scene.close() }
        } finally { Locale.setDefault(previous) }
    }
}
