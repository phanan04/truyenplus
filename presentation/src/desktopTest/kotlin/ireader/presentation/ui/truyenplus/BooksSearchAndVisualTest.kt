package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksSearchAndVisualTest {
    @Test fun searchDockKeepsOneFieldAndBackActionAtLargeText() {
        var query="";var closed=false
        scene(320,180,2f) {ReaderSearchDock(query,{query=it},{closed=true})}.use {s->
            render(s)
            val fields=nodes(s).filter {it.config.getOrNull(SemanticsActions.SetText)!=null}
            assertEquals(1,fields.size)
            fields.single().config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("Vạn Cổ"))
            assertEquals("Vạn Cổ",query)
            val back=tag(s,"search-return")
            assertTrue(back.boundsInRoot.width>=48 && back.boundsInRoot.height>=48)
            back.config[SemanticsActions.OnClick].action!!.invoke();assertTrue(closed)
        }
    }
    @Test fun embeddedSearchDoesNotDuplicateItsDockFieldOrLibraryToolbar() {
        scene {ReaderLibraryScreen(emptyList(),emptyList(),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{},searchMode=true,showSearchField=false)}.use {s->
            render(s)
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsActions.SetText)!=null})
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="library-organize"})
        }
    }
    @Test fun libraryTitleAndFirstCoverShareTheReferenceLeadingEdge() {
        scene {ReaderLibraryScreen(listOf(ReaderBookUi(1,"Một truyện")),emptyList(),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{})}.use {s->
            render(s)
            assertEquals(tag(s,"books-page-title").boundsInRoot.left,tag(s,"library-book-1").boundsInRoot.left,1f)
        }
    }
    @Test fun coverBasedHeroAlwaysKeepsItsWhiteTextReadable() {
        for(c in listOf(Color.Yellow,Color.White,Color.Black,Color.Red,Color(0xFF7B4A30))) {
            val bg=readerBookHeroColor(c)
            assertTrue(1.05f/(bg.luminance()+.05f)>=4.5f,"White text contrast fails for $c")
        }
    }
    private fun scene(w:Int=393,h:Int=850,font:Float=1f,content:@Composable ()->Unit)=ImageComposeScene(w,h,Density(1f,font)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}
    }
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}}
    private fun tag(s:ImageComposeScene,t:String)=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
}
