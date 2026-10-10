package ireader.presentation.ui.truyenplus

import ireader.domain.models.discovery.DiscoveryFeed
import ireader.domain.models.discovery.DiscoveryBook

enum class DiscoverySection { Ranking, Hot, New }

fun filterDiscoveryBooks(feed: DiscoveryFeed?, section: DiscoverySection, genre: String, query: String, translations: Map<String, ireader.domain.data.repository.DiscoveryTranslation> = emptyMap()): List<DiscoveryBook> {
    val books = feed?.books.orEmpty()
    val selected = when (section) {
        DiscoverySection.Ranking -> if (feed?.source == ireader.domain.models.discovery.DiscoverySource.TruyenFull) books.filter { it.rank != null }.sortedBy { it.rank } else books
        DiscoverySection.Hot -> books.filter { it.isHot }
        DiscoverySection.New -> books.filter { it.newOnRanking || it.recentlyUpdated }
    }
    val term = query.trim()
    return selected.filter { (genre.isBlank() || genre in it.genres) &&
        (term.isBlank() || it.title.contains(term, ignoreCase = true) || it.author.contains(term, ignoreCase = true) ||
            translations[it.key]?.title?.contains(term, ignoreCase = true) == true) }
}
