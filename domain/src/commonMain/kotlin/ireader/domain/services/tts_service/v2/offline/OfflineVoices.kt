package ireader.domain.services.tts_service.v2.offline

import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

enum class OfflineVoiceFamily { Piper, Supertonic }
data class OfflineVoiceModel(val id:String,val title:String,val family:OfflineVoiceFamily,
    val folder:String,val archiveBytes:Long,val sha256:String,val speakers:Int,val files:List<String>,val license:String) {
    val url get()="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$folder.tar.bz2"
    fun config(root:String,speaker:Int=0):OfflineVoiceConfig {
        require(speaker in 0 until speakers)
        return OfflineVoiceConfig(this,"${root.trimEnd('/')}/$folder",speaker)
    }
}
data class OfflineVoiceConfig(val model:OfflineVoiceModel,val directory:String,val speaker:Int) {
    val identity get()="${model.id}:$speaker"
}
object OfflineVoiceCatalog {
    val models=listOf(
        OfflineVoiceModel("piper-vi-vais-int8","Piper",OfflineVoiceFamily.Piper,
            "vits-piper-vi_VN-vais1000-medium-int8",21_574_925,
            "60e8b82a27cfb9bfa527f5789f0879d02ec73a7397659973b937c823c326d5dd",1,
            listOf("vi_VN-vais1000-medium.onnx","tokens.txt","espeak-ng-data"),"Piper / eSpeak NG"),
        OfflineVoiceModel("supertonic3-vi-int8","Supertonic 3",OfflineVoiceFamily.Supertonic,
            "sherpa-onnx-supertonic-3-tts-int8-2026-05-11",128_774_318,
            "82fa96f91c4ef8abaae3a14a3f4153facf88bed821d1f7331cec2700f432c427",10,
            listOf("duration_predictor.int8.onnx","text_encoder.int8.onnx","vector_estimator.int8.onnx","vocoder.int8.onnx","tts.json","unicode_indexer.bin","voice.bin"),"OpenRAIL-M")
    )
    fun find(id:String)=models.firstOrNull{it.id==id}
}
data class OfflineVoiceSelection(val modelId:String,val speaker:Int)
enum class VoiceDownloadPhase { Downloading, Installing }
data class VoiceDownload(val phase:VoiceDownloadPhase,val bytes:Long,val totalBytes:Long)
data class OfflineVoiceState(val installed:Set<String> = emptySet(),val selection:OfflineVoiceSelection?=null,
    val downloads:Map<String,VoiceDownload> = emptyMap(),val errors:Set<String> = emptySet())
interface OfflineVoiceStore {
    val state:StateFlow<OfflineVoiceState>
    fun selectedConfig():OfflineVoiceConfig?
    fun config(modelId:String,speaker:Int=0):OfflineVoiceConfig?
    fun select(modelId:String?,speaker:Int=0):Boolean
    fun startDownload(modelId:String)
    fun cancelDownload(modelId:String)
    suspend fun remove(modelId:String)
}

fun safeVoiceArchivePath(root:String,entry:String):String {
    require(!entry.startsWith('/') && '\\' !in entry)
    val normalized=entry.trimEnd('/')
    require(normalized==root || normalized.startsWith("$root/"))
    val relative=normalized.removePrefix(root).trimStart('/')
    require(relative.isEmpty() || relative.split('/').none{it==".." || it=="." || it.isEmpty()})
    return relative
}
fun normalizedVoicePcm(samples:FloatArray):ShortArray {
    require(samples.all {it.isFinite()})
    val peak=samples.maxOfOrNull {abs(it)} ?: 0f
    val scale=if(peak>.98f).98f/peak else 1f
    return ShortArray(samples.size){(samples[it]*scale*32767).toInt().coerceIn(-32112,32112).toShort()}
}
