package ireader.data.discovery

import ireader.domain.models.discovery.DiscoverySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.serialization.json.*

class NovelTrackerParserTest {
    @Test fun preservesRealQidianMetadataWithoutInventingRankOrPublicationDate() {
        val json = requireNotNull(javaClass.getResourceAsStream("/discovery/qidian.json")).bufferedReader().use { it.readText() }
        val feed = NovelTrackerParser.parse(DiscoverySource.Qidian, json)
        val book = feed.books.single()
        assertEquals("青山", book.title)
        assertEquals("会说话的肘子", book.author)
        assertEquals(1, book.rank)
        assertEquals("玄幻", book.genres.first())
        assertTrue(book.coverUrl.orEmpty().startsWith("https://bookcover.yuewen.com/"))
        assertEquals("https://www.qidian.com/book/1033014772/", book.url)
        assertEquals("2026-10-10", feed.updatedDate)
    }
    private fun fixture(source: DiscoverySource) = requireNotNull(javaClass.getResourceAsStream("/discovery/${source.id}.json")).bufferedReader().use { it.readText() }
    @Test fun readsActualFanqieAndJinjiangFields() {
        val fanqie = NovelTrackerParser.parse(DiscoverySource.Fanqie, fixture(DiscoverySource.Fanqie)).books.single()
        assertEquals("神通者", fanqie.title)
        assertEquals("天蚕土豆", fanqie.author)
        assertTrue(fanqie.coverUrl.orEmpty().contains("byteimg.com"))
        val jinjiang = NovelTrackerParser.parse(DiscoverySource.Jinjiang, fixture(DiscoverySource.Jinjiang)).books.single()
        assertEquals("高冷太子的克星", jinjiang.title)
        assertEquals("笑佳人", jinjiang.author)
        assertTrue(jinjiang.url.contains("novelid=11014721"))
    }
    @Test fun absentRankIsNotReplacedWithAnArrayIndex() {
        val root = Json.parseToJsonElement(fixture(DiscoverySource.Qidian)).jsonObject
        val book = root.getValue("books").jsonArray.single().jsonObject
        val raw = JsonObject(root + ("books" to JsonArray(listOf(JsonObject(book - "rank"))))).toString()
        assertNull(NovelTrackerParser.parse(DiscoverySource.Qidian, raw).books.single().rank)
    }
    @Test fun rejectsDemoWrongProviderAndForeignBookLinks() {
        val raw = fixture(DiscoverySource.Qidian)
        assertFailsWith<IllegalArgumentException> { NovelTrackerParser.parse(DiscoverySource.Fanqie, raw) }
        assertFailsWith<IllegalArgumentException> { NovelTrackerParser.parse(DiscoverySource.Qidian, raw.replace("青山", "起点畅销小说1号·玄幻传")) }
        assertFailsWith<IllegalArgumentException> { NovelTrackerParser.parse(DiscoverySource.Qidian, raw.replace("https://www.qidian.com/book/", "https://example.com/book/")) }
    }

    @Test fun newSectionUsesThePublishedNewOnRankingFlag() {
        val raw = requireNotNull(javaClass.getResourceAsStream("/discovery/qidian-new.json")).bufferedReader().use { it.readText() }
        val feed = NovelTrackerParser.parse(DiscoverySource.Qidian, raw)
        val books = ireader.presentation.ui.truyenplus.filterDiscoveryBooks(feed, ireader.presentation.ui.truyenplus.DiscoverySection.New, "", "")
        assertEquals(feed.books, books)
        assertTrue(books.single().newOnRanking)
    }

}
