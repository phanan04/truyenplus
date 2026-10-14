package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPlaybackControlsTest {
    @Test fun controlCollectorIgnoresWordCacheAndTimerNoiseButKeepsPlaybackChanges()=runTest {
        val state=MutableStateFlow(TTSState(paragraphs=listOf("Nàng nói.","Đi thôi."),engineName="Native Android TTS"))
        val received=mutableListOf<TTSState>()
        val job=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {state.readerControlUpdates().collect {received+=it}}
        repeat(100) {i ->state.value=state.value.copy(spokenWord=SpokenSentencePosition(0,0,4,"Nàng"),
            spokenSentence=SpokenSentencePosition(0,0,9,"Nàng nói."),sleepTimeRemaining=i.toLong(),cachedParagraphs=setOf(i));runCurrent()}
        assertEquals(1,received.size,"Controls must not recompose for every spoken word or cache update")
        state.value=state.value.copy(speed=1.5f,playbackState=PlaybackState.PLAYING,currentParagraphIndex=1);runCurrent()
        assertEquals(2,received.size);assertEquals(1.5f,received.last().speed);assertTrue(received.last().isPlaying)
        job.cancel()
    }
    @Test fun offlineVoicesDoNotAdvertiseWordTimestampsAndUnknownSystemVoicesStayConditional() {
        assertEquals(ReaderWordTiming.SentenceOnly,readerWordTiming("Piper",false))
        assertEquals(ReaderWordTiming.SentenceOnly,readerWordTiming("Supertonic 3 · 10",false))
        assertEquals(ReaderWordTiming.Conditional,readerWordTiming("Native Android TTS",false))
        assertEquals(ReaderWordTiming.Reported,readerWordTiming("Native Android TTS",true))
    }
}
