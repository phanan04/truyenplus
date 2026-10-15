package ireader.domain.services.tts_service.v2.online

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Bounded, voice-local online work. Failed speculation never fails the playback scope. */
class CapCutAudioBuffer(
    private val scope:CoroutineScope,
    private val maxBytes:Int=8*1024*1024,
    private val maxEntries:Int=24,
    private val generate:suspend (String)->ByteArray
) {
    private val lock=Any()
    private val permits=Semaphore(2)
    private val pending=linkedMapOf<String,Deferred<Result<ByteArray>>>()
    private val ready=linkedMapOf<String,ByteArray>()
    private var bytes=0
    init {require(maxBytes>0 && maxEntries>0)}

    fun prefetch(texts:List<String>) {
        texts.asSequence().filter {it.isNotBlank()}.distinct().take(3).forEach {text->
            synchronized(lock) {
                if(text !in ready && text !in pending && pending.size<3)request(text)
            }
        }
    }

    suspend fun take(text:String):ByteArray {
        val job=synchronized(lock) {
            ready.remove(text)?.let {ready[text]=it;return it}
            pending[text] ?: request(text)
        }
        return job.await().getOrThrow()
    }

    /** Must be called under lock: register lazy work before it can complete. */
    private fun request(text:String):Deferred<Result<ByteArray>> {
        lateinit var job:Deferred<Result<ByteArray>>
        job=scope.async(start=CoroutineStart.LAZY) {
            try {
                val audio=permits.withPermit {generate(text)}
                ensureActive()
                synchronized(lock) {
                    if(pending[text]===job) {
                        pending.remove(text)
                        if(audio.size<=maxBytes) {
                            ready.remove(text)?.let {bytes-=it.size}
                            ready[text]=audio;bytes+=audio.size
                            while(bytes>maxBytes || ready.size>maxEntries) {
                                val first=ready.keys.first();bytes-=ready.remove(first)!!.size
                            }
                        }
                    }
                }
                Result.success(audio)
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                Result.failure(e)
            } finally {
                // A provider timeout cancels its job too; never retain it as usable lookahead.
                synchronized(lock) {if(pending[text]===job)pending.remove(text)}
            }
        }
        pending[text]=job;job.start();return job
    }

    fun cancelPending()=synchronized(lock) {
        pending.values.forEach {it.cancel()};pending.clear()
    }
    fun clear()=synchronized(lock) {
        cancelPending();ready.clear();bytes=0
    }
}
