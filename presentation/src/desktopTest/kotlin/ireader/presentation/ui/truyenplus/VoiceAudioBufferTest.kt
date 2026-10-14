package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.offline.VoiceAudioBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceAudioBufferTest {
    @Test fun pausingDuringPrefetchedSynthesisCancelsTheNativeWork() = runTest {
        var cancelled=false
        val buffer=VoiceAudioBuffer<String>(backgroundScope) {text,_->
            try {delay(10000);text} finally {cancelled=!currentCoroutineContext().isActive}
        }
        buffer.prefetch("Một câu chưa xong.",1f);runCurrent()
        val playback=launch {buffer.takeOrGenerate("Một câu chưa xong.",1f)}
        runCurrent();playback.cancel();runCurrent()
        assertTrue(cancelled,"Paused playback must not leave consumed native synthesis running")
    }
    @Test fun playbackAwaitsThePendingSentenceInsteadOfSynthesizingItTwice() = runTest {
        var calls=0
        val buffer=VoiceAudioBuffer<String>(backgroundScope) {text,_->calls++;delay(1000);text}
        buffer.prefetch("Một câu tiếp theo.",1f)
        runCurrent()
        val playback=async {buffer.takeOrGenerate("Một câu tiếp theo.",1f)}
        runCurrent()
        assertEquals(1,calls)
        advanceTimeBy(1001);runCurrent()
        assertEquals("Một câu tiếp theo.",playback.await())
        assertEquals(1,calls)
    }
    @Test fun changingSpeedAndStoppingDiscardOnlyThePendingAudio() = runTest {
        val generated=mutableListOf<Float>()
        val buffer=VoiceAudioBuffer<String>(backgroundScope) {text,rate->generated+=rate;delay(100);text}
        buffer.prefetch("Một câu.",1f);runCurrent()
        val result=async {buffer.takeOrGenerate("Một câu.",2f)}
        advanceTimeBy(101);runCurrent()
        assertEquals("Một câu.",result.await())
        assertEquals(listOf(1f,2f),generated)
        buffer.prefetch("Đoạn cũ.",1f);runCurrent();buffer.clear()
        val resumed=async {buffer.takeOrGenerate("Đoạn mới.",1f)}
        advanceTimeBy(101);runCurrent()
        assertEquals("Đoạn mới.",resumed.await())
    }
}
