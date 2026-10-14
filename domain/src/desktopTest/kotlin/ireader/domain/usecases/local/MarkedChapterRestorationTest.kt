package ireader.domain.usecases.local

import ireader.core.source.model.Text
import ireader.domain.models.entities.Chapter
import ireader.domain.usecases.local.book_usecases.*
import ireader.domain.usecases.local.chapter_usecases.FindChaptersByBookId
import ireader.domain.usecases.local.insert_usecases.InsertChapters
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MarkedChapterRestorationTest {
    @Test fun undoOnlyRestoresReadFlagsAndKeepsEditsBookmarksAndScrollMadeAfterwards()=runTest {
        var current=listOf(Chapter(1,7,"1","Một",read=false,content=listOf(Text("Gốc"))),
            Chapter(2,7,"2","Hai",read=true,content=listOf(Text("Gốc hai"))))
        val query=mockk<FindChaptersByBookId>()
        val writer=mockk<InsertChapters>()
        val get=mockk<LocalGetChapterUseCase>()
        val insert=mockk<LocalInsertUseCases>()
        every {get.findChaptersByBookId} returns query
        every {insert.insertChapters} returns writer
        coEvery {query.invoke(7)} answers {current}
        coEvery {writer.invoke(any())} answers {
            val values=firstArg<List<Chapter>>().associateBy {it.id}
            current=current.map {values[it.id]?:it};values.keys.toList()
        }
        coEvery {writer.updateReadState(any())} answers {
            val flags=firstArg<Map<Long,Boolean>>()
            current=current.map {flags[it.id]?.let {read->it.copy(read=read)}?:it};Unit
        }
        val useCase=MarkBookAsReadOrNotUseCase(get,insert)
        val result=useCase.markAsReadWithUndo(listOf(7)) as MarkResult.Success
        current=current.map {it.copy(bookmark=true,lastPageRead=42,content=listOf(Text("Đã biên tập")))}+
            Chapter(3,7,"3","Ba",read=true,content=listOf(Text("Chương mới")))
        useCase.undoMark(result.previousStates)
        assertEquals(listOf(false,true,true),current.map {it.read})
        assertEquals("Đã biên tập",(current[0].content[0] as Text).text)
        assertTrue(current[0].bookmark)
        assertEquals(42L,current[0].lastPageRead)
        assertEquals("Chương mới",(current[2].content[0] as Text).text)
    }
}
