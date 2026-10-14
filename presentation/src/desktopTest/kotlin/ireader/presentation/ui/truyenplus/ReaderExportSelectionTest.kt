package ireader.presentation.ui.truyenplus
import ireader.presentation.ui.book.components.ExportChapterSelection
import kotlin.test.*
class ReaderExportSelectionTest {
    @Test fun changingReadFlagsOrAddingChaptersDoesNotResetACustomSelection() {
        val custom=ExportChapterSelection(true,setOf(1L,2L)).copy(all=false).toggle(2,false)
        assertEquals(setOf(1L),custom.reconcile(setOf(1L,2L)).ids)
        assertEquals(setOf(1L),custom.reconcile(setOf(1L,2L,3L)).ids)
        assertEquals(emptySet(),custom.reconcile(setOf(2L,3L)).ids)
        assertEquals(setOf(1L,2L,3L),ExportChapterSelection(true,setOf(1L,2L)).reconcile(setOf(1L,2L,3L)).ids)
    }
}
