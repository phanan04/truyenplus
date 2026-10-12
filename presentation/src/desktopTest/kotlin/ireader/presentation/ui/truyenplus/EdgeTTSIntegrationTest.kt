package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class EdgeTTSIntegrationTest {
    @Test fun vietnameseVoicesAndTextFramesAreSafe() {
        assertEquals("vi-VN-HoaiMyNeural",EdgeVoice.HoaiMy.id)
        assertEquals("vi-VN-NamMinhNeural",EdgeVoice.NamMinh.id)
        val chunks=ireader.data.tts.edge.EdgeSpeechWire.textChunks("Xin chào & <bạn> 😀. ".repeat(500))
        assertEquals("Xin chào & <bạn> 😀. ".repeat(500),chunks.joinToString(""))
        assertTrue(chunks.all { ireader.data.tts.edge.EdgeSpeechWire.escape(it).encodeToByteArray().size<=3800 })
        val ssml=ireader.data.tts.edge.EdgeSpeechWire.ssml("A & <B>",EdgeVoice.HoaiMy)
        assertTrue("A &amp; &lt;B&gt;" in ssml)
        val header="Path:audio\r\nContent-Type:audio/mpeg\r\n".encodeToByteArray()
        val packet=byteArrayOf((header.size shr 8).toByte(),header.size.toByte())+header+byteArrayOf(1,2,3)
        assertContentEquals(byteArrayOf(1,2,3),ireader.data.tts.edge.EdgeSpeechWire.audioPayload(packet))
        assertFails { ireader.data.tts.edge.EdgeSpeechWire.audioPayload(byteArrayOf(0,100,1)) }
    }
    private class Audio: EdgeAudioOutput {
        var starts=0;var stops=0;var done:(()->Unit)?=null
        override suspend fun play(bytes:ByteArray,onStarted:()->Unit,onCompleted:()->Unit,onError:()->Unit) { starts++;done=onCompleted;onStarted() }
        override fun pause() {}
        override fun resume() {}
        override fun stop() { stops++ }
        override fun release() {}
        override fun setSpeed(speed:Float) {}
        override fun setPitch(pitch:Float) {}
    }
    @Test fun stopCancelsPendingSpeechAndPreventsLatePlayback()=runTest {
        val audio=Audio()
        val synth=EdgeSpeechSynthesizer { _,_->delay(1000);byteArrayOf(1) }
        val engine=EdgeTTSEngine(EdgeVoice.HoaiMy,synth,audio,backgroundScope)
        engine.speak("Đang tải","old");runCurrent();engine.stop();advanceTimeBy(1500);runCurrent()
        assertEquals(0,audio.starts)
        engine.speak("Đoạn mới","new");advanceTimeBy(1500);runCurrent()
        assertEquals(1,audio.starts)
        engine.release()
    }
    @Test fun pauseWhileDownloadingDoesNotStartUntilResume()=runTest {
        val audio=Audio()
        val engine=EdgeTTSEngine(EdgeVoice.NamMinh,EdgeSpeechSynthesizer { _,_->delay(100);byteArrayOf(1) },audio,backgroundScope)
        engine.speak("Một đoạn","p");runCurrent();engine.pause();advanceTimeBy(500);runCurrent()
        assertEquals(0,audio.starts)
        engine.resume();advanceTimeBy(500);runCurrent()
        assertEquals(1,audio.starts)
        engine.release()
    }
    @Test fun failedRequestsEmitErrorsInsteadOfPretendingCompleted()=runTest {
        val events=mutableListOf<EngineEvent>()
        val engine=EdgeTTSEngine(EdgeVoice.HoaiMy,EdgeSpeechSynthesizer { _,_->error("No audio") },Audio(),backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.events.toList(events) }
        engine.speak("Xin chào","p");advanceTimeBy(1000);runCurrent()
        assertTrue(events.any { it is EngineEvent.Error })
        assertFalse(events.any { it is EngineEvent.Completed })
        engine.release()
    }
    @Test fun synthesisTimeoutEmitsAnErrorForFallback()=runTest {
        val events=mutableListOf<EngineEvent>()
        val engine=EdgeTTSEngine(EdgeVoice.HoaiMy,EdgeSpeechSynthesizer { _,_->
            withTimeout(20) { delay(100);byteArrayOf(1) }
        },Audio(),backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.events.toList(events) }
        engine.speak("Một đoạn", "timeout");advanceTimeBy(200);runCurrent()
        assertTrue(events.any { it is EngineEvent.Error }, "A service timeout must not leave the reader loading forever")
        engine.release()
    }

}
