package ireader.domain.services.tts_service.v2.online

import kotlinx.serialization.json.*

data class CapCutVoice(val id:String,val title:String,val resource:String)
object CapCutVoices {
 val voices=listOf(CapCutVoice("BV421_vivn_streaming","Nhỏ Ngọt Ngào","7252594014782755330"),
  CapCutVoice("vi_female_huong","Nữ Phổ Thông","7264854897953083905"),
  CapCutVoice("BV075_streaming","Thanh Niên Tự Tin","7102355803792740865"))
 fun find(id:String)=voices.firstOrNull {it.id==id}
}
interface CapCutSpeechSource {suspend fun synthesize(text:String,voice:CapCutVoice):ByteArray}
/** Safe diagnostic: never contains story text, request headers, task tokens or signed URLs. */
class CapCutSpeechFailure(val stage:String,val code:Int?=null,val transient:Boolean=false):
 IllegalStateException("CapCut $stage"+(code?.let {" ($it)"} ?: ""))
sealed interface CapCutTaskResult {
 data object Pending:CapCutTaskResult
 data class Audio(val url:String):CapCutTaskResult
}
fun parseCapCutTask(body:String):CapCutTaskResult {
 val root=Json.parseToJsonElement(body).jsonObject
 if(root["ret"]?.jsonPrimitive?.content!="0")throw CapCutSpeechFailure("service",root["ret"]?.jsonPrimitive?.intOrNull)
 val task=root["data"]?.jsonObject?.get("tasks")?.jsonArray?.firstOrNull()?.jsonObject ?: error("CapCut returned no task")
 return when(task["status"]?.jsonPrimitive?.content) {
  "success","succeed"->{
   val raw=task["payload"] ?: error("CapCut audio missing")
   val payload=if(raw is JsonPrimitive)Json.parseToJsonElement(raw.content).jsonObject else raw.jsonObject
   val audio=payload["audio_subtitles"]?.jsonArray?.firstOrNull()?.jsonObject ?: error("CapCut audio missing")
   val code=audio["code"]?.jsonPrimitive?.intOrNull
   if((code!=null && code!=0) || audio["invalid_input"]?.jsonPrimitive?.booleanOrNull==true)
    throw CapCutSpeechFailure("speech rejected",code)
   val url=audio["speech_url"]?.jsonPrimitive?.content.orEmpty()
   require(url.startsWith("https://") && url.length<8192) {"Invalid CapCut audio URL"}
   CapCutTaskResult.Audio(url)
  }
  "failed","canceled","cancelled"->error("CapCut synthesis failed")
  "processing","pending","queueing","queued","running",null->CapCutTaskResult.Pending
  else->error("Unsupported CapCut task status")
 }
}
