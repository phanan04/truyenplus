package ireader.domain.services.tts_service.v2

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** Online voices; no voice model or Python runtime is installed on the device. */
enum class EdgeVoice(val id: String) {
    HoaiMy("vi-VN-HoaiMyNeural"), NamMinh("vi-VN-NamMinhNeural");
    companion object { fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: HoaiMy }
}
fun interface EdgeSpeechSynthesizer {
    suspend fun synthesize(text: String, voice: EdgeVoice): ByteArray
    fun stream(text: String, voice: EdgeVoice): Flow<ByteArray> = flow { emit(synthesize(text, voice)) }
}
interface EdgeAudioOutput {
    suspend fun play(bytes: ByteArray, onStarted: () -> Unit, onCompleted: () -> Unit, onError: () -> Unit)
    fun pause(); fun resume(); fun stop(); fun release()
    fun setSpeed(speed: Float); fun setPitch(pitch: Float)
}

/** Two buffered segments per producer; transfers lookahead to playback instead of downloading twice. */
class EdgeTTSEngine(private val voice: EdgeVoice, private val synth: EdgeSpeechSynthesizer,
    private val audio: EdgeAudioOutput,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) : TTSEngine {
    private val eventFlow = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 16)
    override val events: Flow<EngineEvent> = eventFlow
    override val name = "Edge TTS · ${voice.id}"
    private class Source(val text: String, val chunks: Channel<Result<ByteArray>>, val job: Job) {
        fun cancel() { job.cancel(); chunks.cancel() }
    }
    private var request: Job? = null
    private var current: Source? = null
    private var lookahead: Source? = null
    private var generation = 0
    private var last: Pair<String, String>? = null
    private val paused = MutableStateFlow(false)
    private var started = false
    private var released = false
    private val cache = LinkedHashMap<String, List<ByteArray>>()
    private var cacheBytes = 0
    private fun cacheAudio(text: String, chunks: List<ByteArray>) {
        val size = chunks.sumOf { it.size }
        if (size > 2 * 1024 * 1024) return
        cache.remove(text)?.let { cacheBytes -= it.sumOf { bytes -> bytes.size } }
        cache[text] = chunks; cacheBytes += size
        while (cache.size > 3 || cacheBytes > 2 * 1024 * 1024) {
            cacheBytes -= cache.remove(cache.keys.first())!!.sumOf { it.size }
        }
    }
    private fun source(text: String): Source {
        val chunks = Channel<Result<ByteArray>>(2)
        val job = scope.launch {
            try {
                val cached = cache[text]
                if (cached != null) for (bytes in cached) chunks.send(Result.success(bytes))
                else {
                    val collected = mutableListOf<ByteArray>(); var bytesInCache = 0; var cacheable = true
                    synth.stream(text, voice).collect { bytes ->
                        require(bytes.isNotEmpty()) { "Empty Edge audio segment" }
                        ensureActive()
                        if (cacheable) {
                            bytesInCache += bytes.size
                            if (bytesInCache <= 2 * 1024 * 1024) collected += bytes
                            else { collected.clear(); cacheable = false }
                        }
                        chunks.send(Result.success(bytes))
                    }
                    ensureActive()
                    if (cacheable && collected.isNotEmpty() && !released) cacheAudio(text, collected.toList())
                }
            } catch (error: Exception) {
                // Transport timeout cancels its child scope; user cancellation cancels this producer.
                ensureActive()
                chunks.send(Result.failure(error))
            } finally { chunks.close() }
        }
        return Source(text, chunks, job)
    }
    override suspend fun speak(text: String, utteranceId: String) {
        if (released) return
        request?.cancel(); current?.cancel(); audio.stop()
        val token = ++generation
        last = text to utteranceId; paused.value = false; started = false
        val prepared = lookahead?.takeIf { it.text == text }
        if (prepared == null) lookahead?.cancel()
        lookahead = null
        val stream = prepared ?: source(text)
        current = stream
        request = scope.launch {
            try {
                var segments = 0
                for (result in stream.chunks) {
                    val bytes = result.getOrThrow()
                    paused.first { !it }
                    ensureActive()
                    if (token != generation || released) return@launch
                    val completed = CompletableDeferred<Unit>()
                    audio.play(bytes, onStarted = {
                        if (token == generation && !paused.value && !released && !started) {
                            started = true; eventFlow.tryEmit(EngineEvent.Started(utteranceId))
                        }
                    }, onCompleted = { completed.complete(Unit) },
                        onError = { completed.completeExceptionally(IllegalStateException("Edge audio playback failed")) })
                    completed.await()
                    segments++
                }
                require(segments > 0) { "Edge returned no audio" }
                paused.first { !it }
                ensureActive()
                if (token == generation && !released) {
                    started = false; eventFlow.tryEmit(EngineEvent.Completed(utteranceId))
                }
            } catch (error: Exception) {
                ensureActive()
                paused.first { !it }
                if (token == generation && !released) {
                    started = false; eventFlow.tryEmit(EngineEvent.Error(utteranceId, "Edge TTS unavailable"))
                }
            } finally { stream.cancel() }
        }
    }
    override fun pause() {
        paused.value = true
        if (!started) { request?.cancel(); current?.cancel(); generation++; audio.stop() }
        else audio.pause()
    }
    override fun resume() {
        if (released || !paused.value) return
        paused.value = false
        if (started) {
            audio.resume(); last?.let { eventFlow.tryEmit(EngineEvent.Started(it.second)) }
        } else last?.let { (text, id) -> scope.launch { speak(text, id) } }
    }
    override fun stop() {
        generation++; request?.cancel(); current?.cancel(); lookahead?.cancel()
        current = null; lookahead = null; last = null; paused.value = false; started = false; audio.stop()
    }
    override fun release() { stop(); released = true; cache.clear(); cacheBytes = 0; audio.release(); scope.cancel() }
    override fun setSpeed(speed: Float) = audio.setSpeed(speed.coerceIn(.5f, 3f))
    override fun setPitch(pitch: Float) = audio.setPitch(pitch.coerceIn(.5f, 2f))
    override fun isReady() = !released
    override fun clearState() { stop(); cache.clear(); cacheBytes = 0 }
    override fun precacheNext(items: List<Pair<String, String>>) {
        if (released || paused.value || lookahead != null) return
        val text = items.firstOrNull()?.second ?: return
        if (text !in cache) lookahead = source(text)
    }
}
