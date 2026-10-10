package ireader.presentation.ui.truyenplus

import ireader.domain.data.repository.*
import ireader.domain.models.discovery.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryTranslationQueueTest {
    private fun feed(source: DiscoverySource) = ireader.data.discovery.NovelTrackerParser.parse(source,
        requireNotNull(javaClass.getResourceAsStream("/discovery/${source.id}.json")).bufferedReader().use { it.readText() })
    private class Repository(feeds: Map<DiscoverySource, DiscoveryFeed>) : DiscoveryRepository {
        override val state = MutableStateFlow(DiscoveryState(feeds))
        override suspend fun refresh(force: Boolean) {}
    }
    private fun translated(book: DiscoveryBook, language: String) = DiscoveryTranslation(book, language, "$language:${book.title}", "$language:${book.description}", book.genres, book.status)
    @Test fun vietnameseSourceSkipsTranslationAndModelDownloadsWhenUiIsVietnamese() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = DiscoverySource.TruyenFull
        val raw = requireNotNull(javaClass.getResourceAsStream("/discovery/truyenfull-hot.html")).bufferedReader().use { it.readText() }
        val fresh = requireNotNull(javaClass.getResourceAsStream("/discovery/truyenfull-new.html")).bufferedReader().use { it.readText() }
        val books = ireader.data.discovery.TruyenFullParser.listings(raw, fresh).filter { it.author.isNotBlank() }
        val feed = DiscoveryFeed(source, "", "", "2026-10-10", "", books)
        var calls = 0
        val translations = object : DiscoveryTranslations {
            override suspend fun cached(book: DiscoveryBook, language: String): DiscoveryTranslation? { calls++; return null }
            override suspend fun translate(book: DiscoveryBook, language: String): DiscoveryTranslation { calls++; return translated(book, language) }
        }
        val vm = DiscoveryViewModel(Repository(mapOf(source to feed)), translations)
        try {
            vm.configure(source, "vi"); runCurrent()
            assertEquals(0, calls)
            assertTrue(vm.translated.value.values.isEmpty())
            assertFalse(vm.translated.value.failed); assertFalse(vm.translated.value.translating)
            vm.configure(source, "en"); runCurrent()
            assertEquals(books.size, vm.translated.value.values.size)
        } finally { vm.scope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun changingLanguageCancelsOldWorkAndCannotDisplayItsResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val feed = feed(DiscoverySource.Qidian)
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val calls = mutableListOf<String>()
        val translations = object : DiscoveryTranslations {
            override suspend fun cached(book: DiscoveryBook, language: String): DiscoveryTranslation? = null
            override suspend fun translate(book: DiscoveryBook, language: String): DiscoveryTranslation {
                calls.add(language)
                if (language == "vi") {
                    started.complete(Unit)
                    try { awaitCancellation() } finally { cancelled = true }
                }
                return translated(book, language)
            }
        }
        val vm = DiscoveryViewModel(Repository(mapOf(feed.source to feed)), translations)
        try {
            runCurrent(); assertTrue(calls.isEmpty())
            vm.configure(feed.source, "vi"); runCurrent(); assertTrue(started.isCompleted)
            vm.configure(feed.source, "en"); runCurrent()
            assertTrue(cancelled)
            assertEquals("en", vm.translated.value.language)
            assertEquals(feed.books.size, vm.translated.value.values.size)
            assertTrue(vm.translated.value.values.values.all { it.language == "en" })
            assertFalse(vm.translated.value.failed); assertFalse(vm.translated.value.translating)
        } finally { vm.scope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun offlineFailureKeepsCachedEntriesAndRetryTranslatesMissingEntries() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val original = feed(DiscoverySource.Qidian)
        val newEntry = ireader.data.discovery.NovelTrackerParser.parse(DiscoverySource.Qidian,
            requireNotNull(javaClass.getResourceAsStream("/discovery/qidian-new.json")).bufferedReader().use { it.readText() }).books.first()
        val feed = original.copy(books = original.books + newEntry)
        var failing = true
        val translations = object : DiscoveryTranslations {
            override suspend fun cached(book: DiscoveryBook, language: String) = if (book == feed.books.first()) translated(book, language) else null
            override suspend fun translate(book: DiscoveryBook, language: String): DiscoveryTranslation {
                if (failing) error("offline")
                return translated(book, language)
            }
        }
        val vm = DiscoveryViewModel(Repository(mapOf(feed.source to feed)), translations)
        try {
            vm.configure(feed.source, "en"); runCurrent()
            assertEquals(1, vm.translated.value.values.size)
            assertTrue(vm.translated.value.failed); assertFalse(vm.translated.value.translating)
            failing = false; vm.retryTranslation(); runCurrent()
            assertEquals(feed.books.size, vm.translated.value.values.size)
            assertFalse(vm.translated.value.failed)
        } finally { vm.scope.cancel(); Dispatchers.resetMain() }
    }
}
