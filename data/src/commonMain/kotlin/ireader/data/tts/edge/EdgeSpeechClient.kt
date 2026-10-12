package ireader.data.tts.edge

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.util.date.GMTDate
import io.ktor.websocket.*
import ireader.domain.services.tts_service.v2.*
import ireader.domain.utils.extensions.currentTimeToLong
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import kotlin.random.Random

class EdgeSpeechClient(private val client:HttpClient):EdgeSpeechSynthesizer {
    private val connection=Mutex()
    private fun id()=buildString { repeat(32) { append("0123456789abcdef"[Random.nextInt(16)]) } }
    private fun timestamp():String {
        val t=GMTDate(currentTimeToLong())
        val day=t.dayOfWeek.toString().take(3).lowercase().replaceFirstChar { it.uppercase() }
        val month=t.month.toString().take(3).lowercase().replaceFirstChar { it.uppercase() }
        fun n(i:Int)=i.toString().padStart(2,'0')
        return "$day $month ${n(t.dayOfMonth)} ${t.year} ${n(t.hours)}:${n(t.minutes)}:${n(t.seconds)} GMT+0000 (Coordinated Universal Time)"
    }
    override suspend fun synthesize(text:String,voice:EdgeVoice):ByteArray=connection.withLock {
        require(text.encodeToByteArray().size<=65536) { "Edge paragraph too large" }
        val output=Buffer()
        for(chunk in EdgeSpeechWire.textChunks(text)) {
            if(chunk.isBlank()) continue
            val bytes=try { request(chunk,voice) } catch(e:CancellationException) { throw e } catch(_:Exception) {
                delay(350);request(chunk,voice)
            }
            require(output.size+bytes.size<=4*1024*1024) { "Edge audio too large" }
            output.write(bytes)
        }
        output.readByteArray().also { require(it.isNotEmpty()) { "Edge returned no audio" } }
    }
    private suspend fun request(text:String,voice:EdgeVoice):ByteArray=withTimeout(25000) {
        val output=Buffer();var completed=false
        val url="wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=${EdgeSpeechWire.clientToken}&ConnectionId=${id()}&Sec-MS-GEC=${EdgeSpeechWire.token(currentTimeToLong())}&Sec-MS-GEC-Version=1-${EdgeSpeechWire.browserVersion}"
        client.webSocket(request={
            url(url)
            header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            header("Origin","chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            header("Cache-Control","no-cache")
            header("Pragma","no-cache")
            header("Accept-Language","en-US,en;q=0.9")
            header("Cookie","muid=${id().uppercase()}")
        }) {
            send(Frame.Text("X-Timestamp:${timestamp()}\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"true\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n"))
            send(Frame.Text("X-RequestId:${id()}\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:${timestamp()}Z\r\nPath:ssml\r\n\r\n${EdgeSpeechWire.ssml(text,voice)}"))
            for(frame in incoming) when(frame) {
                is Frame.Binary->EdgeSpeechWire.audioPayload(frame.readBytes())?.let {
                    require(output.size+it.size<=2*1024*1024) { "Edge frame limit exceeded" };output.write(it)
                }
                is Frame.Text->if(frame.readText().substringBefore("\r\n\r\n").lineSequence().any {it.trim()=="Path:turn.end"}) { completed=true;break }
                else->Unit
            }
        }
        require(completed && output.size>0) { "Edge returned no audio" }
        output.readByteArray()
    }
}
