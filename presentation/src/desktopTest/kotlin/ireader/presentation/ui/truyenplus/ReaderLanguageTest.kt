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
                click(scene, "English"); scene.render(1_000_000_000)
                assertTrue("App language" in texts(scene))
                assertTrue("Settings" in texts(scene))
                assertFalse("Cài đặt" in texts(scene))
                click(scene, "Reading progress"); assertTrue(openedProgress)
                click(scene, "App updates"); assertTrue(openedUpdate)
                val output = File("build/reports/truyenplus-v2/settings-en.png"); output.parentFile.mkdirs()
                output.writeBytes(requireNotNull(scene.render(2_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
                click(scene, "Tiếng Việt"); scene.render(3_000_000_000)
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
