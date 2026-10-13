package ireader.data.discovery

import ireader.domain.data.repository.BookMetadataLookup
import ireader.domain.models.discovery.DiscoveryBook
import ireader.domain.models.discovery.DiscoverySource
import ireader.domain.models.entities.Book
import ireader.domain.utils.extensions.currentTimeToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TruyenFullMetadataLookup(private val fetch: suspend (String) -> String) : BookMetadataLookup {
    private fun slug(text: String): String {
        val groups = mapOf('a' to "àáạảãâầấậẩẫăằắặẳẵ", 'e' to "èéẹẻẽêềếệểễ", 'i' to "ìíịỉĩ",
            'o' to "òóọỏõôồốộổỗơờớợởỡ", 'u' to "ùúụủũưừứựửữ", 'y' to "ỳýỵỷỹ", 'd' to "đ")
        val plain = text.lowercase().map { c -> groups.entries.firstOrNull { c in it.value }?.key ?: c }.joinToString("")
        return Regex("[a-z0-9]+").findAll(plain).map { it.value }.joinToString("-")
    }
    private fun sourceUrl(book: Book) = "https://${DiscoverySource.TruyenFull.host}/${slug(book.title)}/"
    override suspend fun find(book: Book): BookMetadataLookup.Match? = withContext(Dispatchers.IO) {
        if (book.title.isBlank() || book.author.isBlank() || book.author == "Unknown") return@withContext null
        val id = slug(book.title).takeIf { it.isNotBlank() } ?: return@withContext null
        fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
        val urls = listOf(sourceUrl(book), "https://truyenfull.vision/$id/").distinct()
        for (url in urls) {
            val result = try {
                val seed = DiscoveryBook(id, DiscoverySource.TruyenFull, book.title, book.author, "", null, url, emptyList(), "", null, false)
                TruyenFullParser.details(seed, fetch(url))
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { continue }
            if (normalized(result.title) != normalized(book.title) || normalized(result.author) != normalized(book.author)) continue
            if (result.description.isBlank()) continue
            return@withContext BookMetadataLookup.Match(book.copy(description = result.description, cover = result.coverUrl ?: book.cover,
                genres = result.genres.ifEmpty { book.genres }, lastUpdate = currentTimeToLong()), url)
        }
        null
    }
}
