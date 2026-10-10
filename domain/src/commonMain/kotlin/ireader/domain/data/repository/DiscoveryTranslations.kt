package ireader.domain.data.repository

import ireader.domain.models.discovery.DiscoveryBook

/** Only public catalog metadata is translated; authors and original feeds remain intact. */
data class DiscoveryTranslation(
    val original: DiscoveryBook, val language: String, val title: String,
    val description: String, val genres: List<String>, val status: String,
)

fun interface DiscoveryTranslator {
    suspend fun translate(texts: List<String>, target: String): List<String>
    suspend fun translateFrom(texts: List<String>, source: String, target: String): List<String> {
        require(source == "zh") { "Unsupported source language" }
        return translate(texts, target)
    }
}

interface DiscoveryTranslations {
    suspend fun cached(book: DiscoveryBook, language: String): DiscoveryTranslation?
    suspend fun translate(book: DiscoveryBook, language: String): DiscoveryTranslation
}
