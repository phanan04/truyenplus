package ireader.presentation.ui.truyenplus

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderImportQueueTest {
    @Test fun displayNamesResolveInsideTheWorkerAndFailureDoesNotBlockImport()=runTest {
        var lookedUp=false;val parsed=mutableListOf<String>()
        val queue=ReaderImportQueue(this){_,uri->parsed+=uri}
        queue.enqueue(ReaderImportFormat.Epub,listOf("content://one","content://two")) {uri->
            lookedUp=true;delay(10)
            if(uri.endsWith("two"))error("provider cannot name this file")
            "Vạn Cổ Thần Đế.epub"
        }
        assertFalse(lookedUp,"File-provider queries must not block picker dismissal")
        advanceUntilIdle()
        assertEquals("Vạn Cổ Thần Đế.epub",queue.state.value.files.first().name)
        assertEquals(2,queue.state.value.added);assertEquals(2,parsed.size)
    }
    @Test fun mixedFilesContinueInBackgroundAndRetryOnlyFailedFiles()=runTest {
        val attempts=mutableListOf<String>();var fail=true
        val queue=ReaderImportQueue(this) {_,uri ->
            attempts+=uri
            if(uri=="bad.epub" && fail)error("Invalid archive")
            delay(10)
        }
        queue.enqueue(ReaderImportFormat.Epub,listOf("bad.epub","good.epub"))
        runCurrent()
        assertTrue(queue.state.value.busy)
        advanceUntilIdle()
        assertFalse(queue.state.value.busy)
        assertEquals(1,queue.state.value.added)
        assertEquals(1,queue.state.value.failed)
        fail=false;queue.retryFailed();advanceUntilIdle()
        assertEquals(listOf("bad.epub","good.epub","bad.epub"),attempts)
        assertEquals(2,queue.state.value.added)
        assertEquals(0,queue.state.value.failed)
    }
    @Test fun pickingMoreFilesDuringAnImportDoesNotCancelEarlierFiles()=runTest {
        val imported=mutableListOf<String>()
        val queue=ReaderImportQueue(this) {_,uri->delay(50);imported+=uri}
        queue.enqueue(ReaderImportFormat.Epub,listOf("one"));runCurrent()
        queue.enqueue(ReaderImportFormat.Pdf,listOf("two"));advanceUntilIdle()
        assertEquals(listOf("one","two"),imported)
        assertEquals(2,queue.state.value.added)
    }
}
