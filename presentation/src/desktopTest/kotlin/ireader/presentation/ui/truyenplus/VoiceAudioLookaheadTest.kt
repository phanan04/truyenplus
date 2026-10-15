package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.offline.VoiceAudioLookahead
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceAudioLookaheadTest {
    @Test fun nextParagraphIsPreparedDuringTheLastSentenceAndSurvivesNaturalCompletion() = runTest {
        val generated=mutableListOf<String>()
        val queue=VoiceAudioLookahead<String>(backgroundScope) {text,_->generated+=text;delay(100);text}
        queue.begin(keepPrepared=false)
        queue.announceNextParagraph("Đoạn tiếp theo.")
        queue.currentAudioReady(null,1f)
        advanceTimeBy(101);runCurrent()
        assertEquals(listOf("Đoạn tiếp theo."),generated)
        queue.begin(keepPrepared=true)
        assertEquals("Đoạn tiếp theo.",queue.takeOrGenerate("Đoạn tiếp theo.",1f))
        assertEquals(1,generated.size,"A natural paragraph boundary must not synthesize the same audio again")
    }
    @Test fun futureParagraphCannotEvictTheNextSentenceOfTheCurrentParagraph() = runTest {
        val generated=mutableListOf<String>()
        val queue=VoiceAudioLookahead<String>(backgroundScope) {text,_->generated+=text;delay(100);text}
        queue.begin(false);queue.currentAudioReady("Câu thứ hai.",1f)
        queue.announceNextParagraph("Đoạn khác.")
        advanceTimeBy(101);runCurrent()
        assertEquals(listOf("Câu thứ hai."),generated)
        assertEquals("Câu thứ hai.",queue.takeOrGenerate("Câu thứ hai.",1f))
        queue.currentAudioReady(null,1f)
        advanceTimeBy(101);runCurrent()
        assertEquals(listOf("Câu thứ hai.","Đoạn khác."),generated)
    }
    @Test fun lateLookaheadStartsWhileAudioPlaysButPauseCancelsIt() = runTest {
        var cancelled=false
        val queue=VoiceAudioLookahead<String>(backgroundScope) {text,_->
            try {delay(10_000);text} finally {cancelled=!currentCoroutineContext().isActive}
        }
        queue.begin(false);queue.currentAudioReady(null,1f)
        queue.announceNextParagraph("Âm thanh đang chuẩn bị.");runCurrent()
        queue.begin(false);runCurrent()
        assertTrue(cancelled,"Explicit pause/seek must still cancel lookahead work")
    }
}
