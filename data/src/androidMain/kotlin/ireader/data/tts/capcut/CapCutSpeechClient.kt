package ireader.data.tts.capcut

import android.content.Context
import ireader.domain.services.tts_service.v2.online.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher

/** Direct online adapter; only public protocol constants, no bundled session or captured device ID. */
class CapCutSpeechClient(context:Context,private val http:HttpClient):CapCutSpeechSource {
 private val settings=context.getSharedPreferences("capcut_online",Context.MODE_PRIVATE)
 private val identity=settings.getString("identity",null) ?: (7_000_000_000_000_000_000L+(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE)%999_999_999_999_999_999L).toString().also {settings.edit().putString("identity",it).apply()}
 private val version="8.7.0"
 private val app="359289"
 private fun md5(value:String)=MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString(""){"%02x".format(it.toInt() and 255)}
 private fun xml(value:String)=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
 private fun obj(vararg values:Pair<String,JsonElement>)=JsonObject(values.toMap())
 private fun str(value:String)=JsonPrimitive(value)
 private suspend fun post(path:String,body:JsonObject):JsonObject {
  val stamp=(System.currentTimeMillis()/1000).toString();val encoded=body.toString()
  val response=http.post("https://editor-api-sg.capcutapi.com$path") {
   url {parameters.appendAll(parametersOf("app_name" to listOf("CapCut"),"device_type" to listOf("MacBookPro17,4"),"os_version" to listOf("15.7.4"),"channel" to listOf("capcutpc_google"),"version_name" to listOf(version),"device_brand" to listOf("MacBookPro17,4"),"device_id" to listOf(identity),"iid" to listOf(identity),"version_code" to listOf(version),"device_platform" to listOf("mac"),"aid" to listOf(app),"region" to listOf("VN")))}
   contentType(ContentType.Application.Json)
   headers {
    append("appvr",version);append("ch","capcutpc_google");append("device-time",stamp);append("lan","vi-VN");append("loc","VN");append("pf","3");append("sign-ver","1");append("tdid",identity)
    append("x-ss-stub",md5(encoded));append("x-ss-dp",app);append("x-khronos",stamp);append("app-sdk-version",version);append("appid",app);append("store-country-code","vn");append("store-country-code-src","did")
    append("sign",md5("9e2c|${path.takeLast(7)}|3|$version|$stamp|$identity|11ac"))
   }
   setBody(encoded)
  }
  check(response.status.isSuccess()) {"CapCut HTTP ${response.status.value}"}
  val text=response.bodyAsText();check(text.length<1_000_000)
  return Json.parseToJsonElement(text).jsonObject.also {check(it["ret"]?.jsonPrimitive?.content=="0") {"CapCut request rejected"}}
 }
 override suspend fun synthesize(text:String,voice:CapCutVoice):ByteArray=withTimeout(60_000) {
  require(text.isNotBlank() && text.length<=1600)
  val ssml="<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"vi-VN\"><voice name=\"${voice.id}\" resource_id=\"${voice.resource}\" platform=\"sami\"><prosody rate=\"1.0\">${xml(text)}</prosody></voice></speak>"
  val extra="{\"benefit_info\":{}}"
  val input="appid:$app&did:$identity&creditDisable:false&ssml:${md5(ssml)}&extraInfo:$extra"
  val key=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)))
  val cipher=Cipher.getInstance("RSA/ECB/PKCS1Padding");cipher.init(Cipher.ENCRYPT_MODE,key)
  val sign=Base64.getEncoder().encodeToString(cipher.doFinal(input.toByteArray()))
  val payload=buildJsonObject {
   put("audio_format","mp3");put("babi_param","{\"feature_entrance\":\"editor\",\"feature_entrance_detail\":\"editor-feature-text_to_speech\",\"feature_key\":\"text_to_speech\",\"scenario\":\"video_editor\"}")
   put("credit_disable",false);put("extra_info",extra);put("need_merge_voice",false);put("need_subtitle_timestamp",false);put("scene","text_to_speech");put("ssml",ssml);put("sign",sign)
  }
  val created=post("/lv/v1/common_task/new",obj("bind_id" to str(UUID.randomUUID().toString()),"can_queue" to JsonPrimitive(true),"enter_from" to str("text_to_speech"),"tasks" to JsonArray(listOf(obj("context" to str(UUID.randomUUID().toString()),"payload" to str(payload.toString()),"req_key" to str("sami_text_to_speech"),"task_version" to str("v3"))))))
  val task=created["data"]!!.jsonObject["tasks"]!!.jsonArray.first().jsonObject
  val query=obj("tasks" to JsonArray(listOf(obj("bind_id" to str(""),"id" to task.getValue("id"),"token" to task.getValue("token"),"req_key" to str("sami_text_to_speech"),"task_version" to str("v3")))))
  var audio:String?=null
  while(audio==null) {
   ensureActive()
   when(val result=parseCapCutTask(post("/lv/v1/common_task/query",query).toString())) {
    is CapCutTaskResult.Audio->audio=result.url
    CapCutTaskResult.Pending->delay(400)
   }
  }
  http.prepareGet(audio).execute {response->
   check(response.status.isSuccess());require((response.contentLength() ?: 0)<=16*1024*1024)
   val channel=response.bodyAsChannel();val result=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true){ensureActive();val n=channel.readAvailable(buffer,0,buffer.size);if(n<0)break;if(n>0){check(result.size()+n<=16*1024*1024);result.write(buffer,0,n)}}
   result.toByteArray().also {check(it.size>128)}
  }
 }
 companion object {
  private const val PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmTd34Lw4b7IuldSXh/zYCMla+ITdGG5TeWz6ad+OySd4r+IrY45AoqrYUxhQ2dl+7z+i7r/5vEa8rr39BYfB8AGMQLmZA8HmgpWBsqrn/V6daUALkKnkLb70Fn32CJigIuGXAYqxUdGuI340aC+0v5Es3puJsHyzf01/AelE4Cdc6bZhQrASJLBh8R3BQToYClmDVSDUQk28o8sl/guAZ4n303Vj+6Siv1HayPCdV6kpVVnMBAG4+umUbwGmn132N3fgpzLarFF3XyWmS1zhD/J07iM/rP8GDO9IskHNHd2phrO0G6KzrcFAnTBHjVv+hCBEfzN/no3FNA9AuC36mwIDAQAB"
 }
}
