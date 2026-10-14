package ireader.presentation.ui.truyenplus

import ireader.data.editing.CloudChapterEditingApi
import ireader.domain.models.editing.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ChapterEditingHttpTest {
    @Test fun connectionCheckUsesMetadataAndNeverGeneratesOrLeaksProviderErrors()=runBlocking {
        for(provider in EditingProvider.entries) {
            val config=EditingConfig(provider)
            var requests=0
            val client=HttpClient(MockEngine {request->
                requests++;assertEquals(HttpMethod.Get,request.method)
                if(provider==EditingProvider.Gemini) {
                    assertEquals("/v1beta/models/${config.model}",request.url.encodedPath)
                    assertEquals("secret",request.headers["x-goog-api-key"])
                    respond("""{"name":"models/${config.model}","supportedGenerationMethods":["generateContent"]}""",HttpStatusCode.OK)
                } else {
                    assertEquals("/models",request.url.encodedPath)
                    assertEquals("Bearer secret",request.headers[HttpHeaders.Authorization])
                    respond("""{"data":[{"id":"${config.model}"}]}""",HttpStatusCode.OK)
                }
            })
            try {CloudChapterEditingApi(client).checkConnection(EditingCredentials(config,"secret"));assertEquals(1,requests)}finally {client.close()}
        }
        val failed=HttpClient(MockEngine {respond("secret raw failure",HttpStatusCode.Forbidden)})
        try {val error=assertFails {CloudChapterEditingApi(failed).checkConnection(EditingCredentials(EditingConfig(),"secret"))};assertFalse(error.message.orEmpty().contains("secret"))}finally {failed.close()}
    }
    @Test fun geminiSendsCharacterMemoryAndUsesKeyHeaderWithoutQueryOrLogging()=runBlocking {
        val client=HttpClient(MockEngine { request ->
            assertEquals("generativelanguage.googleapis.com",request.url.host)
            assertEquals("secret",request.headers["x-goog-api-key"])
            assertFalse(request.url.toString().contains("secret"))
            val payload=Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertTrue(payload.toString().contains("Hoàng Yên Trần"))
            assertTrue(payload.toString().contains("Bí danh"))
            respond("""{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\"paragraphs\":[{\"id\":0,\"text\":\"Câu đã sửa.\"}]}"}]}}]}""",HttpStatusCode.OK)
        })
        try {
            val result=CloudChapterEditingApi(client).edit(EditingCredentials(EditingConfig(),"secret"),EditingMode.Grammar,listOf(EditParagraph(0,"Câu gốc.")),listOf(EditingCharacter("Hoàng Yên Trần","Bí danh","Nữ")))
            assertEquals("Câu đã sửa.",result.single().text)
        } finally{client.close()}
    }
    @Test fun deepseekRejectsTruncatedOutputAndDoesNotRetryOrLeakServerError()=runBlocking {
        var requests=0
        val client=HttpClient(MockEngine { req ->
            requests++;assertEquals("api.deepseek.com",req.url.host)
            assertEquals("Bearer secret",req.headers[HttpHeaders.Authorization])
            val payload=Json.parseToJsonElement((req.body as TextContent).text).jsonObject
            assertEquals("disabled",payload["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content)
            respond("""{"choices":[{"finish_reason":"length","message":{"content":"{\"paragraphs\":[]}"}}]}""",HttpStatusCode.OK)
        })
        try {assertFails {CloudChapterEditingApi(client).edit(EditingCredentials(EditingConfig(EditingProvider.DeepSeek),"secret"),EditingMode.Translate,listOf(EditParagraph(0,"原文")),emptyList())};assertEquals(1,requests)}finally{client.close()}
        val failure=HttpClient(MockEngine { respond("secret raw provider error",HttpStatusCode.TooManyRequests) })
        try {val e=assertFails{CloudChapterEditingApi(failure).edit(EditingCredentials(EditingConfig(),"secret"),EditingMode.Grammar,listOf(EditParagraph(0,"Câu.")),emptyList())};assertFalse(e.message.orEmpty().contains("secret"))}finally{failure.close()}
    }
    @Test fun cancellingStopsAnInflightRequest()=runBlocking {
        val started=CompletableDeferred<Unit>()
        val client=HttpClient(MockEngine { started.complete(Unit);awaitCancellation() })
        try {
            val job=launch{CloudChapterEditingApi(client).edit(EditingCredentials(EditingConfig(),"secret"),EditingMode.Grammar,listOf(EditParagraph(0,"Câu.")),emptyList())}
            started.await();job.cancelAndJoin();assertTrue(job.isCancelled)
        }finally{client.close()}
    }
}
