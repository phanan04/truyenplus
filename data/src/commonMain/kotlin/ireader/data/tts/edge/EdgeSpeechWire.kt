package ireader.data.tts.edge

import ireader.domain.services.tts_service.v2.EdgeVoice
import okio.ByteString.Companion.encodeUtf8

/** Independently implemented Read Aloud wire framing. edge-tts is used only as a development test client. */
object EdgeSpeechWire {
    const val clientToken="6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    const val browserVersion="143.0.3650.75"
    fun token(epochMillis:Long):String {
        val seconds=epochMillis/1000+11644473600L
        return "${seconds/300*300*10000000L}$clientToken".encodeUtf8().sha256().hex().uppercase()
    }
    fun escape(text:String)=buildString {
        for(c in text) when(c) {
            '&'->append("&amp;");'<'->append("&lt;");'>'->append("&gt;")
            '"'->append("&quot;");'\''->append("&apos;")
            else->if(c.code>=32 || c=='\n' || c=='\r' || c=='\t') append(c)
        }
    }
    fun textChunks(text:String):List<String> {
        val result=mutableListOf<String>();val current=StringBuilder();var bytes=0
        fun flush() { if(current.isNotEmpty()) { result+=current.toString();current.clear();bytes=0 } }
        for(match in Regex("\\S+\\s*|\\s+").findAll(text)) {
            val word=match.value;val size=escape(word).encodeToByteArray().size
            if(bytes+size>3800) flush()
            if(size<=3800) { current.append(word);bytes+=size;continue }
            var i=0
            while(i<word.length) {
                val count=if(word[i].isHighSurrogate() && i+1<word.length && word[i+1].isLowSurrogate()) 2 else 1
                val point=word.substring(i,i+count);val n=escape(point).encodeToByteArray().size
                if(bytes+n>3800) flush()
                current.append(point);bytes+=n;i+=count
            }
        }
        flush();return result
    }
    fun ssml(text:String,voice:EdgeVoice):String {
        val name="Microsoft Server Speech Text to Speech Voice (vi-VN, ${voice.id.removePrefix("vi-VN-")})"
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'><voice name='$name'><prosody pitch='+0Hz' rate='+0%' volume='+0%'>${escape(text)}</prosody></voice></speak>"
    }
    fun audioPayload(packet:ByteArray):ByteArray? {
        require(packet.size>=2) { "Invalid Edge frame" }
        val headerSize=((packet[0].toInt() and 255) shl 8) or (packet[1].toInt() and 255)
        require(headerSize<=packet.size-2) { "Invalid Edge header length" }
        val headers=packet.copyOfRange(2,2+headerSize).decodeToString().lineSequence().mapNotNull {
            val separator=it.indexOf(':');if(separator<0) null else it.substring(0,separator) to it.substring(separator+1).trim()
        }.toMap()
        require(headers["Path"]=="audio") { "Unexpected Edge binary frame" }
        val data=packet.copyOfRange(2+headerSize,packet.size)
        if(data.isEmpty() && headers["Content-Type"]==null) return null
        require(headers["Content-Type"]=="audio/mpeg" && data.isNotEmpty()) { "Invalid Edge audio" }
        return data
    }
}
