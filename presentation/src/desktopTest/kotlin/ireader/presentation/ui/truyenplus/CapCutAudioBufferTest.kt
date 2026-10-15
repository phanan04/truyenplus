package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.online.CapCutAudioBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CapCutAudioBufferTest {
    @Test fun playbackJoinsSpeculativeWorkAndRetainsReadyAudioAcrossPause() = runTest {
        val calls=mutableListOf<String>()
        val buffer=CapCutAudioBuffer(backgroundScope) {text->calls+=text;delay(100);text.encodeToByteArray()}
        buffer.prefetch(listOf("một","hai","ba"));runCurrent()
        assertEquals(2,calls.size,"Only two online requests may run at once")
        val playback=async {buffer.take("một")}
        advanceTimeBy(101);runCurrent()
        assertEquals("một",playback.await().decodeToString())
        buffer.cancelPending()
        assertEquals("một",buffer.take("một").decodeToString())
        assertEquals(1,calls.count {it=="một"},"Pause/resume must reuse ready audio")
        buffer.clear()
    }
    @Test fun failedPrefetchDoesNotPoisonPlaybackAndCacheHasAHardMemoryLimit() = runTest {
        var attempts=0
        val buffer=CapCutAudioBuffer(backgroundScope,maxBytes=6,maxEntries=2) {
            attempts++;if(attempts==1)error("temporary service failure")
            byteArrayOf(1,2,3,4)
        }
        buffer.prefetch(listOf("a"));runCurrent()
        assertContentEquals(byteArrayOf(1,2,3,4),buffer.take("a"))
        assertEquals(2,attempts,"Failed speculation must be retryable on actual demand")
        buffer.take("b");buffer.take("a")
        assertEquals(4,attempts,"Evict old audio when byte budget is exceeded")
        buffer.clear()
    }
    @Test fun canceledOldRequestCannotOverwriteFreshAudioAfterASeek() = runTest {
        var calls=0
        val buffer=CapCutAudioBuffer(backgroundScope) {
            val id=++calls
            if(id==1)withContext(NonCancellable) {delay(200)}
            byteArrayOf(id.toByte())
        }
        buffer.prefetch(listOf("a"));runCurrent();buffer.cancelPending()
        assertContentEquals(byteArrayOf(2),buffer.take("a"))
        advanceTimeBy(201);runCurrent()
        assertContentEquals(byteArrayOf(2),buffer.take("a"))
        buffer.clear()
    }
    @Test fun timedOutPrefetchIsRemovedBeforePlaybackRequestsItAgain() = runTest {
        var attempts=0
        val buffer=CapCutAudioBuffer(backgroundScope) {
            if(++attempts==1)withTimeout(100) {delay(200)}
            byteArrayOf(7)
        }
        buffer.prefetch(listOf("a"));runCurrent()
        advanceTimeBy(101);runCurrent()
        assertContentEquals(byteArrayOf(7),buffer.take("a"))
        assertEquals(2,attempts,"Timed-out lookahead must not leave a canceled job in the queue")
        buffer.clear()
    }
}
