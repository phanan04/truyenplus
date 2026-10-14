package ireader.domain.services.tts_service.v2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource

private val listeningTimerEpoch = TimeSource.Monotonic.markNow()

/** Session-owned timer. Only actual speech spends time; leaving a screen does not cancel it. */
class TTSSleepTimerUseCase(
    private val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val now: () -> Long = { listeningTimerEpoch.elapsedNow().inWholeMilliseconds }
) {
    data class SleepTimerState(
        val isEnabled: Boolean = false,
        val remainingTimeMs: Long = 0L,
        val totalTimeMs: Long = 0L,
        val isPaused: Boolean = false
    ) {
        val remainingMinutes get() = (remainingTimeMs / 60000).toInt()
        val remainingSeconds get() = ((remainingTimeMs % 60000) / 1000).toInt()
        val progress get() = if (totalTimeMs > 0) remainingTimeMs.toFloat() / totalTimeMs else 0f
        fun formatRemaining() = "${remainingMinutes}:${remainingSeconds.toString().padStart(2, '0')}"
    }
    private val mutableState = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = mutableState.asStateFlow()
    private var timerJob: Job? = null
    private var playbackJob: Job? = null
    private var boundPlayback: StateFlow<TTSState>? = null
    private var pausePlayback: () -> Unit = {}
    private var playing = false
    private var lastAccounted = 0L

    fun bind(playback: StateFlow<TTSState>, onExpired: () -> Unit) {
        if (boundPlayback === playback) return
        playbackJob?.cancel()
        boundPlayback = playback
        pausePlayback = onExpired
        playing = playback.value.isPlaying
        lastAccounted = now()
        playbackJob = sessionScope.launch {
            playback.map { it.isPlaying }.distinctUntilChanged().collect { active ->
                accountTime()
                playing = active
                lastAccounted = now()
                mutableState.value = mutableState.value.copy(isPaused = mutableState.value.isEnabled && !playing)
            }
        }
    }
    @Suppress("UNUSED_PARAMETER")
    fun initialize(controller: TTSController, screenScope: CoroutineScope) {
        bind(controller.state) { controller.dispatch(TTSCommand.Pause) }
    }
    fun start(minutes: Int) {
        if (minutes <= 0) return
        timerJob?.cancel()
        val duration = minutes.coerceAtMost(120) * 60_000L
        lastAccounted = now()
        mutableState.value = SleepTimerState(true, duration, duration, !playing)
        timerJob = sessionScope.launch {
            while (isActive && mutableState.value.isEnabled) {
                delay(1000)
                accountTime()
            }
        }
    }
    private fun accountTime() {
        val current = now()
        val elapsed = (current - lastAccounted).coerceAtLeast(0)
        lastAccounted = current
        val old = mutableState.value
        if (!old.isEnabled || !playing) return
        val remaining = (old.remainingTimeMs - elapsed).coerceAtLeast(0)
        if (remaining == 0L) {
            mutableState.value = SleepTimerState()
            timerJob?.cancel()
            pausePlayback()
        } else mutableState.value = old.copy(remainingTimeMs = remaining, isPaused = false)
    }
    fun addTime(minutes: Int) {
        if (minutes <= 0) return
        if (!isActive()) { start(minutes); return }
        accountTime()
        if (!isActive()) { start(minutes); return }
        val extra = minutes * 60_000L
        mutableState.value = mutableState.value.copy(
            remainingTimeMs = mutableState.value.remainingTimeMs + extra,
            totalTimeMs = mutableState.value.totalTimeMs + extra)
    }
    fun cancel() {
        timerJob?.cancel(); timerJob = null
        mutableState.value = SleepTimerState()
    }
    fun isActive() = mutableState.value.isEnabled
    fun getRemainingTimeMs() = mutableState.value.remainingTimeMs
    /** Application/session teardown only; never call when a reader screen closes. */
    fun cleanup() {
        cancel(); playbackJob?.cancel(); playbackJob = null; boundPlayback = null
    }
}
