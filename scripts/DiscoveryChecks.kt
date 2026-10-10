import ireader.data.discovery.NovelTrackerParser
import ireader.domain.models.discovery.DiscoverySource
import java.io.File
import ireader.presentation.ui.truyenplus.DiscoverySection
import ireader.presentation.ui.truyenplus.filterDiscoveryBooks
import ireader.data.discovery.NovelTrackerDiscoveryRepository
import ireader.data.discovery.DiscoveryCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main(args: Array<String>) {
    val folder = File(args.single())
    val feed = NovelTrackerParser.parse(DiscoverySource.Qidian, File(folder, "qidian.json").readText())
    val book = feed.books.first()
    check(book.title == "青山" && book.author == "会说话的肘子")
    check(book.rank == 1 && book.genres.first() == "玄幻")
    check(book.url == "https://www.qidian.com/book/1033014772/")
    check(book.coverUrl!!.startsWith("https://bookcover.yuewen.com/"))
    check(feed.updatedDate == "2026-10-10")
    check(filterDiscoveryBooks(feed.copy(books = listOf(book)), DiscoverySection.Hot, "玄幻", "会说话").single().id == book.id)
    check(filterDiscoveryBooks(feed.copy(books = listOf(book)), DiscoverySection.New, "", "").isEmpty())
    check(filterDiscoveryBooks(feed.copy(books = listOf(book)), DiscoverySection.Ranking, "言情", "").isEmpty())
    val syntheticTitle = File(folder, "qidian.json").readText().replace("青山", "起点畅销小说1号·玄幻传")
    check(runCatching { NovelTrackerParser.parse(DiscoverySource.Qidian, syntheticTitle) }.isFailure)
    var verified = 0
    DiscoverySource.entries.forEach { source ->
        val raw = File(folder, "${source.id}.json").readText()
        val parsed = NovelTrackerParser.parse(source, raw)
        val count = Json.parseToJsonElement(raw).jsonObject.getValue("books").jsonArray.size
        check(parsed.books.size == count)
        check(parsed.books.all { it.source == source && it.title.isNotBlank() && it.author.isNotBlank() })
        check(filterDiscoveryBooks(parsed, DiscoverySection.New, "", "").all { it.newOnRanking || it.recentlyUpdated })
        verified += parsed.books.size
    }
    println("PASS: parsed $verified real book records across ${DiscoverySource.entries.size} providers")
    val storage = mutableMapOf<String, String>()
    val cache = object : DiscoveryCache {
        override fun read(key: String) = storage[key]
        override fun write(key: String, value: String) { storage[key] = value }
    }
    val calls = java.util.concurrent.atomic.AtomicInteger()
    var offline = false
    var time = 100_000_000L
    fun repository() = NovelTrackerDiscoveryRepository(cache, { source ->
        calls.incrementAndGet()
        if (offline && source == DiscoverySource.Qidian) error("offline")
        File(folder, "${source.id}.json").readText()
    }, { time })
    runBlocking {
        val repo = repository()
        repo.refresh()
        check(repo.state.value.feeds.size == DiscoverySource.entries.size && calls.get() == DiscoverySource.entries.size)
        repo.refresh()
        check(calls.get() == DiscoverySource.entries.size) // fresh cache avoids network
        val restarted = repository()
        restarted.refresh()
        check(restarted.state.value.feeds.size == DiscoverySource.entries.size && calls.get() == DiscoverySource.entries.size) // persisted cache
        time += 30_000_000
        offline = true
        restarted.refresh(true)
        check(restarted.state.value.feeds.size == DiscoverySource.entries.size)
        check(restarted.state.value.checkedAt[DiscoverySource.Qidian] == 100_000_000L)
        check(restarted.state.value.errors.keys == setOf(DiscoverySource.Qidian))
        check(!restarted.state.value.refreshing)
    }
    println("PASS: real fields, persistent cache, TTL, refresh and partial outage")
}
