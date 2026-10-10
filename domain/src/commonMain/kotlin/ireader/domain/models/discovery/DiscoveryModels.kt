package ireader.domain.models.discovery

enum class DiscoverySource(val id: String, val label: String, val host: String, val board: String, val language: String = "zh") {
    Qidian("qidian", "Qidian", "www.qidian.com", "Bán chạy trong ngày"),
    Fanqie("fanqie", "Fanqie", "fanqienovel.com", "Hot trong thư viện"),
    Jinjiang("jjwxc", "Jinjiang", "www.jjwxc.net", "Điểm tháng"),
    TruyenFull("truyenfull", "TruyenFull", "truyenfull.live", "Đọc nhiều trong ngày", "vi"),
    TruyenHH("truyenhh", "TruyenHH", "truyenhh.com", "Đọc nhiều trong tuần", "vi"),
    TruyenYY("truyenyy", "TruyenYY", "www.truyenyy.co", "Kim Thánh Bảng", "vi");
    val feedUrl get() = when (this) {
        TruyenFull -> "https://$host/danh-sach/truyen-hot/"
        TruyenHH -> "https://$host/bang-xep-hang/"
        TruyenYY -> "https://$host/kim-thanh-bang/"
        else -> "https://raw.githubusercontent.com/dylan-byte-max/novel-tracker/main/data/$id/latest.json"
    }
}

data class DiscoveryBook(
    val id: String, val source: DiscoverySource, val title: String, val author: String,
    val description: String, val coverUrl: String?, val url: String, val genres: List<String>,
    val status: String, val rank: Int?, val newOnRanking: Boolean,
    val isHot: Boolean = rank?.let { it <= 10 } == true,
    val recentlyUpdated: Boolean = false,
) { val key get() = "${source.id}:$id" }

data class DiscoveryFeed(
    val source: DiscoverySource, val sourceLabel: String, val sourceUrl: String,
    val updatedDate: String, val updatedTime: String, val books: List<DiscoveryBook>,
    val unavailableBooks: Int = 0,
    val nextPageUrls: List<String> = emptyList(),
)

data class DiscoveryState(
    val feeds: Map<DiscoverySource, DiscoveryFeed> = emptyMap(),
    val checkedAt: Map<DiscoverySource, Long> = emptyMap(),
    val errors: Map<DiscoverySource, String> = emptyMap(),
    val refreshing: Boolean = false,
    val loadingMore: Set<DiscoverySource> = emptySet(),
)
