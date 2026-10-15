package ireader.presentation.ui.truyenplus

import ireader.data.tts.capcut.CapCutHttpTransport
import ireader.domain.services.tts_service.v2.online.CapCutSpeechFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CapCutHttpTransportTest {
    @Test fun providerRateLimitBlocksQueuedSubmissionsUntilRetryAfterExpires() = runTest {
        var now=0L;var requests=0
        val http=HttpClient(MockEngine {
            requests++
            if(requests==1)respond("busy",HttpStatusCode.TooManyRequests,headersOf(HttpHeaders.RetryAfter,"10")) else respond("{}")
        })
        try {
            val transport=CapCutHttpTransport(http,nowMillis={now})
            assertEquals(429,assertFailsWith<CapCutSpeechFailure> {transport.post("https://example.test/query",emptyMap(),"{}",query=true)}.code)
            assertEquals(1,requests,"The throttled query must not be resubmitted")
            assertEquals(429,assertFailsWith<CapCutSpeechFailure> {transport.post("https://example.test/new",emptyMap(),"{}",query=false)}.code)
            assertEquals(1,requests,"Queued jobs must not keep hitting a rate-limited provider")
            now=10_001
            assertEquals("{}",transport.post("https://example.test/new",emptyMap(),"{}",query=false))
            assertEquals(2,requests)
        } finally {http.close()}
    }
    @Test fun signedHeadersAndUtf8BodyReachTheNetworkUnmodified() = runTest {
        val body="{\"text\":\"Nàng khẽ gật đầu.\"}"
        val http=HttpClient(MockEngine {request->
            assertEquals("signed",request.headers["sign"])
            assertEquals("no-store",request.headers[HttpHeaders.CacheControl])
            assertEquals(body,(request.body as io.ktor.http.content.TextContent).text)
            respond("{}")
        })
        try {assertEquals("{}",CapCutHttpTransport(http).post("https://example.test/new",mapOf("sign" to "signed"),body,query=false))}
        finally {http.close()}
    }
    @Test fun transientPollRetriesOnceButTaskSubmissionIsNeverDuplicated() = runTest {
        var requests=0
        val http=HttpClient(MockEngine {
            requests++
            respond("{}",if(requests==1)HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK)
        })
        try {
            val transport=CapCutHttpTransport(http)
            assertEquals("{}",transport.post("https://example.test/query",emptyMap(),"{}",query=true))
            assertEquals(2,requests)
            requests=0
            val failure=assertFailsWith<CapCutSpeechFailure> {transport.post("https://example.test/new",emptyMap(),"private story",query=false)}
            assertEquals(1,requests)
            assertFalse(failure.message.orEmpty().contains("private story"))
            assertEquals(503,failure.code)
        } finally {http.close()}
    }
    @Test fun htmlInsteadOfMp3AndOversizedAudioAreRejectedBeforeThePlayer() = runTest {
        val http=HttpClient(MockEngine {request ->
            if(request.url.encodedPath=="/large")respond("",headers=headersOf(HttpHeaders.ContentLength,"999999999"))
            else respond("<html>server error</html>".repeat(10))
        })
        try {
            val transport=CapCutHttpTransport(http)
            for(path in listOf("html","large")) {
                val error=assertFailsWith<CapCutSpeechFailure> {transport.download("https://example.test/$path")}
                assertFalse(error.message.orEmpty().contains("https://"))
            }
        } finally {http.close()}
    }
    @Test fun cancellationNeverRetriesOrTurnsIntoANetworkError() = runBlocking {
        var requests=0
        val entered=CompletableDeferred<Unit>()
        val http=HttpClient(MockEngine {requests++;entered.complete(Unit);delay(10_000);respond("{}")})
        try {
            val request=async {CapCutHttpTransport(http).post("https://example.test/query",emptyMap(),"{}",query=true)}
            withTimeout(5_000) {entered.await()}
            request.cancel()
            assertFailsWith<CancellationException> {request.await()}
            assertEquals(1,requests)
        } finally {http.close()}
    }
}
