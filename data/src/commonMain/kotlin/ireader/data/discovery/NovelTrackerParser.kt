package ireader.data.discovery

import ireader.domain.models.discovery.*
import kotlinx.serialization.json.*

/** Reads published metadata only. No generated records or synthetic ranks. */
object NovelTrackerParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(source: DiscoverySource, raw: String): DiscoveryFeed {
        require(raw.length <= 2_000_000) { "Dữ liệu quá lớn" }
        val root = json.parseToJsonElement(raw).jsonObject
        require(root.text("platform") == source.id) { "Sai nguồn dữ liệu" }
        require(root["demo"]?.jsonPrimitive?.booleanOrNull != true && root["is_demo"]?.jsonPrimitive?.booleanOrNull != true) { "Dữ liệu mẫu không được hỗ trợ" }
        val date = root.text("update_date")
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "Thiếu ngày cập nhật" }
        val sourceUrl = root.text("source_url")
        require(officialUrl(source, sourceUrl)) { "Liên kết nguồn không hợp lệ" }
        val books = root["books"]?.jsonArray.orEmpty().mapNotNull { entry ->
            val item = entry as? JsonObject ?: return@mapNotNull null
            val id = item.text("book_id")
            val title = item.text("book_name")
            val author = item.text("author")
            val url = item.text("book_url")
            val demo = Regex("^(?:番茄热门小说|起点畅销小说|晋江热文)\\d+号").containsMatchIn(title)
            require(!demo) { "Phát hiện dữ liệu mẫu" }
            if (!Regex("[0-9]+").matches(id) || title.isBlank() || author.isBlank() || !officialBookUrl(source, id, url)) return@mapNotNull null
            val genres = (listOf(item.text("primary_tag")) +
                listOf("all_tags", "secondary_tags").flatMap { field ->
                    (item[field] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                }).filter { it.isNotBlank() }.distinct()
            val cover = item.text("thumb_url").let { if (it.startsWith("//")) "https:$it" else it }
                .takeIf { Regex("https://[A-Za-z0-9.-]+(?::443)?/[^\\s]*").matches(it) }
            DiscoveryBook(id, source, title, author, item.text("abstract"), cover, url, genres,
                item.text("status"), (item["rank"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 },
                item.text("rank_change") == "new")
        }.distinctBy { it.id }.sortedWith(compareBy(nullsLast()) { it.rank })
        require(books.isNotEmpty()) { "Nguồn chưa có truyện hợp lệ" }
        return DiscoveryFeed(source, root.text("source").ifBlank { source.board }, sourceUrl,
            date, root.text("update_time"), books)
    }
    private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
    private fun officialUrl(source: DiscoverySource, url: String) =
        url.startsWith("https://${source.host}/") && !url.any { it.isWhitespace() }
    private fun officialBookUrl(source: DiscoverySource, id: String, url: String) = when (source) {
        DiscoverySource.Qidian -> url == "https://${source.host}/book/$id/" || url == "https://${source.host}/book/$id"
        DiscoverySource.Fanqie -> url == "https://${source.host}/page/$id"
        DiscoverySource.Jinjiang -> url == "https://${source.host}/onebook.php?novelid=$id"
    }
}
