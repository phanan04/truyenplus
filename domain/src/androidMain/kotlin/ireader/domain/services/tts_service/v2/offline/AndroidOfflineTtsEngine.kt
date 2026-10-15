package ireader.domain.services.tts_service.v2.offline

import android.content.Context
import android.media.*
import com.k2fsa.sherpa.onnx.*
import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.coroutines.CoroutineContext

/** JNI looks up invoke(float[]): Integer; an indy/SAM lambda only exposes invoke(Object). */
private class NativeSpeechProgress(private val context: CoroutineContext) : (FloatArray) -> Int {
    override fun invoke(samples: FloatArray): Int = if (context.isActive) 1 else 0
}

/** One native model at a time, shared by playback and auditions. All inference is off the UI thread. */
private object OfflineRuntime {
    private val lock=Mutex()
    private var tts:OfflineTts?=null
    @Volatile private var nano:VieNeuNanoRuntime?=null
    private var loaded=""
    private val owners=mutableMapOf<Any,String>()
    private fun open(config:OfflineVoiceConfig) {
        val key=config.directory
        if((tts!=null || nano!=null) && loaded==key)return
        tts?.release();tts=null;nano?.close();nano=null;loaded=""
        val path=config.directory+"/"
        val model=OfflineTtsModelConfig(numThreads=2,debug=false,provider="cpu")
        when(config.model.family) {
            OfflineVoiceFamily.Piper -> model.vits=OfflineTtsVitsModelConfig(
                model=path+config.model.files.first(),tokens=path+"tokens.txt",dataDir=path+"espeak-ng-data")
            OfflineVoiceFamily.VieNeuNano -> {
                nano=VieNeuNanoRuntime(config.directory);loaded=key;return
            }
        }
        tts=OfflineTts(assetManager=null,config=OfflineTtsConfig(model=model,maxNumSentences=1))
        loaded=key
        check(config.speaker<tts!!.numSpeakers())
    }
    suspend fun warm(config:OfflineVoiceConfig,owner:Any)=lock.withLock {open(config);owners[owner]=config.directory}
    suspend fun synthesize(config:OfflineVoiceConfig,text:String,speed:Float):GeneratedAudio=lock.withLock {
        currentCoroutineContext().ensureActive();open(config)
        val coroutine=currentCoroutineContext()
        nano?.let {runtime->
            val samples=runtime.generate(text,config.speaker,speed)
            coroutine.ensureActive()
            check(samples.isNotEmpty() && samples.all {it.isFinite()} && samples.any {abs(it)>.00001f})
            return@withLock GeneratedAudio(samples=samples,sampleRate=24000)
        }
        val generation=GenerationConfig(sid=config.speaker,speed=speed.coerceIn(.5f,2f),numSteps=3,extra=mapOf("lang" to "vi"))
        val audio=tts!!.generateWithConfigAndCallback(text,generation,NativeSpeechProgress(coroutine))
        coroutine.ensureActive()
        check(audio.sampleRate>0 && audio.samples.isNotEmpty() && audio.samples.any {abs(it)>.00001f})
        audio
    }
    fun abortNano(){nano?.abort()}
    suspend fun free(config:OfflineVoiceConfig,owner:Any)=lock.withLock {
        owners.remove(owner)
        if(loaded==config.directory && loaded !in owners.values) {tts?.release();tts=null;nano?.close();nano=null;loaded=""}
    }
}

/** Synthesizes bounded sentences, streams at the model's own rate, and reports real playback starts. */
class AndroidOfflineTtsEngine(context:Context,private val config:OfflineVoiceConfig):TTSEngine {
    private val appContext=context.applicationContext
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private val owner=Any()
    private val generation=AtomicLong()
    private val mutableEvents=ttsEngineEventBuffer(replay=1)
    override val events:Flow<EngineEvent> = mutableEvents
    override val name get()=fallback?.name ?: config.model.title+if(config.model.speakers>1)" · "+(config.model.voiceNames.getOrNull(config.speaker) ?: (config.speaker+1).toString()) else ""
    @Volatile private var ready=false
    @Volatile private var speed=1f
    @Volatile private var pitch=1f
    @Volatile private var synthSpeed=1f
    @Volatile private var track:AudioTrack?=null
    private var speechJob:Job?=null
    private var fallback:TTSEngine?=null
    private var fallbackEvents:Job?=null
    private var pendingFallback:Pair<String,String>?=null
    @Volatile private var completedNaturally=false
    private val audioBuffer=VoiceAudioLookahead(scope) {text,rate -> OfflineRuntime.synthesize(config,text,rate)}

