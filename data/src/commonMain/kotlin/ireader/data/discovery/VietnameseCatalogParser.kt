package ireader.data.discovery

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import ireader.domain.models.discovery.*
import kotlinx.serialization.json.*

data class VietnameseCatalogListing(val books: List<DiscoveryBook>, val nextPageUrls: List<String>)

/** Public catalog metadata only. Never derive ranks from the position of a book in a list. */
class VietnameseCatalogParser(private val source: DiscoverySource) {
    private val base = "https://${source.host}/"
    private val yy = source == DiscoverySource.TruyenYY
    private val bookUrl = Regex("${Regex.escape(base)}truyen/([a-z0-9]+(?:-[a-z0-9]+)*)${if (yy) "/?" else "/"}")
    private fun document(html: String) = Ksoup.parse(html.also { require(it.length <= 2_000_000) }, base)
    private fun text(element: Element?) = element?.also { it.select(".screen-reader-text, script, style, [hidden]").remove() }?.text().orEmpty()
    private fun book(row: Element, hot: Boolean, updated: Boolean, rank: Int? = null): DiscoveryBook? {
        val anchor = row.selectFirst("h3 a, h2 a, h4 a, .name a, .book-title a, .novel-title a") ?: return null
        val url = anchor.absUrl("href").takeIf { bookUrl.matches(it) } ?: return null
        val title = text(anchor).takeIf { it.isNotBlank() } ?: return null
        return DiscoveryBook(bookUrl.matchEntire(url)!!.groupValues[1], source, title,
            text(row.selectFirst(".bxh-pod-author, .bxh-meta > span:first-child, .author, .book-author")), "",
            row.selectFirst("img")?.absUrl("src"), url, emptyList(), "", rank, false, hot, updated)
    }
    fun isPageUrl(url: String) = if (yy) Regex("${Regex.escape(base)}truyen-moi-cap-nhat/?\\?(?:page|p)=[1-9][0-9]*").matches(url)
        else Regex("${Regex.escape(base)}truyen/page/[1-9][0-9]*/").matches(url)

    fun listings(rankHtml: String, hotHtml: String, newHtml: String): VietnameseCatalogListing {
        if (yy) {
            val ranked = yyRows(document(rankHtml)).mapNotNull { row ->
                val number = row.selectFirst(".rank, .rank-number, .rank-num, .ranking-number")?.text()
                    ?.trim()?.removePrefix("#")?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                book(row, false, false, number)
            }
            require(ranked.isNotEmpty()) { "Không đọc được bảng xếp hạng" }
            // Like the Chinese providers, YY Hot is the first ten published positions.
            val hot = ranked.filter { requireNotNull(it.rank) <= 10 }.map { it.copy(isHot = true) }
            val fresh = page(newHtml)
            return VietnameseCatalogListing(merge(ranked + hot + fresh.books), fresh.nextPageUrls)
        }
        val rank = document(rankHtml)
        val ranked = rank.select(".bxh-pod, .bxh-row").mapNotNull { row ->
            val number = row.selectFirst(".bxh-medal, .bxh-num")?.text()?.toIntOrNull()?.takeIf { it in 1..100 }
                ?: return@mapNotNull null
            book(row, false, false, number)
        }
        require(ranked.isNotEmpty()) { "Không đọc được bảng xếp hạng" }
        val hot = document(hotHtml).select(".home-hot .truyen-col").mapNotNull { book(it, true, false) }
        val fresh = page(newHtml)
        return VietnameseCatalogListing(merge(ranked + hot + fresh.books), fresh.nextPageUrls)
    }
    fun page(html: String): VietnameseCatalogListing {
        val doc = document(html)
        val rows = if (yy) yyRows(doc) else doc.select(".home-main-left .truyen-grid .truyen-col")
        val books = rows.mapNotNull { book(it, false, true) }
        require(books.isNotEmpty()) { "Không đọc được danh sách truyện" }
        val next = doc.select(".pagination a.next, a[rel=next], .page-item.next a").map { it.absUrl("href") }.filter(::isPageUrl).distinct()
        return VietnameseCatalogListing(books, next)
    }
    private fun yyRows(doc: Element) = doc.select(".media, .book-item, .novel-item, .ranking-item")
    private fun merge(books: List<DiscoveryBook>) = books.groupBy { it.id }.values.map { values ->
        values.first().copy(isHot = values.any { it.isHot }, recentlyUpdated = values.any { it.recentlyUpdated })
    }
    fun details(book: DiscoveryBook, html: String): DiscoveryBook {
        val doc = document(html)
        val canonical = doc.selectFirst("link[rel=canonical]")?.absUrl("href")
        require(canonical == null || canonical == book.url) { "Trang trả về sai truyện" }
        val title = text(doc.selectFirst(if (yy) ".novel-info .name" else ".truyen-detail-info h1"))
        val author = text(doc.selectFirst(if (yy) ".novel-info .author a" else ".truyen-detail-meta a[href*=/tac-gia/]"))
        val description = text(doc.selectFirst(if (yy) ".novel-description, .novel-desc, .summary, [itemprop=description]" else ".truyen-intro-body"))
        val image = doc.selectFirst(if (yy) ".novel-info img" else ".truyen-detail-thumb img, .truyen-detail-cover img")
        val cover = image?.absUrl("data-src")?.takeIf { it.isNotBlank() } ?: image?.absUrl("src")?.takeIf { it.isNotBlank() } ?: book.coverUrl
        require(title.isNotBlank() && author.isNotBlank() && description.isNotBlank() && cover != null) { "Thiếu thông tin truyện" }
        return book.copy(title = title, author = author, description = description, coverUrl = cover,
            genres = doc.select(if (yy) ".novel-info a[href*=/the-loai/], .novel-tags a" else ".truyen-detail-genre a").map { it.text() }.filter { it.isNotBlank() }.distinct(),
            status = text(doc.selectFirst(if (yy) ".novel-info .status" else ".truyen-detail-meta a[href*=/tinh-trang/]")))
    }
    fun snapshot(books: List<DiscoveryBook>, fetchedAt: String, nextPageUrls: List<String> = emptyList(), unavailable: Int = 0) = buildJsonObject {
        put("platform", source.id); put("source", source.board); put("source_url", source.feedUrl)
        put("update_date", fetchedAt.take(10)); put("update_time", fetchedAt); put("unavailable_books", unavailable)
        put("next_pages", JsonArray(nextPageUrls.filter(::isPageUrl).map(::JsonPrimitive)))
        put("books", buildJsonArray { books.forEach { book -> add(buildJsonObject {
            put("book_id", book.id); put("book_name", book.title); put("author", book.author)
            put("abstract", book.description); put("book_url", book.url); put("thumb_url", book.coverUrl.orEmpty())
            put("all_tags", JsonArray(book.genres.map(::JsonPrimitive))); put("status", book.status)
            book.rank?.let { put("rank", it) }; put("is_hot", book.isHot); put("recently_updated", book.recentlyUpdated)
        }) } })
    }.toString()
}
