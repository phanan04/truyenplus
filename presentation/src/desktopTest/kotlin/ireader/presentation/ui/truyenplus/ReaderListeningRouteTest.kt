package ireader.presentation.ui.truyenplus
import kotlin.test.*
import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.services.tts_service.v2.*
class ReaderListeningRouteTest {
    @Test fun theExistingReaderDestinationKeepsItsIdentityForSavedNavigationState() {
        assertEquals("reader/{bookId}/{chapterId}",ireader.presentation.core.NavigationRoutes.READER)
    }
    @Test fun navigationPreservesListeningModeAndParagraphInsteadOfOnlyTheChapter() {
        assertEquals("reader/7/273?listen=true&paragraph=12",readerListeningDestination(7,273,12).navigationRoute())
        assertEquals("reader/7/273",ireader.presentation.core.ui.ReaderScreenSpec(7,273).navigationRoute())
        assertTrue(ReaderAppScope.allows(readerListeningDestination(7,273,12).navigationRoute()))
        assertFalse(ReaderAppScope.allows("reader/7/273?listen=true&paragraph=invalid"))
    }
    @Test fun continueUsesTheLiveChapterAndPositionWhileOtherBooksAndPausedReadingStayIndependent() {
        val playing=TTSState(book=Book(id=7,title="Truyện",key="7",sourceId=1),
            chapter=Chapter(id=273,bookId=7,key="273",name="Chương 273"),currentParagraphIndex=12,playbackState=PlaybackState.PLAYING)
        for(state in listOf(playing,playing.copy(playbackState=PlaybackState.LOADING))) {
            val route=readerResumeDestination(7,1,state)
            assertEquals(273L,route.chapterId);assertEquals(12,route.listeningParagraph);assertTrue(route.listenOnOpen)
        }
        assertFalse(readerResumeDestination(8,1,playing).listenOnOpen)
        assertFalse(readerResumeDestination(7,1,playing.copy(playbackState=PlaybackState.PAUSED)).listenOnOpen)
    }
    @Test fun listeningOpensTheFullChapterReaderAndRetainsTheSpeechPosition() {
        val route=readerListeningDestination(7,273,12)
        assertEquals(7L,route.bookId);assertEquals(273L,route.chapterId)
        assertTrue(route.listenOnOpen);assertEquals(12,route.listeningParagraph)
    }
}
