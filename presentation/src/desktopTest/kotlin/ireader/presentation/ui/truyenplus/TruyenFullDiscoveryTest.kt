package ireader.data.discovery

import ireader.domain.models.discovery.*
import ireader.presentation.ui.truyenplus.*
import kotlinx.coroutines.*
import kotlin.test.*

class TruyenFullDiscoveryTest {
    private fun html(name: String) = requireNotNull(javaClass.getResourceAsStream("/discovery/truyenfull-$name.html"))
        .bufferedReader().use { it.readText() }

    @Test fun realListsKeepPublishedRanksSeparateFromHotAndUpdatedLists() {
        val books = TruyenFullParser.listings(html("hot"), html("new"))
        val first = books.first { it.title == "Thần Đạo Đan Tôn" }
        assertEquals(1, first.rank)
        assertTrue(first.isHot)
        assertFalse(first.recentlyUpdated)
        assertEquals("Cô Đơn Địa Phi", first.author)
        assertEquals("https://truyenfull.live/than-dao-dan-ton-6060282/", first.url)
        val fresh = books.first { it.title == "Đại Hạ Phế Vật Ký: Cả Triều Đình Cùng Ta Ăn Dưa" }
        assertNull(fresh.rank) // List order is not a published ranking.
        assertTrue(fresh.recentlyUpdated)
        assertFalse(fresh.newOnRanking)
        val feed = DiscoveryFeed(DiscoverySource.TruyenFull, "", "", "", "", books)
        assertEquals(10, filterDiscoveryBooks(feed, DiscoverySection.Ranking, "", "").size)
        assertEquals(20, filterDiscoveryBooks(feed, DiscoverySection.Hot, "", "").size)
        assertEquals(20, filterDiscoveryBooks(feed, DiscoverySection.New, "", "").size)
    }

    @Test fun realDetailPreservesVietnameseAndRemovesHiddenSeoText() {
        val book = TruyenFullParser.listings(html("hot"), html("new")).first()
        val result = TruyenFullParser.details(book, html("detail"))
        assertEquals("Thần Đạo Đan Tôn", result.title)
        assertEquals("Cô Đơn Địa Phi", result.author)
        assertEquals(listOf("Tiên Hiệp", "Huyền Huyễn"), result.genres)
        assertTrue(result.description.startsWith("Lăng Hàn - Một Đan Đế"))
        assertFalse(result.description.contains("truyenfullvn"))
        assertTrue(requireNotNull(result.coverUrl).startsWith("https://static.truyenfull.live/cover/"))
        assertEquals(book.rank, result.rank)
        assertFailsWith<IllegalArgumentException> { TruyenFullParser.details(book, "<html>Access denied</html>") }
        assertFailsWith<IllegalArgumentException> { TruyenFullParser.listings("<html>Access denied</html>", html("new")) }
    }

    @Test fun externalLinksAreRejectedAndSnapshotRetainsMembershipAndOrder() {
        val raw = html("hot").replace("https://truyenfull.live/than-dao-dan-ton-6060282/", "https://evil.example/than-dao-dan-ton-6060282/")
        assertTrue(TruyenFullParser.listings(raw, html("new")).none { it.title == "Thần Đạo Đan Tôn" })
        val books = TruyenFullParser.listings(html("hot"), html("new")).filter { it.author.isNotBlank() }
        val parsed = NovelTrackerParser.parse(DiscoverySource.TruyenFull, TruyenFullParser.snapshot(books, "2026-10-10 20:00:00 UTC"))
        assertEquals(books, parsed.books)
        assertEquals("vi", parsed.source.language)
        assertEquals(20, filterDiscoveryBooks(parsed, DiscoverySection.New, "", "").size)
    }

    @Test fun detailRequestsAreBoundedAndCancellationDoesNotBecomeAFeed() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val loader = TruyenFullDiscoveryFetcher({ url ->
            when {
                url.endsWith("/danh-sach/truyen-hot/") -> html("hot")
                url.endsWith("/danh-sach/truyen-moi/") -> html("new")
                else -> { if (calls.incrementAndGet() == 3) started.complete(Unit); awaitCancellation() }
            }
        }, { "2026-10-10 20:00:00 UTC" })
        var completed = false
        val job = launch { loader.fetch(); completed = true }
        started.await()
        assertEquals(3, calls.get())
        job.cancelAndJoin()
        assertFalse(completed)
        assertEquals(3, calls.get())
    }

    @Test fun incompleteDetailDoesNotOverwritePreviouslyCachedVietnameseFeed() = runBlocking {
        val storage = java.util.concurrent.ConcurrentHashMap<String, String>()
        val cache = object : DiscoveryCache {
            override fun read(key: String) = storage[key]
            override fun write(key: String, value: String) { storage[key] = value }
        }
        val book = TruyenFullParser.details(TruyenFullParser.listings(html("hot"), html("new")).first(), html("detail"))
        val snapshot = TruyenFullParser.snapshot(listOf(book), "2026-10-10 20:00:00 UTC")
        fun fixture(source: DiscoverySource) = if (source == DiscoverySource.TruyenFull) snapshot else
            requireNotNull(javaClass.getResourceAsStream("/discovery/${source.id}.json")).bufferedReader().use { it.readText() }
        val first = NovelTrackerDiscoveryRepository(cache, ::fixture, { 100_000_000L })
        first.refresh()
        val loader = TruyenFullDiscoveryFetcher({ url -> if (url.endsWith("truyen-hot/")) html("hot") else if (url.endsWith("truyen-moi/")) html("new") else "<html>blocked</html>" }, { "2026-10-11 20:00:00 UTC" })
        val restarted = NovelTrackerDiscoveryRepository(cache, { source -> if (source == DiscoverySource.TruyenFull) loader.fetch() else fixture(source) }, { 130_000_000L })
        restarted.refresh(true)
        assertEquals(DiscoverySource.entries.size, restarted.state.value.feeds.size)
        assertEquals(first.state.value.feeds[DiscoverySource.TruyenFull], restarted.state.value.feeds[DiscoverySource.TruyenFull])
        assertEquals(100_000_000L, restarted.state.value.checkedAt[DiscoverySource.TruyenFull])
        assertEquals(setOf(DiscoverySource.TruyenFull), restarted.state.value.errors.keys)
    }

    @Test fun individualBlockedPagesDoNotHideOtherRealBooksAndAreReported() = runBlocking {
        val loader = TruyenFullDiscoveryFetcher({ url -> when {
            url.endsWith("truyen-hot/") -> html("hot")
            url.endsWith("truyen-moi/") -> html("new")
            url.endsWith("than-dao-dan-ton-6060282/") -> html("detail")
            url.endsWith("dai-ha-phe-vat-ky-ca-trieu-dinh-cung-ta-an-dua/") -> html("new-detail")
            else -> error("HTTP 403")
        } }, { "2026-10-10 20:00:00 UTC" })
        val feed = NovelTrackerParser.parse(DiscoverySource.TruyenFull, loader.fetch())
        assertEquals(2, feed.books.size)
        assertTrue(feed.books.all { it.description.isNotBlank() && it.coverUrl != null })
        assertEquals(TruyenFullParser.listings(html("hot"), html("new")).size - 2, feed.unavailableBooks)
        assertEquals(1, feed.books.first().rank)
        assertNull(feed.books.last().rank)
    }
}
