package ireader.presentation.ui.truyenplus
import kotlin.test.*
class ReaderAppScopeTest {
    @Test fun onlyRequestedFeaturesAreExposed() {
        assertEquals(setOf(ReaderFeature.Library, ReaderFeature.Reading, ReaderFeature.Progress, ReaderFeature.TextToSpeech, ReaderFeature.EbookFiles, ReaderFeature.Discovery), ReaderAppScope.features)
        assertEquals(listOf("Home", "Library", "Settings", "Discover"), ReaderDestination.entries.map { it.name })
        assertEquals(listOf("Home", "Discover", "Library", "Settings"), ReaderAppScope.destinations.map { it.name })
    }
    @Test fun onlineAndUnrelatedRoutesAreNotPartOfTheReader() {
        listOf("globalSearch", "explore/1", "repository", "updates", "pluginManagement", "communityHub", "auth", "cloudBackup", "webView").forEach { assertFalse(ReaderAppScope.allows(it), it) }
        listOf("main", "bookDetail/12", "reader/12/-1", "ttsV2/12/3/-1/0", "category").forEach { assertTrue(ReaderAppScope.allows(it), it) }
    }
}
