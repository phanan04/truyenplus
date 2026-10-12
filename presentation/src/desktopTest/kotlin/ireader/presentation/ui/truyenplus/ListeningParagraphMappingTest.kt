package ireader.presentation.ui.truyenplus
import ireader.core.source.model.*
import ireader.domain.services.tts_service.v2.assembleSpeechParagraphs
import kotlinx.coroutines.runBlocking
import kotlin.test.*
class ListeningParagraphMappingTest {
    @Test fun filteringAndSplittingKeepOriginalReaderPositions() = runBlocking {
        val result = assembleSpeechParagraphs(listOf(ImageUrl("cover"), Text("<p>Đoạn đầu</p><br/>Đoạn hai"), Text(" "), Text("Bỏ quảng cáo"), Text("Đoạn đầu"))) { text ->
            if (text=="Bỏ quảng cáo") emptyList() else listOf(text.replace("hai","thứ hai"))
        }
        assertEquals(listOf("Đoạn đầu","Đoạn thứ hai","Đoạn đầu"),result.texts)
        assertEquals(listOf(1,1,4),result.sources)
    }
}
