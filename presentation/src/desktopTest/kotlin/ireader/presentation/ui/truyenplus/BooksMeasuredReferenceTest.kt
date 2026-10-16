package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.i18n.resources.Res
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import kotlin.test.*

/** Coordinates measured from the supplied 1179px (393pt @3x) screenshots 7 and 32. */
@OptIn(ExperimentalComposeUiApi::class)
class BooksMeasuredReferenceTest {
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @Test fun editorialFaceRemainsTheApprovedBoldAsset() {
        val bytes=runBlocking {Res.readBytes("font/books_editorial_bold.ttf")}
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
        assertEquals("c779701e5205f4200d862c492b39da1ef311ac0d71768ec741f8ef874add5ac9",hash)
    }
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
            assertEquals(62f,search.width,1f);assertEquals(62f,search.height,1f)
            assertEquals(311f,search.left,1f)
            assertEquals(54f,tag(s,"nav-Home").boundsInRoot.height,1f)
        }
    }
    @Test fun editorialHeaderKeepsTheReferenceBoldInkWeight() {
        // Measure at the reference screenshot's @3x scale. At @1x, CoreText and
        // FreeType stem hinting move too many edge pixels across the dark cutoff.
        scene(scale=3) {ReaderBooksHeader("Home")}.use {s->
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
            // The chosen Source Serif Bold measures ~37.8% at @3x; allow rasterizer
            // variation while keeping the approved heavy face (not Semibold).
            assertTrue(coverage>=.36f,"Editorial title is too light at @3x: $coverage")
        }
    }
    private fun scene(scale:Int=1,content:@Composable ()->Unit)=ImageComposeScene(393*scale,852*scale,density=Density(scale.toFloat())) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}}
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun tag(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
