package ireader.data.discovery

import ireader.domain.models.discovery.DiscoverySource
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Uses the app's HTTP client and snapshot cache, with at most three detail requests at once. */
class TruyenFullDiscoveryFetcher(
    private val fetchHtml: suspend (String) -> String,
    private val fetchedAt: () -> String,
) {
    suspend fun fetch(): String = withContext(Dispatchers.IO) {
        coroutineScope {
            val hot = async { fetchHtml(DiscoverySource.TruyenFull.feedUrl) }
            val fresh = async { fetchHtml("https://${DiscoverySource.TruyenFull.host}/danh-sach/truyen-moi/") }
            val listings = TruyenFullParser.listings(hot.await(), fresh.await())
            val permits = Semaphore(3)
            val books = listings.map { book -> async {
                permits.withPermit {
                    this@coroutineScope.ensureActive()
                    try { TruyenFullParser.details(book, fetchHtml(book.url)) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { null }
                }
            } }.awaitAll().filterNotNull()
            // Never fabricate missing metadata. A completely unavailable source keeps its old cache.
            check(books.isNotEmpty()) { "Chưa tải được thông tin truyện" }
            TruyenFullParser.snapshot(books, fetchedAt(), listings.size - books.size)
        }
    }
}
