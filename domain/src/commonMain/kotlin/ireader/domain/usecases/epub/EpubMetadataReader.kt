package ireader.domain.usecases.epub

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import io.ktor.http.decodeURLPart
import ireader.domain.models.entities.Book

/** OPF2/3 metadata and explicit cover references; never substitutes chapter text for a description. */
object EpubMetadataReader {
    data class Metadata(val title: String, val author: String, val description: String, val coverPath: String?)
    fun read(opfPath: String, xml: String, readText: (String) -> String?): Metadata = read(opfPath, Ksoup.parse(xml, Parser.xmlParser()), readText)
    fun read(opfPath: String, doc: Document, readText: (String) -> String?): Metadata {
        val metadata = doc.selectFirst("metadata") ?: doc.getAllElements().firstOrNull { it.tagName().substringAfterLast(':') == "metadata" }
        fun field(name: String) = metadata?.getAllElements()?.firstOrNull { it.tagName().substringAfterLast(':') == name }?.text().orEmpty().trim()
        val items = doc.select("manifest item")
        val coverId = metadata?.select("meta[name=cover]")?.firstOrNull()?.attr("content")
        val marked = items.firstOrNull { "cover-image" in it.attr("properties").split(' ') }
            ?: items.firstOrNull { !coverId.isNullOrBlank() && it.attr("id") == coverId }
            ?: items.firstOrNull { it.attr("id") in listOf("cover", "cover-image") && it.attr("media-type").startsWith("image/") }
        var cover = marked?.attr("href")?.let { resolve(opfPath, it) }
        if (cover == null) {
            val reference = doc.select("guide reference[type=cover]").firstOrNull()?.attr("href")?.let { resolve(opfPath, it) }
            if (reference != null) {
                if (reference.substringAfterLast('.').lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")) cover = reference
                else readText(reference)?.let { html ->
                    val image = Ksoup.parse(html).getAllElements().firstOrNull { it.tagName().substringAfterLast(':') in listOf("img", "image") }
                    val href = image?.attr("src").orEmpty().ifBlank { image?.attr("xlink:href").orEmpty() }.ifBlank { image?.attr("href").orEmpty() }
                    cover = resolve(reference, href)
                }
            }
        }
        return Metadata(field("title"), field("creator"), Ksoup.parse(field("description")).text().trim(), cover)
    }
    fun resolve(baseFile: String, href: String): String? {
        if (href.isBlank()) return null
        val path = runCatching { href.substringBefore('#').substringBefore('?').decodeURLPart() }.getOrNull() ?: return null
        if (path.startsWith('/') || ':' in path || '\u0000' in path || '\\' in path) return null
        val parts = mutableListOf<String>()
        val base = baseFile.substringBeforeLast('/', "")
        for (part in (if (base.isBlank()) path else "$base/$path").split('/')) when (part) {
            "", "." -> Unit
            ".." -> if (parts.isEmpty()) return null else parts.removeAt(parts.lastIndex)
            else -> parts += part
        }
        return parts.joinToString("/").takeIf { it.isNotBlank() }
    }
    fun merge(book: Book, metadata: Metadata, cover: String, now: Long): Book = book.copy(
        author = metadata.author.ifBlank { book.author.orEmpty() },
        description = metadata.description.ifBlank { book.description.orEmpty() },
        cover = cover.ifBlank { book.cover }, lastUpdate = now)
}
