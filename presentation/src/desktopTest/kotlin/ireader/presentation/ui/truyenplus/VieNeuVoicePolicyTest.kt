package ireader.presentation.ui.truyenplus
import ireader.domain.services.tts_service.v2.offline.*
import kotlin.test.*
class VieNeuVoicePolicyTest {
    @Test fun onlyVerifiedNanoAssetsCanReplaceTheRetiredVoiceFamily() {
        assertNull(OfflineVoiceCatalog.find("supertonic3-vi-int8"))
        val nano=assertNotNull(OfflineVoiceCatalog.find("vieneu-v3-nano-vi"))
        assertEquals(11,nano.speakers)
        assertEquals("Minh Quân",nano.voiceNames[nano.defaultSpeaker])
        assertEquals(11,nano.voiceNames.size)
        assertTrue(nano.artifacts.any {it.name=="sea_g2p.bin"})
        assertTrue(nano.artifacts.any {it.name=="codec_decoder.onnx"})
        assertTrue(nano.artifacts.all {it.sha256.matches(Regex("[a-f0-9]{64}")) && it.bytes>0})
        assertEquals(nano.archiveBytes,nano.artifacts.sumOf {it.bytes})
        assertTrue(nano.artifacts.none {it.name.contains("speaker_encoder") || it.name.contains("reference_encoder")})
    }
}
