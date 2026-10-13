package ireader.presentation.ui.truyenplus

import ireader.domain.models.editing.*
import ireader.core.source.model.*
import kotlin.test.*

class ChapterEditingTest {
    @Test fun resultMustKeepEveryParagraphIdAndAllNonTextPages() {
        val pages = listOf(Text("Hoàng Yên Trần rút ra màu xanh ngọc chiến kiếm."), ImageUrl("cover"), Text("Nàng tiến lên."))
        val result = EditingParagraphs.apply(pages, listOf(EditParagraph(0, "Hoàng Yên Trần rút ra thanh chiến kiếm màu xanh ngọc."), EditParagraph(2, "Nàng tiến lên.")))
        assertEquals(pages[1], result[1])
        assertEquals(3, result.size)
        assertFails { EditingParagraphs.apply(pages, listOf(EditParagraph(0, "Câu mới."))) }
        assertFails { EditingParagraphs.apply(pages, listOf(EditParagraph(0, "Câu mới."), EditParagraph(0, "Trùng."))) }
        assertFails { EditingParagraphs.apply(pages, listOf(EditParagraph(2, "Nàng tiến lên."), EditParagraph(0, "Câu mới."))) }
        assertFails { EditingParagraphs.apply(pages, listOf(EditParagraph(0, ""), EditParagraph(2, "Nàng tiến lên."))) }
    }
    @Test fun batchesAreBoundedAndRetainOriginalIndices() {
        val pages = listOf(Text("a".repeat(3000)), ImageUrl("image"), Text("b".repeat(3000)), Text("c".repeat(3000)))
        val batches = EditingParagraphs.batches(pages)
        assertEquals(listOf(0,2,3), batches.flatten().map { it.id })
        assertTrue(batches.all { batch -> batch.sumOf { it.text.length } <= 6000 })
        assertFails { EditingParagraphs.batches(listOf(Text("a".repeat(6001)))) }
    }
}
