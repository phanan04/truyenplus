package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.online.*
import kotlin.test.*

class CapCutProtocolTest {
 @Test fun liveSuccessSpellingAndStringPayloadReturnTheActualAudioUrl() {
  val body="""{"ret":"0","data":{"tasks":[{"status":"succeed","payload":"{\"audio_subtitles\":[{\"speech_url\":\"https://audio.example.com/a.mp3\"}]}"}]}}"""
  assertEquals(CapCutTaskResult.Audio("https://audio.example.com/a.mp3"),parseCapCutTask(body))
  assertEquals(CapCutTaskResult.Pending,parseCapCutTask("""{"ret":0,"data":{"tasks":[{"status":"processing"}]}}"""))
 }
 @Test fun failureOrNonHttpsAudioIsRejectedInsteadOfPollingForever() {
  assertFails {parseCapCutTask("""{"ret":100,"errmsg":"denied"}""")}
  assertFails {parseCapCutTask("""{"ret":0,"data":{"tasks":[{"status":"failed"}]}}""")}
  assertFails {parseCapCutTask("""{"ret":0,"data":{"tasks":[{"status":"success","payload":{"audio_subtitles":[{"speech_url":"http://example.com/a.mp3"}]}}]}}""")}
 }
}
