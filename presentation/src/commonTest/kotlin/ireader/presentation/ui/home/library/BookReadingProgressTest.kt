package ireader.presentation.ui.home.library

import ireader.presentation.ui.component.list.layouts.bookReadingProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class BookReadingProgressTest {
    @Test fun showsBookChapterProgressRatherThanChapterScrollPosition() {
        val progress = bookReadingProgress(520, 275)
        assertEquals(245, progress.readChapters)
        assertEquals(47, progress.percent)
    }

    @Test fun emptyAndUnknownChapterCountsDoNotShowCompletedBooks() {
        assertEquals(0f, bookReadingProgress(0, 0).fraction)
        assertEquals(0, bookReadingProgress(-1, 4).percent)
        assertEquals(0, bookReadingProgress(10, null).percent)
    }

    @Test fun staleCountsStayWithinProgressBounds() {
        assertEquals(0, bookReadingProgress(10, 15).readChapters)
        assertEquals(100, bookReadingProgress(10, -2).percent)
    }
}
