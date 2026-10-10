package ireader.data.discovery

import ireader.domain.data.repository.*
import ireader.domain.models.discovery.DiscoveryBook
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8

class CachedDiscoveryTranslations(
    private val cache: DiscoveryCache, private val translator: DiscoveryTranslator,
    private val timeoutMillis: Long = 120_000,
) : DiscoveryTranslations {
    private val lock = Mutex()
    private fun texts(book: DiscoveryBook) = listOf(book.title, book.description, book.status) + book.genres
    private fun signature(book: DiscoveryBook) = JsonArray(texts(book).map(::JsonPrimitive)).toString().encodeUtf8().sha256().hex()
    private fun key(book: DiscoveryBook, language: String): String {
        require(language in setOf("vi", "en"))
        return "truyenplus.discovery.translation.v1.$language.${book.key.encodeUtf8().sha256().hex()}"
    }
    private fun result(book: DiscoveryBook, language: String, values: List<String>): DiscoveryTranslation {
        check(values.size == texts(book).size)
        texts(book).zip(values).forEach { (original, translated) ->
            check(original.isBlank() || translated.isNotBlank()) { "Empty translation" }
            check(!original.any { it in '\u3400'..'\u9fff' } || original != translated) { "Translation returned original Chinese" }
        }
        return DiscoveryTranslation(book, language, values[0], values[1], values.drop(3), values[2])
    }
    override suspend fun cached(book: DiscoveryBook, language: String): DiscoveryTranslation? = withContext(Dispatchers.IO) {
        try {
            val data = Json.parseToJsonElement(cache.read(key(book, language)) ?: return@withContext null).jsonObject
            if (data["signature"]?.jsonPrimitive?.content != signature(book)) return@withContext null
            result(book, language, data.getValue("values").jsonArray.map { it.jsonPrimitive.content })
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { null }
    }
    override suspend fun translate(book: DiscoveryBook, language: String): DiscoveryTranslation = lock.withLock {
        cached(book, language)?.let { return@withLock it }
        val input = texts(book)
        val indices = input.indices.filter { input[it].isNotBlank() }
        val output = withTimeoutOrNull(timeoutMillis) { translator.translateFrom(indices.map { input[it] }, book.source.language, language) }
            ?: error("Translation timed out")
        check(output.size == indices.size) { "Incomplete translation" }
        val values = input.toMutableList()
        indices.zip(output).forEach { (index, value) -> values[index] = value }
        val translated = result(book, language, values)
        withContext(Dispatchers.IO) {
            // Cache failure should not hide a valid translation or block the rest of the feed.
            try { cache.write(key(book, language), buildJsonObject {
                put("signature", signature(book)); put("values", JsonArray(values.map(::JsonPrimitive)))
            }.toString()) } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { }
        }
        translated
    }
}
