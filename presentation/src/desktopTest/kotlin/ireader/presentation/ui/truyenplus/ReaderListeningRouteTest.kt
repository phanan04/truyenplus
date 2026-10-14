package ireader.presentation.ui.truyenplus
import kotlin.test.*
class ReaderListeningRouteTest {
    @Test fun listeningOpensTheFullChapterReaderAndRetainsTheSpeechPosition() {
        val route=readerListeningDestination(7,273,12)
        assertEquals(7L,route.bookId);assertEquals(273L,route.chapterId)
        assertTrue(route.listenOnOpen);assertEquals(12,route.listeningParagraph)
    }
}
