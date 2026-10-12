package ireader.presentation.ui.truyenplus

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import ireader.data.tts.edge.EdgeSpeechClient
import ireader.domain.services.tts_service.v2.EdgeVoice
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Explicit, opt-in service check; regular regression tests require no network. */
class EdgeLiveProbeTest {
    @Test fun bothVietnameseVoicesReturnRealMp3() = runBlocking {
        assumeTrue(System.getenv("EDGE_TTS_LIVE") == "1")
        val http = HttpClient(CIO) { install(WebSockets) }
        try {
            val client = EdgeSpeechClient(http)
            for (voice in EdgeVoice.entries) {
                val bytes = client.synthesize("Xin chào bạn.", voice)
                assertTrue(bytes.size > 1000, "${voice.id} must return audio")
                assertTrue(bytes[0].toInt() and 255 == 255 || bytes.take(3).toByteArray().decodeToString() == "ID3", "Expected MP3")
                val file = File("../.opencode/edge-reference/kotlin-${voice.id}.mp3")
                file.parentFile.mkdirs(); file.writeBytes(bytes)
                println("EDGE_REAL_AUDIO ${voice.id}: ${bytes.size} bytes")
            }
        } finally { http.close() }
    }
}
