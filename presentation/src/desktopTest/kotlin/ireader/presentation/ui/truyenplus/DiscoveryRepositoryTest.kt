package ireader.data.discovery

import ireader.domain.models.discovery.DiscoverySource
import kotlinx.coroutines.*
import kotlin.test.*
import okio.Path.Companion.toPath

class DiscoveryRepositoryTest {
    private class Cache : DiscoveryCache {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String) { synchronized(values) { values[key] = value } }
    }
    private fun fixture(source: DiscoverySource) = requireNotNull(javaClass.getResourceAsStream("/discovery/${source.id}.json")).bufferedReader().use { it.readText() }

    @Test fun cacheSurvivesRestartAndOneProviderFailureWithoutRenewingItsTimestamp() = runBlocking {
        val cache = Cache()
        var clock = 100_000_000L
        val calls = java.util.concurrent.atomic.AtomicInteger()
        var failing = false
        fun repository() = NovelTrackerDiscoveryRepository(cache, { source ->
            calls.incrementAndGet()
            if (failing && source == DiscoverySource.Qidian) error("offline")
            fixture(source)
        }, { clock })
        val first = repository()
        first.refresh()
        assertEquals(3, calls.get())
        val restarted = repository()
        restarted.refresh()
        assertEquals(3, calls.get())
        assertEquals(3, restarted.state.value.feeds.size)
        failing = true
        clock += 30_000_000L
        restarted.refresh(true)
        assertEquals(100_000_000L, restarted.state.value.checkedAt[DiscoverySource.Qidian])
        assertEquals(clock, restarted.state.value.checkedAt[DiscoverySource.Fanqie])
        assertEquals(setOf(DiscoverySource.Qidian), restarted.state.value.errors.keys)
        assertEquals(3, restarted.state.value.feeds.size)
        assertFalse(restarted.state.value.refreshing)
    }
    @Test fun overlappingRefreshIsCoalescedAndCancellationDoesNotBecomeAnOfflineError() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val repo = NovelTrackerDiscoveryRepository(Cache(), { source ->
            calls.incrementAndGet(); started.complete(Unit); release.await(); fixture(source)
        }, { 100_000_000L })
        val job = launch { repo.refresh(true) }
        started.await()
        repo.refresh(true)
        assertTrue(calls.get() <= 3)
        job.cancelAndJoin()
        assertFalse(repo.state.value.refreshing)
        assertTrue(repo.state.value.errors.isEmpty())
        release.complete(Unit)
        repo.refresh(true)
        assertEquals(3, repo.state.value.feeds.size)
    }
    @Test fun corruptCacheIsReplacedByValidLiveData() = runBlocking {
        val cache = Cache()
        cache.values["truyenplus.discovery.v1.qidian"] = "broken JSON"
        val repo = NovelTrackerDiscoveryRepository(cache, ::fixture, { 100_000_000L })
        repo.refresh()
        assertEquals(3, repo.state.value.feeds.size)
        assertTrue(cache.values.getValue("truyenplus.discovery.v1.qidian").contains("青山"))
    }
    @Test fun diskSnapshotCanBeOverwrittenAndReadByANewCacheInstance() {
        val directory = java.nio.file.Files.createTempDirectory("discovery-cache-").toFile()
        try {
            val cache = FileDiscoveryCache(directory.path.toPath(), okio.FileSystem.SYSTEM)
            assertNull(cache.read("qidian"))
            cache.write("qidian", fixture(DiscoverySource.Qidian))
            cache.write("qidian", fixture(DiscoverySource.Fanqie))
            val restarted = FileDiscoveryCache(directory.path.toPath(), okio.FileSystem.SYSTEM)
            assertEquals(fixture(DiscoverySource.Fanqie), restarted.read("qidian"))
            assertFalse(directory.resolve("qidian.tmp").exists())
        } finally { directory.deleteRecursively() }
    }

}
