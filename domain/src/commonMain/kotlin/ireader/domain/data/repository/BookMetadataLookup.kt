package ireader.domain.data.repository

import ireader.domain.models.entities.Book

/** Optional, explicit online enrichment; source identity and chapters stay local. */
interface BookMetadataLookup {
    data class Match(val book: Book, val sourceUrl: String)
    suspend fun find(book: Book): Match?
}
