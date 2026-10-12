package ireader.presentation.ui.truyenplus

import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.services.tts_service.v2.TTSState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** Reader ignores cache downloads, sentence timing and timer ticks; the player owns those. */
data class ReaderPlaybackProjection(val book: Book?, val chapter: Chapter?, val currentParagraphIndex: Int,
    val paragraphSourceIndices: List<Int>, val isPlaying: Boolean, val chunkModeEnabled: Boolean)
fun TTSState.readerProjection() = ReaderPlaybackProjection(book,chapter,currentParagraphIndex,paragraphSourceIndices,isPlaying,chunkModeEnabled)
fun Flow<TTSState>.readerUpdates(): Flow<ReaderPlaybackProjection> = map { it.readerProjection() }.distinctUntilChanged()

class ReaderSpeechIndex(sourceIndices: List<Int>) {
    private val firstParagraph=buildMap<Int,Int> { sourceIndices.forEachIndexed { speech, page -> if(page !in this) put(page,speech) } }
    fun paragraphForReaderPage(index: Int): Int? = firstParagraph[index]
}
