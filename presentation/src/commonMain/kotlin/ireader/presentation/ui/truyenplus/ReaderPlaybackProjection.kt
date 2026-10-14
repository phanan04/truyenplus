package ireader.presentation.ui.truyenplus

import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.services.tts_service.v2.TTSState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

fun readerVoiceLabel(selection:ireader.domain.services.tts_service.v2.offline.OfflineVoiceSelection?,
    actualEngine:String,systemVoice:String):String {
    if(selection==null || actualEngine=="Native Android TTS")return systemVoice
    if(actualEngine.isNotBlank())return actualEngine
    val model=ireader.domain.services.tts_service.v2.offline.OfflineVoiceCatalog.find(selection.modelId) ?: return systemVoice
    return model.title+if(model.speakers>1)" · ${selection.speaker+1}" else ""
}

/** Reader ignores cache downloads, word timing and timer ticks; the player owns those. */
data class ReaderPlaybackProjection(val book: Book?, val chapter: Chapter?, val currentParagraphIndex: Int,
    val paragraphSourceIndices: List<Int>, val isPlaying: Boolean, val chunkModeEnabled: Boolean,
    val spokenSentence: ireader.domain.services.tts_service.v2.SpokenSentencePosition? = null, val isLoading:Boolean=false)
fun TTSState.readerProjection() = ReaderPlaybackProjection(book,chapter,currentParagraphIndex,paragraphSourceIndices,isPlaying,chunkModeEnabled,spokenSentence?.takeIf { it.paragraphIndex==currentParagraphIndex || (chunkModeEnabled && it.paragraphIndex in currentChunkParagraphs) },isLoading)
fun Flow<TTSState>.readerUpdates(): Flow<ReaderPlaybackProjection> = map { it.readerProjection() }.distinctUntilChanged()

class ReaderSpeechIndex(sourceIndices: List<Int>) {
    private val firstParagraph=buildMap<Int,Int> { sourceIndices.forEachIndexed { speech, page -> if(page !in this) put(page,speech) } }
    fun paragraphForReaderPage(index: Int): Int? = firstParagraph[index]
}
