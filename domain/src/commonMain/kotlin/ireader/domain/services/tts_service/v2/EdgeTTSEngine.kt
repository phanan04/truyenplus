package ireader.domain.services.tts_service.v2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Online voices; no voice model or Python runtime is installed on the device. */
enum class EdgeVoice(val id:String) {
    HoaiMy("vi-VN-HoaiMyNeural"), NamMinh("vi-VN-NamMinhNeural");
    companion object { fun fromId(id:String)=entries.firstOrNull { it.id==id } ?: HoaiMy }
}
fun interface EdgeSpeechSynthesizer { suspend fun synthesize(text:String,voice:EdgeVoice):ByteArray }
interface EdgeAudioOutput {
    suspend fun play(bytes:ByteArray,onStarted:()->Unit,onCompleted:()->Unit,onError:()->Unit)
    fun pause();fun resume();fun stop();fun release()
    fun setSpeed(speed:Float);fun setPitch(pitch:Float)
}

/** One current request, one lookahead, and a bounded memory cache; all playback callbacks are versioned. */
class EdgeTTSEngine(private val voice:EdgeVoice,private val synth:EdgeSpeechSynthesizer,
    private val audio:EdgeAudioOutput,private val scope:CoroutineScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)):TTSEngine {
    private val eventFlow=MutableSharedFlow<EngineEvent>(extraBufferCapacity=16)
    override val events:Flow<EngineEvent> = eventFlow
    override val name="Edge TTS · ${voice.id}"
    private var request:Job?=null
    private var lookahead:Job?=null
    private var generation=0
    private var last:Pair<String,String>?=null
    private var paused=false
    private var started=false
    private var released=false
    private val cache=LinkedHashMap<String,ByteArray>()
    private var cacheBytes=0
    private fun cacheAudio(text:String,bytes:ByteArray) {
        if(bytes.size>2*1024*1024) return
        cache.remove(text)?.let { cacheBytes-=it.size }
        cache[text]=bytes;cacheBytes+=bytes.size
        while(cache.size>3 || cacheBytes>2*1024*1024) {
            val key=cache.keys.first();cacheBytes-=cache.remove(key)!!.size
        }
    }
    override suspend fun speak(text:String,utteranceId:String) {
        if(released) return
        request?.cancel();lookahead?.cancel();audio.stop()
        val token=++generation
        last=text to utteranceId;paused=false;started=false
        request=scope.launch {
            try {
                val bytes=cache[text] ?: synth.synthesize(text,voice).also { require(it.isNotEmpty());cacheAudio(text,it) }
                ensureActive()
                if(token!=generation || paused || released) return@launch
                audio.play(bytes,onStarted={
                    if(token==generation && !paused && !released) { started=true;eventFlow.tryEmit(EngineEvent.Started(utteranceId)) }
                },onCompleted={
                    if(token==generation && !paused && !released) { started=false;eventFlow.tryEmit(EngineEvent.Completed(utteranceId)) }
                },onError={
                    if(token==generation && !released) {
                        started=false
                        if(!paused) eventFlow.tryEmit(EngineEvent.Error(utteranceId,"Edge audio playback failed"))
                    }
                })
            } catch(e:Exception) {
                // A transport timeout cancels its own child scope, not this playback request.
                // Explicit pause/stop cancels this request and must stay silent.
                ensureActive()
                if(token==generation && !paused && !released)
                    eventFlow.tryEmit(EngineEvent.Error(utteranceId,"Edge TTS unavailable"))
            }
        }
    }
    override fun pause() {
        paused=true
        if(!started) { request?.cancel();lookahead?.cancel();generation++;audio.stop() } else audio.pause()
    }
    override fun resume() {
        if(released || !paused) return
        paused=false
        if(started) { audio.resume(); if(started) last?.let { eventFlow.tryEmit(EngineEvent.Started(it.second)) } } else last?.let { (text,id)->scope.launch { speak(text,id) } }
    }
    override fun stop() {
        generation++;request?.cancel();lookahead?.cancel();last=null;paused=false;started=false;audio.stop()
    }
    override fun release() { stop();released=true;cache.clear();cacheBytes=0;audio.release();scope.cancel() }
    override fun setSpeed(speed:Float)=audio.setSpeed(speed.coerceIn(.5f,3f))
    override fun setPitch(pitch:Float)=audio.setPitch(pitch.coerceIn(.5f,2f))
    override fun isReady()=!released
    override fun clearState() { stop();cache.clear();cacheBytes=0 }
    override fun precacheNext(items:List<Pair<String,String>>) {
        if(released || paused || lookahead?.isActive==true) return
        val text=items.firstOrNull()?.second ?: return
        if(text in cache) return
        val token=generation
        lookahead=scope.launch {
            try { val bytes=synth.synthesize(text,voice);ensureActive();if(token==generation && !released && bytes.isNotEmpty()) cacheAudio(text,bytes) }
            catch(e:CancellationException) { throw e } catch(_:Exception) { /* Current speech owns fallback; lookahead is optional. */ }
        }
    }
}
