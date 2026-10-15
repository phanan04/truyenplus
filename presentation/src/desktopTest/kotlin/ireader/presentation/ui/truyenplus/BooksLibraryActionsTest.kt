package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksLibraryActionsTest {
    @Test fun listLayoutRetainsBookDetailsWithoutAccidentallyStartingReading() {
        var read=0L;var details=0L
        scene(2f) {ReaderLibraryScreen(listOf(ReaderBookUi(1,"Một tên truyện")),emptyList(),0,"",false,false,emptySet(),{},{},{read=it},{},{},{},{},{},preferList=true,onDetails={details=it})}.use {s->
            render(s);node(s,"library-details-1").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,details);assertEquals(0,read)
        }
    }
    @Test fun doneClosesSelectionStartedByLongPressInOneAction() {
        var cleared=0
        scene {ReaderLibraryScreen(listOf(ReaderBookUi(1,"Truyện")),emptyList(),0,"",false,false,setOf(1L),{},{},{},{},{},{},{},{},onClearSelection={cleared++})}.use {s->
            render(s);node(s,"library-edit").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(1,cleared)
        }
    }
    private fun scene(font:Float=1f,content:@Composable ()->Unit)=ImageComposeScene(393,1000,Density(1f,font)) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}}
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun node(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
