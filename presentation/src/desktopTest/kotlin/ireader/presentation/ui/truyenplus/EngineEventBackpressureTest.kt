package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class EngineEventBackpressureTest {
    @Test fun aBusyReaderCannotRejectTheFinalCompletionBehindWordRanges() = runTest {
        val events=ttsEngineEventBuffer()
        val completed=async(start=CoroutineStart.UNDISPATCHED) {
            withTimeout(20000){events.onEach {delay(100)}.first {it is EngineEvent.Completed}}
        }
        events.tryEmit(EngineEvent.RangeStarted("chapter",0,1));runCurrent()
        repeat(200){events.tryEmit(EngineEvent.RangeStarted("chapter",it,it+1))}
        assertTrue(events.tryEmit(EngineEvent.Completed("chapter")))
        advanceTimeBy(10000);runCurrent()
        assertEquals(EngineEvent.Completed("chapter"),completed.await())
    }
}
