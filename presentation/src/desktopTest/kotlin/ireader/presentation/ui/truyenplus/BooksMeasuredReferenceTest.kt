package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

/** Coordinates measured from the supplied 1179px (393pt @3x) screenshots 7 and 32. */
@OptIn(ExperimentalComposeUiApi::class)
class BooksMeasuredReferenceTest {
    @Test fun bottomNavigationAndSearchKeepClearanceAboveTheSystemInset() {
        for(search in listOf(false,true)) {
            scene {Box(Modifier.fillMaxSize()) {Box(Modifier.align(Alignment.BottomCenter)) {
                if(search)ReaderSearchDock("",{},{}) else ReaderBottomNav(ReaderDestination.Home,{},{})
            }}}.use {s->
                render(s)
                val action=tag(s,if(search)"search-return" else "nav-search").boundsInRoot
                assertTrue(852f-action.bottom>=12f,"Dock is too close to the safe-area edge: ${852f-action.bottom}")
            }
        }
    }
    @Test fun continueHasTheReferenceCompactWidthAndOpeningSectionHeight() {
        scene {ReaderHomeScreen(listOf(ReaderBookUi(1,"Tên truyện",read=1,total=10,author="Tác giả")),false,{},{},{},{})}.use {s->
            render(s)
            val card=tag(s,"home-continue").boundsInRoot
            assertEquals(244f,card.width,1f)
            assertEquals(80f,card.height,1f)
            assertEquals(145f,card.top,1f)
            assertEquals(257f,tag(s,"books-home-opening").boundsInRoot.height,1f)
        }
    }
    @Test fun libraryCoverUsesTheReferenceHeightAndHeaderOffset() {
        scene {ReaderLibraryScreen(listOf(ReaderBookUi(1,"Truyện")),listOf("Tất cả"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{})}.use {s->
            render(s)
            val cover=tag(s,"library-book-1").boundsInRoot
            assertEquals(112f,cover.top,1f)
            assertEquals(156.5f,cover.width,1f)
            assertEquals(292.5f,cover.height,1f) // 244.5pt cover + Android 48dp action row.
        }
    }
    @Test fun bottomSearchCircleAndCapsuleMatchReferenceScale() {
        scene {ReaderBottomNav(ReaderDestination.Home,{},{})}.use {s->
            render(s)
            val search=tag(s,"nav-search").boundsInRoot
            assertEquals(64f,search.width,1f);assertEquals(64f,search.height,1f)
            assertEquals(309f,search.left,1f)
            assertEquals(56f,tag(s,"nav-Home").boundsInRoot.height,1f)
        }
    }
    @Test fun editorialHeaderKeepsTheReferenceBoldInkWeight() {
        scene {ReaderBooksHeader("Home")}.use {s->
            render(s)
            val bounds=tag(s,"books-page-title").boundsInRoot
            val pixels=s.render(2_000_000_000).toComposeImageBitmap().toPixelMap()
            var ink=0;var left=pixels.width;var right=0;var top=pixels.height;var bottom=0
            for(y in bounds.top.toInt() until bounds.bottom.toInt())for(x in bounds.left.toInt() until bounds.right.toInt()) {
                val color=pixels[x,y]
                if(color.red<.4f && color.green<.4f && color.blue<.4f) {
                    ink++;left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)
                }
            }
            val coverage=ink.toFloat()/((right-left+1)*(bottom-top+1))
            // Screenshot 7: Home's dark ink occupies 40.18% of its tight glyph bounds.
            assertTrue(coverage>=.38f,"Editorial title is too light: $coverage")
        }
    }
    private fun scene(content:@Composable ()->Unit)=ImageComposeScene(393,852) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}}
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun tag(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
