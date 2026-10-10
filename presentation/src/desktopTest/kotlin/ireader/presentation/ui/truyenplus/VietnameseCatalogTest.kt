package ireader.data.discovery

import ireader.domain.models.discovery.*
import ireader.presentation.ui.truyenplus.*
import kotlin.test.*
import kotlinx.coroutines.*

class VietnameseCatalogTest {
    private fun html(name: String) = requireNotNull(javaClass.getResourceAsStream("/discovery/truyenhh-$name.html"))
        .bufferedReader().use { it.readText() }

    @Test fun yyAdapterAcceptsOnlySourceBookLinksAndExplicitRanks() {
        // Contract fixture, not a captured YY response. Live YY access is unavailable locally.
        val parser = VietnameseCatalogParser(DiscoverySource.TruyenYY)
        val list = """<div class="media"><h3><a href="/truyen/tu-la-thien-de-ban-dich">[Dịch] Tu La Thiên Đế</a></h3><div class="author">Thực Nghiệm Tiểu Bạch Thử</div><span class="rank">2</span></div>"""
        val fresh = """$list <a rel="next" href="/truyen-moi-cap-nhat?page=2">Tiếp</a>"""
        val listings = parser.listings(list, list, fresh)
        assertEquals(2, listings.books.single().rank)
        assertTrue(listings.books.single().isHot && listings.books.single().recentlyUpdated)
        assertEquals("https://www.truyenyy.co/truyen/tu-la-thien-de-ban-dich", listings.books.single().url)
        val detail = """<div class="novel-info"><h1 class="name">[Dịch] Tu La Thiên Đế</h1><div class="author"><a>Thực Nghiệm Tiểu Bạch Thử</a></div><img data-src="https://www.truyenyy.co/cover.jpg"></div><div class="novel-description">Tám năm trước, Lôi Đình Cổ Thành một đêm kinh biến.</div>"""
        val book = parser.details(listings.books.single(), detail)
        assertTrue(book.description.isNotBlank())
        assertEquals("vi", book.source.language)
        assertEquals(listOf(book), NovelTrackerParser.parse(DiscoverySource.TruyenYY,
            parser.snapshot(listOf(book), "2026-10-10 20:00:00 UTC", listings.nextPageUrls)).books)
        assertFalse(parser.isPageUrl("https://www.truyenyy.co/login?page=2"))
        assertFailsWith<IllegalArgumentException> { parser.listings(list.replace("/truyen/", "https://evil.example/truyen/"), list, fresh) }
    }

    @Test fun truyenHHUsesRealRanksMetadataAndNextPage() {
        val parser = VietnameseCatalogParser(DiscoverySource.TruyenHH)
        val listings = parser.listings(html("rank"), html("home"), html("new"))
        assertEquals(100, listings.books.count { it.rank != null })
        val first = listings.books.first { it.rank == 1 }
        assertEquals("Cưng Chiều Cô Vợ Quân Nhân", first.title)
        val book = parser.details(first, html("detail"))
        assertEquals("Huỳnh Hạ", book.author)
        assertTrue(book.description.startsWith("Một nữ sát thủ hàng đầu"))
        assertEquals(listOf("Ngôn Tình", "Sủng"), book.genres)
        assertNotNull(book.coverUrl)
        assertEquals(listOf("https://truyenhh.com/truyen/page/2/"), listings.nextPageUrls)
        val feed = NovelTrackerParser.parse(DiscoverySource.TruyenHH,
            parser.snapshot(listOf(book), "2026-10-10 20:00:00 UTC", listings.nextPageUrls))
        assertEquals(listOf(book), feed.books)
        assertEquals(listings.nextPageUrls, feed.nextPageUrls)
        assertEquals(1, filterDiscoveryBooks(feed, DiscoverySection.Ranking, "", "").size)
        assertFailsWith<IllegalArgumentException> { parser.details(first, "<html>Access denied</html>") }
        assertFalse(parser.isPageUrl("https://evil.example/truyen/page/2/"))
        assertFalse(parser.isPageUrl("https://truyenhh.com/wp-admin/"))
    }

    @Test fun loaderRetainsValidBooksWhenIndividualDetailsFail(): Unit = runBlocking {
        val loader = VietnameseCatalogFetcher(DiscoverySource.TruyenHH, { url -> when (url) {
            DiscoverySource.TruyenHH.feedUrl -> html("rank")
            "https://truyenhh.com/" -> html("home")
            "https://truyenhh.com/truyen/" -> html("new")
            "https://truyenhh.com/truyen/cung-chieu-co-vo-quan-nhan/" -> html("detail")
            else -> error("HTTP 403")
        } }, { "2026-10-10 20:00:00 UTC" })
        val feed = NovelTrackerParser.parse(DiscoverySource.TruyenHH, loader.fetch())
        assertEquals(1, feed.books.size)
        assertTrue(feed.unavailableBooks > 0)
        assertEquals(listOf("https://truyenhh.com/truyen/page/2/"), feed.nextPageUrls)
        assertFailsWith<IllegalArgumentException> { loader.fetchPage("https://evil.example/truyen/page/2/") }
    }

    @Test fun morePagesMergeMembershipPersistAndKeepCacheOnFailure() = runBlocking {
        val source = DiscoverySource.TruyenHH
        val parser = VietnameseCatalogParser(source)
        val first = parser.details(parser.listings(html("rank"), html("home"), html("new")).books.first(), html("detail"))
        val storage = java.util.concurrent.ConcurrentHashMap<String, String>()
        val cache = object : DiscoveryCache {
            override fun read(key: String) = storage[key]
            override fun write(key: String, value: String) { storage[key] = value }
        }
        val next = "https://truyenhh.com/truyen/page/2/"
        val snapshot = parser.snapshot(listOf(first), "2026-10-10 20:00:00 UTC", listOf(next))
        var failing = false
        val repo = NovelTrackerDiscoveryRepository(cache, { s -> if (s == source) snapshot else
            requireNotNull(javaClass.getResourceAsStream("/discovery/${s.id}.json")).bufferedReader().use { it.readText() }
        }, { 100_000_000L }, fetchPage = { s, url ->
            assertEquals(source, s); assertEquals(next, url)
            if (failing) error("offline")
            parser.snapshot(listOf(first.copy(rank = null, recentlyUpdated = true)), "2026-10-10 20:01:00 UTC", listOf("https://truyenhh.com/truyen/page/3/"))
        })
        repo.refresh()
        repo.loadMore(source)
        val feed = repo.state.value.feeds.getValue(source)
        assertEquals(1, feed.books.size)
        assertEquals(1, feed.books.single().rank)
        assertTrue(feed.books.single().recentlyUpdated)
        assertEquals(listOf("https://truyenhh.com/truyen/page/3/"), feed.nextPageUrls)
        val restarted = NovelTrackerDiscoveryRepository(cache, { error("must use cache") }, { 100_000_001L })
        restarted.refresh()
        assertEquals(feed, restarted.state.value.feeds[source])
        failing = true
        // The failed request must not clear existing books or the cursor.
        val failed = NovelTrackerDiscoveryRepository(cache, { error("must use cache") }, { 100_000_001L }, fetchPage = { _, _ -> error("offline") })
        failed.refresh(); failed.loadMore(source)
        assertEquals(feed, failed.state.value.feeds[source])
        assertTrue(source in failed.state.value.errors)
        assertTrue(failed.state.value.loadingMore.isEmpty())
    }
}
