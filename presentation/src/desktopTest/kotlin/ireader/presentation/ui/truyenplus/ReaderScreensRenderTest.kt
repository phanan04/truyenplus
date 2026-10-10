package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.*
import ireader.presentation.ui.reader.ReaderChapterTopBar
import ireader.presentation.ui.reader.ReaderTextPreferencesState
import ireader.presentation.ui.reader.StyleTextOptimized
import ireader.domain.models.prefs.PreferenceValues
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import ireader.domain.models.entities.Chapter
import ireader.domain.models.entities.ReadingStatisticsType1
import ireader.presentation.ui.home.tts.CommonTTSScreenState
import ireader.presentation.ui.home.tts.CommonTTSActions
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.Shapes
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/** Visual verification of production layouts. Fixtures exist only in the test source set. */
class ReaderScreensRenderTest {
    @Composable
    private fun ReaderTestTheme(colorScheme: ColorScheme, shapes: androidx.compose.material3.Shapes = Shapes, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
            MaterialTheme(colorScheme = colorScheme, shapes = shapes, content = content)
        }
    }

    private val books = listOf(
        ReaderBookUi(1, "Hồ Yêu Tiểu Thư", read = 245, total = 520, pinned = true, chapter = "Chương 245 · Tương phùng nơi nhân gian"),
        ReaderBookUi(2, "Thần Vực", read = 102, total = 680),
        ReaderBookUi(3, "Bảy Kiếp May Mắn", read = 12, total = 300),
        ReaderBookUi(4, "Những ngày bình yên", total = 80)
    )

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun renderProductionScreensAtPhoneWidth() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val pages: Map<String, @Composable () -> Unit> = linkedMapOf(
            "home" to { Shell(ReaderDestination.Home) { ReaderHomeScreen(books, false, {}, {}, {}, {}, onUpdate = {}) } },
            "library" to { Shell(ReaderDestination.Library) {
                ReaderLibraryScreen(books, listOf("Tất cả", "Yêu thích", "Đang đọc"), 0, "", false, false, emptySet(), {}, {}, {}, {}, {}, {}, {}, {})
            } },
            "progress" to { Shell(ReaderDestination.Settings) {
                ReaderProgressScreen(ReadingStatisticsType1(totalChaptersRead = 359, totalReadingTimeMinutes = 2175, readingStreak = 21, booksCompleted = 12), books, emptyList(), {}, {}, {})
            } },
            "book" to { ReaderBookScreen(books.first(), "Tác giả của bạn", (1..20).map {
                Chapter(id = it.toLong(), bookId = 1, key = "chapter-$it", name = "Chương $it · Một câu chuyện mới", number = it.toFloat(), read = it < 5, bookmark = it == 4)
            }, {}, {}, {}, {}, {}) },
            "player" to { Column {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    IconButton(onClick = {}) { Icon(Icons.Default.KeyboardArrowDown, "Quay lại") }
                    Text("ĐANG NGHE", modifier = Modifier.weight(1f).padding(top = 16.dp), style = MaterialTheme.typography.labelMedium)
                    IconButton(onClick = {}) { Icon(Icons.Default.List, "Chương") }
                }
                ReaderNowPlayingScreen(books.first(), books.first().chapter, "Gió đêm thổi qua, mang theo hương hoa mộc lan thoang thoảng. Ánh trăng phủ lên vai, như rót bạc xuống nhân gian.", true, .47f, 23, 50, 1f, {}, {}, {}, {}, {}, {}, {})
            } },
            "reader" to { ReadingChapterPreview() },
            "listening-chapter" to { ReaderListenChapterScreen(CommonTTSScreenState(
                content = chapterParagraphs, bookTitle = books.first().title, chapterName = "Chương 245 · Tương phùng nơi nhân gian",
                currentReadingParagraph = 1, isPlaying = true), PreviewTtsActions(), {}) },
            "empty" to { Shell(ReaderDestination.Home, miniPlayer = false) { ReaderHomeScreen(emptyList(), false, {}, {}, {}, {}) } },
            "update" to { Surface(Modifier.fillMaxWidth().padding(top = 24.dp)) { ReaderUpdateContent("2.0.26", "https://example.com/update.json", true,
                ReaderUpdateState(stage = ReaderUpdateStage.Available, update = ReaderAvailableUpdate(
                    ReaderUpdateManifest(81, "2.0.27", "app.truyenplus", notes = "Giao diện gọn hơn, cải thiện trải nghiệm đọc và nghe.", assets = emptyList()),
                    ReaderUpdateAsset("arm64-v8a", "https://example.com/app.apk", "a".repeat(64), 90_000_000))),
                {}, {}, {}, {}) } }
        )
        for ((name, content) in pages) {
            val scene = ImageComposeScene(width = 360, height = 800, density = Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    ReaderTestTheme(colorScheme = TruyenPlusDarkColors, shapes = Shapes) {
                        ReaderBackdrop { content() }
                    }
                }
            }
            try {
                scene.render(0)
                val bitmap = scene.render(1_000_000_000)
                val data = requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG))
                val output = File("build/reports/truyenplus-v2/$name.png")
                output.parentFile.mkdirs()
                output.writeBytes(data.bytes)
                assertTrue(output.length() > 1000, "Screen $name should render")
            } finally { scene.close() }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun discoveryRendersRealMetadataAtPhoneAndTabletWidths() {
        val raw = requireNotNull(javaClass.getResourceAsStream("/discovery/qidian.json")).bufferedReader().use { it.readText() }
        val feed = ireader.data.discovery.NovelTrackerParser.parse(ireader.domain.models.discovery.DiscoverySource.Qidian, raw)
        for (width in listOf(360, 800)) {
            val scene = ImageComposeScene(width = width, height = 1000, density = Density(1f)) {
                ReaderTestTheme(colorScheme = TruyenPlusDarkColors) {
                    ReaderBackdrop { Shell(ReaderDestination.Discover, miniPlayer = false) {
                        DiscoveryScreen(ireader.domain.models.discovery.DiscoveryState(feeds = mapOf(feed.source to feed)), {})
                    } }
                }
            }
            try {
                scene.render(0)
                val bitmap = scene.render(1_000_000_000)
                fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
                val text = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
                    .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
                assertTrue("青山" in text && "会说话的肘子" in text)
                assertTrue(text.any { it.contains("2026-10-10") })
                val output = File("build/reports/truyenplus-v2/discovery-$width.png")
                output.parentFile.mkdirs()
                output.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally { scene.close() }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun discoverySourceChipSwitchesToRealFanqieData() {
        val feeds = ireader.domain.models.discovery.DiscoverySource.entries.associateWith { source ->
            val raw = requireNotNull(javaClass.getResourceAsStream("/discovery/${source.id}.json")).bufferedReader().use { it.readText() }
            ireader.data.discovery.NovelTrackerParser.parse(source, raw)
        }
        val scene = ImageComposeScene(width = 360, height = 1000, density = Density(1f)) {
            ReaderTestTheme(colorScheme = TruyenPlusDarkColors) {
                ReaderBackdrop { DiscoveryScreen(ireader.domain.models.discovery.DiscoveryState(feeds = feeds), {}) }
            }
        }
        try {
            scene.render(0); scene.render(1_000_000_000)
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            fun nodes() = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            // The source list now overflows a phone. Swipe it before selecting a Chinese source.
            val sourceRow = nodes().first { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null }
            assertTrue(requireNotNull(sourceRow.config.getOrNull(SemanticsActions.ScrollToIndex)?.action).invoke(4))
            scene.render(2_000_000_000); scene.render(3_000_000_000)
            val chip = nodes().first { it.config.getOrNull(SemanticsActions.OnClick)?.action != null &&
                it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "Fanqie" } == true }
            assertTrue(requireNotNull(chip.config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
            scene.render(4_000_000_000); scene.render(5_000_000_000)
            val text = nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
            assertTrue("神通者" in text && "天蚕土豆" in text)
            assertTrue("青山" !in text)
        } finally { scene.close() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun discoveryDetailsShowsOriginalDescriptionAndOpensOfficialUrl() {
        val raw = requireNotNull(javaClass.getResourceAsStream("/discovery/jjwxc.json")).bufferedReader().use { it.readText() }
        val book = ireader.data.discovery.NovelTrackerParser.parse(ireader.domain.models.discovery.DiscoverySource.Jinjiang, raw).books.single()
        var opened: String? = null
        val scene = ImageComposeScene(width = 360, height = 1000, density = Density(1f)) {
            ReaderTestTheme(colorScheme = TruyenPlusDarkColors) { Surface { DiscoveryDetailsContent(book) { opened = it } } }
        }
        try {
            scene.render(0)
            val bitmap = scene.render(1_000_000_000)
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            val nodes = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            val text = nodes.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
            assertTrue(book.description in text && book.title in text && book.author in text)
            val button = nodes.first { it.config.getOrNull(SemanticsActions.OnClick)?.action != null &&
                it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "Xem trên Jinjiang" } == true }
            requireNotNull(button.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(book.url, opened)
            val output = File("build/reports/truyenplus-v2/discovery-details.png")
            output.parentFile.mkdirs()
            output.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
        } finally { scene.close() }
    }

    private val chapterParagraphs = listOf(
        "Gió đêm thổi qua, mang theo hương hoa mộc lan thoang thoảng. Nàng đứng trên bậc thềm cũ, ánh trăng phủ lên vai, như rót bạc xuống nhân gian.",
        "Đã bao năm trôi qua, gặp lại người trong hoàn cảnh này, trái tim vẫn khẽ run lên như thuở ban đầu. Có những chuyện, dù thời gian có xóa nhòa, cũng không thể nào quên được.",
        "Tiếng bước chân dừng lại phía sau. Nàng quay đầu, bắt gặp ánh mắt quen thuộc. Giữa khoảng sân tĩnh lặng, những điều chưa nói dường như đều trở lại.",
        "Cánh cửa cũ mở ra. Hai người bước vào căn phòng còn nguyên mùi giấy và hương trà. Trên bàn, cuốn sách năm ấy vẫn nằm ở chỗ cũ.",
        "Nàng chậm rãi lật từng trang. Những dòng chữ đã đọc từ lâu nay mang một ý nghĩa khác. Câu chuyện chưa kết thúc, và họ vẫn còn thời gian để viết tiếp.",
        "Ngoài cửa sổ, trời bắt đầu sáng. Một ngày mới đang chờ đợi. Nàng khép sách, mỉm cười và bước về phía ánh sáng."
    )

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun navigationUsesTheSameWidthAsTheMiniPlayerOnWidePhones() {
        val scene = ImageComposeScene(width = 600, height = 1000, density = Density(1f)) {
            ReaderTestTheme(colorScheme = TruyenPlusDarkColors) {
                ReaderBackdrop { Shell(ReaderDestination.Home) { ReaderHomeScreen(books, false, {}, {}, {}, {}, onUpdate = {}) } }
            }
        }
        try {
            scene.render(0)
            val bitmap = scene.render(1_000_000_000)
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            val nodes = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            val navButtons = nodes.filter { node ->
                node.config.getOrNull(SemanticsActions.OnClick)?.action != null &&
                    node.config.getOrNull(SemanticsProperties.Text)?.any { text ->
                        ReaderDestination.entries.any { it.localizedLabel(LocalizeHelper()) == text.text }
                    } == true
            }
            assertEquals(4, navButtons.size)
            assertTrue(navButtons.minOf { it.boundsInRoot.left } <= 27f, "Navigation should start at the mini player inset")
            assertTrue(navButtons.maxOf { it.boundsInRoot.right } >= 573f, "Navigation should reach the right mini player inset")
            val output = File("build/reports/truyenplus-v2/home-wide.png")
            output.parentFile.mkdirs()
            output.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
        } finally { scene.close() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun homeShowsOnlyOneShelfForBooksAlreadyBeingRead() {
        val scene = ImageComposeScene(width = 600, height = 1600, density = Density(1f)) {
            ReaderTestTheme(colorScheme = TruyenPlusDarkColors) {
                ReaderBackdrop { ReaderHomeScreen(books, false, {}, {}, {}, {}) }
            }
        }
        try {
            scene.render(0)
            scene.render(1_000_000_000)
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            val text = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
                .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
            assertTrue("Đọc tiếp" in text)
            assertTrue("TRONG THƯ VIỆN" !in text && "Chọn một câu chuyện" !in text,
                "The home screen should not repeat the library shelf below continue reading")
        } finally { scene.close() }
    }

    private class PreviewTtsActions(val onSelect: (Int) -> Unit = {}) : CommonTTSActions {
        override fun onPlay() {}
        override fun onPause() {}
        override fun onNextParagraph() {}
        override fun onPreviousParagraph() {}
        override fun onNextChapter() {}
        override fun onPreviousChapter() {}
        override fun onParagraphClick(index: Int) { onSelect(index) }
        override fun onToggleTranslation() {}
        override fun onToggleBilingualMode() {}
        override fun onToggleFullScreen() {}
        override fun onSpeedChange(speed: Float) {}
        override fun onAutoNextChange(enabled: Boolean) {}
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun wholeListeningChapterAllowsChoosingTheFinalParagraph() {
        val scroll = LazyListState()
        val selected = mutableListOf<Int>()
        val state = CommonTTSScreenState(content = chapterParagraphs, bookTitle = books.first().title,
            chapterName = "Chương 245", showCacheIndicators = false)
        val scene = ImageComposeScene(width = 360, height = 800, density = Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                ReaderTestTheme(colorScheme = TruyenPlusDarkColors) {
                    ReaderListenChapterScreen(state, PreviewTtsActions(selected::add), {}, lazyListState = scroll)
                }
            }
        }
        try {
            scene.render(0)
            scene.render(1_000_000_000)
            runBlocking { scroll.scrollToItem(chapterParagraphs.lastIndex) }
            scene.render(2_000_000_000)
            scene.render(3_000_000_000)
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            val nodes = scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            val paragraph = nodes.first { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.contains(chapterParagraphs.last()) } == true &&
                    node.config.getOrNull(SemanticsActions.OnClick)?.action != null
            }
            assertTrue(paragraph.config[SemanticsActions.OnClick].action!!.invoke())
            assertEquals(listOf(chapterParagraphs.lastIndex), selected)
        } finally { scene.close() }
    }

    @Composable
    private fun ReadingChapterPreview() {
        val ink = Color(0xFF292521)
        val preferences = ReaderTextPreferencesState(18, 29, 0, 400, 0,
            PreferenceValues.PreferenceTextAlignment.Left, ink, FontFamily.Serif,
            0, 1, 0, true, false, false, 0, false)
        Surface(color = Color(0xFFFAF4E6), contentColor = ink, modifier = Modifier.fillMaxSize()) {
            Column {
                ReaderChapterTopBar(Chapter(id = 245, bookId = 1, key = "245", name = "Chương 245"), {}, {}, {}, onListen = {})
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Tương phùng nơi nhân gian", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 20.dp))
                    chapterParagraphs.forEachIndexed { index, paragraph ->
                        StyleTextOptimized(Modifier.fillMaxWidth(), index, ireader.core.source.model.Text(paragraph), index == chapterParagraphs.lastIndex, preferences)
                    }
                }
            }
        }
    }

    @Composable
    private fun Shell(destination: ReaderDestination, miniPlayer: Boolean = true, content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            content()
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                if (miniPlayer) ReaderMiniPlayer(books.first(), {}, {}, isPlaying = true, onTogglePlayback = {})
                ReaderBottomNav(destination, {})
            }
        }
    }
}
