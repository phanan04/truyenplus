package ireader.data.discovery

import ireader.domain.models.discovery.*
import ireader.domain.data.repository.DiscoveryTranslator
import kotlinx.coroutines.*
import kotlin.test.*
import okio.Path.Companion.toPath

class DiscoveryTranslationTest {
    private fun book() = NovelTrackerParser.parse(DiscoverySource.Qidian,
        requireNotNull(javaClass.getResourceAsStream("/discovery/qidian.json")).bufferedReader().use { it.readText() }).books.first()

    @Test fun persistedTranslationsAreSeparatedByLanguageAndInvalidatedByContent() = runBlocking {
        val directory = java.nio.file.Files.createTempDirectory("discovery-translations-").toFile()
        try {
            var calls = 0
            val targets = mutableListOf<String>()
            fun repo() = CachedDiscoveryTranslations(FileDiscoveryCache(directory.path.toPath(), okio.FileSystem.SYSTEM),
                DiscoveryTranslator { texts, target -> calls++; targets.add(target); texts.map { "$target:$it" } })
            val first = repo().translate(book(), "vi")
            assertEquals("vi:" + book().title, first.title)
            assertEquals(first, repo().translate(book(), "vi"))
            assertEquals(1, calls)
            assertEquals("en:" + book().title, repo().translate(book(), "en").title)
            repo().translate(book().copy(description = "新的简介"), "vi")
            assertEquals(listOf("vi", "en", "vi"), targets)
            assertEquals(book().author, first.original.author)
        } finally { directory.deleteRecursively() }
    }
    @Test fun failureAndUnchangedChineseAreNotCachedAndRetryCanSucceed() = runBlocking {
        val cache = object : DiscoveryCache {
            val values = mutableMapOf<String, String>()
            override fun read(key: String) = values[key]
            override fun write(key: String, value: String) { values[key] = value }
        }
        var attempt = 0
        val repo = CachedDiscoveryTranslations(cache, DiscoveryTranslator { texts, _ ->
            attempt++
            when (attempt) { 1 -> error("offline"); 2 -> texts; else -> texts.map { "Vietnamese $it" } }
        })
        assertFailsWith<IllegalStateException> { repo.translate(book(), "vi") }
        assertFailsWith<IllegalStateException> { repo.translate(book(), "vi") }
        assertTrue(cache.values.isEmpty())
        assertTrue(repo.translate(book(), "vi").title.startsWith("Vietnamese"))
        assertEquals(3, attempt)
    }
    @Test fun cancellationRemainsCancellationAndDoesNotWriteCache() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var writes = 0
        val cache = object : DiscoveryCache {
            override fun read(key: String): String? = null
            override fun write(key: String, value: String) { writes++ }
        }
        val repo = CachedDiscoveryTranslations(cache, DiscoveryTranslator { _, _ -> started.complete(Unit); awaitCancellation() })
        val job = launch { repo.translate(book(), "en") }
        started.await(); job.cancelAndJoin()
        assertEquals(0, writes)
    }
    @Test fun timeoutIsAnErrorThatCanBeRetriedInsteadOfCancellingTheQueue() = runBlocking {
        val cache = object : DiscoveryCache {
            val values = mutableMapOf<String, String>()
            override fun read(key: String) = values[key]
            override fun write(key: String, value: String) { values[key] = value }
        }
        var timedOut = true
        val repo = CachedDiscoveryTranslations(cache, DiscoveryTranslator { texts, _ ->
            if (timedOut) awaitCancellation()
            texts.map { "Translated $it" }
        }, timeoutMillis = 20)
        assertFailsWith<IllegalStateException> { repo.translate(book(), "vi") }
        assertTrue(cache.values.isEmpty())
        timedOut = false
        assertTrue(repo.translate(book(), "vi").title.startsWith("Translated"))
    }

}
