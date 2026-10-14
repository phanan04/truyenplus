package ireader.domain.services.tts_service.v2.offline

import kotlinx.coroutines.*
import kotlin.concurrent.atomics.*

/** One upcoming sentence. Playback consumes and awaits the same work even if synthesis is unfinished. */
@OptIn(ExperimentalAtomicApi::class)
class VoiceAudioBuffer<A>(private val scope:CoroutineScope,private val generate:suspend (String,Float)->A) {
    private data class Pending<A>(val text:String,val rate:Float,val audio:Deferred<A>)
    private val pending=AtomicReference<Pending<A>?>(null)
    fun prefetch(text:String,rate:Float) {
        while(true) {
            val old=pending.load()
            if(old?.text==text && old.rate==rate && !old.audio.isCancelled)return
            val next=Pending(text,rate,scope.async(start=CoroutineStart.LAZY){generate(text,rate)})
            if(pending.compareAndSet(old,next)) {
                old?.audio?.cancel();next.audio.start();return
            }
            next.audio.cancel()
        }
    }
    suspend fun takeOrGenerate(text:String,rate:Float):A {
        val next=pending.exchange(null)
        return if(next?.text==text && next.rate==rate && !next.audio.isCancelled) {
            try {next.audio.await()} catch(e:CancellationException){next.audio.cancel();throw e}
        }
        else {next?.audio?.cancel();generate(text,rate)}
    }
    fun clear() {pending.exchange(null)?.audio?.cancel()}
}