    init {scope.launch {
        try {OfflineRuntime.warm(config,owner);ready=true;mutableEvents.emit(EngineEvent.Ready)}
        catch(e:CancellationException){throw e}
        catch(e:Throwable){activateFallback(null)}
    }}
    private fun activateFallback(pending:Pair<String,String>?) {
        pendingFallback=pending
        if(fallback!=null) {
            pending?.let {scope.launch {fallback?.speak(it.first,it.second)}}
            return
        }
        val native=AndroidNativeTTSEngineV2(appContext)
        fallback=native;native.setSpeed(speed);native.setPitch(pitch)
        audioBuffer.begin(keepPrepared=false)
        scope.launch {OfflineRuntime.free(config,owner)}
        fallbackEvents=scope.launch {
            native.events.collect {event ->
                if(event is EngineEvent.Ready) {
                    ready=true;mutableEvents.emit(event)
                    pendingFallback?.let {pendingFallback=null;native.speak(it.first,it.second)}
                } else mutableEvents.emit(event)
            }
        }
    }
    override suspend fun speak(text:String,utteranceId:String) {
        cancelCurrent(keepPrepared=completedNaturally)
        fallback?.let {it.speak(text,utteranceId);return}
        val token=generation.get()
        speechJob=scope.launch {
            try {
                val chunks=TTSSentenceSplitter.split(text,maxChunkLength=if(config.model.family==OfflineVoiceFamily.VieNeuNano)140 else 220,speechRate=speed)
                val ranges=speechChunkRanges(text,chunks)
                if(chunks.isEmpty()){mutableEvents.emit(EngineEvent.Completed(utteranceId));return@launch}
                for((index,chunk) in chunks.withIndex()) {
                    ensureActive();if(token!=generation.get())return@launch
                    val rate=speed
                    val audio=audioBuffer.takeOrGenerate(chunk,rate)
                    if(token!=generation.get())return@launch
                    audioBuffer.currentAudioReady(chunks.getOrNull(index+1),rate)
                    val pcm=normalizedVoicePcm(audio.samples)
                    val minimum=AudioTrack.getMinBufferSize(audio.sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)
                    check(minimum>0)
                    val player=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(audio.sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(maxOf(minimum,pcm.size*2)).setTransferMode(AudioTrack.MODE_STATIC).build()
                    track=player;synthSpeed=rate
                    try {
                        ensureActive();check(player.write(pcm,0,pcm.size)==pcm.size)
                        check(player.state==AudioTrack.STATE_INITIALIZED)
                        player.play();applyPlaybackParams()
                        withTimeout(2000){while(player.playbackHeadPosition==0){ensureActive();delay(10)}}
                        if(index==0)mutableEvents.emit(EngineEvent.Started(utteranceId))
                        ranges.getOrNull(index)?.let {mutableEvents.emit(EngineEvent.RangeStarted(utteranceId,it.start,it.end))}
                        while(player.playbackHeadPosition<pcm.size) {ensureActive();delay(20)}
                    } finally {
                        if(track===player)track=null
                        runCatching {player.stop()};player.release()
                    }
                }
                if(token==generation.get()) {
                    completedNaturally=true
                    mutableEvents.emit(EngineEvent.Completed(utteranceId))
                }
            } catch(e:TimeoutCancellationException){if(token==generation.get())mutableEvents.emit(EngineEvent.Error(utteranceId,"Offline audio playback timed out"))}
            catch(e:CancellationException){throw e}
            catch(e:Throwable) {if(token==generation.get())activateFallback(text to utteranceId)}
        }
    }
    override fun precacheNext(items:List<Pair<String,String>>) {
        if(fallback!=null)return
        // At most one sentence; never pre-synthesize a whole long chapter or ten paragraphs.
        val next=items.firstOrNull()?.second?.let {TTSSentenceSplitter.split(it,maxChunkLength=if(config.model.family==OfflineVoiceFamily.VieNeuNano)140 else 220,speechRate=speed).firstOrNull()}
        audioBuffer.announceNextParagraph(next)
    }
    private fun cancelCurrent(keepPrepared:Boolean) {
        generation.incrementAndGet();pendingFallback=null
        if(!keepPrepared && config.model.family==OfflineVoiceFamily.VieNeuNano)OfflineRuntime.abortNano()
        completedNaturally=false
        speechJob?.cancel();speechJob=null;audioBuffer.begin(keepPrepared)
        runCatching {track?.pause();track?.flush()}
        fallback?.stop()
    }
    override fun stop()=cancelCurrent(keepPrepared=false)
    override fun pause()=stop()
    override fun resume() { /* Controller replays the retained paragraph, as with system TTS. */ }
    private fun applyPlaybackParams() {runCatching {track?.playbackParams=PlaybackParams().setSpeed((speed/synthSpeed).coerceIn(.25f,4f)).setPitch(pitch)}}
    override fun setSpeed(speed:Float) {this.speed=speed.coerceIn(.5f,2f);applyPlaybackParams();fallback?.setSpeed(this.speed)}
    override fun setPitch(pitch:Float) {this.pitch=pitch.coerceIn(.5f,2f);applyPlaybackParams();fallback?.setPitch(this.pitch)}
    override fun isReady()=ready
    override fun release() {
        stop();fallbackEvents?.cancel();fallback?.release();scope.cancel()
        CoroutineScope(Dispatchers.Default).launch {OfflineRuntime.free(config,owner)}
    }
}
