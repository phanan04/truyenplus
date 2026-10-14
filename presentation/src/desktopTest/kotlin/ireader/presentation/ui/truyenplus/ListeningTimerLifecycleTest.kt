package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningTimerLifecycleTest {
    @Test fun countsOnlyActualPlaybackAndSurvivesScreenExit() = runTest {
        val playback = MutableStateFlow(TTSState(playbackState = PlaybackState.PLAYING))
        var expired = 0
        val sessionScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val timer = TTSSleepTimerUseCase(sessionScope) { testScheduler.currentTime }
        timer.bind(playback) { expired++ }
        timer.start(1)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(50_000L, timer.state.value.remainingTimeMs)
        playback.value = playback.value.copy(playbackState = PlaybackState.PAUSED); runCurrent()
        advanceTimeBy(40_000); runCurrent()
        assertEquals(50_000L, timer.state.value.remainingTimeMs)
        assertTrue(timer.state.value.isPaused)
        // A screen has a separate scope; ending it must not destroy the session timer.
        val screenScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        screenScope.cancel()
        playback.value = playback.value.copy(playbackState = PlaybackState.PLAYING); runCurrent()
        advanceTimeBy(50_000); runCurrent()
        assertEquals(1, expired)
        assertFalse(timer.isActive())
        timer.cleanup(); sessionScope.cancel()
    }
    @Test fun preparingAudioDoesNotSpendTheTimerAndAddingMinutesKeepsSpentTime() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val playback = MutableStateFlow(TTSState(playbackState = PlaybackState.LOADING))
        val timer = TTSSleepTimerUseCase(scope) { testScheduler.currentTime }
        timer.bind(playback) {}
        timer.start(1)
        advanceTimeBy(20_000); runCurrent()
        assertEquals(60_000L, timer.getRemainingTimeMs())
        playback.value = playback.value.copy(playbackState = PlaybackState.PLAYING); runCurrent()
        advanceTimeBy(12_000); runCurrent()
        timer.addTime(1)
        assertEquals(108_000L, timer.getRemainingTimeMs())
        timer.cleanup(); scope.cancel()
    }
}
