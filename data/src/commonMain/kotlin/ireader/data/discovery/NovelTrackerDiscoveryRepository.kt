package ireader.data.discovery

import ireader.domain.data.repository.DiscoveryRepository
import ireader.domain.models.discovery.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*

interface DiscoveryCache {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

/** Per-source persistent snapshots: one failed provider never invalidates the others. */
class NovelTrackerDiscoveryRepository(
    private val cache: DiscoveryCache,
    private val fetch: suspend (DiscoverySource) -> String,
    private val now: () -> Long,
    private val ttlMillis: Long = 6 * 60 * 60 * 1000L,
    private val fetchPage: (suspend (DiscoverySource, String) -> String)? = null,
) : DiscoveryRepository {
    private val mutableState = MutableStateFlow(DiscoveryState())
    override val state = mutableState.asStateFlow()
    private val refreshLock = Mutex()
    private var cacheLoaded = false
    private fun key(source: DiscoverySource) = "truyenplus.discovery.v1.${source.id}"

    override suspend fun loadMore(source: DiscoverySource) = withContext(Dispatchers.IO) {
        val loader = fetchPage ?: return@withContext
        if (!refreshLock.tryLock()) return@withContext
        try {
            val old = state.value.feeds[source] ?: return@withContext
            if (old.nextPageUrls.isEmpty()) return@withContext
            mutableState.update { it.copy(loadingMore = it.loadingMore + source) }
            val pages = old.nextPageUrls.map { NovelTrackerParser.parse(source, loader(source, it)) }
            val books = (old.books + pages.flatMap { it.books }).groupBy { it.id }.values.map { versions ->
                versions.last().copy(rank = versions.firstNotNullOfOrNull { it.rank },
                    isHot = versions.any { it.isHot }, recentlyUpdated = versions.any { it.recentlyUpdated })
            }
            val feed = old.copy(books = books,
                unavailableBooks = old.unavailableBooks + pages.sumOf { it.unavailableBooks },
                nextPageUrls = pages.flatMap { it.nextPageUrls }.distinct().filter { it !in old.nextPageUrls })
            val raw = VietnameseCatalogParser(source).snapshot(feed.books, feed.updatedTime,
                feed.nextPageUrls, feed.unavailableBooks)
            val snapshot = buildJsonObject {
                put("checkedAt", state.value.checkedAt.getValue(source)); put("feed", Json.parseToJsonElement(raw))
            }.toString()
            var cacheError: String? = null
            try { cache.write(key(source), snapshot) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { cacheError = "Đã tải dữ liệu, nhưng chưa lưu được cache trên máy." }
            mutableState.update { it.copy(feeds = it.feeds + (source to feed),
                errors = if (cacheError == null) it.errors - source else it.errors + (source to cacheError!!)) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { mutableState.update { it.copy(errors = it.errors + (source to "Chưa tải được trang tiếp theo.")) } }
        finally {
            mutableState.update { it.copy(loadingMore = it.loadingMore - source) }
            refreshLock.unlock()
        }
    }

    override suspend fun refresh(force: Boolean) = withContext(Dispatchers.IO) {
        if (!refreshLock.tryLock()) return@withContext
        try {
            if (!cacheLoaded) {
                for (source in DiscoverySource.entries) {
                    try {
                        val raw = cache.read(key(source)) ?: continue
                        val snapshot = Json.parseToJsonElement(raw).jsonObject
                        val feed = NovelTrackerParser.parse(source, snapshot.getValue("feed").toString())
                        val timestamp = snapshot.getValue("checkedAt").jsonPrimitive.long
                        mutableState.update { it.copy(feeds = it.feeds + (source to feed), checkedAt = it.checkedAt + (source to timestamp)) }
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { /* Corrupt cache is replaced only by a valid live response. */ }
                }
                cacheLoaded = true
            }
            val due = DiscoverySource.entries.filter { source ->
                val checked = state.value.checkedAt[source]
                force || checked == null || now() - checked !in 0 until ttlMillis
            }
            if (due.isEmpty()) return@withContext
            mutableState.update { it.copy(refreshing = true) }
            supervisorScope {
                due.map { source -> async {
                    try {
                        val raw = fetch(source)
                        val feed = NovelTrackerParser.parse(source, raw)
                        val checkedAt = now()
                        val snapshot = buildJsonObject {
                            put("checkedAt", checkedAt)
                            put("feed", Json.parseToJsonElement(raw))
                        }.toString()
                        var cacheError: String? = null
                        try { cache.write(key(source), snapshot) }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { cacheError = "Đã tải dữ liệu, nhưng chưa lưu được cache trên máy." }
                        mutableState.update { it.copy(feeds = it.feeds + (source to feed),
                            checkedAt = it.checkedAt + (source to checkedAt),
                            errors = if (cacheError == null) it.errors - source else it.errors + (source to cacheError!!)) }
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) {
                        mutableState.update { it.copy(errors = it.errors + (source to
                            "Chưa tải được ${source.label}. Kiểm tra mạng rồi thử làm mới." +
                                if (source in it.feeds) " Đang hiển thị dữ liệu lần tải trước." else "")) }
                    }
                } }.awaitAll()
            }
        } finally {
            mutableState.update { it.copy(refreshing = false) }
            refreshLock.unlock()
        }
    }
}
