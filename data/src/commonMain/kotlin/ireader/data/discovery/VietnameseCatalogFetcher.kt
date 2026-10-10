package ireader.data.discovery

import ireader.domain.models.discovery.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Reuses the HTTP client and feed cache; keeps source failures isolated and cancellable. */
class VietnameseCatalogFetcher(
    private val source: DiscoverySource,
    private val fetchHtml: suspend (String) -> String,
    private val fetchedAt: () -> String,
) {
    private val parser = VietnameseCatalogParser(source)
    suspend fun fetch(): String = withContext(Dispatchers.IO) {
        coroutineScope {
            val rank = async { fetchHtml(source.feedUrl) }
            val hot = async { if (source == DiscoverySource.TruyenYY) "" else fetchHtml("https://${source.host}/") }
            val fresh = async { fetchHtml("https://${source.host}/" + if (source == DiscoverySource.TruyenYY) "truyen-moi-cap-nhat" else "truyen/") }
            complete(parser.listings(rank.await(), hot.await(), fresh.await()))
        }
    }
    suspend fun fetchPage(url: String): String = withContext(Dispatchers.IO) {
        require(parser.isPageUrl(url)) { "Liên kết phân trang không hợp lệ" }
        complete(parser.page(fetchHtml(url)))
    }
    private suspend fun complete(listing: VietnameseCatalogListing): String = coroutineScope {
        val permits = Semaphore(3)
        val books = listing.books.map { book -> async {
            permits.withPermit {
                this@coroutineScope.ensureActive()
                try { parser.details(book, fetchHtml(book.url)) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { null }
            }
        } }.awaitAll().filterNotNull()
        check(books.isNotEmpty()) { "Chưa tải được thông tin truyện" }
        parser.snapshot(books, fetchedAt(), listing.nextPageUrls, listing.books.size - books.size)
    }
}
