package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksProductStructureTest {
    @Test fun homeHasAccountAndContinueInsteadOfUtilityDashboard() {
        var settings=0;var opened=0L
        scene {ReaderHomeScreen(listOf(ReaderBookUi(1,"Truyện đang đọc",read=2,total=10)),false,{},{},{},{},onContinue={opened=it},onShelf={},onSettings={settings++})}.use {s->
            render(s)
            click(s,"books-account");assertEquals(1,settings)
            click(s,"home-continue");assertEquals(1,opened)
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="home-shelf"})
            assertNotNull(tag(s,"books-page-title"))
        }
    }
    @Test fun libraryStartsWithCoversWhileSearchIsAnExplicitMode() {
        val book=ReaderBookUi(1,"Tên truyện",author="Tác giả")
        scene {ReaderLibraryScreen(listOf(book),listOf("Tất cả"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{},onSettings={})}.use {s->
            render(s)
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsActions.SetText)!=null},"Library must not duplicate Search")
            assertNotNull(tag(s,"library-book-1"));assertNotNull(tag(s,"library-organize"))
            click(s,"library-organize");s.render(2_000_000_000);s.render(3_000_000_000)
            assertNotNull(tag(s,"books-account"))
        }
        scene {ReaderLibraryScreen(emptyList(),emptyList(),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{},searchMode=true)}.use {s->
            render(s)
            assertNotNull(tag(s,"books-search-field"))
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="library-organize"})
        }
    }
    @Test fun sharedHeaderKeepsBackAndAccountReachableWithLargeText() {
        scene(320,600,2f) {ReaderBooksHeader("Thư viện",onSettings={},onBack={})}.use {s->
            render(s)
            for(t in listOf("books-back","books-account")) {
                val b=tag(s,t).boundsInRoot
                assertTrue(b.width>=48 && b.height>=48 && b.left>=0 && b.right<=320,"$t $b")
            }
        }
    }
    private fun scene(w:Int=393,h:Int=1000,font:Float=1f,content:@Composable ()->Unit)=ImageComposeScene(w,h,Density(1f,font)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}
    }
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
    private fun tag(s:ImageComposeScene,t:String)=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    private fun click(s:ImageComposeScene,t:String){tag(s,t).config[SemanticsActions.OnClick].action!!.invoke()}
}
