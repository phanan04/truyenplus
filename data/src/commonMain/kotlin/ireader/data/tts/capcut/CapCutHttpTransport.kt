package ireader.data.tts.capcut

import ireader.domain.services.tts_service.v2.online.CapCutSpeechFailure
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import okio.Buffer
import kotlin.time.TimeSource

private val onlineClock=TimeSource.Monotonic.markNow()

/** No story/session data is cached by HTTP. Only safe, idempotent operations may retry. */
class CapCutHttpTransport(private val http:HttpClient,private val nowMillis:()->Long={onlineClock.elapsedNow().inWholeMilliseconds}) {
    private val lock=Any()
    private var limitedUntil=0L
    suspend fun post(url:String,headers:Map<String,String>,body:String,query:Boolean):String =
        request(if(query)"query" else "submit",retry=query) {
            http.preparePost(url) {
                contentType(ContentType.Application.Json)
                headers.forEach {(key,value)->header(key,value)}
                header(HttpHeaders.CacheControl,"no-store")
                setBody(body)
            }.execute {response ->
                checkStatus(response,if(query)"query" else "submit")
                readBounded(response,1_000_000,"response").decodeToString()
            }
        }

    suspend fun download(url:String):ByteArray = request("download",retry=true) {
        http.prepareGet(url) {header(HttpHeaders.CacheControl,"no-store")}.execute {response ->
            checkStatus(response,"download")
            val bytes=readBounded(response,16*1024*1024,"audio")
            val id3=bytes.size>=3 && bytes[0]==73.toByte() && bytes[1]==68.toByte() && bytes[2]==51.toByte()
            val frame=bytes.size>=2 && bytes[0].toInt() and 255==255 && bytes[1].toInt() and 224==224
            if(bytes.size<=128 || (!id3 && !frame))throw CapCutSpeechFailure("invalid audio")
            bytes
        }
    }

    private fun checkStatus(response:HttpResponse,stage:String) {
        if(!response.status.isSuccess()) {
            val code=response.status.value
            if(code==429) {
                val seconds=response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.coerceIn(1,60) ?: 5L
                synchronized(lock) {limitedUntil=maxOf(limitedUntil,nowMillis()+seconds*1000)}
            }
            throw CapCutSpeechFailure(stage,code,code==408 || code in 500..599)
        }
    }
    private suspend fun readBounded(response:HttpResponse,max:Int,stage:String):ByteArray {
        if((response.contentLength() ?: 0)>max)throw CapCutSpeechFailure("$stage too large")
        val channel=response.bodyAsChannel();val output=Buffer();val bytes=ByteArray(8192)
        while(true) {
            currentCoroutineContext().ensureActive()
            val n=channel.readAvailable(bytes,0,bytes.size)
            if(n<0)break
            if(n>0) {
                if(output.size+n>max)throw CapCutSpeechFailure("$stage too large")
                output.write(bytes,0,n)
            }
        }
        return output.readByteArray()
    }
    private suspend fun <T> request(stage:String,retry:Boolean,block:suspend ()->T):T {
        var attempt=0
        while(true) {
            if(synchronized(lock) {nowMillis()<limitedUntil})throw CapCutSpeechFailure("rate limited",429)
            try {return block()}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                val failure=e as? CapCutSpeechFailure ?: CapCutSpeechFailure("$stage network",transient=true)
                if(!retry || !failure.transient || attempt++>=1)throw failure
                delay(300)
            }
        }
    }
}
