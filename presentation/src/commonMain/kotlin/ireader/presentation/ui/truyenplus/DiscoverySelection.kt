package ireader.presentation.ui.truyenplus

import ireader.domain.models.discovery.DiscoveryFeed
import ireader.domain.models.discovery.DiscoveryBook

enum class DiscoverySection(val label: String) { Ranking("Bảng xếp hạng"), Hot("Truyện hot"), New("Mới lên bảng") }

fun filterDiscoveryBooks(feed: DiscoveryFeed?, section: DiscoverySection, genre: String, query: String): List<DiscoveryBook> {
    val books = feed?.books.orEmpty()
    val selected = when (section) {
        DiscoverySection.Ranking -> books
        DiscoverySection.Hot -> books.filter { it.rank?.let { rank -> rank <= 10 } == true }
        DiscoverySection.New -> books.filter { it.newOnRanking }
    }
    val term = query.trim()
    return selected.filter { (genre.isBlank() || genre in it.genres) &&
        (term.isBlank() || it.title.contains(term, ignoreCase = true) || it.author.contains(term, ignoreCase = true)) }
}
