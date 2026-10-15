package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import ireader.domain.models.entities.Chapter
import ireader.presentation.ui.reader.ReaderChapterTopBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksReaderChromeTest {
    @Test fun readingToolsAndChapterNavigationFloatInSeparateReachableClusters() {
        var previous=0
        ImageComposeScene(393,200) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderChapterFooter(1,3,false,false,{previous++},{},{},{},{},{})
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val reading=node(s,"reader-toolbar-reading").boundsInRoot
            val navigation=node(s,"reader-toolbar-navigation").boundsInRoot
            assertTrue(reading.right<navigation.left && reading.height>=48 && navigation.height>=48)
            node(s,"reader-previous").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(1,previous)
        }
    }
    @Test fun chapterIdentityHasItsOwnPaperBackingOverContrastingChapterText() {
        for(paper in listOf(Color.White,Color.Black)) {
            val underlay=if(paper==Color.White)Color.Black else Color.White
            ImageComposeScene(393,160) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
                Box(Modifier.fillMaxSize().background(underlay)) {
                    ReaderChapterTopBar(Chapter(id=1,bookId=1,key="1",name="Chương 1"),{},{},{},
                        backgroundColor=paper,textColor=underlay,bookTitle="Tên truyện",onEdit={})
                }
            }}}.use {s->
                s.render(0);s.render(1_000_000_000)
                val bounds=node(s,"reader-identity").boundsInRoot
                val pixels=s.render(2_000_000_000).toComposeImageBitmap().toPixelMap()
                val pixel=pixels[bounds.center.x.toInt(),bounds.top.toInt()+3]
                assertEquals(paper.red,pixel.red,.05f,"Chapter identity must cover the underlying text in either paper color")
            }
        }
    }
    private fun node(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
