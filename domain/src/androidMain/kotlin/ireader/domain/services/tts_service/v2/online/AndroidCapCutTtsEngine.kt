package ireader.domain.services.tts_service.v2.online

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Bounded online lookahead/cache, independent of text highlighting and native/offline engines. */
class AndroidCapCutTtsEngine(context:Context,private val source:CapCutSpeechSource,private val voice:CapCutVoice):TTSEngine {
 private val directory=File(context.cacheDir,"capcut-speech").apply {mkdirs()}
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
 private val mutableEvents=ttsEngineEventBuffer(replay=1)
 override val events:Flow<EngineEvent> = mutableEvents
 override val name="CapCut · ${voice.title}"
 private val buffer=CapCutAudioBuffer(scope) {text->source.synthesize(text,voice)}
 @Volatile private var followingChunks:List<String> = emptyList()
 @Volatile private var currentAudioReady=false
 private val generation=AtomicLong()
 @Volatile private var completed=false
 @Volatile private var player:MediaPlayer?=null
 @Volatile private var speed=1f
 @Volatile private var pitch=1f
 private var job:Job?=null
 init {scope.launch {mutableEvents.emit(EngineEvent.Ready)}}
 override fun isReady()=true
 override suspend fun speak(text:String,utteranceId:String) {
  cancel(completed);val token=generation.get()
  val chunks=TTSSentenceSplitter.split(text,maxChunkLength=320,speechRate=1f)
  val ranges=speechChunkRanges(text,chunks)
  job=scope.launch {
   try {
    if(ranges.isEmpty()){completed=true;mutableEvents.emit(EngineEvent.Completed(utteranceId));return@launch}
    for((index,chunk) in chunks.withIndex()) {
     ensureActive();if(token!=generation.get())return@launch
     val bytes=buffer.take(chunk)
     ensureActive();if(token!=generation.get())return@launch
     currentAudioReady=true
     buffer.prefetch(chunks.drop(index+1).take(3)+followingChunks)
     val file=File.createTempFile("sentence-",".mp3",directory)
     try {
      file.writeBytes(bytes)
      withContext(Dispatchers.Main) {
       val p=MediaPlayer();player=p
       try {
        val prepared=CompletableDeferred<Unit>();val done=CompletableDeferred<Unit>()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        p.setOnPreparedListener {prepared.complete(Unit)}
        p.setOnCompletionListener {done.complete(Unit)}
        p.setOnErrorListener {_,what,extra->val error=IllegalStateException("CapCut audio playback failed ($what/$extra)");prepared.completeExceptionally(error);done.completeExceptionally(error);true}
        p.setDataSource(file.absolutePath);p.prepareAsync();withTimeout(10_000){prepared.await()}
        ensureActive();p.start();applyParams()
        withTimeout(3000){while(p.currentPosition==0 && !done.isCompleted){ensureActive();delay(10)}}
        if(index==0)mutableEvents.emit(EngineEvent.Started(utteranceId))
        ranges.getOrNull(index)?.let {mutableEvents.emit(EngineEvent.RangeStarted(utteranceId,it.start,it.end))}
        withTimeout(180_000){done.await()}
       } finally {if(player===p)player=null;runCatching {p.stop()};p.release()}
      }
     } finally {file.delete()}
    }
    if(token==generation.get()){completed=true;mutableEvents.emit(EngineEvent.Completed(utteranceId))}
   } catch(e:TimeoutCancellationException){if(token==generation.get())mutableEvents.emit(EngineEvent.Error(utteranceId,"CapCut request timed out",retryable=false))}
   catch(e:CancellationException){throw e}
   catch(e:Exception){if(token==generation.get())mutableEvents.emit(EngineEvent.Error(utteranceId,(e as? CapCutSpeechFailure)?.message ?: "CapCut network/audio unavailable",retryable=false))}
  }
 }
 private fun cancel(keepPrepared:Boolean) {
  generation.incrementAndGet();completed=false;currentAudioReady=false;job?.cancel();job=null
  if(!keepPrepared){buffer.cancelPending();followingChunks=emptyList()}
 }
 override fun precacheNext(items:List<Pair<String,String>>) {
  followingChunks=items.take(2).flatMap {TTSSentenceSplitter.split(it.second,320,1f)}.take(3)
  if(currentAudioReady)buffer.prefetch(followingChunks)
 }
 override fun stop()=cancel(false)
 override fun pause()=stop()
 override fun resume() {}
 private fun applyParams(){runCatching {player?.playbackParams=PlaybackParams().setSpeed(speed).setPitch(pitch)}}
 override fun setSpeed(speed:Float){this.speed=speed.coerceIn(.5f,2f);scope.launch(Dispatchers.Main){applyParams()}}
 override fun setPitch(pitch:Float){this.pitch=pitch.coerceIn(.5f,2f);scope.launch(Dispatchers.Main){applyParams()}}
 override fun release(){stop();buffer.clear();scope.cancel()}
}
