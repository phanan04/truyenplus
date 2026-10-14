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
    return model.title+if(model.speakers>1)" · "+(model.voiceNames.getOrNull(selection.speaker) ?: (selection.speaker+1).toString()) else ""
}

/** Reader ignores cache/timer ticks; word ranges are opt-in so other screens stay unchanged. */
data class ReaderPlaybackProjection(val book: Book?, val chapter: Chapter?, val currentParagraphIndex: Int,
    val paragraphSourceIndices: List<Int>, val isPlaying: Boolean, val chunkModeEnabled: Boolean,
    val spokenSentence: ireader.domain.services.tts_service.v2.SpokenSentencePosition? = null, val isLoading:Boolean=false,
    val spokenWord:ireader.domain.services.tts_service.v2.SpokenSentencePosition?=null, val hasListeningSession:Boolean=false)
fun TTSState.readerProjection(includeWordRanges:Boolean=false):ReaderPlaybackProjection {
    val sentence=spokenSentence?.takeIf { it.paragraphIndex==currentParagraphIndex || (chunkModeEnabled && it.paragraphIndex in currentChunkParagraphs) }
    return ReaderPlaybackProjection(book,chapter,currentParagraphIndex,paragraphSourceIndices,isPlaying,chunkModeEnabled,sentence,isLoading,
        spokenWord?.takeIf {includeWordRanges && sentence?.paragraphIndex==it.paragraphIndex},
        isPlaying || isPaused || isLoading)
}
fun Flow<TTSState>.readerUpdates(includeWordRanges:Boolean=false): Flow<ReaderPlaybackProjection> = map { it.readerProjection(includeWordRanges) }.distinctUntilChanged()

class ReaderSpeechIndex(sourceIndices: List<Int>) {
    private val firstParagraph=buildMap<Int,Int> { sourceIndices.forEachIndexed { speech, page -> if(page !in this) put(page,speech) } }
    fun paragraphForReaderPage(index: Int): Int? = firstParagraph[index]
}

/** Listening controls do not consume word positions, cache transfer ticks or the timer mirror. */
fun Flow<TTSState>.readerControlUpdates():Flow<TTSState> = map {it.copy(
    spokenSentence=null,spokenWord=null,sleepTimeRemaining=0,
    cachedParagraphs=emptySet(),loadingParagraphs=emptySet(),cachedChunks=emptySet())}.distinctUntilChanged()

enum class ReaderWordTiming { SentenceOnly, Conditional, Reported }
fun readerWordTiming(engine:String,reported:Boolean):ReaderWordTiming = when {
    engine.startsWith("Piper") || engine.startsWith("VieNeu v3 Nano")->ReaderWordTiming.SentenceOnly
    reported->ReaderWordTiming.Reported
    else->ReaderWordTiming.Conditional
}
