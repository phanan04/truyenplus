package ireader.domain.services.tts_service.v2.offline

import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

enum class OfflineVoiceFamily { Piper, VieNeuNano }
data class OfflineVoiceArtifact(val name:String,val url:String,val bytes:Long,val sha256:String)
data class OfflineVoiceModel(val id:String,val title:String,val family:OfflineVoiceFamily,
    val folder:String,val archiveBytes:Long,val sha256:String,val speakers:Int,val files:List<String>,val license:String,val artifacts:List<OfflineVoiceArtifact> = emptyList(),val voiceNames:List<String> = emptyList(),val defaultSpeaker:Int=0) {
    val url get()="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$folder.tar.bz2"
    fun config(root:String,speaker:Int=defaultSpeaker):OfflineVoiceConfig {
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
        OfflineVoiceModel("vieneu-v3-nano-vi","VieNeu v3 Nano",OfflineVoiceFamily.VieNeuNano,
            "vieneu-v3-nano-aba295eb",346903105L,
            "c34102cd13e2081199b8b8977f778f98995a633bfb94f33d2325a6019e323c19",11,
            listOf("text_encoder.onnx","duration_predictor.onnx","vector_estimator.onnx","codec_decoder.onnx","config.json","constants.npz","voices.json","sea_g2p.bin"),"Apache-2.0",
            artifacts=listOf(
                OfflineVoiceArtifact("text_encoder.onnx","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/text_encoder.onnx",26519943L,"204f02cccae1f16ccb2d3840f05721a37fe250b82cbd456337a0ccb49615e4bf"),
                OfflineVoiceArtifact("duration_predictor.onnx","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/duration_predictor.onnx",727809L,"20fd7fa60006d0a48ee82e0451b3f920d2052588c083c756cce669f3947c0a68"),
                OfflineVoiceArtifact("vector_estimator.onnx","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/vector_estimator.onnx",155132418L,"c6c1d4398ca35d3ad1bd3f0459413d1b975d09e7f2f3a2b4493a7b92bbf6ce93"),
                OfflineVoiceArtifact("codec_decoder.onnx","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/codec_decoder.onnx",99319941L,"b0ab15e7828a39d53679e25b1ba4ba415a61311307202a6323130b9e1cc3029d"),
                OfflineVoiceArtifact("config.json","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/config.json",2927L,"3762f1716fd8d7a1451ddf2e051c03b0a341e8adda4f1dbd0db0ae9777514e56"),
                OfflineVoiceArtifact("constants.npz","https://huggingface.co/pnnbao-ump/VieNeu-TTS-v3-Nano/resolve/aba295eb96a6fa6003ebe417cc1f2802a7adc1dc/constants.npz",53448L,"7c011938effe41687a9af85107a31a040dfcf0fb3d0f048ec10f4fb65c1a8829"),
                OfflineVoiceArtifact("voices.json","https://raw.githubusercontent.com/pnnbao97/VieNeu-TTS/bd0f88eb8de7d560753a6172ba9c868f309a101c/src/vieneu/assets/voices_v3_nano.json",2316799L,"2ca4cbf475409e61fda662fbd3fb3a3ac3e980bbc3925fedb4d1199c4ccac3e7"),
                OfflineVoiceArtifact("sea_g2p.bin","https://raw.githubusercontent.com/pnnbao97/sea-g2p/e825173f235d08ea19315b2b279fb11153b44cea/python/sea_g2p/sea_g2p.bin",62829820L,"4346e690d0711ebc5231e7a42c5c88aaf6e40377e894b4617c018fd81c6f4096")
            ),voiceNames=listOf("Adam","Ái Hân","Mỹ Duyên","Đức Trí","Hữu Quân","Xuân Tiên","Mai Anh","Trúc Ly","Anh Khôi","Minh Quân","Mạnh Dũng"),defaultSpeaker=9)
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
