package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderChapterContentsTest {
    private val chapters=listOf(Chapter(1,7,"a","Mở đầu",number=1f),Chapter(273,7,"b","Luận Kiếm Thiếp",number=273f,bookmark=true,read=true))
    @Test fun chaptersCanBeFoundByActualNumberUnaccentedNameAndBookmark() {
        assertEquals(listOf(273L),filterReaderChapters(chapters,"273",0).map {it.id})
        assertEquals(listOf(273L),filterReaderChapters(chapters,"luan kiem",0).map {it.id})
        assertEquals(listOf(273L),filterReaderChapters(chapters,"",1).map {it.id})
        assertEquals(listOf(1L),filterReaderChapters(chapters,"",2).map {it.id})
    }
    @Test fun chapterRowsExposeCurrentAndBookmarkStatusAndKeepTouchTargets() {
        val scene=ImageComposeScene(320,800) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {ReaderChapterContents(chapters,273,{},onDismiss={})}
            }
        }
        try {
            scene.render();scene.render()
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            val row=all.first {it.config.getOrNull(SemanticsProperties.TestTag)=="toc-chapter-273"}
            assertEquals(true,row.config.getOrNull(SemanticsProperties.Selected))
            assertTrue(row.boundsInRoot.height>=48)
            assertTrue(all.any {it.config.getOrNull(SemanticsProperties.TestTag)=="toc-current"})
        }finally {scene.close()}
    }
}
