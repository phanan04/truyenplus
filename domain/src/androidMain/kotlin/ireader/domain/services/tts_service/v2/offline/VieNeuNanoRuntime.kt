package ireader.domain.services.tts_service.v2.offline

import androidx.annotation.Keep
import kotlinx.serialization.json.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

/** Preset-only ONNX/G2P bridge. All calls run inside the shared offline worker lock. */
internal class VieNeuNanoRuntime(private val directory:String):AutoCloseable {
    private val config=Json.parseToJsonElement(File(directory,"config.json").readText()).jsonObject
    private val vocab=config.getValue("vocab").jsonObject.mapValues {it.value.jsonPrimitive.long}
    private val voices=Json.parseToJsonElement(File(directory,"voices.json").readText()).jsonObject.getValue("presets").jsonObject.values.toList()
    private val gate=Any()
    private var handle=0L
    init {
        require(config.getValue("sample_rate").jsonPrimitive.int==24000 && voices.size==11)
        val constants=readConstants(File(directory,"constants.npz"))
        val opened=VieNeuNative.open(directory.toByteArray(Charsets.UTF_8),constants.getValue("null_spk"),constants.getValue("null_style"))
        check(opened!=0L);handle=opened
    }
    fun generate(text:String,speaker:Int,speed:Float):FloatArray {
        check(handle!=0L);require(speaker in voices.indices && text.length<=512)
        val phones=String(VieNeuNative.phonemes(handle,text.toByteArray(Charsets.UTF_8)),Charsets.UTF_8)
        val ids=mutableListOf(config.getValue("bos_id").jsonPrimitive.long)
        // Unicode code points, rather than UTF16 units, preserve phoneme characters.
        val iter=phones.codePoints().iterator()
        while(iter.hasNext()) {
            val phone=String(Character.toChars(iter.nextInt()))
            ids+=vocab[phone] ?: error("Unsupported phoneme in pinned Vietnamese frontend")
        }
        ids+=config.getValue("eos_id").jsonPrimitive.long
        val voice=voices[speaker].jsonObject
        val spk=voice.getValue("speaker_emb").jsonArray.map {it.jsonPrimitive.float}.toFloatArray()
        val style=voice.getValue("style").jsonArray.flatMap {row->row.jsonArray.map {it.jsonPrimitive.float}}.toFloatArray()
        return VieNeuNative.generate(handle,ids.toLongArray(),spk,style,speed.coerceIn(.5f,2f),8)
    }
    fun abort()=synchronized(gate){if(handle!=0L)VieNeuNative.abort(handle)}
    override fun close() {val old=synchronized(gate){handle.also {handle=0L}};if(old!=0L)VieNeuNative.close(old)}
    companion object {
        private fun readConstants(file:File):Map<String,FloatArray> {
            val result=mutableMapOf<String,FloatArray>()
            ZipInputStream(file.inputStream().buffered()).use {zip->
                while(true) {
                    val entry=zip.nextEntry ?: break
                    if(entry.name !in listOf("null_spk.npy","null_style.npy"))continue
                    val output=java.io.ByteArrayOutputStream()
                    val chunk=ByteArray(8192)
                    while(true){val n=zip.read(chunk);if(n<0)break;require(output.size()+n<512*1024);output.write(chunk,0,n)}
                    val bytes=output.toByteArray();require(bytes.size>10)
                    require(bytes.copyOfRange(0,6).contentEquals(byteArrayOf(0x93.toByte(),78,85,77,80,89)))
                    val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    val version=bytes[6].toInt();val headerStart=if(version==1)10 else 12
                    val headerSize=if(version==1)buffer.getShort(8).toInt() and 65535 else buffer.getInt(8)
                    require(headerSize in 1..4096 && headerStart+headerSize<=bytes.size)
                    val header=String(bytes,headerStart,headerSize,Charsets.US_ASCII)
                    require(header.contains("'<f4'") && header.contains("'fortran_order': False"))
                    val count=if(entry.name.startsWith("null_spk"))192 else 12800
                    require(bytes.size-headerStart-headerSize==count*4)
                    buffer.position(headerStart+headerSize)
                    result[entry.name.removeSuffix(".npy")]=FloatArray(count){buffer.float.also {require(it.isFinite())}}
                }
            }
            require(result.size==2);return result
        }
    }
}

@Keep internal object VieNeuNative {
    init {System.loadLibrary("sea_g2p_rs");System.loadLibrary("vieneu-nano-jni")}
    @JvmStatic external fun open(path:ByteArray,nullSpeaker:FloatArray,nullStyle:FloatArray):Long
    @JvmStatic external fun phonemes(handle:Long,text:ByteArray):ByteArray
    @JvmStatic external fun generate(handle:Long,ids:LongArray,speaker:FloatArray,style:FloatArray,speed:Float,steps:Int):FloatArray
    @JvmStatic external fun abort(handle:Long)
    @JvmStatic external fun close(handle:Long)
}
