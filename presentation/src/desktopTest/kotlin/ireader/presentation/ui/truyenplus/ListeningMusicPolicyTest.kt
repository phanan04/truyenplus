package ireader.presentation.ui.truyenplus
import ireader.domain.services.tts_service.v2.PlaybackState
import kotlin.test.*
class ListeningMusicPolicyTest {
    @Test fun musicWaitsForSpeechThenSurvivesChapterLoadingAndStopsWithSpeech() {
        assertFalse(keepListeningMusicPlaying(false, PlaybackState.LOADING))
        assertTrue(keepListeningMusicPlaying(false, PlaybackState.PLAYING))
        assertTrue(keepListeningMusicPlaying(true, PlaybackState.LOADING))
        for (state in listOf(PlaybackState.PAUSED, PlaybackState.STOPPED, PlaybackState.ERROR, PlaybackState.IDLE)) {
            assertFalse(keepListeningMusicPlaying(true, state), "Music must stop with $state")
        }
    }
}
