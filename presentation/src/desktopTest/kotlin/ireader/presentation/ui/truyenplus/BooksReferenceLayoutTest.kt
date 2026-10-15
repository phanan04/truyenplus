package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.ReaderChapterTopBar
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksReferenceLayoutTest {
    @Test fun libraryReachesItsFirstCoverWithoutAStackOfToolbars() {
        scene {ReaderLibraryScreen(listOf(ReaderBookUi(1,"Truyện")),listOf("Tất cả"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{})}.use {s->
            render(s);assertTrue(tag(s,"library-book-1").boundsInRoot.top<120,"Library chrome hides its books")
        }
    }
    @Test fun recentQueriesAreVerticalRowsWithRealActions() {
        var value=""
        scene {ReaderRecentSearches(listOf("Vạn Cổ","Cầu Ma","Khủng Bố"),{value=it},{})}.use {s->
            render(s)
            val rows=listOf("Vạn Cổ","Cầu Ma","Khủng Bố").map {q->nodes(s).first {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==q}==true}}
            assertTrue(rows[1].boundsInRoot.top>rows[0].boundsInRoot.bottom)
            nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)=="recent-query-1"}.config[SemanticsActions.OnClick].action!!.invoke();assertEquals("Cầu Ma",value)
        }
    }
    @Test fun bookHeroRetainsReadListenAndExportActions() {
        var read=0;var listen=0
        scene {ReaderBookScreen(ReaderBookUi(1,"Truyện thật"),"Tác giả",listOf(Chapter(id=1,bookId=1,key="c",name="Chương 1")),{},{read++},{listen++},{},{},artworkColor=readerBookHeroColor(Color(0xFF895039)))}.use {s->
            render(s)
            val hero=tag(s,"book-artwork-hero").boundsInRoot
            assertEquals(393f,hero.width,1f)
            tag(s,"book-read").config[SemanticsActions.OnClick].action!!.invoke()
            tag(s,"book-listen").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,read);assertEquals(1,listen)
        }
    }
    @Test fun chapterIdentityIsQuietAndAppearanceStillAccessible() {
        var settings=0
        scene {ReaderChapterTopBar(Chapter(id=1,bookId=1,key="c",name="Chương 1"),{},{},{settings++},bookTitle="Tên truyện")}.use {s->
            render(s);assertNotNull(tag(s,"reader-identity"))
            tag(s,"reader-settings").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(1,settings)
        }
    }
    @Test fun emptyBookDoesNotOfferDeadReadingOrContentsActions() {
        scene {ReaderBookScreen(ReaderBookUi(1,"Chưa có chương"),"",emptyList(),{},{},{},{},{})}.use {s->
            render(s)
            for(t in listOf("book-read","book-listen","book-contents"))assertNotNull(tag(s,t).config.getOrNull(SemanticsProperties.Disabled),"$t should be disabled")
        }
    }
    private fun scene(content:@Composable ()->Unit)=ImageComposeScene(393,1000) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}}
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}}
    private fun tag(s:ImageComposeScene,t:String)=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
}
