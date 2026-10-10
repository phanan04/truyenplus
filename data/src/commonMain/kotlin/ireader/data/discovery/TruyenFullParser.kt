package ireader.data.discovery

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import ireader.domain.models.discovery.*
import kotlinx.serialization.json.*

/** Public Vietnamese catalog only; explicit top-num values are the only source of ranks. */
object TruyenFullParser {
    private val source = DiscoverySource.TruyenFull
    private val base = "https://${source.host}/"
    private val bookUrl = Regex("https://${Regex.escape(source.host)}/([a-z0-9]+(?:-[a-z0-9]+)*)/")
    private fun document(html: String) = Ksoup.parse(html.also { require(it.length <= 2_000_000) }, base)
    private fun link(element: Element?) = element?.absUrl("href")?.takeIf { bookUrl.matches(it) }
    private fun cover(value: String?) = value?.let { if (it.startsWith("//")) "https:$it" else it }
        ?.takeIf { Regex("https://[A-Za-z0-9.-]+/[^\\s]*").matches(it) }

    fun listings(hotHtml: String, newHtml: String): List<DiscoveryBook> {
        val hot = document(hotHtml)
        val fresh = document(newHtml)
        fun list(doc: Element, updated: Boolean) = doc.select(".col-truyen-main .list-truyen > .row[itemtype*=Book]")
            .mapNotNull { row ->
                val anchor = row.selectFirst(".truyen-title a")
                val url = link(anchor) ?: return@mapNotNull null
                val title = anchor?.text().orEmpty()
                val author = row.selectFirst("[itemprop=author]")?.text().orEmpty()
                if (title.isBlank() || author.isBlank()) return@mapNotNull null
                DiscoveryBook(bookUrl.matchEntire(url)!!.groupValues[1], source, title, author, "",
                    cover(row.selectFirst("[data-image]")?.attr("data-image")), url, emptyList(),
                    if (row.selectFirst(".label-full") != null) "Full" else "", null, false,
                    isHot = !updated, recentlyUpdated = updated)
            }.distinctBy { it.id }.take(20)
        val hotBooks = list(hot, false)
        val newBooks = list(fresh, true)
        require(hotBooks.isNotEmpty() && newBooks.isNotEmpty()) { "Không đọc được danh sách TruyenFull" }
        val books = (hotBooks + newBooks).groupBy { it.id }.mapValues { (_, items) ->
            items.first().copy(isHot = items.any { it.isHot }, recentlyUpdated = items.any { it.recentlyUpdated })
        }.toMutableMap()
        // Only the active day board is read; never infer a rank from the main hot/new lists.
        val board = hot.select(".list-side").firstOrNull { it.selectFirst(".top-nav .active")?.attr("data-type") == "day" }
            ?: error("Thiếu bảng xếp hạng ngày")
        for (row in board.select(".top-item")) {
            val url = link(row.selectFirst(".s-title a")) ?: continue
            val id = bookUrl.matchEntire(url)!!.groupValues[1]
            val rank = row.selectFirst(".top-num")?.text()?.toIntOrNull()?.takeIf { it in 1..10 } ?: continue
            val genres = row.select("[itemprop=genre]").map { it.text() }.filter { it.isNotBlank() }.distinct()
            val existing = books[id]
            books[id] = existing?.copy(rank = rank, genres = genres) ?: DiscoveryBook(id, source,
                row.selectFirst(".s-title a")!!.text(), "", "", null, url, genres, "", rank, false, isHot = false)
        }
        require(books.values.any { it.rank != null }) { "Bảng xếp hạng chưa có truyện hợp lệ" }
        return books.values.toList()
    }

    fun details(book: DiscoveryBook, html: String): DiscoveryBook {
        val doc = document(html)
        val canonical = doc.selectFirst("link[rel=canonical]")?.absUrl("href")
        require(canonical == null || canonical == book.url) { "Trang trả về sai truyện" }
        val title = doc.selectFirst("#truyen .title[itemprop=name], #truyen h3.title")?.text().orEmpty()
        val author = doc.selectFirst(".info [itemprop=author]")?.text().orEmpty()
        val description = doc.selectFirst(".desc-text[itemprop=description]")
        require(title.isNotBlank() && author.isNotBlank() && description != null) { "Thiếu thông tin truyện" }
        description.select("script, style, [hidden], [style*=display:none], [style*=display: none], [style*=visibility:hidden], [style*=visibility: hidden]").remove()
        return book.copy(title = title, author = author, description = description.text(),
            coverUrl = cover(doc.selectFirst(".book [itemprop=image]")?.absUrl("src")) ?: book.coverUrl,
            genres = doc.select(".info [itemprop=genre]").map { it.text() }.filter { it.isNotBlank() }.distinct(),
            status = doc.select(".info > div").firstOrNull { it.selectFirst("h3")?.text() == "Trạng thái:" }
                ?.selectFirst("span")?.text().orEmpty())
    }

    /** Normalize HTML metadata into the existing, persistable discovery snapshot schema. */
    fun snapshot(books: List<DiscoveryBook>, fetchedAt: String, unavailableBooks: Int = 0): String = buildJsonObject {
        put("platform", source.id); put("source", source.board); put("source_url", source.feedUrl)
        put("update_date", fetchedAt.take(10)); put("update_time", fetchedAt)
        put("unavailable_books", unavailableBooks)
        put("books", buildJsonArray { books.forEach { book -> add(buildJsonObject {
            put("book_id", book.id); put("book_name", book.title); put("author", book.author)
            put("abstract", book.description); put("book_url", book.url); put("thumb_url", book.coverUrl.orEmpty())
            put("all_tags", JsonArray(book.genres.map(::JsonPrimitive))); put("status", book.status)
            book.rank?.let { put("rank", it) }; put("is_hot", book.isHot); put("recently_updated", book.recentlyUpdated)
        }) } })
    }.toString()
}
