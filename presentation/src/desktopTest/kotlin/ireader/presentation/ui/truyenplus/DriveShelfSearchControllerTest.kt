package ireader.presentation.ui.truyenplus

import ireader.domain.models.shelf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DriveShelfSearchControllerTest {
    @Test fun aSlowOldSearchCannotReplaceNewResultsOrLoadingState()=runTest {
        val started=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val old=ShelfEntry("root","old","Kết quả cũ.epub");val fresh=ShelfEntry("root","fresh","Kết quả mới.epub")
        val search=ShelfSearchController(this) {q,_,_->
            if(q=="an")withContext(NonCancellable){started.complete(Unit);release.await();ShelfSearchPage(listOf(old),false)}
            else ShelfSearchPage(listOf(fresh),false)
        }
        search.submit("an",null,0);advanceTimeBy(250);runCurrent();started.await()
        search.invalidate(true);assertTrue(search.state.value.page.entries.isEmpty())
        search.submit("van",null,0);advanceTimeBy(250);runCurrent()
        assertEquals("fresh",search.state.value.page.entries.single().fileId)
        release.complete(Unit);advanceUntilIdle()
        assertEquals("fresh",search.state.value.page.entries.single().fileId)
        assertFalse(search.state.value.loading)
        search.invalidate(false)
    }
    @Test fun typingIsDebouncedAndEmptyQueryNeverSearches()=runTest {
        val calls=mutableListOf<String>()
        val search=ShelfSearchController(this){q,_,_->calls.add(q);ShelfSearchPage(emptyList(),false)}
        search.submit("a",null,0);advanceTimeBy(100);search.invalidate(true);search.submit("an",null,0)
        advanceTimeBy(249);runCurrent();assertTrue(calls.isEmpty())
        advanceTimeBy(1);runCurrent();assertEquals(listOf("an"),calls)
        search.invalidate(false);search.submit("",null,0);advanceUntilIdle()
        assertEquals(listOf("an"),calls);assertFalse(search.state.value.loading)
    }
}
